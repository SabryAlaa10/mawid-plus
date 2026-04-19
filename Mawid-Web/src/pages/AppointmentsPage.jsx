import { useState, useEffect } from 'react'
import { format } from 'date-fns'
import { useOutletContext, useSearchParams } from 'react-router-dom'
import { Search, Calendar, RefreshCw, CalendarX } from 'lucide-react'
import { useAppointments } from '../hooks/useAppointments'
import AppointmentsTable from '../components/appointments/AppointmentsTable'
import AppointmentModal from '../components/appointments/AppointmentModal'
import AppointmentsPageSkeleton from '../components/appointments/AppointmentsPageSkeleton'
import PageHeader from '../components/ui/PageHeader'

export default function AppointmentsPage() {
  const { doctor, doctorLoading, doctorError, refreshDoctor } = useOutletContext() ?? {}
  const [searchParams] = useSearchParams()

  useEffect(() => {
    console.log('AppointmentsPage doctor id:', doctor?.id)
  }, [doctor])

  const [viewMode, setViewMode] = useState('today')
  const [date, setDate] = useState(() => format(new Date(), 'yyyy-MM-dd'))
  const [status, setStatus] = useState('all')
  const [search, setSearch] = useState('')
  const [selected, setSelected] = useState(null)

  const { rows, loading, refresh } = useAppointments(doctor?.id, date, status, search, viewMode)

  useEffect(() => {
    const dateParam = searchParams.get('date')
    if (dateParam && /^\d{4}-\d{2}-\d{2}$/.test(dateParam)) {
      setDate(dateParam)
      setViewMode('custom')
    }
  }, [searchParams])

  useEffect(() => {
    console.log('appointments rows count:', rows?.length ?? 0)
  }, [rows])

  if (doctorLoading && !doctor) {
    return <AppointmentsPageSkeleton />
  }

  if (!doctor && doctorError) {
    return (
      <div className="dashboard-card p-8 max-w-lg mx-auto text-center opacity-0 animate-fade-in-up [animation-fill-mode:forwards]" dir="rtl">
        <CalendarX className="w-14 h-14 text-red-300 mx-auto mb-4" aria-hidden />
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
        <Calendar className="w-12 h-12 text-slate-300 mx-auto mb-3" />
        <p className="text-slate-600 font-bold">لا يوجد ملف طبيب مرتبط بهذا الحساب.</p>
        <button
          type="button"
          onClick={() => refreshDoctor?.()}
          className="mt-4 text-sm font-bold text-primary hover:underline"
        >
          تحديث
        </button>
      </div>
    )
  }

  return (
    <div className="space-y-6 max-w-[1600px] mx-auto" dir="rtl">
      <PageHeader
        title="المواعيد"
        subtitles={['إدارة مواعيد المرضى', 'تصفية وبحث سريع', 'تحديث فوري من قاعدة البيانات']}
      />

      <div className="dashboard-card p-4 md:p-5 opacity-0 animate-fade-in-up [animation-fill-mode:forwards] [animation-delay:0.08s]">
        <div className="flex flex-col gap-4">
          <div>
            <label className="text-xs font-semibold text-slate-500 block mb-2">عرض المواعيد</label>
            <div className="flex flex-wrap gap-2">
              {[
                { id: 'today', label: 'اليوم' },
                { id: 'week', label: 'هذا الأسبوع' },
                { id: 'custom', label: 'تاريخ محدد' },
              ].map((opt) => (
                <button
                  key={opt.id}
                  type="button"
                  onClick={() => setViewMode(opt.id)}
                  className={`px-4 py-2 rounded-xl text-sm font-bold border transition-colors ${
                    viewMode === opt.id
                      ? 'bg-primary text-white border-primary shadow-sm'
                      : 'bg-slate-50 text-slate-700 border-slate-200 hover:border-primary/40'
                  }`}
                >
                  {opt.label}
                </button>
              ))}
            </div>
          </div>

          <div className="flex flex-col lg:flex-row flex-wrap gap-4 md:gap-5 items-stretch lg:items-end">
            {viewMode === 'custom' && (
              <div className="relative group">
                <label className="text-xs font-semibold text-slate-500 block mb-1.5">التاريخ</label>
                <div className="relative">
                  <Calendar className="absolute right-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400 pointer-events-none group-focus-within:text-primary transition-colors" />
                  <input
                    type="date"
                    value={date}
                    onChange={(e) => setDate(e.target.value)}
                    className="dashboard-input pr-10 min-w-[200px]"
                  />
                </div>
              </div>
            )}
            <div>
              <label className="text-xs font-semibold text-slate-500 block mb-1.5">الحالة</label>
              <select
                value={status}
                onChange={(e) => setStatus(e.target.value)}
                className="dashboard-input min-w-[180px]"
              >
                <option value="all">الكل</option>
                <option value="waiting">في الانتظار</option>
                <option value="in_progress">قيد المعاينة</option>
                <option value="done">مكتمل</option>
                <option value="cancelled">ملغى</option>
              </select>
            </div>
            <div className="flex-1 min-w-[200px]">
              <label className="text-xs font-semibold text-slate-500 block mb-1.5">بحث بالاسم</label>
              <div className="relative group">
                <Search className="absolute right-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400 pointer-events-none group-focus-within:text-primary transition-colors" />
                <input
                  type="search"
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                  placeholder="اسم المريض"
                  className="dashboard-input pr-10"
                />
              </div>
            </div>
          </div>
        </div>
      </div>

      <div className="opacity-0 animate-fade-in-up [animation-fill-mode:forwards] [animation-delay:0.14s]">
        <AppointmentsTable
          rows={rows}
          loading={loading}
          onRowClick={setSelected}
          weekView={viewMode === 'week'}
        />
      </div>

      {selected && (
        <AppointmentModal
          appointment={selected}
          onClose={() => setSelected(null)}
          onUpdated={refresh}
        />
      )}
    </div>
  )
}
