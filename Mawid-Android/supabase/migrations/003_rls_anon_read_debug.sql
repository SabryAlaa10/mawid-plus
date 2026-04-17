-- تشغيل من Supabase → SQL Editor (بعد 001 و 002)
-- السبب: بدون جلسة Supabase (مثلاً وضع تجاوز OTP) الطلبات تُنفَّذ كدور anon
-- والسياسات القديمة كانت "authenticated" فقط → قائمة أطباء فارغة.
--
-- ⚠️ للتطوير والاختبار فقط — احذف هذه السياسات أو عطّلها قبل الإنتاج.

drop policy if exists "doctors_select_anon_debug" on public.doctors;
create policy "doctors_select_anon_debug" on public.doctors
  for select to anon using (true);

drop policy if exists "clinics_select_anon_debug" on public.clinics;
create policy "clinics_select_anon_debug" on public.clinics
  for select to anon using (true);

drop policy if exists "queue_select_anon_debug" on public.queue_settings;
create policy "queue_select_anon_debug" on public.queue_settings
  for select to anon using (true);

-- لعرض كل المواعيد بدون تسجيل (اختبار فقط)
drop policy if exists "appointments_select_anon_debug" on public.appointments;
create policy "appointments_select_anon_debug" on public.appointments
  for select to anon using (true);
