import { useCountUp } from '../../hooks/useCountUp'

export default function StatCard({
  title,
  value,
  subtitle,
  icon: Icon,
  colorClass = 'text-primary',
  className = '',
  loading = false,
}) {
  const raw = typeof value === 'number' ? value : Number(value)
  const numeric = !Number.isNaN(raw) && Number.isFinite(raw)
  const count = useCountUp(numeric ? raw : 0, {
    durationMs: 950,
    active: !loading && numeric,
  })

  const display = loading ? null : numeric ? count : value

  return (
    <div
      className={`dashboard-card group p-5 flex items-start gap-4 hover:-translate-y-0.5 hover:shadow-[0_12px_40px_-12px_rgba(15,23,42,0.12)] ${className}`}
      dir="rtl"
    >
      {Icon && (
        <div
          className={`p-3.5 rounded-xl bg-gradient-to-br from-slate-50 to-slate-100/80 ring-1 ring-slate-200/80 shadow-inner transition-transform duration-300 group-hover:scale-105 ${colorClass}`}
        >
          <Icon className="w-6 h-6" />
        </div>
      )}
      <div className="min-w-0 flex-1">
        <p className="text-sm font-medium text-slate-500">{title}</p>
        <div className="mt-1 h-9 flex items-center">
          {loading ? (
            <span className="block h-8 w-16 rounded-lg skeleton-shimmer" />
          ) : (
            <p className="text-3xl font-black tracking-tight text-slate-900 tabular-nums">{display}</p>
          )}
        </div>
        {subtitle && <p className="text-xs text-slate-400 mt-1">{subtitle}</p>}
      </div>
    </div>
  )
}
