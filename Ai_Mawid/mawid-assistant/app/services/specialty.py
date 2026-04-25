# app/services/specialty.py

# Must match public.doctors.specialty values seeded in the DB.
CANONICAL_DB_SPECIALTIES: tuple[str, ...] = (
    "أمراض القلب",
    "طبيب العظام",
    "طب الأسرة",
)

# (canonical, needles) — first match wins; more specific / cardio before short tokens like "عام"
_DB_SPECIALTY_NEEDLES: tuple[tuple[str, tuple[str, ...]], ...] = (
    (
        "أمراض القلب",
        (
            "أمراض القلب",
            "أمراض قلب",
            "طبيب قلب",
            "دكتور قلب",
            "قلب",
            "القلب",
            "صدر",
            "خفقان",
            "ضغط",
            "نبض",
        ),
    ),
    (
        "طبيب العظام",
        (
            "طبيب العظام",
            "دكتور عظام",
            "عظام",
            "العظام",
            "مفاصل",
            "ركبة",
            "مفصل",
            "عمود",
            "ظهر",
        ),
    ),
    (
        "طب الأسرة",
        (
            "عيون",
            "عين",
            "بصر",
            "نظر",
            "رؤية",
            "جفن",
            "قرنية",
        ),
    ),
    (
        "طب الأسرة",
        (
            "طب الأسرة",
            "طب اسرة",
            "الأسرة",
            "أسرة",
            "عام",
            "برد",
            "حمى",
            "انفلونزا",
            "إنفلونزا",
        ),
    ),
)

SPECIALTY_KEYWORDS = {
    "أمراض القلب":     ["قلب", "صدر", "خفقان", "ضغط الدم"],
    "طبيب العظام":     ["عظام", "مفاصل", "ركبة", "ظهر", "عمود فقري"],
    "طب الأسرة":       ["عام", "حرارة", "حمى", "برد", "إنفلونزا"],
    "طب اسنان":        ["أسنان", "اسنان", "ضرس", "لثة", "سن"],
    "الجهاز الهضمي":   ["هضم", "بطن", "معدة", "قولون", "إسهال"],
    "الأمراض الجلدية": ["جلد", "طفح", "حكة", "أكزيما"],
    "طب العيون":       ["عيون", "رؤية", "نظر", "احمرار العين"],
    "أنف وأذن وحنجرة": ["أنف", "أذن", "حنجرة", "سمع", "التهاب حلق"],
    "الأمراض العصبية":  ["أعصاب", "صداع", "تنميل", "دوخة"],
    "أمراض الصدر":     ["تنفس", "ربو", "سعال", "كحة"],
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


def normalize_specialty(raw: str) -> str | None:
    """
    Map a free-form specialty (LLM or keywords) to a canonical ``doctors.specialty`` value.
    Returns None if no row in the DB is expected to match.
    """
    t = (raw or "").strip()
    if not t:
        return None
    for canonical, needles in _DB_SPECIALTY_NEEDLES:
        if any(needle in t for needle in needles):
            return canonical
    return None