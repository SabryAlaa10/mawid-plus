import { useCallback, useEffect, useState } from 'react'
import { useOutletContext } from 'react-router-dom'
import { format } from 'date-fns'
import { Calendar, Users, CheckCircle, XCircle, RefreshCw, LayoutDashboard } from 'lucide-react'
import StatCard from '../components/dashboard/StatCard'
import LiveQueueWidget from '../components/dashboard/LiveQueueWidget'
import AppointmentsChart from '../components/dashboard/AppointmentsChart'
import DashboardSkeleton from '../components/dashboard/DashboardSkeleton'
import PageHeader from '../components/ui/PageHeader'
import { useToast } from '../context/ToastContext'
import * as appointmentService from '../services/appointmentService'

const STAGGER = ['[animation-delay:0ms]', '[animation-delay:100ms]', '[animation-delay:200ms]', '[animation-delay:300ms]']

export default function DashboardPage() {
  const { doctor, queue, doctorLoading, refreshDoctor } = useOutletContext()
  const { showToast } = useToast()
  const [stats, setStats] = useState({ today: 0, waiting: 0, in_progress: 0, done: 0, cancelled: 0 })
  const [chartByDay, setChartByDay] = useState({})
  const [loading, setLoading] = useState(true)
  const [fetchError, setFetchError] = useState(null)

  const loadStats = useCallback(
    async (options = {}) => {
      const silent = options.silent === true
      if (!doctor?.id) {
        if (!silent) setLoading(false)
        return
      }
      if (!silent) {
        setLoading(true)
        setFetchError(null)
      }
      try {
        const today = format(new Date(), 'yyyy-MM-dd')
        const [s, c] = await Promise.all([
          appointmentService.fetchAppointmentStatsForDay(doctor.id, today),
          appointmentService.fetchAppointmentsLast7Days(doctor.id),
        ])
        setStats(s)
        setChartByDay(c)
        if (!silent) setFetchError(null)
      } catch (e) {
        const msg = e.message || 'تعذر تحميل الإحصائيات'
        if (!silent) {
          setFetchError(msg)
          setStats({ today: 0, waiting: 0, in_progress: 0, done: 0, cancelled: 0 })
          showToast(msg, 'error')
        }
      } finally {
        if (!silent) setLoading(false)
      }
    },
    [doctor?.id, showToast]
  )

  useEffect(() => {
    loadStats()
  }, [loadStats])

  /** تحديث البطاقات فوراً عند أي تغيير على مواعيد هذا الطبيب (إلغاء، إنهاء، حجز جديد…) */
  useEffect(() => {
    if (!doctor?.id) return undefined
    return appointmentService.subscribeToAppointments(doctor.id, () => {
      loadStats({ silent: true })
    }, 'dashboard')
  }, [doctor?.id, loadStats])

  if (doctorLoading && !doctor) {
    return <DashboardSkeleton />
  }

  if (!doctor) {
    return (
      <div
        className="dashboard-card p-8 border-amber-200/80 bg-amber-50/90 text-amber-950 text-sm leading-relaxed max-w-2xl opacity-0 animate-fade-in-up [animation-fill-mode:forwards]"
        dir="rtl"
      >
        <div className="flex flex-col sm:flex-row sm:items-start gap-4">
          <div className="p-3 rounded-2xl bg-amber-100/80 shrink-0">
            <LayoutDashboard className="w-8 h-8 text-amber-800" />
          </div>
          <div>
            <p className="font-bold text-amber-950 mb-2">لا يوجد ملف طبيب مرتبط</p>
            <p>
              اربط <code className="bg-amber-100/80 px-1.5 py-0.5 rounded-md font-mono text-xs">auth.users.id</code> مع{' '}
              <code className="bg-amber-100/80 px-1.5 py-0.5 rounded-md font-mono text-xs">doctors.id</code> في Supabase (انظر migration 008).
            </p>
            <button
              type="button"
              onClick={() => refreshDoctor?.()}
              className="mt-4 inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-amber-800 text-white text-sm font-bold hover:bg-amber-900 transition-transform hover:scale-[1.02]"
            >
              <RefreshCw className="w-4 h-4" />
              إعادة المحاولة
            </button>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="space-y-8 max-w-[1600px] mx-auto" dir="rtl">
      <PageHeader
        title="لوحة التحكم"
        subtitles={[
          'نظرة عامة على مواعيد اليوم والطابور',
          'متابعة فورية لأداء العيادة',
          'بيانات آمنة ومحدّثة من Supabase',
        ]}
      />

      {fetchError && (
        <div className="dashboard-card p-4 flex flex-wrap items-center justify-between gap-3 border-red-200 bg-red-50/90 text-red-900 animate-error-slide">
          <p className="text-sm font-medium">{fetchError}</p>
          <button
            type="button"
            onClick={() => loadStats()}
            className="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-red-700 text-white text-sm font-bold hover:bg-red-800 transition-transform hover:scale-[1.02]"
          >
            <RefreshCw className="w-4 h-4" />
            إعادة المحاولة
          </button>
        </div>
      )}

      <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4 md:gap-5">
        <StatCard
          title="مواعيد اليوم"
          value={stats.today}
          subtitle="إجمالي اليوم"
          icon={Calendar}
          colorClass="text-primary"
          loading={loading}
          className={`opacity-0 animate-fade-in-up [animation-fill-mode:forwards] ${STAGGER[0]}`}
        />
        <StatCard
          title="في الانتظار"
          value={stats.waiting}
          subtitle="بانتظار الدور"
          icon={Users}
          colorClass="text-amber-600"
          loading={loading}
          className={`opacity-0 animate-fade-in-up [animation-fill-mode:forwards] ${STAGGER[1]}`}
        />
        <StatCard
          title="مكتملة اليوم"
          value={stats.done}
          subtitle="منتهية"
          icon={CheckCircle}
          colorClass="text-emerald-600"
          loading={loading}
          className={`opacity-0 animate-fade-in-up [animation-fill-mode:forwards] ${STAGGER[2]}`}
        />
        <StatCard
          title="ملغاة اليوم"
          value={stats.cancelled}
          subtitle="ملغاة"
          icon={XCircle}
          colorClass="text-red-600"
          loading={loading}
          className={`opacity-0 animate-fade-in-up [animation-fill-mode:forwards] ${STAGGER[3]}`}
        />
      </div>

      <LiveQueueWidget queue={queue} />

      <AppointmentsChart byDay={chartByDay} loading={loading} />
    </div>
  )
}
