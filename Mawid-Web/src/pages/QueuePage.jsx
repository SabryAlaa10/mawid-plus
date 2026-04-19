import { useState } from 'react'
import { useOutletContext } from 'react-router-dom'
import { RefreshCw, ListOrdered } from 'lucide-react'
import CurrentNumberDisplay from '../components/queue/CurrentNumberDisplay'
import CallNextButton from '../components/queue/CallNextButton'
import WaitingList from '../components/queue/WaitingList'
import PageHeader from '../components/ui/PageHeader'
import * as queueService from '../services/queueService'
import * as appointmentService from '../services/appointmentService'

export default function QueuePage() {
  const { doctor, queue, doctorLoading, doctorError, refreshDoctor } = useOutletContext()

  if (doctorLoading && !doctor) {
    return (
      <div className="space-y-6 max-w-[1600px] mx-auto animate-fade-in-up [animation-fill-mode:forwards]" dir="rtl">
        <div className="h-10 w-48 skeleton-shimmer rounded-lg" />
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <div className="dashboard-card min-h-[400px] skeleton-shimmer" />
          <div className="dashboard-card min-h-[400px] skeleton-shimmer" />
        </div>
      </div>
    )
  }

  if (!doctor && doctorError) {
    return (
      <div className="dashboard-card p-8 max-w-lg mx-auto text-center opacity-0 animate-fade-in-up [animation-fill-mode:forwards]" dir="rtl">
        <p className="text-red-800 font-bold mb-2">تعذر تحميل بيانات الطبيب</p>
        <p className="text-sm text-red-700/90 mb-4">{doctorError}</p>
        <button
          type="button"
          onClick={() => refreshDoctor?.()}
          className="btn-gradient-primary inline-flex items-center gap-2 px-6 py-2.5 rounded-xl text-sm"
        >
          <RefreshCw className="w-4 h-4" />
          إعادة المحاولة
        </button>
      </div>
    )
  }

  if (!doctor) {
    return (
      <div className="dashboard-card p-8 max-w-md mx-auto text-center opacity-0 animate-fade-in-up [animation-fill-mode:forwards]" dir="rtl">
        <ListOrdered className="w-12 h-12 text-slate-300 mx-auto mb-3" />
        <p className="text-slate-600 font-bold">لا يوجد ملف طبيب.</p>
      </div>
    )
  }

  const [callError, setCallError] = useState(null)
  const handleCallPatient = async (w) => {
    try {
      setCallError(null)
      await queueService.updateCurrentNumber(doctor.id, w.queue_number)
      await appointmentService.updateAppointment(w.id, { status: 'in_progress' })
      await queue.refresh()
    } catch (err) {
      console.error('handleCallPatient error:', err)
      setCallError('تعذر استدعاء المريض. حاول مرة أخرى.')
      setTimeout(() => setCallError(null), 4000)
    }
  }

  return (
    <div className="space-y-8 max-w-[1600px] mx-auto" dir="rtl">
      <PageHeader
        title="إدارة الطابور"
        subtitles={['التحكم بالرقم الحالي وقائمة الانتظار', 'تحديث مباشر من الطابور', 'تنسيق مع تطبيق المريض']}
      />

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 lg:gap-8">
        <div className="dashboard-card p-6 md:p-8 space-y-6 opacity-0 animate-fade-in-up [animation-fill-mode:forwards] [animation-delay:0.06s]">
          <CurrentNumberDisplay currentNumber={queue.currentNumber} isOpen={queue.isOpen} />
          <CallNextButton
            onClick={() => queue.callNext()}
            loading={queue.loading}
            disabled={!doctor.id}
            noPatients={queue.waitingList.length === 0}
            canCallNext={queue.canCallNext}
          />
          <div className="flex flex-wrap gap-3">
            <button
              type="button"
              onClick={() => {
                if (!window.confirm('هل تريد تصفير الطابور؟ سيعود العداد إلى الصفر')) return
                queue.reset()
              }}
              className="px-5 py-2.5 rounded-xl border border-slate-200 bg-white text-slate-700 text-sm font-bold hover:bg-slate-50 hover:border-slate-300 transition-all duration-200 hover:scale-[1.02] shadow-sm"
            >
              تصفير الطابور
            </button>
            <button
              type="button"
              onClick={() => queue.toggle(!queue.isOpen)}
              className="px-5 py-2.5 rounded-xl border border-slate-200 bg-white text-slate-700 text-sm font-bold hover:bg-slate-50 hover:border-slate-300 transition-all duration-200 hover:scale-[1.02] shadow-sm"
            >
              {queue.isOpen ? 'إغلاق الطابور' : 'فتح الطابور'}
            </button>
          </div>
          {queue.flash?.type === 'success' && (
            <p className="text-sm text-emerald-800 px-4 py-3 rounded-xl bg-emerald-50 border-s-4 border-emerald-500 animate-error-slide">
              {queue.flash.text}
            </p>
          )}
          {queue.flash?.type === 'error' && (
            <p className="text-sm text-amber-900 px-4 py-3 rounded-xl bg-amber-50 border-s-4 border-amber-500 animate-error-slide">
              {queue.flash.text}
            </p>
          )}
          {queue.error && (
            <p className="text-sm text-red-700 px-4 py-3 rounded-xl bg-red-50 border-s-4 border-red-500 animate-error-slide">
              {queue.error}
            </p>
          )}
        </div>

        <div className="dashboard-card p-6 md:p-8 bg-gradient-to-br from-white to-slate-50/80 opacity-0 animate-fade-in-up [animation-fill-mode:forwards] [animation-delay:0.12s]">
          <WaitingList
            items={queue.waitingList}
            loading={queue.loading}
            onCallPatient={handleCallPatient}
          />
        </div>
      </div>
    </div>
  )
}
