import { Phone, Loader2 } from 'lucide-react'

export default function LiveQueueWidget({ queue }) {
  const { currentNumber, waitingList, callNext, loading, error, isOpen, canCallNext, flash } = queue
  const nextThree = (waitingList || []).slice(0, 3)
  const hasWaiting = (waitingList || []).length > 0
  const disabled = !canCallNext || loading
  const canAct = canCallNext && !loading

  return (
    <div
      className="dashboard-card p-6 md:p-7 relative overflow-hidden opacity-0 animate-fade-in-up [animation-fill-mode:forwards] [animation-delay:0.15s]"
      dir="rtl"
    >
      <div
        className="pointer-events-none absolute -top-24 -left-24 w-48 h-48 rounded-full bg-primary/10 blur-3xl"
        aria-hidden
      />
      <div className="relative z-10">
        <h2 className="text-lg font-black text-slate-900 mb-1">الطابور المباشر</h2>
        <p className="text-xs text-slate-500 mb-5">تحديث فوري من النظام</p>
        {flash?.type === 'success' && (
          <p className="text-sm text-emerald-800 mb-3 px-3 py-2 rounded-lg bg-emerald-50 border border-emerald-100 animate-error-slide">
            {flash.text}
          </p>
        )}
        {flash?.type === 'error' && (
          <p className="text-sm text-amber-900 mb-3 px-3 py-2 rounded-lg bg-amber-50 border border-amber-100 animate-error-slide">
            {flash.text}
          </p>
        )}
        {error && (
          <p className="text-sm text-red-600 mb-3 px-3 py-2 rounded-lg bg-red-50 border border-red-100 animate-error-slide">
            {error}
          </p>
        )}
        <div className="flex flex-col lg:flex-row lg:items-center gap-8">
          <div className="flex-1 text-center lg:text-right">
            <p className="text-sm font-medium text-slate-500">الرقم الحالي</p>
            <div className="mt-2 relative inline-block min-w-[4ch]">
              {isOpen && (
                <span
                  className="absolute inset-0 -m-3 rounded-3xl bg-primary/25 blur-2xl animate-pulse pointer-events-none"
                  aria-hidden
                />
              )}
              <p className="relative text-5xl md:text-6xl font-black text-transparent bg-clip-text bg-gradient-to-l from-[#1A73E8] to-[#0D47A1] tabular-nums drop-shadow-sm">
                #{currentNumber}
              </p>
            </div>
          </div>
          <div className="flex-1">
            <p className="text-sm font-bold text-slate-800 mb-3">التالي في الانتظار</p>
            <ul className="space-y-2">
              {nextThree.length === 0 ? (
                <li className="text-slate-400 text-sm py-4 text-center rounded-xl bg-slate-50/80 border border-dashed border-slate-200">
                  لا يوجد مرضى في الانتظار
                </li>
              ) : (
                nextThree.map((w, i) => (
                  <li
                    key={w.id}
                    className="flex justify-between items-center text-sm bg-slate-50/90 rounded-xl px-4 py-2.5 border border-slate-100 transition-all duration-200 hover:border-primary/30 hover:shadow-sm animate-row-enter opacity-0 [animation-fill-mode:forwards]"
                    style={{ animationDelay: `${80 + i * 60}ms` }}
                  >
                    <span className="font-black text-primary tabular-nums">#{w.queue_number}</span>
                    <span className="text-slate-700 truncate max-w-[180px] font-medium">{w.patient_name}</span>
                  </li>
                ))
              )}
            </ul>
          </div>
          <div className="shrink-0 flex flex-col items-center lg:items-start gap-2">
            <button
              type="button"
              disabled={disabled}
              onClick={() => canAct && callNext()}
              className={`flex items-center gap-2 px-6 py-3.5 rounded-xl font-bold text-base min-w-[200px] justify-center transition-all duration-200 ${
                canCallNext || loading
                  ? 'btn-gradient-primary disabled:opacity-90'
                  : 'bg-slate-200 text-slate-500 border border-slate-300 cursor-not-allowed opacity-80'
              }`}
            >
              {loading && canCallNext ? (
                <Loader2 className="w-5 h-5 animate-spin" />
              ) : (
                <Phone className="w-5 h-5" />
              )}
              {loading && canCallNext ? 'جارٍ الاستدعاء…' : 'استدعاء التالي'}
            </button>
            {!hasWaiting && !loading && (
              <p className="text-sm text-slate-500 text-center lg:text-right max-w-[220px]">لا يوجد مرضى في قائمة الانتظار</p>
            )}
            {hasWaiting && !canCallNext && !loading && (
              <p className="text-sm text-amber-600 text-center lg:text-right max-w-[220px] font-medium">تم استدعاء جميع المرضى</p>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}
