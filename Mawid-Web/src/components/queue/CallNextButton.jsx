import { Loader2, Phone } from 'lucide-react'

export default function CallNextButton({ onClick, loading, disabled, noPatients, canCallNext }) {
  const blocked = loading || disabled || !canCallNext
  const gray = noPatients || (!loading && (!canCallNext || disabled))

  return (
    <div className="space-y-2 w-full" dir="rtl">
      <button
        type="button"
        disabled={blocked}
        onClick={() => {
          if (!blocked) onClick()
        }}
        className={`w-full py-4 rounded-xl font-black text-lg flex items-center justify-center gap-2 transition-all duration-200 ${
          gray
            ? 'bg-slate-200 text-slate-500 border border-slate-300 cursor-not-allowed opacity-80'
            : 'btn-gradient-primary disabled:opacity-90'
        }`}
      >
        {loading && canCallNext ? (
          <>
            <Loader2 className="w-6 h-6 animate-spin" />
            جارٍ الاستدعاء…
          </>
        ) : (
          <>
            <Phone className="w-6 h-6" />
            استدعاء المريض التالي
          </>
        )}
      </button>
      {noPatients && !loading && (
        <p className="text-sm text-slate-500 text-center">لا يوجد مرضى في قائمة الانتظار</p>
      )}
      {!noPatients && !canCallNext && !loading && (
        <p className="text-sm text-amber-600 text-center font-medium">تم استدعاء جميع المرضى</p>
      )}
    </div>
  )
}
