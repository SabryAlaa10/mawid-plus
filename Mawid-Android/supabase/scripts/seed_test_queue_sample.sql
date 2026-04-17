-- اختياري: بيانات تجريبية للطابور والموعد (عدّل patient_id و doctor_id حسب مشروعك).
-- نفّذ من SQL Editor بعد تطبيق المايجريشن ووجود مريض في profiles.

-- INSERT INTO public.appointments
-- (patient_id, doctor_id, appointment_date, queue_number, status)
-- SELECT
--   (SELECT id FROM public.profiles WHERE role = 'patient' LIMIT 1),
--   'c170b81c-d743-4632-9c26-9500445241b1'::uuid,
--   CURRENT_DATE,
--   1,
--   'waiting'
-- WHERE NOT EXISTS (
--   SELECT 1 FROM public.appointments
--   WHERE appointment_date = CURRENT_DATE
--     AND doctor_id = 'c170b81c-d743-4632-9c26-9500445241b1'::uuid
-- );

-- UPDATE public.queue_settings
-- SET queue_date = CURRENT_DATE, current_number = 0, is_open = true
-- WHERE doctor_id = 'c170b81c-d743-4632-9c26-9500445241b1'::uuid;
