import { Outlet } from 'react-router-dom'
import { useAuth } from '../../hooks/useAuth'
import { useDoctor } from '../../hooks/useDoctor'
import { useQueue } from '../../hooks/useQueue'
import { ToastProvider } from '../../context/ToastContext'
import Sidebar from './Sidebar'
import TopBar from './TopBar'
import DashboardDecor from './DashboardDecor'

export default function DashboardShell() {
  const { user } = useAuth()
  const { doctor, loading: doctorLoading, error: doctorError, refresh } = useDoctor(user?.id)
  const queue = useQueue(user?.id)

  return (
    <ToastProvider>
      <div className="flex min-h-screen bg-slate-100" dir="rtl">
        <Sidebar doctor={doctor} loading={doctorLoading} />
        <div className="flex-1 flex flex-col min-w-0 relative z-10">
          <TopBar
            doctor={doctor}
            isOpen={queue.isOpen}
            onToggleOpen={(v) => queue.toggle(v)}
            queueLoading={queue.loading}
          />
          <main className="relative flex-1 overflow-auto dashboard-bg">
            <DashboardDecor />
            <div className="relative z-10 p-4 md:p-6 lg:p-8 min-h-full">
              <Outlet
                context={{
                  doctor,
                  queue,
                  refreshDoctor: refresh,
                  doctorLoading,
                  doctorError,
                }}
              />
            </div>
          </main>
        </div>
      </div>
    </ToastProvider>
  )
}
