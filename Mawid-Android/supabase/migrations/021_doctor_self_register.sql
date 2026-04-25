-- تسجيل طبيب ذاتي: بعد إنشاء auth user مع role=doctor في الميتاداتا

create or replace function public.handle_new_doctor()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  if (new.raw_user_meta_data->>'role') = 'doctor' then
    insert into public.doctors (id, email, full_name, specialty)
    values (
      new.id,
      new.email,
      coalesce(new.raw_user_meta_data->>'full_name', ''),
      coalesce(new.raw_user_meta_data->>'specialty', '')
    );
    -- handle_new_user قد يكون أنشأ صف patient؛ نحوّله إلى doctor
    insert into public.profiles (id, full_name, role)
    values (
      new.id,
      coalesce(new.raw_user_meta_data->>'full_name', ''),
      'doctor'
    )
    on conflict (id) do update
    set
      full_name = excluded.full_name,
      role = excluded.role;
    -- إنشاء إعدادات الطابور تلقائياً للدكتور الجديد
    insert into public.queue_settings (doctor_id, queue_date, current_number, is_open)
    values (
      new.id,
      current_date,
      0,
      false
    )
    on conflict (doctor_id) do nothing;
  end if;
  return new;
end;
$$;

drop trigger if exists on_doctor_signup on auth.users;
create trigger on_doctor_signup
after insert on auth.users
for each row
execute function public.handle_new_doctor();

-- إكمال الملف بعد التسجيل: السماح للطبيب بإدراج صفه (نادراً) أو التوسع لاحقاً
drop policy if exists "Doctors can insert own row" on public.doctors;
create policy "Doctors can insert own row"
on public.doctors
for insert
to authenticated
with check (auth.uid() = id);
