import { useEffect, useMemo, useRef, useState } from 'react'
import { useOutletContext } from 'react-router-dom'
import { Loader2, Share2, CalendarClock, RefreshCw, FileDown } from 'lucide-react'
import PageHeader from '../components/ui/PageHeader'
import { useToast } from '../context/ToastContext'
import * as doctorService from '../services/doctorService'
import { elementToPdfBlob, downloadBlob, sharePdfBlob } from '../utils/schedulePdfExport'
import { buildSchedulePdfFilename } from '../utils/scheduleFilename'

const WEEK = [
  { v: 6, label: 'السبت' },
  { v: 0, label: 'الأحد' },
  { v: 1, label: 'الإثنين' },
  { v: 2, label: 'الثلاثاء' },
  { v: 3, label: 'الأربعاء' },
  { v: 4, label: 'الخميس' },
  { v: 5, label: 'الجمعة' },
]

function parseDays(raw) {
  if (!raw) return []
  if (Array.isArray(raw)) return raw
  try {
    const j = typeof raw === 'string' ? JSON.parse(raw) : raw
    return Array.isArray(j) ? j : []
  } catch {
    return []
  }
}

function formatTimeAr(hhmm) {
  const s = String(hhmm || '09:00').slice(0, 5)
  const [hs, ms] = s.split(':')
  const h = parseInt(hs, 10) || 0
  const m = parseInt(ms ?? '0', 10) || 0
  const d = new Date()
  d.setHours(h, m, 0, 0)
  return d.toLocaleTimeString('ar-SA', { hour: 'numeric', minute: '2-digit', hour12: true })
}

export default function SchedulePage() {
  const { doctor, refreshDoctor, doctorLoading, doctorError } = useOutletContext()
  const { showToast } = useToast()
  const [days, setDays] = useState([])
  const [start, setStart] = useState('09:00')
  const [end, setEnd] = useState('17:00')
  const [slot, setSlot] = useState(15)
  const [fee, setFee] = useState(0)
  const [saving, setSaving] = useState(false)
  const [msg, setMsg] = useState(null)
  const [previewPulseKey, setPreviewPulseKey] = useState(0)
  const previewSigRef = useRef(null)
  const schedulePreviewRef = useRef(null)
  const [pdfBusy, setPdfBusy] = useState(false)

  useEffect(() => {
    if (!doctor) return
    setDays(parseDays(doctor.available_days))
    if (doctor.start_time) setStart(String(doctor.start_time).slice(0, 5))
    if (doctor.end_time) setEnd(String(doctor.end_time).slice(0, 5))
    setSlot(doctor.slot_duration_minutes ?? 15)
    setFee(doctor.consultation_fee_sar ?? 0)
    previewSigRef.current = null
  }, [doctor])

  useEffect(() => {
    const sig = JSON.stringify({ days, start, end, slot, fee })
    if (previewSigRef.current === null) {
      previewSigRef.current = sig
      return
    }
    if (previewSigRef.current === sig) return
    previewSigRef.current = sig
    setPreviewPulseKey((k) => k + 1)
  }, [days, start, end, slot, fee])

  const selectedDayLabels = useMemo(
    () => WEEK.filter(({ v }) => days.includes(v)).map(({ label }) => label),
    [days]
  )

  const receptionRangeAr = useMemo(() => `${formatTimeAr(start)} — ${formatTimeAr(end)}`, [start, end])

  const slotLabelAr = useMemo(() => {
    const n = new Intl.NumberFormat('ar-SA')
    return `${n.format(Number(slot) || 0)} دقيقة`
  }, [slot])

  const feeLabelAr = useMemo(() => {
    const n = new Intl.NumberFormat('ar-SA')
    return `${n.format(Number(fee) || 0)} ر.س`
  }, [fee])

  const experienceLabelAr = useMemo(() => {
    const y = Number(doctor?.experience_years)
    if (!Number.isFinite(y) || y <= 0) return null
    const n = new Intl.NumberFormat('ar-SA')
    return y === 1 ? `${n.format(1)} سنة خبرة` : `${n.format(y)} سنوات خبرة`
  }, [doctor?.experience_years])

  const aboutText = (doctor?.about || '').trim()
  const clinicText = (doctor?.clinic_address || '').trim()
  const hasDoctorBio = Boolean(experienceLabelAr || aboutText || clinicText)

  const toggleDay = (v) => {
    setDays((d) => (d.includes(v) ? d.filter((x) => x !== v) : [...d, v].sort((a, b) => a - b)))
  }

  const schedulePdfFilename = useMemo(
    () => buildSchedulePdfFilename(doctor?.full_name, doctor?.id),
    [doctor?.full_name, doctor?.id]
  )

  const buildSchedulePdf = async () => {
    const el = schedulePreviewRef.current
    if (!el) throw new Error('معاينة الجدول غير جاهزة')
    return elementToPdfBlob(el)
  }

  const handleExportPdf = async () => {
    if (!doctor?.id || pdfBusy) return
    setPdfBusy(true)
    try {
      const blob = await buildSchedulePdf()
      downloadBlob(blob, schedulePdfFilename)
      showToast('تم تنزيل ملف PDF')
    } catch (e) {
      showToast(e.message || 'تعذر إنشاء PDF', 'error')
    } finally {
      setPdfBusy(false)
    }
  }

  const handleShareSchedule = async () => {
    if (!doctor?.id || pdfBusy) return
    setPdfBusy(true)
    try {
      const blob = await buildSchedulePdf()
      const shared = await sharePdfBlob(blob, schedulePdfFilename, {
        title: 'جدول المواعيد',
        text: doctor?.full_name ? `جدول عمل ${doctor.full_name}` : 'جدول المواعيد',
      })
      if (shared) {
        showToast('تمت المشاركة')
      } else {
        downloadBlob(blob, schedulePdfFilename)
        showToast('المتصفح لا يدعم المشاركة — تم تنزيل الملف')
      }
    } catch (e) {
      if (e?.name === 'AbortError') return
      showToast(e.message || 'تعذر المشاركة', 'error')
    } finally {
      setPdfBusy(false)
    }
  }

  const minutesFromHHMM = (s) => {
    const str = String(s || '00:00').slice(0, 5)
    const [hs, ms] = str.split(':')
    const h = parseInt(hs, 10)
    const m = parseInt(ms ?? '0', 10)
    if (!Number.isFinite(h) || !Number.isFinite(m)) return NaN
    return h * 60 + m
  }

  const save = async () => {
    if (!doctor?.id) return
    const ms = minutesFromHHMM(start)
    const me = minutesFromHHMM(end)
    if (Number.isFinite(ms) && Number.isFinite(me) && ms >= me) {
      showToast('بداية الدوام يجب أن تكون قبل نهاية الدوام (مثلاً 12:00 ص إلى 5:00 م وليس العكس)', 'error')
      return
    }
    setSaving(true)
    setMsg(null)
    try {
      await doctorService.updateSchedule(doctor.id, {
        available_days: days,
        start_time: start ? `${start}:00` : null,
        end_time: end ? `${end}:00` : null,
        slot_duration_minutes: Number(slot),
      })
      await doctorService.updateFee(doctor.id, Number(fee))
      await refreshDoctor?.()
      showToast('تم حفظ الجدول والرسوم بنجاح')
    } catch (e) {
      const m = e.message || 'فشل الحفظ'
      setMsg(m)
      showToast(m, 'error')
    } finally {
      setSaving(false)
    }
  }

  if (doctorLoading && !doctor) {
    return (
      <div className="max-w-5xl space-y-6 mx-auto animate-fade-in-up [animation-fill-mode:forwards]" dir="rtl">
        <div className="h-10 w-64 skeleton-shimmer rounded-lg" />
        <div className="dashboard-card min-h-[280px] skeleton-shimmer" />
        <div className="dashboard-card h-40 skeleton-shimmer" />
        <div className="dashboard-card h-40 skeleton-shimmer" />
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
      <p className="text-slate-500 dashboard-card px-6 py-8 text-center opacity-0 animate-fade-in-up [animation-fill-mode:forwards]" dir="rtl">
        لا يوجد ملف طبيب.
      </p>
    )
  }

  return (
    <div className="max-w-5xl space-y-6 mx-auto" dir="rtl">
      <PageHeader
        title="الجدول والرسوم"
        subtitles={[
          'أيام العمل وساعات الاستقبال ومدة الموعد',
          'معاينة مباشرة قبل الحفظ',
          'يتم حفظ الإعدادات في Supabase',
        ]}
      />

      <div className="dashboard-card p-6 md:p-8 relative overflow-hidden opacity-0 animate-fade-in-up [animation-fill-mode:forwards] [animation-delay:0.04s]">
        <div
          className="pointer-events-none absolute -top-20 left-0 w-64 h-64 rounded-full bg-primary/15 blur-3xl animate-float-slow"
          aria-hidden
        />
        <div className="relative z-10 flex flex-col lg:flex-row lg:items-start lg:justify-between gap-6">
          <div ref={schedulePreviewRef} className="min-w-0 flex-1 space-y-8 rounded-2xl bg-white/60 p-4 -m-4 border border-transparent">
            <div className="flex items-center gap-4">
              <div className="w-16 h-16 rounded-2xl bg-gradient-to-br from-primary/30 to-primary/5 ring-2 ring-primary/25 flex items-center justify-center text-primary shadow-inner shrink-0">
                <CalendarClock className="w-8 h-8" />
              </div>
              <div>
                <p
                  lang="ar"
                  className="text-xs font-semibold text-slate-600 tracking-normal normal-case leading-normal"
                >
                  معاينة الجدول
                </p>
                <p className="text-xl font-black text-slate-900">{doctor.full_name ?? '—'}</p>
                <p className="text-sm text-slate-600 font-medium">{doctor.specialty ?? '—'}</p>
              </div>
            </div>

            <dl
              key={previewPulseKey}
              className={`grid gap-4 sm:grid-cols-2 ${previewPulseKey > 0 ? 'animate-flash-blue' : ''}`}
            >
          <div className="rounded-xl border border-slate-100 bg-slate-50/80 p-4 transition-colors duration-300 flex flex-col min-h-[8.5rem]">
            <dt className="text-xs font-bold text-slate-500 mb-2 shrink-0">أيام العمل</dt>
            <dd className="flex flex-1 flex-wrap gap-2 items-center content-center justify-start min-h-0">
              {selectedDayLabels.length === 0 ? (
                <span className="text-sm text-slate-400 self-center">لم يُختر يوم بعد</span>
              ) : (
                selectedDayLabels.map((label) => (
                  <span
                    key={label}
                    className="inline-flex items-center justify-center min-h-[2.25rem] px-3 rounded-full text-xs font-bold bg-gradient-to-l from-[#1A73E8]/15 to-[#0D47A1]/10 text-primary border border-primary/25 leading-normal"
                  >
                    {label}
                  </span>
                ))
              )}
            </dd>
          </div>
          <div className="rounded-xl border border-slate-100 bg-slate-50/80 p-4 transition-colors duration-300">
            <dt className="text-xs font-bold text-slate-500 mb-2">ساعات الاستقبال</dt>
            <dd className="text-lg font-black text-slate-900 tabular-nums">{receptionRangeAr}</dd>
          </div>
          <div className="rounded-xl border border-slate-100 bg-slate-50/80 p-4 transition-colors duration-300">
            <dt className="text-xs font-bold text-slate-500 mb-2">مدة الموعد</dt>
            <dd className="text-lg font-black text-slate-900">{slotLabelAr}</dd>
          </div>
          <div className="rounded-xl border border-slate-100 bg-slate-50/80 p-4 transition-colors duration-300">
            <dt className="text-xs font-bold text-slate-500 mb-2">رسوم الكشف</dt>
            <dd className="text-lg font-black text-slate-900">{feeLabelAr}</dd>
          </div>
            </dl>

            {hasDoctorBio && (
              <div className="rounded-xl border border-slate-200/90 bg-slate-50/90 p-5 space-y-4 text-right">
                <h3 className="text-sm font-black text-slate-900 border-b border-slate-200/80 pb-2">
                  عن الطبيب
                </h3>
                <div className="space-y-4">
                  {experienceLabelAr && (
                    <div>
                      <p className="text-xs font-bold text-slate-500 mb-1">الخبرة</p>
                      <p className="text-sm font-semibold text-slate-800">{experienceLabelAr}</p>
                    </div>
                  )}
                  {aboutText && (
                    <div>
                      <p className="text-xs font-bold text-slate-500 mb-1">نبذة، تأهيل ودراسة</p>
                      <p className="text-sm text-slate-700 leading-relaxed whitespace-pre-wrap">{aboutText}</p>
                    </div>
                  )}
                  {clinicText && (
                    <div>
                      <p className="text-xs font-bold text-slate-500 mb-1">عنوان العيادة</p>
                      <p className="text-sm font-medium text-slate-800 leading-relaxed">{clinicText}</p>
                    </div>
                  )}
                </div>
              </div>
            )}
          </div>

          <div className="flex flex-wrap gap-2 shrink-0 lg:flex-col lg:items-stretch">
            <button
              type="button"
              disabled={pdfBusy}
              onClick={handleExportPdf}
              className="btn-gradient-primary inline-flex items-center justify-center gap-2 px-5 py-2.5 rounded-xl text-sm disabled:opacity-60"
            >
              {pdfBusy ? <Loader2 className="w-4 h-4 animate-spin" /> : <FileDown className="w-4 h-4" />}
              تصدير PDF
            </button>
            <button
              type="button"
              disabled={pdfBusy}
              onClick={handleShareSchedule}
              className="inline-flex items-center justify-center gap-2 px-5 py-2.5 rounded-xl text-sm font-bold border-2 border-primary/35 bg-white text-primary hover:bg-primary/5 transition-colors disabled:opacity-60"
            >
              <Share2 className="w-4 h-4" />
              مشاركة الجدول
            </button>
          </div>
        </div>

        {!hasDoctorBio && (
          <p className="relative z-10 mt-4 text-sm text-slate-500 leading-relaxed">
            أضف سنوات الخبرة والنبذة (تأهيل، دراسة، تفاصيل عنك) وعنوان العيادة من صفحة{' '}
            <span className="font-bold text-primary">الملف الشخصي</span> ليظهر ذلك في المعاينة وفي PDF.
          </p>
        )}
      </div>

      <section className="dashboard-card p-6 md:p-7 space-y-4 opacity-0 animate-fade-in-up [animation-fill-mode:forwards] [animation-delay:0.08s]">
        <h2 className="font-black text-slate-900 text-lg">أيام العمل</h2>
        <div className="flex flex-wrap gap-3">
          {WEEK.map(({ v, label }) => (
            <label
              key={v}
              className="flex items-center gap-2 cursor-pointer rounded-xl px-3 py-2 border border-slate-200 bg-slate-50/50 hover:bg-primary/5 hover:border-primary/30 transition-all duration-200"
            >
              <input
                type="checkbox"
                checked={days.includes(v)}
                onChange={() => toggleDay(v)}
                className="rounded border-slate-300 text-primary focus:ring-primary"
              />
              <span className="text-sm font-medium text-slate-800">{label}</span>
            </label>
          ))}
        </div>
      </section>

      <section className="dashboard-card p-6 md:p-7 grid grid-cols-1 sm:grid-cols-2 gap-5 opacity-0 animate-fade-in-up [animation-fill-mode:forwards] [animation-delay:0.11s]">
        <div>
          <label className="text-xs font-semibold text-slate-500 block mb-1.5">بداية الدوام</label>
          <input
            type="time"
            value={start}
            onChange={(e) => setStart(e.target.value)}
            className="dashboard-input"
          />
        </div>
        <div>
          <label className="text-xs font-semibold text-slate-500 block mb-1.5">نهاية الدوام</label>
          <input
            type="time"
            value={end}
            onChange={(e) => setEnd(e.target.value)}
            className="dashboard-input"
          />
        </div>
      </section>

      <section className="dashboard-card p-6 md:p-7 space-y-3 opacity-0 animate-fade-in-up [animation-fill-mode:forwards] [animation-delay:0.14s]">
        <h2 className="font-black text-slate-900 text-lg mb-1">مدة الموعد</h2>
        <select
          value={slot}
          onChange={(e) => setSlot(Number(e.target.value))}
          className="dashboard-input min-w-[220px]"
        >
          {[10, 15, 20, 30].map((m) => (
            <option key={m} value={m}>
              {m} دقيقة
            </option>
          ))}
        </select>
      </section>

      <section className="dashboard-card p-6 md:p-7 space-y-3 opacity-0 animate-fade-in-up [animation-fill-mode:forwards] [animation-delay:0.17s]">
        <h2 className="font-black text-slate-900 text-lg mb-1">رسوم الاستشارة (ر.س)</h2>
        <input
          type="number"
          min={0}
          value={fee}
          onChange={(e) => setFee(e.target.value)}
          className="dashboard-input max-w-xs"
        />
      </section>

      {msg && (
        <p
          className={`text-sm px-4 py-3 rounded-xl border ${
            msg.includes('فشل')
              ? 'text-red-800 bg-red-50 border-red-200'
              : 'text-emerald-800 bg-emerald-50 border-emerald-200'
          } animate-error-slide`}
        >
          {msg}
        </p>
      )}

      <button
        type="button"
        disabled={saving}
        onClick={save}
        className="btn-gradient-primary inline-flex items-center justify-center gap-2 px-8 py-3.5 rounded-xl text-base disabled:opacity-60"
      >
        {saving ? 'جاري الحفظ…' : 'حفظ التغييرات'}
        {saving && <Loader2 className="w-5 h-5 animate-spin" />}
      </button>
    </div>
  )
}
