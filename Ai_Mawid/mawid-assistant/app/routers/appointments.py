# app/routers/appointments.py
from fastapi import APIRouter, Depends, HTTPException
from app.models.schemas import BookRequest
from app.database import book_appointment, supabase
from app.auth import get_current_user

router = APIRouter()


@router.post("/book")
async def book(req: BookRequest, user=Depends(get_current_user)):
    result = book_appointment(
        patient_id=user["id"],
        doctor_id=req.doctor_id,
        date=req.date,
        time_slot=req.time_slot
    )

    if "error" in result:
        raise HTTPException(409, result["error"])

    # جلب اسم الطبيب للرد
    doctor_name = "الطبيب"
    try:
        doc = supabase.table("doctors") \
            .select("full_name, specialty") \
            .eq("id", req.doctor_id) \
            .single() \
            .execute()
        if doc.data:
            doctor_name = doc.data["full_name"]
    except Exception:
        pass

    return {
        "success": True,
        "appointment": result["appointment"],
        "queue_number": result["queue_number"],
        "message": (
            f"تم حجز موعدك بنجاح!\n"
            f"الطبيب: {doctor_name}\n"
            f"التاريخ: {req.date}\n"
            f"الوقت: {req.time_slot}\n"
            f"رقم الطابور: {result['queue_number']}"
        )
    }