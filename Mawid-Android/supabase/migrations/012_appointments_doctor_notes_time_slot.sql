-- ملاحظات الطبيب للمريض + وقت الحجز (للتطبيق والويب)
ALTER TABLE public.appointments
ADD COLUMN IF NOT EXISTS doctor_notes text,
ADD COLUMN IF NOT EXISTS time_slot text;
