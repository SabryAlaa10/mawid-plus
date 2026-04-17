-- صور أطباء: تخزين عام + عمود avatar_url (نفّذ من SQL Editor بعد المايجريشن السابقة).

-- دلو عام لصور الملفات الشخصية
INSERT INTO storage.buckets (id, name, public)
VALUES ('doctor-avatars', 'doctor-avatars', true)
ON CONFLICT (id) DO NOTHING;

-- رفع: المسار يجب أن يكون بالضبط {auth.uid()}.jpg (بدون مجلدات فرعية)
DROP POLICY IF EXISTS "Doctors can upload own avatar" ON storage.objects;
CREATE POLICY "Doctors can upload own avatar"
ON storage.objects FOR INSERT TO authenticated
WITH CHECK (
  bucket_id = 'doctor-avatars'
  AND name = (auth.uid()::text || '.jpg')
);

DROP POLICY IF EXISTS "Doctors can update own avatar" ON storage.objects;
CREATE POLICY "Doctors can update own avatar"
ON storage.objects FOR UPDATE TO authenticated
USING (
  bucket_id = 'doctor-avatars'
  AND name = (auth.uid()::text || '.jpg')
)
WITH CHECK (
  bucket_id = 'doctor-avatars'
  AND name = (auth.uid()::text || '.jpg')
);

DROP POLICY IF EXISTS "Doctors can delete own avatar" ON storage.objects;
CREATE POLICY "Doctors can delete own avatar"
ON storage.objects FOR DELETE TO authenticated
USING (
  bucket_id = 'doctor-avatars'
  AND name = (auth.uid()::text || '.jpg')
);

-- قراءة للجميع (روابط عامة)
DROP POLICY IF EXISTS "Anyone can view doctor avatars" ON storage.objects;
CREATE POLICY "Anyone can view doctor avatars"
ON storage.objects FOR SELECT TO public
USING (bucket_id = 'doctor-avatars');

ALTER TABLE public.doctors
ADD COLUMN IF NOT EXISTS avatar_url text;
