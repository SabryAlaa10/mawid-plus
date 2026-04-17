-- حقول إضافية للعيادة والموقع (بعد 007 و 009)
ALTER TABLE public.doctors ADD COLUMN IF NOT EXISTS latitude double precision;
ALTER TABLE public.doctors ADD COLUMN IF NOT EXISTS longitude double precision;
ALTER TABLE public.doctors ADD COLUMN IF NOT EXISTS clinic_address text;
