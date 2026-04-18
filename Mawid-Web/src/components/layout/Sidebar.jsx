import { useState } from 'react'
import { NavLink, useNavigate } from 'react-router-dom'
import { LayoutGrid, Calendar, ListOrdered, Clock, User, LogOut, Loader2 } from 'lucide-react'
import { useAuth } from '../../hooks/useAuth'

const nav = [
  { to: '/dashboard', label: 'لوحة التحكم', icon: LayoutGrid },
  { to: '/appointments', label: 'المواعيد', icon: Calendar },
  { to: '/queue', label: 'إدارة الطابور', icon: ListOrdered },
  { to: '/schedule', label: 'الجدول', icon: Clock },
  { to: '/profile', label: 'الملف الشخصي', icon: User },
]

export default function Sidebar({ doctor, loading }) {
  const navigate = useNavigate()
  const { signOut } = useAuth()
  const [signingOut, setSigningOut] = useState(false)

  const handleLogout = async () => {
    setSigningOut(true)
    try {
      await signOut()
      navigate('/login', { replace: true })
    } catch {
      setSigningOut(false)
    }
  }

  return (
    <aside className="hidden md:flex w-64 flex-col shrink-0 min-h-screen bg-gradient-to-b from-[#1e293b] via-sidebar to-[#0f172a] text-white shadow-[4px_0_24px_-8px_rgba(15,23,42,0.35)] relative overflow-hidden">
      <div
        className="pointer-events-none absolute inset-0 opacity-[0.06]"
        style={{
          backgroundImage: 'radial-gradient(rgba(148, 163, 184, 1) 1px, transparent 1px)',
          backgroundSize: '20px 20px',
        }}
        aria-hidden
      />
      <div className="relative z-10 p-6 border-b border-slate-600/40">
        <h1 className="text-xl font-black tracking-tight text-white drop-shadow-[0_0_12px_rgba(26,115,232,0.35)]">
          Mawid+
        </h1>
        <p className="text-xs text-slate-400 mt-1">بوابة الطبيب</p>
        {loading ? (
          <p className="text-sm text-slate-400 mt-4 animate-pulse">…</p>
        ) : (
          <div className="flex items-center gap-3 mt-4 min-w-0">
            {doctor?.avatar_url || doctor?.image_url ? (
              <img
                src={doctor.avatar_url || doctor.image_url}
                alt=""
                className="w-12 h-12 rounded-full object-cover shrink-0 ring-2 ring-white/15 shadow-md"
              />
            ) : (
              <div className="w-12 h-12 rounded-full shrink-0 bg-gradient-to-br from-[#1A73E8] to-[#1557b0] flex items-center justify-center text-white text-sm font-black ring-2 ring-white/15">
                {(doctor?.full_name || 'ط')
                  .trim()
                  .split(/\s+/)
                  .filter(Boolean)
                  .slice(0, 2)
                  .map((w) => w[0])
                  .join('') || 'ط'}
              </div>
            )}
            <div className="min-w-0 flex-1">
              <p className="text-sm font-semibold truncate text-slate-100">{doctor?.full_name ?? 'طبيب'}</p>
              <p className="text-xs text-slate-400 truncate">{doctor?.specialty ?? ''}</p>
              {(doctor?.review_count > 0 || doctor?.rating != null) && (
                <p className="text-[11px] text-amber-200/95 font-semibold mt-1 m-0 tabular-nums truncate">
                  ★ {doctor.rating != null ? Number(doctor.rating).toFixed(1) : '—'} · {doctor.review_count ?? 0}{' '}
                  تقييم
                </p>
              )}
            </div>
          </div>
        )}
      </div>
      <nav className="relative z-10 flex-1 p-3 space-y-1">
        {nav.map(({ to, label, icon: Icon }) => (
          <NavLink
            key={to}
            to={to}
            className={({ isActive }) =>
              `flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium transition-all duration-200 transform ${
                isActive
                  ? 'bg-primary text-white shadow-lg shadow-primary/30 scale-[1.02]'
                  : 'text-slate-300 hover:bg-white/10 hover:text-white hover:translate-x-1'
              }`
            }
          >
            <Icon className="w-5 h-5 shrink-0" />
            {label}
          </NavLink>
        ))}
      </nav>
      <div className="relative z-10 mt-auto p-3 border-t border-slate-600/40">
        <button
          type="button"
          onClick={handleLogout}
          disabled={signingOut}
          className="flex items-center justify-center gap-3 w-full px-3 py-2.5 rounded-xl text-sm font-medium text-red-400 transition-all duration-200 hover:bg-red-600 hover:text-white hover:scale-[1.02] disabled:opacity-70 disabled:pointer-events-none disabled:hover:scale-100"
        >
          {signingOut ? (
            <Loader2 className="w-5 h-5 shrink-0 animate-spin" aria-hidden />
          ) : (
            <LogOut className="w-5 h-5 shrink-0" aria-hidden />
          )}
          تسجيل الخروج
        </button>
      </div>
    </aside>
  )
}
