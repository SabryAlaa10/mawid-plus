-- تشغيل من Supabase Dashboard → SQL Editor → New query → لصق ثم Run
-- يُنشئ جداول موعد+ ويربط profiles مع auth.users ويفعّل RLS.

-- جدول الملفات الشخصية (مرتبط بـ auth.users)
create table if not exists public.profiles (
  id uuid primary key references auth.users (id) on delete cascade,
  full_name text not null default '',
  phone text,
  role text not null default 'patient',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

-- عيادات (اختياري — يمكن ربط الأطباء لاحقاً بعيادة حقيقية)
create table if not exists public.clinics (
  id uuid primary key default gen_random_uuid(),
  name text not null,
  address text,
  created_at timestamptz not null default now()
);

create table if not exists public.doctors (
  id uuid primary key default gen_random_uuid(),
  clinic_id uuid references public.clinics (id) on delete set null,
  specialty text not null default '',
  slot_duration_minutes int not null default 15,
  created_at timestamptz not null default now()
);

create table if not exists public.appointments (
  id uuid primary key default gen_random_uuid(),
  patient_id uuid not null references public.profiles (id) on delete cascade,
  doctor_id uuid not null references public.doctors (id) on delete cascade,
  queue_number int not null default 0,
  status text not null default 'scheduled',
  appointment_date date not null,
  created_at timestamptz not null default now()
);

create table if not exists public.queue_settings (
  doctor_id uuid primary key references public.doctors (id) on delete cascade,
  current_number int not null default 0,
  queue_date date not null,
  is_open boolean not null default false,
  updated_at timestamptz not null default now()
);

-- عند إنشاء مستخدم جديد في Auth (هاتف أو بريد) يُنشأ صف في profiles
create or replace function public.handle_new_user ()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  insert into public.profiles (id, full_name, phone, role)
  values (
    new.id,
    coalesce(new.raw_user_meta_data ->> 'full_name', ''),
    coalesce(new.phone, ''),
    'patient'
  )
  on conflict (id) do nothing;
  return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
after insert on auth.users for each row
execute function public.handle_new_user ();

-- RLS
alter table public.profiles enable row level security;
alter table public.doctors enable row level security;
alter table public.appointments enable row level security;
alter table public.queue_settings enable row level security;
alter table public.clinics enable row level security;

-- profiles: المستخدم يرى ويعدّل صفه فقط
create policy "profiles_select_own" on public.profiles for select using (auth.uid () = id);
create policy "profiles_update_own" on public.profiles for update using (auth.uid () = id);

-- أطباء وعيادات: قراءة للجميع المسجّلين (يمكن تقييدها لاحقاً)
create policy "doctors_select_authenticated" on public.doctors for select to authenticated using (true);
create policy "clinics_select_authenticated" on public.clinics for select to authenticated using (true);

-- مواعيد المريض
create policy "appointments_select_own" on public.appointments for select using (auth.uid () = patient_id);
create policy "appointments_insert_own" on public.appointments for insert with check (auth.uid () = patient_id);
create policy "appointments_update_own" on public.appointments for update using (auth.uid () = patient_id);

create policy "queue_select_authenticated" on public.queue_settings for select to authenticated using (true);
