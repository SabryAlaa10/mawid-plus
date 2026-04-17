-- يضمن أن المريض يستطيع تحديث مواعيده (مثلاً إلغاء الحالة إلى cancelled).
-- نفّذ في Supabase SQL Editor إذا كان إلغاء الموعد من التطبيق يفشل بسبب RLS.

DROP POLICY IF EXISTS "appointments_update_own" ON public.appointments;

CREATE POLICY "appointments_update_own" ON public.appointments
FOR UPDATE TO authenticated
USING (auth.uid() = patient_id)
WITH CHECK (auth.uid() = patient_id);
