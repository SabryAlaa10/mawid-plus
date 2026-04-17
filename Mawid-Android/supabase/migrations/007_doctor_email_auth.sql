-- بوابة الطبيب (Web): ربط حساب auth.users.id = doctors.id + حقول إضافية + سياسات RLS.
-- نفّذ في Supabase SQL Editor بعد 001–006.

-- أعمدة إضافية للأطباء (تجنّب التكرار مع 002: full_name, rating, review_count, consultation_fee_sar موجودة)
ALTER TABLE public.doctors ADD COLUMN IF NOT EXISTS email text;
ALTER TABLE public.doctors ADD COLUMN IF NOT EXISTS about text;
ALTER TABLE public.doctors ADD COLUMN IF NOT EXISTS experience_years integer DEFAULT 0;
ALTER TABLE public.doctors ADD COLUMN IF NOT EXISTS available_days jsonb DEFAULT '[]'::jsonb;
ALTER TABLE public.doctors ADD COLUMN IF NOT EXISTS start_time time;
ALTER TABLE public.doctors ADD COLUMN IF NOT EXISTS end_time time;

-- ملاحظات على المواعيد (لوحة الطبيب)
ALTER TABLE public.appointments ADD COLUMN IF NOT EXISTS notes text;

-- سياسات الطبيب على جدول doctors (المعرّف = auth.uid)
DROP POLICY IF EXISTS "Doctors can update own profile" ON public.doctors;
CREATE POLICY "Doctors can update own profile"
ON public.doctors FOR UPDATE TO authenticated
USING (auth.uid() = id)
WITH CHECK (auth.uid() = id);

DROP POLICY IF EXISTS "Doctors can read own profile" ON public.doctors;
CREATE POLICY "Doctors can read own profile"
ON public.doctors FOR SELECT TO authenticated
USING (auth.uid() = id);

-- قراءة مواعيد المرضى للطبيب (doctor_id = auth.uid)
DROP POLICY IF EXISTS "appointments_select_as_doctor" ON public.appointments;
CREATE POLICY "appointments_select_as_doctor"
ON public.appointments FOR SELECT TO authenticated
USING (auth.uid() = doctor_id);

DROP POLICY IF EXISTS "appointments_update_as_doctor" ON public.appointments;
CREATE POLICY "appointments_update_as_doctor"
ON public.appointments FOR UPDATE TO authenticated
USING (auth.uid() = doctor_id)
WITH CHECK (auth.uid() = doctor_id);

-- قراءة ملف المريض عند وجود موعد مع الطبيب
DROP POLICY IF EXISTS "profiles_select_for_doctor_patients" ON public.profiles;
CREATE POLICY "profiles_select_for_doctor_patients"
ON public.profiles FOR SELECT TO authenticated
USING (
  EXISTS (
    SELECT 1 FROM public.appointments a
    WHERE a.patient_id = profiles.id AND a.doctor_id = auth.uid()
  )
);

-- تحديث إعدادات الطابور من الطبيب (إن لم تكن موجودة من 006)
DROP POLICY IF EXISTS "queue_update_doctor_own" ON public.queue_settings;
CREATE POLICY "queue_update_doctor_own"
ON public.queue_settings FOR UPDATE TO authenticated
USING (auth.uid() = doctor_id)
WITH CHECK (auth.uid() = doctor_id);

-- ملاحظة: فعّل Realtime على الجداول appointments و queue_settings من لوحة Supabase → Database → Replication
