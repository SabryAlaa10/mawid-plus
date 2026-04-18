-- إلغاء موعد المريض عبر RPC يتجاوز تعقيدات RLS + عدم إرجاع صفوف بعد PATCH من العميل.
-- نفّذ في Supabase SQL Editor (أو ضمن سلسلة المايجريشنز).

CREATE OR REPLACE FUNCTION public.cancel_my_appointment(p_appointment_id uuid)
RETURNS jsonb
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  v_uid uuid := auth.uid();
BEGIN
  IF v_uid IS NULL THEN
    RETURN jsonb_build_object('ok', false, 'error', 'not_authenticated');
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM public.appointments a
    WHERE a.id = p_appointment_id AND a.patient_id = v_uid
  ) THEN
    RETURN jsonb_build_object('ok', false, 'error', 'not_found_or_forbidden');
  END IF;

  UPDATE public.appointments
  SET status = 'cancelled'
  WHERE id = p_appointment_id
    AND patient_id = v_uid;

  RETURN jsonb_build_object('ok', true);
END;
$$;

REVOKE ALL ON FUNCTION public.cancel_my_appointment(uuid) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.cancel_my_appointment(uuid) TO authenticated;
