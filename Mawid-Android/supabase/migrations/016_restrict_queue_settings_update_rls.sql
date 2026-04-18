-- أمن: سياسة 006 كانت تسمح لأي مستخدم مصادق (بما فيه المريض بجلسة Anonymous)
-- بتحديث أي صف في queue_settings — يكفي طلب REST مُوجَّه لطبيب آخر لتلاعب بالطابور.
-- الطبيب يحدّث الطابور فقط عبر سياسة 007 queue_update_doctor_own (auth.uid() = doctor_id).
-- الحجز من التطبيق لا يحدّث queue_settings (يحسب max من appointments فقط).

DROP POLICY IF EXISTS "queue_update_authenticated" ON public.queue_settings;
