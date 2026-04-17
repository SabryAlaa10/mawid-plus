-- إرشادات ربط حسابات الطبيب (لا يُنفَّذ تلقائياً لأن إنشاء auth.users يتم من Dashboard)
--
-- 1) في Supabase: Authentication → Users → Add user
--    Email: مثلاً dr.ahmed@mawidplus.com
--    Password: (آمن) ثم انسخ UUID المستخدم.
--
-- 2) اربط صف الطبيب بنفس UUID:
--    UPDATE public.doctors SET id = '<uuid-من-auth>'::uuid WHERE full_name = 'د. أحمد خالد';
--    (احذر: قد تتعارض المفاتيح الأجنبية إن وُجدت مواعيد؛ نفّذ على نسخة تجريبية أولاً.)
--
-- 3) حدّث البريد في doctors:
--    UPDATE public.doctors SET email = 'dr.ahmed@mawidplus.com' WHERE id = '<uuid>'::uuid;
--
-- 4) أنشئ صف profile للدور doctor (اختياري — المشغّل الحالي ينشئ patient عند التسجيل):
--    INSERT INTO public.profiles (id, full_name, role) VALUES ('<uuid>', 'د. أحمد خالد', 'doctor')
--    ON CONFLICT (id) DO UPDATE SET full_name = EXCLUDED.full_name, role = 'doctor';

SELECT id, full_name, specialty, email FROM public.doctors LIMIT 10;
