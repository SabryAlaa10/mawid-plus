/**
 * تحميل التطبيق ديناميكيًا حتى تُمسَك أخطاء الاستيراد (مثل supabaseClient)
 * قبل أن يعمل ErrorBoundary — وإلا تبقى الشاشة بيضاء بدون أي رسالة.
 */
function escapeHtml(s) {
  return String(s)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}

const el = document.getElementById('root')
if (!el) {
  document.body.insertAdjacentHTML(
    'beforeend',
    '<p dir="rtl" style="padding:24px;font-family:system-ui">لا يوجد عنصر #root</p>',
  )
} else {
  import('./bootstrap.jsx').catch((err) => {
    const msg = err?.message || String(err)
    el.innerHTML = `
      <div dir="rtl" style="font-family:system-ui,Tahoma,sans-serif;padding:24px;max-width:640px;margin:24px auto;background:#fef2f2;border:1px solid #fecaca;border-radius:12px;color:#991b1b">
        <h1 style="font-size:1.15rem;margin:0 0 12px">تعذّر تشغيل التطبيق</h1>
        <p style="margin:0 0 12px;font-size:0.95rem;line-height:1.5">تحقّق من ملف <code style="background:#fff;padding:2px 6px;border-radius:4px">Mawid-Web/.env</code> وأعد تشغيل <code style="background:#fff;padding:2px 6px;border-radius:4px">npm run dev</code>. افتح <strong>Console (F12)</strong> لمزيد من التفاصيل.</p>
        <pre style="white-space:pre-wrap;word-break:break-word;font-size:0.82rem;background:#fff;padding:12px;border-radius:8px;border:1px solid #e5e7eb;color:#111;margin:0">${escapeHtml(msg)}</pre>
      </div>
    `
  })
}
