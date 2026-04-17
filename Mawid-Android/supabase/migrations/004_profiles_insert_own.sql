-- بعد 001: إن لم يُنشأ صف في profiles (مثلاً تعطيل التريغر)، يمكن للمستخدم المسجّل إدراج صفه.
-- Anonymous ما زال ضمن دور authenticated في JWT.

drop policy if exists "profiles_insert_own" on public.profiles;
create policy "profiles_insert_own" on public.profiles
  for insert to authenticated
  with check (auth.uid () = id);
