-- فهارس لاستعلامات شائعة (طبيب + تاريخ، مريض، ترتيب رقم الطابور).
-- نفّذ في Supabase SQL Editor بعد المايجريشنز السابقة.

CREATE INDEX IF NOT EXISTS idx_appointments_doctor_id_appointment_date
  ON public.appointments (doctor_id, appointment_date);

CREATE INDEX IF NOT EXISTS idx_appointments_patient_id
  ON public.appointments (patient_id);

CREATE INDEX IF NOT EXISTS idx_appointments_doctor_date_queue_number
  ON public.appointments (doctor_id, appointment_date, queue_number DESC);

CREATE INDEX IF NOT EXISTS idx_doctors_clinic_id
  ON public.doctors (clinic_id)
  WHERE clinic_id IS NOT NULL;
