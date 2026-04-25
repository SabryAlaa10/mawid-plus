# يجب استخدام نفس نموذج التضمين عند لاحق في load_dataset/seed (vector 384)
import logging

from sentence_transformers import SentenceTransformer
from app.database import supabase

logger = logging.getLogger(__name__)

# متعدد اللغات: يواءم العربية بشكل أفضل من L6-v2 (نفس بُعد المتجه 384)
EMBEDDING_MODEL_NAME = "sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2"
_model = SentenceTransformer(EMBEDDING_MODEL_NAME)

# كلمات تدل على أعراض/سياق قلبي/وعائي — تقليل إرجاع مقاطع جلدية عند تضارب
_CIRCULATORY_HINTS = (
    "قلب", "صدر", "خفقان", "ضغط", "نبض", "دورة", "وعائية",
    "أزمة", "ذبحة", "cardio",
)
_DERMATOLOGY_CATEGORIES = ("جلد", "طفح", "أكزيما")

# رسائل قصيرة/تحيات — لا تحتاج بحث RAG
_TRIVIAL_MESSAGES = frozenset({
    "نعم", "لا", "أحياناً", "شكراً", "شكرا", "أهلاً", "أهلا", "اهلا",
    "هلا", "تمام", "ايه", "لأ", "ok", "yes", "no", "حسناً",
    "ممتاز", "طيب", "اوكي", "خلاص", "مع السلامة",
})


def _turn_content(t) -> str:
    if t is None:
        return ""
    if isinstance(t, dict):
        return (t.get("content") or "").strip()
    return (getattr(t, "content", None) or "").strip()


def _build_composite_rag_query(message: str, history: list | None) -> str:
    message = (message or "").strip()
    if not history:
        return message
    tail = history[-12:]
    parts: list[str] = []
    for t in tail:
        c = _turn_content(t)
        if c:
            parts.append(c)
    parts.append(message)
    joined = " ".join(parts)
    return joined if joined.strip() else message


def _should_downrank_derm(chunks: list, composite: str) -> bool:
    if not chunks:
        return False
    c = composite
    if any(h in c for h in _CIRCULATORY_HINTS) and "جلد" not in c and "طفح" not in c:
        return True
    return False


def _filter_chunks_for_context(chunks: list, composite: str) -> list:
    if not _should_downrank_derm(chunks, composite):
        return chunks
    kept = [ch for ch in chunks if not any(
        d in (ch.get("category") or "") + (ch.get("content") or "")
        for d in _DERMATOLOGY_CATEGORIES
    )]
    return kept if kept else chunks  # إن حذفنا كل شيء نعيد الأصل

def search_medical_knowledge(query: str, limit: int = 3):
    query_embedding = _model.encode(query).tolist()
    results = supabase.rpc('match_kb_chunks', {
        "query_embedding": query_embedding,
        "match_threshold": 0.35,
        "match_count": limit,
    }).execute()
    return results.data or []


def _is_trivial_message(message: str) -> bool:
    """هل الرسالة قصيرة/تحية ولا تحتاج بحث RAG؟"""
    stripped = (message or "").strip()
    if not stripped or len(stripped) < 4:
        return True
    if stripped in _TRIVIAL_MESSAGES:
        return True
    # رسائل أحرف وحيدة أو أرقام فقط
    if stripped.isdigit():
        return True
    return False


def build_rag_context(
    message: str,
    history: list | None = None,
) -> str:
    """
    RAG: استعلام مدمج من تاريخ قصير + آخر رسالة.
    يتخطى البحث للرسائل القصيرة/التحيات لمنع حقن سياق غير مفيد.
    """
    try:
        # تخطي RAG للرسائل البسيطة — خلي الـ LLM يتعامل بمرونة
        if _is_trivial_message(message):
            return ""

        composite = _build_composite_rag_query(message, history)
        chunks = search_medical_knowledge(composite, limit=5)
        chunks = _filter_chunks_for_context(chunks, composite)[:3]

        if not chunks:
            return ""  # لا سياق — بدل "لم يتم العثور على معلومات"

        context_parts = []
        for chunk in chunks:
            context_parts.append(
                f"[{chunk['category']}] {chunk['content']}"
            )
        return "\n\n".join(context_parts)
    except Exception as e:
        logger.warning(f"RAG failed, continuing without context: {e}")
        return ""


# Backwards-compat alias
Build_rag_context = build_rag_context
