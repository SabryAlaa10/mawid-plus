import { createClient } from '@supabase/supabase-js'

/**
 * Vite يستبدل فقط الوصول الثابت: import.meta.env.VITE_SUPABASE_URL
 * (لا تستخدم import.meta.env[name] الديناميكي.)
 */
function normalizeEnvString(value) {
  if (value == null) return ''
  let s = String(value).replace(/^\uFEFF/, '').trim()
  s = s.replace(/^['"]+|['"]+$/g, '')
  s = s.replace(/[\u200B-\u200D\uFEFF]/g, '')
  return s.trim()
}

const testUrl = 'https://example.supabase.co'
const testAnonKey = 'vitest-ci-placeholder-not-real'

const useTestPlaceholders =
  import.meta.env.VITEST === true ||
  (import.meta.env.MODE === 'test' && !import.meta.env.PROD)

const rawUrl = normalizeEnvString(import.meta.env.VITE_SUPABASE_URL)
const rawKey = normalizeEnvString(import.meta.env.VITE_SUPABASE_ANON_KEY)

const url = rawUrl || (useTestPlaceholders ? testUrl : '')
const anonKey = rawKey || (useTestPlaceholders ? testAnonKey : '')

if (import.meta.env.DEV && !useTestPlaceholders && (!url || !anonKey)) {
  throw new Error(
    'ملف Mawid-Web/.env ناقص أو غير مُحمَّل: عيّن VITE_SUPABASE_URL و VITE_SUPABASE_ANON_KEY في نفس مجلد vite.config.js ثم أعد تشغيل npm run dev من مجلد Mawid-Web.',
  )
}

if (import.meta.env.PROD && (!rawUrl || !rawKey)) {
  throw new Error(
    'إعدادات Supabase ناقصة: عيّن VITE_SUPABASE_URL و VITE_SUPABASE_ANON_KEY قبل بناء الإنتاج.',
  )
}

if (!useTestPlaceholders && url) {
  let parsed
  try {
    parsed = new URL(url)
  } catch (e) {
    throw new Error(
      `VITE_SUPABASE_URL ليس رابطاً صالحاً بعد التنظيف. راجع المسافات والاقتباسات في .env. (${e?.message || 'خطأ parse'})`,
    )
  }
  if (parsed.protocol !== 'https:') {
    throw new Error('VITE_SUPABASE_URL يجب أن يبدأ بـ https://')
  }
  if (!parsed.hostname) {
    throw new Error('VITE_SUPABASE_URL لا يحتوي على اسم مضيف صالح.')
  }
  if (parsed.hostname === 'example.supabase.co') {
    throw new Error(
      'VITE_SUPABASE_URL ما زال يشير إلى example.supabase.co (عنوان وهمي). ضع رابط مشروعك من لوحة Supabase (Settings → API → Project URL) ثم أعد تشغيل npm run dev.',
    )
  }
}

export const supabase = createClient(url, anonKey)
