import { useMemo, useState } from 'react'
import { Loader2, CalendarOff } from 'lucide-react'
import { format, parseISO, isValid } from 'date-fns'
import StatusBadge from './StatusBadge'
import { formatArabicAppointmentDate } from '../../utils/appointmentFormat'

function formatTimeSafe(createdAt) {
  if (!createdAt) return '—'
  const d = parseISO(createdAt)
  return isValid(d) ? format(d, 'HH:mm') : '—'
}

function groupRowsByDate(rows) {
  const map = new Map()
  for (const r of rows || []) {
    const k = r.appointment_date || ''
    if (!map.has(k)) map.set(k, [])
    map.get(k).push(r)
  }
  const keys = [...map.keys()].sort()
  return keys.map((dateKey) => ({
    dateKey,
    rows: map.get(dateKey),
    label: formatArabicAppointmentDate(dateKey),
    count: map.get(dateKey).length,
  }))
}

export default function AppointmentsTable({ rows = [], loading, onRowClick, weekView = false }) {
  const [page, setPage] = useState(0)
  const pageSize = 10

  const pages = useMemo(() => {
    if (weekView) {
      return rows?.length ? [rows] : [[]]
    }
    const chunks = []
    for (let i = 0; i < (rows || []).length; i += pageSize) {
      chunks.push(rows.slice(i, i + pageSize))
    }
    return chunks.length ? chunks : [[]]
  }, [rows, weekView])

  const pageRows = pages[page] || []

  const weekSections = useMemo(() => {
    if (!weekView) return null
    return groupRowsByDate(rows || [])
  }, [rows, weekView])

  if (loading) {
    return (
      <div className="dashboard-card overflow-hidden" dir="rtl">
        <div className="p-4 space-y-3">
          <div className="h-10 w-full skeleton-shimmer rounded-xl" />
          {[0, 1, 2, 3, 4].map((i) => (
            <div
              key={i}
              className="h-12 w-full skeleton-shimmer rounded-lg opacity-0 animate-fade-in-up [animation-fill-mode:forwards]"
              style={{ animationDelay: `${i * 70}ms` }}
            />
          ))}
        </div>
        <div className="flex justify-center py-4 border-t border-slate-100">
          <Loader2 className="w-6 h-6 text-primary animate-spin" />
        </div>
      </div>
    )
  }

  const renderRow = (row) => (
    <tr
      key={row.id}
      className="border-t border-slate-100 hover:bg-primary/[0.04] cursor-pointer transition-colors duration-200 group"
      onClick={() => onRowClick(row)}
    >
      <td className="px-4 py-3.5 font-black text-primary tabular-nums">#{row.queue_number}</td>
      <td className="px-4 py-3.5 font-semibold text-slate-800">{row.patient_name}</td>
      <td className="px-4 py-3.5 text-slate-700 text-sm">{formatArabicAppointmentDate(row.appointment_date)}</td>
      <td className="px-4 py-3.5 text-slate-600 tabular-nums">{formatTimeSafe(row.created_at)}</td>
      <td className="px-4 py-3.5">
        <StatusBadge status={row.status} />
      </td>
      <td className="px-4 py-3.5 text-primary font-bold group-hover:underline">عرض</td>
    </tr>
  )

  return (
    <div className="dashboard-card overflow-hidden" dir="rtl">
      <div className="overflow-x-auto">
        <table className="w-full text-sm text-right">
          <thead className="bg-gradient-to-l from-slate-100 to-slate-50/90 text-slate-700 border-b border-slate-200/80">
            <tr>
              <th className="px-4 py-3.5 font-bold">رقم الطابور</th>
              <th className="px-4 py-3.5 font-bold">اسم المريض</th>
              <th className="px-4 py-3.5 font-bold">تاريخ الموعد</th>
              <th className="px-4 py-3.5 font-bold">وقت الموعد</th>
              <th className="px-4 py-3.5 font-bold">الحالة</th>
              <th className="px-4 py-3.5 font-bold">إجراءات</th>
            </tr>
          </thead>
          <tbody>
            {!weekView && pageRows.length === 0 ? (
              <tr>
                <td colSpan={6} className="px-4 py-16 text-center">
                  <div className="inline-flex flex-col items-center gap-3 text-slate-500 max-w-sm">
                    <CalendarOff className="w-14 h-14 text-slate-300" aria-hidden />
                    <span className="text-base font-bold text-slate-700">لا توجد مواعيد</span>
                    <span className="text-sm text-slate-500 leading-relaxed">
                      جرّب تغيير التاريخ أو إزالة عوامل التصفية لعرض المزيد من النتائج.
                    </span>
                  </div>
                </td>
              </tr>
            ) : weekView && (!weekSections || weekSections.length === 0) ? (
              <tr>
                <td colSpan={6} className="px-4 py-16 text-center">
                  <div className="inline-flex flex-col items-center gap-3 text-slate-500 max-w-sm">
                    <CalendarOff className="w-14 h-14 text-slate-300" aria-hidden />
                    <span className="text-base font-bold text-slate-700">لا توجد مواعيد</span>
                  </div>
                </td>
              </tr>
            ) : weekView ? (
              weekSections.flatMap((section) => [
                <tr key={`header-${section.dateKey}`} className="bg-slate-100/90">
                  <td colSpan={6} className="px-4 py-3 font-black text-slate-900 text-sm border-t border-slate-200">
                    {section.label} — {section.count} {section.count === 1 ? 'موعد' : 'مواعيد'}
                  </td>
                </tr>,
                ...section.rows.map((row) => renderRow(row)),
              ])
            ) : (
              pageRows.map((row) => renderRow(row))
            )}
          </tbody>
        </table>
      </div>
      {!weekView && (rows?.length ?? 0) > pageSize && (
        <div className="flex items-center justify-between px-4 py-3.5 border-t border-slate-100 bg-slate-50/80">
          <button
            type="button"
            disabled={page <= 0}
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            className="text-sm font-bold text-primary disabled:opacity-40 hover:underline transition-opacity"
          >
            السابق
          </button>
          <span className="text-xs font-medium text-slate-500 tabular-nums">
            صفحة {page + 1} من {pages.length}
          </span>
          <button
            type="button"
            disabled={page >= pages.length - 1}
            onClick={() => setPage((p) => Math.min(pages.length - 1, p + 1))}
            className="text-sm font-bold text-primary disabled:opacity-40 hover:underline transition-opacity"
          >
            التالي
          </button>
        </div>
      )}
    </div>
  )
}
