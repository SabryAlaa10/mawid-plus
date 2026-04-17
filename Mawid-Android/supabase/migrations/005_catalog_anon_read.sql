-- نفّذ في SQL Editor بعد 001 و 002 (ويمكن تجاهل 003 إن استخدمت هذا الملف فقط).
-- بدون هذه السياسات: طلبات REST بمفتاح anon بدون JWT لا ترى أطباء/عيادات/طابور → قائمة فارغة أو خطأ.

drop policy if exists "doctors_select_anon_catalog" on public.doctors;
create policy "doctors_select_anon_catalog" on public.doctors
  for select to anon using (true);

drop policy if exists "clinics_select_anon_catalog" on public.clinics;
create policy "clinics_select_anon_catalog" on public.clinics
  for select to anon using (true);

drop policy if exists "queue_select_anon_catalog" on public.queue_settings;
create policy "queue_select_anon_catalog" on public.queue_settings
  for select to anon using (true);
