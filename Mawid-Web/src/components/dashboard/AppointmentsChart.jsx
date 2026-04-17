import { useId } from 'react'
import { format, eachDayOfInterval, subDays } from 'date-fns'
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts'
import { BarChart3 } from 'lucide-react'

export default function AppointmentsChart({ byDay, loading = false }) {
  const uid = useId().replace(/:/g, '')
  const gradId = `barGradient-${uid}`

  const end = new Date()
  const start = subDays(end, 6)
  const days = eachDayOfInterval({ start, end })

  const data = days.map((d) => {
    const key = format(d, 'yyyy-MM-dd')
    return {
      name: format(d, 'dd/MM'),
      count: byDay[key] || 0,
    }
  })

  const isEmpty = !loading && data.every((d) => d.count === 0)

  if (loading) {
    return (
      <div className="dashboard-card p-6 md:p-7 min-h-[320px] skeleton-shimmer opacity-0 animate-fade-in-up [animation-fill-mode:forwards] [animation-delay:0.2s]" dir="rtl" />
    )
  }

  return (
    <div
      className="dashboard-card p-6 md:p-7 opacity-0 animate-fade-in-up [animation-fill-mode:forwards] [animation-delay:0.2s]"
      dir="rtl"
    >
      <h2 className="text-lg font-black text-slate-900 mb-1">المواعيد (آخر 7 أيام)</h2>
      <p className="text-xs text-slate-500 mb-5">ملخص أسبوعي</p>

      {isEmpty ? (
        <div className="flex flex-col items-center justify-center py-16 text-center rounded-xl border border-dashed border-slate-200 bg-slate-50/80">
          <BarChart3 className="w-14 h-14 text-slate-300 mb-3" aria-hidden />
          <p className="text-slate-600 font-bold">لا توجد مواعيد مسجّلة في آخر أسبوع</p>
          <p className="text-sm text-slate-500 mt-1 max-w-sm">ستظهر البيانات هنا عند حجز المرضى عبر التطبيق.</p>
        </div>
      ) : (
        <div className="h-72 w-full rounded-xl bg-slate-50/50 border border-slate-100 p-2" style={{ direction: 'ltr' }}>
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={data} margin={{ top: 12, right: 8, left: 0, bottom: 4 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" vertical={false} />
              <XAxis dataKey="name" tick={{ fontSize: 11, fill: '#64748b' }} axisLine={{ stroke: '#e2e8f0' }} />
              <YAxis allowDecimals={false} tick={{ fontSize: 11, fill: '#64748b' }} axisLine={false} />
              <Tooltip
                contentStyle={{
                  borderRadius: '12px',
                  border: '1px solid #e2e8f0',
                  boxShadow: '0 8px 24px -8px rgba(15,23,42,0.15)',
                }}
              />
              <Bar dataKey="count" fill={`url(#${gradId})`} radius={[8, 8, 0, 0]} maxBarSize={48} />
              <defs>
                <linearGradient id={gradId} x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stopColor="#1A73E8" />
                  <stop offset="100%" stopColor="#0D47A1" />
                </linearGradient>
              </defs>
            </BarChart>
          </ResponsiveContainer>
        </div>
      )}
    </div>
  )
}
