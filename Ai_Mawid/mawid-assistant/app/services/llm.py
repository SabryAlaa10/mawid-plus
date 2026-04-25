# app/services/llm.py
import os
import json as _json
import logging
import httpx

logger = logging.getLogger(__name__)

SYSTEM_PROMPT = """\
أنت مساعد ذكي لتطبيق «موعد+» — منصة حجز مواعيد طبية سعودية.
تتحدث بالعربية بشكل طبيعي ودافئ.

─── طريقة العمل (مهم جداً) ───
اتبع هذا التسلسل بدقة:

الخطوة 1 — الرسالة الأولى من المريض:
  • اطمئنه بجملة قصيرة
  • اسأل سؤال توضيحي واحد فقط يساعدك تحدد التخصص المناسب
  • لا تقترح تخصص في هذه المرحلة
  • اجعل suggested_questions = خيارات إجابة مناسبة للسؤال الذي طرحته
  • اجعل detected_specialty = null
  • اجعل ready_for_doctors = false

الخطوة 2 — بعد رد المريض على سؤالك:
  • لا تقدم أي نصائح علاجية أو وصفات منزلية أو خطوات علاج — هذا ليس دورك
  • حدد التخصص المناسب واذكره بوضوح: "أنصحك بمراجعة طبيب [التخصص]"
  • اختم بجملة تطمينية مثل "سلامتك تهمنا" أو "ما تقلق"
  • اجعل detected_specialty = اسم التخصص
  • اجعل ready_for_doctors = true
  • اجعل suggested_questions = []

─── قواعد مهمة ───
1. سؤال واحد فقط — لا تسأل أكثر من سؤال في نفس الرد
2. لا تشخّص أمراضاً — أنت لست طبيباً
3. لا تصف أدوية بأسمائها
3b. لا تقدم أي علاجات منزلية أو خطوات عملية للعلاج (مثل "اغسل عينيك" أو "ارتاح في الظلام") — دورك فقط توجيه المريض للطبيب المناسب
3c. لا تذكر أسماء أطباء أبداً في ردودك — أنت لا تعرف من هم الأطباء المتاحون، التطبيق هو الذي يعرض الأطباء الحقيقيين. إذا سألك المستخدم عن أسماء أطباء، قل فقط: "التطبيق سيعرض لك الأطباء المتاحين في تخصصك."
4. الحالات الخطيرة (ألم صدر حاد، صعوبة تنفس، نزيف، فقدان وعي) → انصح بالطوارئ فوراً واجعل severity = "emergency"
5. أجب بالعربي دائماً
6. كن مختصراً — 3-4 جمل كحد أقصى في كل رد
7. بعد ما تقترح التخصص والدكاتره، لا تسأل أسئلة إضافية — أنهِ المحادثة بشكل لطيف

─── تنسيق الرد (JSON فقط) ───
{
  "message": "ردك على المريض",
  "suggested_questions": ["خيار 1", "خيار 2", "خيار 3"],
  "detected_specialty": "التخصص بالعربي أو null",
  "severity": "low أو medium أو high أو emergency",
  "summary": "ملخص قصير للحالة أو null",
  "ready_for_doctors": false
}

─── أمثلة ───
مريض: "عندي صداع شديد"
ردك الأول: {"message": "سلامتك! الصداع الشديد له أسباب متعددة. هل الصداع في جهة واحدة من الرأس أم كامل الرأس؟", "suggested_questions": ["جهة واحدة", "كامل الرأس", "خلف الرأس"], "detected_specialty": null, "severity": "medium", "summary": "صداع شديد", "ready_for_doctors": false}

مريض: "جهة واحدة"
ردك الثاني: {"message": "الصداع في جهة واحدة قد يحتاج تقييم من طبيب متخصص. أنصحك بمراجعة طبيب أعصاب لتقييم حالتك وعمل الفحوصات اللازمة. سلامتك تهمنا!", "suggested_questions": [], "detected_specialty": "الأمراض العصبية", "severity": "medium", "summary": "صداع شديد في جهة واحدة", "ready_for_doctors": true}
"""


def _coerce_history(history: list | None) -> list[dict]:
    if not history:
        return []
    out: list[dict] = []
    for h in history[-20:]:
        if isinstance(h, dict):
            role = h.get("role", "").strip()
            content = (h.get("content") or "").strip()
        else:
            role = (getattr(h, "role", None) or "").strip()
            content = (getattr(h, "content", None) or "").strip()
        if not content or role not in ("user", "assistant"):
            continue
        out.append(
            {
                "role": role,
                "content": content[:3500],
            }
        )
    return out


def _parse_llm_json(raw: str) -> dict:
    """حاول تستخرج JSON من رد الـ LLM — مع fallback لو الرد نص عادي."""
    raw = (raw or "").strip()

    # أحياناً الـ LLM يلف الـ JSON بـ ```json ... ```
    if raw.startswith("```"):
        lines = raw.split("\n")
        # احذف أول سطر (```json) وآخر سطر (```)
        inner = "\n".join(
            l for l in lines
            if not l.strip().startswith("```")
        )
        raw = inner.strip()

    try:
        parsed = _json.loads(raw)
        if isinstance(parsed, dict) and "message" in parsed:
            return parsed
    except (_json.JSONDecodeError, ValueError):
        pass

    # fallback: الـ LLM رجّع نص عادي بدل JSON
    return {
        "message": raw,
        "suggested_questions": [],
        "detected_specialty": None,
        "severity": "low",
        "summary": None,
    }


async def call_llm(
    user_message: str,
    rag_context: str = "",
    session_id: str = None,
    history: list | None = None,
) -> dict:
    """استدعاء نموذج اللغة — يرجع dict مع message, suggested_questions, الخ"""

    system = SYSTEM_PROMPT
    if rag_context:
        system += f"\n\n─── معلومات طبية مرجعية (استخدمها إن كانت مفيدة) ───\n{rag_context}"

    messages: list[dict] = [{"role": "system", "content": system}]
    for turn in _coerce_history(history):
        messages.append({"role": turn["role"], "content": turn["content"]})
    messages.append({"role": "user", "content": (user_message or "").strip()[:4000]})

    # ── الخيار 1: OpenRouter ──
    api_key = os.getenv("OPENROUTER_API_KEY")
    if api_key:
        raw = await _call_openrouter(messages, api_key)
        return _parse_llm_json(raw)

    # ── الخيار 2: Groq ──
    groq_key = os.getenv("GROQ_API_KEY")
    if groq_key:
        raw = await _call_groq(messages, groq_key)
        return _parse_llm_json(raw)

    raise Exception("لم يتم تعيين مفتاح LLM API")


async def _call_openrouter(messages: list, api_key: str) -> str:
    """استدعاء OpenRouter API"""
    models = [
        "google/gemma-3-4b-it:free",
        "google/gemma-3n-e4b-it:free",
        "openai/gpt-oss-20b:free",
        "qwen/qwen3-coder:free",
        "meta-llama/llama-3.2-3b-instruct:free",
    ]

    async with httpx.AsyncClient(timeout=60) as client:
        for model in models:
            try:
                resp = await client.post(
                    "https://openrouter.ai/api/v1/chat/completions",
                    headers={
                        "Authorization": f"Bearer {api_key}",
                        "Content-Type": "application/json",
                    },
                    json={
                        "model": model,
                        "max_tokens": 800,
                        "messages": messages,
                    },
                )

                if resp.status_code == 429:
                    continue

                data = resp.json()
                content = data.get("choices", [{}])[0] \
                              .get("message", {}) \
                              .get("content")
                if content:
                    return content

            except Exception as e:
                logger.warning("OpenRouter model %s failed: %s", model, e)
                continue

    return '{"message": "تعذر الاتصال بنموذج اللغة حالياً. حاول مرة أخرى بعد قليل.", "suggested_questions": ["حاول مرة أخرى"], "detected_specialty": null, "severity": "low", "summary": null}'


async def _call_groq(messages: list, api_key: str) -> str:
    """استدعاء Groq API (أسرع خيار)"""
    try:
        async with httpx.AsyncClient(timeout=60) as client:
            resp = await client.post(
                "https://api.groq.com/openai/v1/chat/completions",
                headers={
                    "Authorization": f"Bearer {api_key}",
                    "Content-Type": "application/json",
                },
                json={
                    "model": "llama-3.1-8b-instant",
                    "max_tokens": 800,
                    "messages": messages,
                },
            )

            if resp.status_code != 200:
                return '{"message": "تعذر الاتصال بنموذج اللغة حالياً. حاول مرة أخرى بعد قليل.", "suggested_questions": ["حاول مرة أخرى"], "detected_specialty": null, "severity": "low", "summary": null}'

            data = resp.json()
            content = data.get("choices", [{}])[0] \
                          .get("message", {}) \
                          .get("content")
            return content or '{"message": "لم أتمكن من توليد رد. حاول مرة أخرى.", "suggested_questions": [], "detected_specialty": null, "severity": "low", "summary": null}'
    except Exception:
        return '{"message": "تعذر الاتصال بنموذج اللغة حالياً. حاول مرة أخرى بعد قليل.", "suggested_questions": ["حاول مرة أخرى"], "detected_specialty": null, "severity": "low", "summary": null}'
