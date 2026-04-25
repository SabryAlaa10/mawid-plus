from pydantic import BaseModel
from typing import List, Optional


class ChatTurn(BaseModel):
    """تبادل واحد في التاريخ: ليس الرسالة الحالية — الأدوار: user / assistant فقط"""
    role: str
    content: str


class ChatRequest(BaseModel):
    message: str
    session_id: Optional[str] = None
    # الرسائل السابقة (بدون الرسالة الحالية في `message`) — اختياري للتوافق العكسي
    history: Optional[List[ChatTurn]] = None


class ChatResponse(BaseModel):
    session_id: Optional[str] = None
    assistant_message: str
    quick_replies: list[str] = []
    detected_specialty: Optional[str] = None
    recommended_doctor: Optional[list] = None
    severity: Optional[str] = None       # low / medium / high / emergency
    summary: Optional[str] = None        # ملخص الحالة من الـ LLM
    ready_for_doctors: bool = False       # هل المساعد جاهز لعرض الأطباء؟
    specialty_available: bool = False     # True only when at least one doctor was returned

class DoctorRequest(BaseModel):
    specialty: str

class SlotRequest(BaseModel):
    doctor_id: str
    date: str

class BookRequest(BaseModel):
    doctor_id: str
    date: str
    time_slot: str

class BookResponse(BaseModel):
    doctor_id: str
    date: str
    time_slot: str


# Backwards-compat alias (older name used in some modules)
chat_request = ChatRequest
DoctorsRequest = DoctorRequest
SlotsRequest = SlotRequest