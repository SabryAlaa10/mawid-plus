import os
import time
from supabase import create_client, Client
from dotenv import load_dotenv

load_dotenv()

_url = (os.getenv("SUPABASE_URL") or "").strip()
if not _url:
    raise RuntimeError("SUPABASE_URL مفقود في ملف .env")

if not os.environ.get("SUPABASE_SERVICE_ROLE_KEY"):
    raise RuntimeError(
        "SUPABASE_SERVICE_ROLE_KEY is not set. "
        "Never use the anon key for the assistant backend. "
        "Set this environment variable before starting the server."
    )

_key = os.environ["SUPABASE_SERVICE_ROLE_KEY"].strip()

# WARNING: This uses the service role key which bypasses RLS.
# Keep this service internal — never expose it to clients.
# Restrict network access to this service in production.
supabase: Client = create_client(_url, _key)

_specialties_cache: dict = {"fetched_at": 0.0, "specialties": []}


def get_all_specialties(ttl_seconds: int = 600) -> list[str]:
    global _specialties_cache
    now = time.monotonic()
    if now - _specialties_cache["fetched_at"] < ttl_seconds and _specialties_cache["specialties"]:
        return _specialties_cache["specialties"]
    try:
        response = supabase.table("doctors").select("specialty").execute()
        specialties = list({
            row["specialty"].strip()
            for row in (response.data or [])
            if row.get("specialty") and row["specialty"].strip()
        })
        specialties.sort()
        _specialties_cache = {"fetched_at": now, "specialties": specialties}
        return specialties
    except Exception as e:
        import logging

        logging.getLogger(__name__).warning(f"get_all_specialties failed: {e}")
        return _specialties_cache.get("specialties", [])


def get_doctors_by_specialty(specialty: str, limit: int = 3) -> dict:
    """Return doctors for a specialty, or an empty list with matched=False. No unfiltered fallback."""
    response = (
        supabase.table("doctors")
        .select("*")
        .ilike("specialty", f"%{specialty}%")
        .order("rating", desc=True)
        .limit(limit)
        .execute()
    )

    if not response.data:
        return {"doctors": [], "matched": False}

    return {"doctors": response.data, "matched": True}


def get_available_slots(doctor_id: str, date: str):
    doctor = (
        supabase.table("doctors")
        .select("start_time, end_time, slot_duration_minutes")
        .eq("id", doctor_id)
        .single()
        .execute()
    )
    
    if not doctor.data:
        return []
    
    # 2 - جلب المواعيد المحجوزه
    booked = (
        supabase.table("appointments")
        .select("time_slot")
        .eq("doctor_id", doctor_id)
        .eq("appointment_date", date)
        .neq("status", "cancelled")
        .execute()
    )
    
    booked_slots = {b["time_slot"] for b in (booked.data or [])}

    # 3. حساب كل الأوقات الممكنة
    start_h, start_m = map(int, doctor.data["start_time"].split(":")[:2])
    end_h, end_m = map(int, doctor.data["end_time"].split(":")[:2])
    duration = doctor.data.get("slot_duration_minutes", 30)

    slots = []
    h , m = start_h , start_m
    while h < end_h or (h == end_h and m < end_m):
        time_str = f"{h:02d}:{m:02d}"
        slots.append({
            "time": time_str,
            "available": time_str not in booked_slots
        })
        m += duration
        if m >= 60:
            h += m // 60
            m = m % 60

    return slots

# ______ حجز موعد ______

def book_appointment(patient_id : str , doctor_id : str , date : str , time_slot : str):
    existing = (
        supabase.table("appointments")
        .select("id")
        .eq("doctor_id", doctor_id)
        .eq("appointment_date", date)
        .eq("time_slot", time_slot)
        .neq("status", "cancelled")
        .limit(1)
        .execute()
    )

    
    if existing.data:
        return {"error": "هذا الموعد محجوز من قبل"}
    
    #  حساب رقم الطابور 
    day_appts = (
        supabase.table("appointments")
        .select("id", count="exact")
        .eq("doctor_id", doctor_id)
        .eq("appointment_date", date)
        .neq("status", "cancelled")
        .execute()
    )

    queue_number = (day_appts.count or 0) + 1

    # انشاء الموعد 
    result = supabase.table('appointments').insert({
        "patient_id": patient_id,
        "doctor_id": doctor_id,
        "appointment_date": date,
        "time_slot": time_slot,
        "queue_number": queue_number,
        "status": "scheduled"
    }).execute()

    return {
        "success": True,
        "appointment": result.data[0] if result.data else None,
        "queue_number": queue_number
    }