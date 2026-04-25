-- تشغيل من Supabase Dashboard → SQL Editor (بعد 001_initial_schema.sql)
-- يضيف حقول العرض للأطباء، عيادة تجريبية، أطباء، وإعدادات طابور.

alter table public.doctors
  add column if not exists full_name text not null default '';

alter table public.doctors
  add column if not exists image_url text;

alter table public.doctors
  add column if not exists rating numeric(3, 1);

alter table public.doctors
  add column if not exists review_count int not null default 0;

alter table public.doctors
  add column if not exists consultation_fee_sar int not null default 0;

-- عيادة تجريبية (معرّف ثابت يطابق التطبيق)
insert into public.clinics (id, name, address)
values (
  'a0000000-0000-4000-8000-000000000001'::uuid,
  'عيادة موعد+ التجريبية',
  'الرياض'
)
on conflict (id) do update
set name = excluded.name,
    address = excluded.address;

-- أطباء (معرّفات UUID ثابتة للتطبيق — انظر SeedDoctorIds في الكود)
insert into public.doctors (
  id,
  clinic_id,
  full_name,
  specialty,
  slot_duration_minutes,
  image_url,
  rating,
  review_count,
  consultation_fee_sar
)
values
  (
    'b0000000-0000-4000-8000-000000000001'::uuid,
    'a0000000-0000-4000-8000-000000000001'::uuid,
    'د. أحمد خالد',
    'طب الأسنان',
    15,
    'https://images.unsplash.com/photo-1612349317150-e413f6a5b16d?w=400&h=400&fit=crop&q=80',
    4.8,
    120,
    200
  ),
  (
    'b0000000-0000-4000-8000-000000000002'::uuid,
    'a0000000-0000-4000-8000-000000000001'::uuid,
    'د. نورة السبيعي',
    'طب الأسرة',
    15,
    'https://images.unsplash.com/photo-1559839734-2b71ea197ec2?w=400&h=400&fit=crop&q=80',
    4.9,
    85,
    180
  ),
  (
    'b0000000-0000-4000-8000-000000000003'::uuid,
    'a0000000-0000-4000-8000-000000000001'::uuid,
    'د. فيصل القحطاني',
    'أمراض القلب',
    20,
    'https://images.unsplash.com/photo-1622253692010-333f2da6031d?w=400&h=400&fit=crop&q=80',
    4.9,
    120,
    350
  ),
  (
    'b0000000-0000-4000-8000-000000000004'::uuid,
    'a0000000-0000-4000-8000-000000000001'::uuid,
    'د. أحمد القحطاني',
    'طبيب العظام',
    15,
    'https://images.unsplash.com/photo-1582750433449-648ed127bb54?w=400&h=400&fit=crop&q=80',
    4.7,
    90,
    220
  )
on conflict (id) do update
set
  clinic_id = excluded.clinic_id,
  full_name = excluded.full_name,
  specialty = excluded.specialty,
  slot_duration_minutes = excluded.slot_duration_minutes,
  image_url = excluded.image_url,
  rating = excluded.rating,
  review_count = excluded.review_count,
  consultation_fee_sar = excluded.consultation_fee_sar;

-- إعدادات الطابور (تاريخ اليوم عند التشغيل — يُحدَّث يومياً من لوحة التحكم لاحقاً)
insert into public.queue_settings (doctor_id, current_number, queue_date, is_open)
values
  ('b0000000-0000-4000-8000-000000000001'::uuid, 5, (current_date at time zone 'UTC')::date, true),
  ('b0000000-0000-4000-8000-000000000002'::uuid, 2, (current_date at time zone 'UTC')::date, true),
  ('b0000000-0000-4000-8000-000000000003'::uuid, 8, (current_date at time zone 'UTC')::date, true),
  ('b0000000-0000-4000-8000-000000000004'::uuid, 3, (current_date at time zone 'UTC')::date, true)
on conflict (doctor_id) do update
set
  current_number = excluded.current_number,
  queue_date = excluded.queue_date,
  is_open = excluded.is_open,
  updated_at = now();
