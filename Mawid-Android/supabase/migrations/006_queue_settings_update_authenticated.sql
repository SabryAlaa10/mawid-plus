-- يتيح للمستخدمين المسجّلين تحديث رقم الطابور الحالي بعد الحجز (من التطبيق).
-- نفّذ في SQL Editor بعد 001–005.

drop policy if exists "queue_update_authenticated" on public.queue_settings;
create policy "queue_update_authenticated" on public.queue_settings
  for update to authenticated using (true) with check (true);
