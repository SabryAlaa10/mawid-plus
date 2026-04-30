# app/services/specialty.py

import difflib

SPECIALTY_KEYWORDS = {
    "أمراض القلب": [
        "قلب", "قلبي", "صدر", "خفقان", "ضغط الدم", "شرايين", "نبض", "ضغط"
    ],
    "طبيب العظام": [
        "عظام", "مفاصل", "كسر", "ركبة", "ظهر", "عمود فقري", "عمود", "مفصل"
    ],
    "طب اسنان": [
        "أسنان", "اسنان", "سنان", "ضرس", "ضروس", "لثة", "تسوس", "فم"
    ],
    "طب الأسرة": [
        "طب الأسرة", "طب اسرة", "عام", "برد", "رشح", "حرارة", "حمى",
        "إنفلونزا", "انفلونزا", "عيون", "عين", "بصر", "نظر", "جلد",
        "جلدية", "باطني", "أعصاب", "عصبي", "نساء", "أطفال", "طفل",
        "أنف", "أذن", "حنجرة", "معدة", "هضم", "كلى", "بول"
    ],
}


def detect_specialty(text: str) -> str | None:
    """كشف التخصص الطبي من نص رد المساعد"""
    for specialty, keywords in SPECIALTY_KEYWORDS.items():
        if any(kw in text for kw in keywords):
            return specialty
    return None


def detect_specialty_combined(assistant_text: str, user_context: str) -> str | None:
    """يُفضّل إشارات المريض (السياق المتراكم) ثم رد المساعد — يقلّل القفز بين التخصصات"""
    u = (user_context or "").strip()
    a = (assistant_text or "").strip()
    s = detect_specialty(u)
    if s:
        return s
    return detect_specialty(a)


def match_specialty_to_db(raw: str, db_specialties: list[str]) -> str | None:
    t = (raw or "").strip()
    if not t or not db_specialties:
        return None

    # Step A — exact match (case-insensitive)
    for s in db_specialties:
        if t == s.strip():
            return s

    # Step B — substring match (longest DB specialty wins)
    candidates = [s for s in db_specialties if t in s or s in t]
    if candidates:
        return max(candidates, key=len)

    # Step C — fuzzy match
    matches = difflib.get_close_matches(t, db_specialties, n=1, cutoff=0.6)
    if matches:
        return matches[0]

    return None
