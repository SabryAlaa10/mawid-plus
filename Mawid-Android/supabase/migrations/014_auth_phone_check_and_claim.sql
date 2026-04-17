-- تدقيق الهاتف وربط الجلسة الحالية بملف موجود (بدون كشف كل الملفات لـ anon).
-- نفّذ في Supabase SQL Editor بعد المايجريشنز السابقة.

-- فهرس فريد للهاتف (يسمح بعدة صفوف phone = NULL)
CREATE UNIQUE INDEX IF NOT EXISTS profiles_phone_e164_unique
  ON public.profiles (phone)
  WHERE phone IS NOT NULL AND btrim(phone) <> '';

-- هل يوجد ملف بهذا الرقم؟ (للاستدعاء من التطبيق قبل تسجيل الدخول)
CREATE OR REPLACE FUNCTION public.check_phone_registered(phone_e164 text)
RETURNS boolean
LANGUAGE sql
SECURITY DEFINER
SET search_path = public
STABLE
AS $$
  SELECT EXISTS (
    SELECT 1 FROM public.profiles p
    WHERE p.phone IS NOT NULL AND p.phone = phone_e164
  );
$$;

REVOKE ALL ON FUNCTION public.check_phone_registered(text) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.check_phone_registered(text) TO anon, authenticated;

-- ربط المستخدم المجهول الحالي (auth.uid()) بصف profiles الموجود لنفس الهاتف
CREATE OR REPLACE FUNCTION public.claim_patient_profile_by_phone(phone_e164 text)
RETURNS jsonb
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  uid uuid := auth.uid();
  old_row public.profiles%ROWTYPE;
BEGIN
  IF uid IS NULL THEN
    RETURN jsonb_build_object('ok', false, 'error', 'not_authenticated');
  END IF;

  SELECT * INTO old_row FROM public.profiles WHERE phone = phone_e164 LIMIT 1;
  IF NOT FOUND THEN
    RETURN jsonb_build_object('ok', false, 'error', 'phone_not_found');
  END IF;

  IF old_row.id = uid THEN
    RETURN jsonb_build_object('ok', true, 'profile_id', uid);
  END IF;

  UPDATE public.appointments SET patient_id = uid WHERE patient_id = old_row.id;
  DELETE FROM public.profiles WHERE id = uid AND id <> old_row.id;
  UPDATE public.profiles SET id = uid WHERE id = old_row.id;

  RETURN jsonb_build_object('ok', true, 'profile_id', uid);
EXCEPTION WHEN OTHERS THEN
  RETURN jsonb_build_object('ok', false, 'error', SQLERRM);
END;
$$;

REVOKE ALL ON FUNCTION public.claim_patient_profile_by_phone(text) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.claim_patient_profile_by_phone(text) TO anon, authenticated;
