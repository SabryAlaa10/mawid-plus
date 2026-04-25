# منطق حجز المواعيد موجود في app/database.py
# هذا الملف محجوز لنقل المنطق لاحقاً عند التوسع
from app.database import book_appointment, get_available_slots

__all__ = ["book_appointment", "get_available_slots"]
