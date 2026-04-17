import { createClient } from '@supabase/supabase-js'

// في وضع vitest (MODE=test) لا تُحمَّل دائماً متغيرات CI — قيم وهمية آمنة للتهيئة فقط
const testUrl = 'https://example.supabase.co'
const testAnonKey = 'vitest-ci-placeholder-not-real'

const url = import.meta.env.VITE_SUPABASE_URL || (import.meta.env.MODE === 'test' ? testUrl : '')
const anonKey =
  import.meta.env.VITE_SUPABASE_ANON_KEY || (import.meta.env.MODE === 'test' ? testAnonKey : '')

if (import.meta.env.PROD && (!import.meta.env.VITE_SUPABASE_URL || !import.meta.env.VITE_SUPABASE_ANON_KEY)) {
  throw new Error(
    'إعدادات Supabase ناقصة: عيّن VITE_SUPABASE_URL و VITE_SUPABASE_ANON_KEY قبل بناء الإنتاج.',
  )
}

if (
  !import.meta.env.PROD &&
  import.meta.env.MODE !== 'test' &&
  (!import.meta.env.VITE_SUPABASE_URL || !import.meta.env.VITE_SUPABASE_ANON_KEY)
) {
  console.warn('Missing VITE_SUPABASE_URL or VITE_SUPABASE_ANON_KEY')
}

export const supabase = createClient(url, anonKey)
