-- تفعيل Realtime لجدول المواعيد (لوحة الطبيب — إشعارات الحجز الفورية)
-- نفّذ في Supabase إذا لم يكن الجدول مضافاً لـ publication بعد.
-- Database → Replication أو SQL Editor

DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM pg_publication WHERE pubname = 'supabase_realtime')
     AND NOT EXISTS (
       SELECT 1 FROM pg_publication_tables
       WHERE pubname = 'supabase_realtime' AND schemaname = 'public' AND tablename = 'appointments'
     ) THEN
    ALTER PUBLICATION supabase_realtime ADD TABLE public.appointments;
  END IF;
END $$;
image.png