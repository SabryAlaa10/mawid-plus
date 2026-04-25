# app/routers/doctors.py
from fastapi import APIRouter, Depends
from app.models.schemas import DoctorsRequest, SlotsRequest
from app.database import get_doctors_by_specialty, get_available_slots
from app.auth import get_current_user

router = APIRouter()


@router.post("/get-doctors")
async def get_doctors(req: DoctorsRequest, user=Depends(get_current_user)):
    out = get_doctors_by_specialty(req.specialty)
    doctors = out["doctors"]
    return {
        "doctors": doctors,
        "message": f"إليك أفضل الأطباء في تخصص {req.specialty}"
    }


@router.post("/get-slots")
async def get_slots(req: SlotsRequest, user=Depends(get_current_user)):
    slots = get_available_slots(req.doctor_id, req.date)
    return {
        "slots": slots,
        "date": req.date,
        "doctor_id": req.doctor_id
    }