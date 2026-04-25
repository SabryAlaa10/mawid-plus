import logging

from fastapi import APIRouter, Depends, HTTPException
from app.models.schemas import ChatRequest, ChatResponse
from app.services.rag import build_rag_context
from app.services.llm import call_llm
from app.services.specialty import detect_specialty_combined, normalize_specialty
from app.database import get_doctors_by_specialty
from app.auth import get_current_user

router = APIRouter()
logger = logging.getLogger(__name__)

_NO_DOCTORS_IN_SPECIALTY = (
    "\n\nلا يوجد أطباء متاحون في هذا التخصص حالياً. "
    "يمكنك التواصل مع طب الأسرة كبديل."
)


def _composite_user_text(req: ChatRequest) -> str:
    """كل ما كتبه المريض (بدون الردود الطبية) — لقواعد RAG/التخصص"""
    parts: list[str] = []
    for t in (req.history or []):
        if t.role == "user" and (t.content or "").strip():
            parts.append(t.content.strip())
    if (req.message or "").strip():
        parts.append(req.message.strip())
    return " ".join(parts)


def _history_for_llm(req: ChatRequest) -> list[dict]:
    return [
        {"role": t.role, "content": t.content}
        for t in (req.history or [])
        if t.role in ("user", "assistant") and (t.content or "").strip()
    ]


@router.post("/chat", response_model=ChatResponse)
async def chat(req: ChatRequest, user=Depends(get_current_user)):
    if not req.message or not req.message.strip():
        raise HTTPException(status_code=400, detail="الرسالة فارغة")

    hist = _history_for_llm(req)
    composite = _composite_user_text(req)

    try:
        rag_context = build_rag_context(req.message, history=req.history)
        # call_llm الآن يرجع dict مع message, suggested_questions, detected_specialty, severity, summary
        llm_result = await call_llm(
            user_message=req.message,
            rag_context=rag_context,
            session_id=req.session_id,
            history=hist,
        )
    except Exception as e:
        logger.error(f"Assistant error: {e}")
        return ChatResponse(
            session_id=req.session_id,
            assistant_message="عذراً، المساعد غير متاح حالياً. يرجى المحاولة مرة أخرى.",
            quick_replies=[],
            ready_for_doctors=False,
            specialty_available=False,
        )

    assistant_message = llm_result.get("message", "")
    suggested_questions = llm_result.get("suggested_questions") or []
    llm_specialty = llm_result.get("detected_specialty")
    severity = llm_result.get("severity", "low")
    summary = llm_result.get("summary")
    ready_for_doctors = bool(llm_result.get("ready_for_doctors", False))

    # التخصص: أولوية للـ LLM، ثم fallback بالـ keyword detection (قيمة خام للعرض)
    specialty = llm_specialty
    if not specialty:
        specialty = detect_specialty_combined(assistant_message, composite)

    specialty_available = False
    recommended_doctor: list | None = None
    no_match = False

    if ready_for_doctors:
        if not (specialty and str(specialty).strip()):
            no_match = True
            recommended_doctor = []
        else:
            canonical = normalize_specialty(specialty)
            if canonical is None:
                no_match = True
                recommended_doctor = []
            else:
                out = get_doctors_by_specialty(canonical)
                recommended_doctor = out["doctors"] or []
                if out["matched"] and recommended_doctor:
                    specialty_available = True
                else:
                    no_match = True
        if no_match:
            assistant_message = f"{assistant_message}{_NO_DOCTORS_IN_SPECIALTY}"

    # الـ suggested_questions من الـ LLM — مش hardcoded
    quick_replies = []
    if isinstance(suggested_questions, list):
        quick_replies = [str(q) for q in suggested_questions if q][:5]

    return ChatResponse(
        session_id=req.session_id,
        assistant_message=assistant_message,
        quick_replies=quick_replies,
        detected_specialty=specialty,
        recommended_doctor=recommended_doctor,
        severity=severity,
        summary=summary,
        ready_for_doctors=ready_for_doctors,
        specialty_available=specialty_available,
    )
