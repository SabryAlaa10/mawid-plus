const styles = {
  waiting: 'bg-amber-100 text-amber-900 border-amber-200',
  scheduled: 'bg-amber-100 text-amber-900 border-amber-200',
  in_progress: 'bg-blue-100 text-blue-900 border-blue-200',
  done: 'bg-emerald-100 text-emerald-900 border-emerald-200',
  cancelled: 'bg-red-100 text-red-900 border-red-200',
}

const labels = {
  waiting: 'في الانتظار',
  scheduled: 'مجدول',
  in_progress: 'قيد المعاينة',
  done: 'مكتمل',
  cancelled: 'ملغى',
}

export default function StatusBadge({ status }) {
  const s = status === 'scheduled' ? 'waiting' : status
  const cls = styles[s] || 'bg-slate-100 text-slate-800 border-slate-200'
  const label = labels[s] || status

  return (
    <span className={`inline-flex px-2.5 py-0.5 rounded-full text-xs font-semibold border ${cls}`} dir="rtl">
      {label}
    </span>
  )
}
