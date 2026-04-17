import { useState, useEffect } from 'react'
import { X } from 'lucide-react'
import StatusBadge from './StatusBadge'
import { formatArabicAppointmentDate } from '../../utils/appointmentFormat'
import * as appointmentService from '../../services/appointmentService'

export default function AppointmentModal({ appointment, onClose, onUpdated }) {
  const [notes, setNotes] = useState(appointment?.notes ?? '')
  const [saving, setSaving] = useState(false)
  const [err, setErr] = useState(null)

  useEffect(() => {
    setNotes(appointment?.notes ?? '')
  }, [appointment])

  if (!appointment) return null

  const patchStatus = async (status) => {
    setSaving(true)
    setErr(null)
    try {
      if (status === 'done') {
        await appointmentService.completeAppointment(appointment.id, notes)
        window.alert('تم إنهاء الحالة وإرسال الملاحظات للمريض')
      } else {
        await appointmentService.updateAppointment(appointment.id, { status, notes })
      }
      onUpdated?.()
      onClose()
    } catch (e) {
      setErr(e.message || 'فشل الحفظ')
    } finally {
      setSaving(false)
    }
  }

  const saveNotes = async () => {
    setSaving(true)
    setErr(null)
    try {
      await appointmentService.updateAppointment(appointment.id, { notes })
      onUpdated?.()
    } catch (e) {
      setErr(e.message || 'فشل الحفظ')
    } finally {
      setSaving(false)
    }
  }

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/50 backdrop-blur-sm opacity-0 animate-fade-in-up [animation-fill-mode:forwards]"
      dir="rtl"
    >
      <div
        className="dashboard-card max-w-lg w-full max-h-[90vh] overflow-y-auto shadow-2xl border-slate-200/90 scale-100 animate-fade-in-up [animation-fill-mode:forwards] [animation-duration:0.35s]"
        role="dialog"
        aria-modal="true"
      >
        <div className="flex items-center justify-between p-5 border-b border-slate-100 bg-gradient-to-l from-slate-50 to-white rounded-t-2xl">
          <h2 className="text-xl font-black text-slate-900">تفاصيل الموعد</h2>
          <button
            type="button"
            onClick={onClose}
            className="p-2.5 rounded-xl text-slate-500 hover:bg-slate-100 hover:text-slate-800 transition-all duration-200 hover:scale-105"
            aria-label="إغلاق"
          >
            <X className="w-5 h-5" />
          </button>
        </div>
        <div className="p-5 space-y-5">
          <div className="rounded-xl border border-slate-100 bg-slate-50/80 p-4">
            <p className="text-xs font-bold text-slate-500 mb-1">المريض</p>
            <p className="font-black text-slate-900 text-lg">{appointment.patient_name}</p>
            <p className="text-sm text-slate-600 mt-1">{appointment.patient_phone || '—'}</p>
          </div>
          <div className="flex flex-wrap gap-4">
            <div>
              <p className="text-xs font-bold text-slate-500 mb-1">رقم الطابور</p>
              <p className="font-black text-transparent bg-clip-text bg-gradient-to-l from-[#1A73E8] to-[#0D47A1] text-2xl tabular-nums">
                #{appointment.queue_number}
              </p>
            </div>
            <div>
              <p className="text-xs font-bold text-slate-500 mb-1">الحالة</p>
              <StatusBadge status={appointment.status} />
            </div>
            <div>
              <p className="text-xs font-bold text-slate-500 mb-1">تاريخ الموعد</p>
              <p className="text-sm font-semibold text-slate-800">
                {formatArabicAppointmentDate(appointment.appointment_date)}
              </p>
            </div>
            {appointment.time_slot ? (
              <div>
                <p className="text-xs font-bold text-slate-500 mb-1">وقت الحجز</p>
                <p className="text-sm font-semibold text-slate-800 tabular-nums">{appointment.time_slot}</p>
              </div>
            ) : null}
          </div>

          {appointment.doctor_notes?.trim() ? (
            <div className="rounded-xl border border-emerald-100 bg-emerald-50/80 p-4">
              <p className="text-xs font-bold text-emerald-800 mb-1">ملاحظات للمريض (مرسلة من العيادة)</p>
              <p className="text-sm text-emerald-950 whitespace-pre-wrap">{appointment.doctor_notes.trim()}</p>
            </div>
          ) : null}

          <div>
            <label className="text-xs font-bold text-slate-500 block mb-1.5">ملاحظات داخلية / للمريض عند الإنهاء</label>
            <p className="text-xs text-slate-500 mb-2">
              عند الضغط على «إنهاء» تُحفظ الملاحظات وتظهر للمريض في تطبيق موعد+.
            </p>
            <textarea
              className="dashboard-input min-h-[100px] resize-y"
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
            />
            <button
              type="button"
              onClick={saveNotes}
              disabled={saving}
              className="mt-3 text-sm font-bold text-primary hover:underline disabled:opacity-50"
            >
              {saving ? 'جاري الحفظ…' : 'حفظ الملاحظات'}
            </button>
          </div>

          {err && (
            <p className="text-sm text-red-700 px-3 py-2 rounded-xl bg-red-50 border border-red-100 animate-error-slide">{err}</p>
          )}

          <div className="flex flex-wrap gap-2 pt-1">
            <button
              type="button"
              disabled={saving}
              onClick={() => patchStatus('in_progress')}
              className="px-4 py-2.5 rounded-xl bg-gradient-to-l from-blue-600 to-blue-800 text-white text-sm font-bold disabled:opacity-50 hover:scale-[1.02] transition-transform shadow-md"
            >
              استدعاء المريض
            </button>
            <button
              type="button"
              disabled={saving}
              onClick={() => patchStatus('done')}
              className="px-4 py-2.5 rounded-xl bg-gradient-to-l from-emerald-600 to-emerald-800 text-white text-sm font-bold disabled:opacity-50 hover:scale-[1.02] transition-transform shadow-md"
            >
              إنهاء
            </button>
            <button
              type="button"
              disabled={saving}
              onClick={() => patchStatus('cancelled')}
              className="px-4 py-2.5 rounded-xl bg-gradient-to-l from-red-600 to-red-800 text-white text-sm font-bold disabled:opacity-50 hover:scale-[1.02] transition-transform shadow-md"
            >
              إلغاء
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}
