import { createClient } from '@supabase/supabase-js'

const url = import.meta.env.VITE_SUPABASE_URL
const anonKey = import.meta.env.VITE_SUPABASE_ANON_KEY

if (import.meta.env.PROD && (!url || !anonKey)) {
  throw new Error(
    'إعدادات Supabase ناقصة: عيّن VITE_SUPABASE_URL و VITE_SUPABASE_ANON_KEY قبل بناء الإنتاج.',
  )
}

if (!import.meta.env.PROD && (!url || !anonKey)) {
  console.warn('Missing VITE_SUPABASE_URL or VITE_SUPABASE_ANON_KEY')
}

export const supabase = createClient(url ?? '', anonKey ?? '')
