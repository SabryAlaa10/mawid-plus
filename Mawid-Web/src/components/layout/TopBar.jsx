import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Bell, Calendar, Star, X } from 'lucide-react'
import { useNotifications } from '../../hooks/useNotifications'
import { AR_LOCALE } from '../../constants/region'

function NotificationItem({ notif, timeAgo, onNavigate, onClose }) {
  const isBooking = notif.type === 'new_booking'
  const isRating = notif.type === 'new_rating'

  return (
    <button
      type="button"
      onClick={() => {
        if (notif.appointmentDate) onNavigate(notif.appointmentDate)
        onClose()
      }}
      className={`w-full text-right flex gap-3 items-start px-5 py-3.5 border-b border-slate-100 transition-colors ${
        notif.read ? 'bg-white hover:bg-slate-50' : 'bg-blue-50/90 hover:bg-slate-50'
      }`}
    >
      <div
        className={`w-10 h-10 rounded-full shrink-0 flex items-center justify-center ${
          isBooking ? 'bg-blue-100' : isRating ? 'bg-amber-100' : 'bg-red-100'
        }`}
      >
        {isBooking ? (
          <Calendar className="w-[18px] h-[18px] text-[#1A73E8]" aria-hidden />
        ) : isRating ? (
          <Star className="w-[18px] h-[18px] text-amber-600 fill-amber-400/30" aria-hidden />
        ) : (
          <X className="w-[18px] h-[18px] text-red-500" aria-hidden />
        )}
      </div>
      <div className="flex-1 min-w-0">
        <p className="text-[13px] font-semibold text-slate-900 m-0 mb-1">{notif.title}</p>
        <p className="text-xs text-slate-600 m-0 mb-1.5 leading-relaxed break-words">{notif.message}</p>
        <span className="text-[11px] text-slate-400">{timeAgo(notif.timestamp)}</span>
      </div>
      {!notif.read && (
        <div className="w-2 h-2 rounded-full bg-[#1A73E8] shrink-0 mt-1" aria-hidden />
      )}
    </button>
  )
}

export default function TopBar({ doctor, isOpen, onToggleOpen, queueLoading }) {
  const navigate = useNavigate()
  const { notifications, unreadCount, markAllRead, clearAll, timeAgo } = useNotifications(doctor?.id)

  const [panelOpen, setPanelOpen] = useState(false)
  const panelWrapRef = useRef(null)

  const today = new Intl.DateTimeFormat(AR_LOCALE, {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
    year: 'numeric',
  }).format(new Date())

  const navigateToAppointments = (appointmentDate) => {
    setPanelOpen(false)
    if (appointmentDate) {
      navigate(`/appointments?date=${encodeURIComponent(appointmentDate)}`)
    } else {
      navigate('/appointments')
    }
  }

  const handleBellClick = () => {
    setPanelOpen((prev) => {
      const next = !prev
      if (!prev && next) {
        markAllRead()
      }
      return next
    })
  }

  useEffect(() => {
    if (!panelOpen) return undefined
    const handler = (e) => {
      if (panelWrapRef.current && !panelWrapRef.current.contains(e.target)) {
        setPanelOpen(false)
      }
    }
    document.addEventListener('mousedown', handler)
    return () => document.removeEventListener('mousedown', handler)
  }, [panelOpen])

  return (
    <header
      className="h-16 bg-white/85 backdrop-blur-md border-b border-slate-200/80 flex items-center justify-between px-4 md:px-6 shrink-0 shadow-sm shadow-slate-900/5 relative z-50"
      dir="rtl"
    >
      <div className="opacity-0 animate-fade-in-up [animation-fill-mode:forwards] [animation-duration:0.5s]">
        <p className="text-[11px] uppercase tracking-wider text-slate-500 font-semibold">اليوم</p>
        <p className="text-sm font-bold text-slate-800">{today}</p>
      </div>

      <div className="flex items-center gap-3 md:gap-5">
        <label className="flex items-center gap-2 cursor-pointer select-none group">
          <span className="text-sm text-slate-600 hidden sm:inline">الطابور</span>
          <span
            className={`text-sm font-semibold transition-colors ${isOpen ? 'text-emerald-600' : 'text-slate-500'}`}
          >
            {isOpen ? 'مفتوح' : 'مغلق'}
          </span>
          <input
            type="checkbox"
            className="sr-only peer"
            checked={isOpen}
            disabled={queueLoading}
            onChange={(e) => onToggleOpen(e.target.checked)}
          />
          <span
            className={`relative w-11 h-6 rounded-full transition-all duration-300 ${
              isOpen ? 'bg-primary shadow-md shadow-primary/40' : 'bg-slate-300'
            }`}
          >
            <span
              className={`absolute top-1 right-1 w-4 h-4 bg-white rounded-full shadow transition-transform duration-300 ${
                isOpen ? '-translate-x-5' : 'translate-x-0'
              }`}
            />
          </span>
        </label>

        <div ref={panelWrapRef} className="relative">
          <button
            type="button"
            onClick={handleBellClick}
            className={`relative p-2 rounded-full flex items-center justify-center transition-colors duration-200 ${
              panelOpen ? 'bg-blue-50' : 'bg-transparent hover:bg-slate-100'
            }`}
            aria-label="الإشعارات"
            aria-expanded={panelOpen}
          >
            <Bell className="w-[22px] h-[22px] text-slate-500" strokeWidth={2} />
            {unreadCount > 0 && (
              <span
                className="absolute top-0.5 end-0.5 min-w-[18px] h-[18px] px-1 rounded-full bg-red-500 text-white text-[11px] font-bold flex items-center justify-center leading-none border-2 border-white shadow-sm notification-badge-pulse"
                style={{ fontVariantNumeric: 'tabular-nums' }}
              >
                {unreadCount > 99 ? '99+' : unreadCount}
              </span>
            )}
          </button>

          {panelOpen && (
            <div
              className="absolute top-[110%] left-0 w-[min(100vw-2rem,360px)] bg-white border border-slate-200 rounded-2xl shadow-[0_8px_32px_rgba(0,0,0,0.12)] z-[1000] overflow-hidden animate-notification-slide-down"
              dir="rtl"
            >
              <div className="flex items-center justify-between gap-2 px-5 py-4 border-b border-slate-200">
                <span className="font-semibold text-[15px] text-slate-900">الإشعارات</span>
                {notifications.length > 0 && (
                  <button
                    type="button"
                    onClick={() => clearAll()}
                    className="text-xs font-bold text-red-500 hover:text-red-600 bg-transparent border-none cursor-pointer p-0"
                  >
                    مسح الكل
                  </button>
                )}
              </div>
              <div className="max-h-[400px] overflow-y-auto">
                {notifications.length === 0 ? (
                  <div className="px-5 py-10 text-center text-slate-400">
                    <Bell className="w-8 h-8 mx-auto mb-3 opacity-40" />
                    <p className="text-sm m-0">لا توجد إشعارات</p>
                  </div>
                ) : (
                  notifications.map((notif) => (
                    <NotificationItem
                      key={notif.id}
                      notif={notif}
                      timeAgo={timeAgo}
                      onNavigate={navigateToAppointments}
                      onClose={() => setPanelOpen(false)}
                    />
                  ))
                )}
              </div>
            </div>
          )}
        </div>

        <div className="flex items-center gap-2.5 ps-3 ms-2 border-s border-slate-200/80">
          <div className="w-10 h-10 rounded-full bg-gradient-to-br from-primary/25 to-primary/5 ring-2 ring-primary/20 flex items-center justify-center text-primary font-bold text-sm shadow-inner">
            {(doctor?.full_name || 'د')[0]}
          </div>
          <div className="hidden sm:block text-right min-w-0">
            <p className="text-sm font-bold text-slate-800 truncate max-w-[160px]">{doctor?.full_name ?? '—'}</p>
            <p className="text-xs text-slate-500 truncate max-w-[160px]">{doctor?.specialty ?? ''}</p>
          </div>
        </div>
      </div>
    </header>
  )
}
