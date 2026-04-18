/**
 * يحوّل أخطاء الشبكة العامة (مثل Failed to fetch) إلى نص عربي قابل للفهم.
 * @param {unknown} e
 * @returns {string}
 */
export function formatAuthOrNetworkError(e) {
  const raw = typeof e?.message === 'string' ? e.message : String(e ?? '')
  const isNetworkLike =
    /failed to fetch|networkerror|load failed|fetch/i.test(raw) ||
    /err_name_not_resolved|name not resolved|getaddrinfo|ENOTFOUND|net::err/i.test(raw)

  if (isNetworkLike) {
    return [
      'تعذّر الاتصال بخادم تسجيل الدخول (شبكة أو عنوان خادم غير صالح).',
      'إن ظهر في الـ Network طلبًا إلى example.supabase.co فالمشروع يعمل بوضع اختبار خاطئ: أعد البناء بدون --mode test واحذف dist ثم npm run dev.',
      'جرّب من الكمبيوتر: http://localhost:5173 — تحقّق من الإنترنت وVPN ووقت الجهاز/المحاكي.',
      'تأكد أن Mawid-Web/.env يحتوي VITE_SUPABASE_URL الصحيح (https://…supabase.co) ثم أعد تشغيل npm run dev.',
    ].join(' ')
  }
  return raw || 'فشل تسجيل الدخول'
}
