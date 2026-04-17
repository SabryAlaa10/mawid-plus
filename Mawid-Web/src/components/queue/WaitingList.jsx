export default function WaitingList({ items, onCallPatient, loading }) {
  return (
    <div className="space-y-3" dir="rtl">
      <h3 className="font-black text-slate-900 text-lg mb-4">قائمة الانتظار</h3>
      {items.length === 0 ? (
        <p className="text-slate-400 text-sm py-10 text-center rounded-xl border border-dashed border-slate-200 bg-slate-50/50">
          لا يوجد مرضى في الانتظار
        </p>
      ) : (
        <ul className="space-y-2.5">
          {items.map((w, i) => (
            <li
              key={w.id}
              className="flex items-center justify-between gap-3 bg-white border border-slate-200/90 rounded-xl px-4 py-3 shadow-sm transition-all duration-200 hover:border-primary/35 hover:shadow-md opacity-0 animate-fade-in-up [animation-fill-mode:forwards]"
              style={{ animationDelay: `${60 + i * 45}ms` }}
            >
              <div className="min-w-0 flex items-baseline gap-2 flex-wrap">
                <span className="font-black text-primary text-xl tabular-nums">#{w.queue_number}</span>
                <span className="font-bold text-slate-800 truncate">{w.patient_name}</span>
              </div>
              <button
                type="button"
                disabled={loading}
                onClick={() => onCallPatient?.(w)}
                className="shrink-0 text-sm px-4 py-2 rounded-xl bg-gradient-to-l from-[#1A73E8] to-[#0D47A1] text-white font-bold shadow-md hover:scale-[1.03] hover:shadow-lg transition-all duration-200 disabled:opacity-50 disabled:hover:scale-100"
              >
                استدعاء
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
