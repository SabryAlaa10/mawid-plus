-- Realtime على جدول الأطباء لتحديث المتوسط والعداد في لوحة الطبيب فور تقييم مريض.
-- نفّذ في Supabase SQL Editor إن لم يكن الجدول مضافاً لـ publication.

DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM pg_publication WHERE pubname = 'supabase_realtime')
     AND NOT EXISTS (
       SELECT 1 FROM pg_publication_tables
       WHERE pubname = 'supabase_realtime' AND schemaname = 'public' AND tablename = 'doctors'
     ) THEN
    ALTER PUBLICATION supabase_realtime ADD TABLE public.doctors;
  END IF;
END $$;
