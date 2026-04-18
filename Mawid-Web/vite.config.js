import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

const __dirname = path.dirname(fileURLToPath(import.meta.url))

/** قراءة ملف .env يدويًا (نفس أسلوب Vite تقريبًا) */
function parseEnvFile(filePath) {
  const out = {}
  if (!fs.existsSync(filePath)) return out
  const text = fs.readFileSync(filePath, 'utf8')
  for (const rawLine of text.split(/\r?\n/)) {
    const line = rawLine.trim()
    if (!line || line.startsWith('#')) continue
    const eq = line.indexOf('=')
    if (eq === -1) continue
    const key = line.slice(0, eq).trim()
    let val = line.slice(eq + 1).trim()
    if ((val.startsWith('"') && val.endsWith('"')) || (val.startsWith("'") && val.endsWith("'"))) {
      val = val.slice(1, -1)
    }
    out[key] = val
  }
  return out
}

/** دمج ملفات البيئة بنفس ترتيب Vite (الأخير يتفوق). */
function loadEnvFromFiles(mode) {
  const base = __dirname
  const names = ['.env', '.env.local', `.env.${mode}`, `.env.${mode}.local`]
  let merged = {}
  for (const name of names) {
    merged = { ...merged, ...parseEnvFile(path.join(base, name)) }
  }
  return merged
}

/**
 * Vite يعطي أولوية لمتغيرات النظام على ملف .env — فيتجاهل الملف أحيانًا.
 * نُثبّت قيم Supabase من الملف إن وُجدت (غير فارغة)، وإلا نستخدم process.env (مثل CI).
 */
function supabaseEnvForDefine(mode) {
  const file = loadEnvFromFiles(mode)
  const urlFromFile = (file.VITE_SUPABASE_URL || '').trim()
  const keyFromFile = (file.VITE_SUPABASE_ANON_KEY || '').trim()
  const url = urlFromFile || (process.env.VITE_SUPABASE_URL || '').trim()
  const key = keyFromFile || (process.env.VITE_SUPABASE_ANON_KEY || '').trim()
  return {
    VITE_SUPABASE_URL: url,
    VITE_SUPABASE_ANON_KEY: key,
    usedFileUrl: Boolean(urlFromFile),
    usedFileKey: Boolean(keyFromFile),
  }
}

export default defineConfig(({ mode }) => {
  const sb = supabaseEnvForDefine(mode)
  if (mode === 'production' && (!sb.VITE_SUPABASE_URL || !sb.VITE_SUPABASE_ANON_KEY)) {
    throw new Error(
      'بناء الإنتاج: VITE_SUPABASE_URL أو VITE_SUPABASE_ANON_KEY ناقصة. أضفهما في Mawid-Web/.env أو في متغيرات CI.',
    )
  }
  if (mode === 'development' && sb.VITE_SUPABASE_URL) {
    try {
      const host = new URL(sb.VITE_SUPABASE_URL).hostname
      console.info(
        `[vite] Supabase URL من ${sb.usedFileUrl ? 'ملف .env' : 'process.env'} → https://${host}/`,
      )
    } catch {
      console.warn('[vite] VITE_SUPABASE_URL في الملف غير صالح كرابط')
    }
  }

  return {
    envDir: __dirname,
    define: {
      'import.meta.env.VITE_SUPABASE_URL': JSON.stringify(sb.VITE_SUPABASE_URL),
      'import.meta.env.VITE_SUPABASE_ANON_KEY': JSON.stringify(sb.VITE_SUPABASE_ANON_KEY),
    },
    plugins: [react()],
    server: {
      host: true,
      port: 5173,
    },
    preview: {
      host: true,
      port: 4173,
    },
    test: {
      environment: 'jsdom',
      globals: true,
      include: ['src/**/*.{test,spec}.{js,jsx}'],
    },
    build: {
      target: 'es2015',
      minify: 'terser',
      rollupOptions: {
        output: {
          manualChunks: {
            vendor: ['react', 'react-dom', 'react-router-dom'],
            supabase: ['@supabase/supabase-js'],
            charts: ['recharts'],
            utils: ['date-fns'],
          },
        },
      },
    },
    optimizeDeps: {
      include: ['react', 'react-dom', '@supabase/supabase-js'],
    },
  }
})
