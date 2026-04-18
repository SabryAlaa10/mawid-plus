-- تقييم الطبيب بعد اكتمال الموعد: يخزّن التقييم على الموعد ويحدّث متوسط الأطباء.
-- نفّذ في Supabase SQL Editor (أو ضمن سلسلة المايجريشنز).

ALTER TABLE public.appointments
  ADD COLUMN IF NOT EXISTS patient_rating smallint
    CHECK (patient_rating IS NULL OR (patient_rating >= 1 AND patient_rating <= 5));

CREATE OR REPLACE FUNCTION public.submit_doctor_rating(
  p_appointment_id uuid,
  p_stars int
)
RETURNS jsonb
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  v_uid uuid := auth.uid();
  v_appt_status text;
  v_appt_rating smallint;
  v_doc_id uuid;
  v_old_rating numeric;
  v_old_count int;
  v_new_rating numeric;
BEGIN
  IF v_uid IS NULL THEN
    RETURN jsonb_build_object('ok', false, 'error', 'not_authenticated');
  END IF;

  IF p_stars IS NULL OR p_stars < 1 OR p_stars > 5 THEN
    RETURN jsonb_build_object('ok', false, 'error', 'invalid_rating');
  END IF;

  SELECT a.status, a.patient_rating, a.doctor_id
  INTO v_appt_status, v_appt_rating, v_doc_id
  FROM public.appointments AS a
  WHERE a.id = p_appointment_id AND a.patient_id = v_uid
  FOR UPDATE;

  IF NOT FOUND THEN
    RETURN jsonb_build_object('ok', false, 'error', 'not_found_or_forbidden');
  END IF;

  IF lower(trim(v_appt_status)) IS DISTINCT FROM 'done' THEN
    RETURN jsonb_build_object('ok', false, 'error', 'invalid_status');
  END IF;

  IF v_appt_rating IS NOT NULL THEN
    RETURN jsonb_build_object('ok', false, 'error', 'already_rated');
  END IF;

  UPDATE public.appointments
  SET patient_rating = p_stars
  WHERE id = p_appointment_id;


  SELECT rating, review_count INTO v_old_rating, v_old_count
  FROM public.doctors
  WHERE id = v_doc_id
  FOR UPDATE;

  IF NOT FOUND THEN
    RAISE EXCEPTION 'doctor row missing for appointment %', p_appointment_id;
  END IF;

  v_old_count := coalesce(v_old_count, 0);
  IF v_old_count <= 0 OR v_old_rating IS NULL THEN
    v_new_rating := p_stars;
  ELSE
    v_new_rating := round(
      (v_old_rating * v_old_count + p_stars)::numeric / (v_old_count + 1),
      1
    );
  END IF;

  UPDATE public.doctors
  SET
    rating = v_new_rating,
    review_count = v_old_count + 1
  WHERE id = v_doc_id;

  RETURN jsonb_build_object('ok', true);
END;
$$;

REVOKE ALL ON FUNCTION public.submit_doctor_rating(uuid, int) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.submit_doctor_rating(uuid, int) TO authenticated;
