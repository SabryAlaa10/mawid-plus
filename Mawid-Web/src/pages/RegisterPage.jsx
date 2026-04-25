import { useState, useEffect, useRef } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { Mail, Lock, Eye, EyeOff, Loader2, User, Stethoscope } from 'lucide-react'
import { useAuth } from '../hooks/useAuth'
import { signUpDoctor } from '../services/authService'
import { formatAuthOrNetworkError } from '../utils/authErrorMessage'

const PHRASES = [
  'إدارة مواعيدك بكل سهولة',
  'تابع طابور مرضاك لحظة بلحظة',
  'داشبورد احترافي لكل طبيب',
  'ربط فوري مع تطبيق المريض',
]

const SPECIALTY_SUGGESTIONS = [
  'أمراض القلب',
  'طبيب العظام',
  'طب الأسرة',
  'طب اسنان',
  'طب الأطفال',
  'جلدية',
  'عيون',
  'أنف وأذن وحنجرة',
  'نساء وتوليد',
  'أمراض الجهاز الهضمي',
  'طب الأعصاب',
  'طب نفسي',
  'تقويم أسنان',
  'جراحة عامة',
]

function useTypewriterPhrases() {
  const [text, setText] = useState('')
  const [opacity, setOpacity] = useState(1)
  const idxRef = useRef(0)
  const timersRef = useRef([])

  useEffect(() => {
    let cancelled = false
    const clearTimers = () => {
      timersRef.current.forEach((t) => clearTimeout(t))
      timersRef.current = []
    }

    const sleep = (ms) =>
      new Promise((resolve) => {
        const t = setTimeout(resolve, ms)
        timersRef.current.push(t)
      })

    const loop = async () => {
      while (!cancelled) {
        const phrase = PHRASES[idxRef.current % PHRASES.length]
        setOpacity(1)
        setText('')
        for (let c = 1; c <= phrase.length; c++) {
          if (cancelled) return
          await sleep(42)
          setText(phrase.slice(0, c))
        }
        await sleep(2200)
        if (cancelled) return
        setOpacity(0)
        await sleep(550)
        if (cancelled) return
        idxRef.current += 1
      }
    }

    loop()
    return () => {
      cancelled = true
      clearTimers()
    }
  }, [])

  return { text, opacity }
}

export default function RegisterPage() {
  const { user, loading } = useAuth()
  const navigate = useNavigate()
  const [fullName, setFullName] = useState('')
  const [specialty, setSpecialty] = useState('')
  const [showSuggestions, setShowSuggestions] = useState(false)
  const specialtyRef = useRef(null)
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [passwordConfirm, setPasswordConfirm] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [showPasswordConfirm, setShowPasswordConfirm] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState(null)
  const [success, setSuccess] = useState(false)
  const typewriter = useTypewriterPhrases()

  useEffect(() => {
    if (!success) return
    const t = setTimeout(() => {
      navigate('/login', { replace: true })
    }, 3000)
    return () => clearTimeout(t)
  }, [success, navigate])

  useEffect(() => {
    const handleDown = (e) => {
      if (specialtyRef.current && !specialtyRef.current.contains(e.target)) {
        setShowSuggestions(false)
      }
    }
    document.addEventListener('mousedown', handleDown)
    return () => document.removeEventListener('mousedown', handleDown)
  }, [])

  if (!loading && user) {
    return <Navigate to="/dashboard" replace />
  }

  const inputClassName =
    'w-full rounded-xl border border-slate-200 pr-11 pl-4 py-3.5 text-slate-900 transition-all duration-200 focus:outline-none focus:border-primary focus:ring-2 focus:ring-primary/25'

  const specialtyQuery = specialty.trim()
  const filteredSpecialtySuggestions =
    specialtyQuery === ''
      ? SPECIALTY_SUGGESTIONS
      : SPECIALTY_SUGGESTIONS.filter((s) => s.includes(specialtyQuery))

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError(null)

    const name = fullName.trim()
    if (!name || !specialty || !email.trim() || !password || !passwordConfirm) {
      setError('يرجى تعبئة جميع الحقول.')
      return
    }
    if (password.length < 8) {
      setError('كلمة المرور يجب ألا تقل عن 8 أحرف.')
      return
    }
    if (password !== passwordConfirm) {
      setError('كلمات المرور غير متطابقة.')
      return
    }

    setSubmitting(true)
    try {
      await signUpDoctor({
        fullName: name,
        specialty,
        email: email.trim(),
        password,
      })
      setSuccess(true)
    } catch (err) {
      setError(formatAuthOrNetworkError(err))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="min-h-screen flex flex-col lg:flex-row" dir="ltr">
      <section
        dir="rtl"
        className="relative login-gradient-bg login-dot-grid overflow-hidden lg:w-[60%] min-h-[320px] lg:min-h-screen flex flex-col items-center justify-center px-8 py-12 text-white animate-grid-shift"
      >
        <div className="absolute inset-0 opacity-[0.07] pointer-events-none bg-[linear-gradient(90deg,transparent_0%,rgba(26,115,232,0.15)_50%,transparent_100%)] animate-pulse" />

        <div className="relative z-10 flex flex-col items-center text-center max-w-lg animate-fade-in-up opacity-0 [animation-fill-mode:forwards]">
          <div className="relative mb-8">
            <div className="absolute inset-0 rounded-full bg-primary/20 animate-pulse-glow blur-xl scale-150" />
            <div className="relative flex items-center justify-center w-24 h-24 rounded-2xl bg-slate-800/80 border border-primary/40 shadow-xl">
              <svg
                className="w-14 h-14 text-primary animate-rotate-cross drop-shadow-[0_0_12px_rgba(26,115,232,0.8)]"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                aria-hidden
              >
                <path d="M12 3v18M3 12h18" strokeLinecap="round" />
              </svg>
            </div>
          </div>

          <h1 className="text-5xl md:text-6xl font-black tracking-tight login-logo-glow mb-4">Mawid+</h1>

          <p
            className="text-lg md:text-xl text-slate-300 min-h-[3.5rem] md:min-h-[4rem] transition-opacity duration-500"
            style={{ opacity: typewriter.opacity }}
          >
            {typewriter.text}
            <span className="inline-block w-0.5 h-6 bg-primary/80 ms-1 animate-pulse align-middle" />
          </p>
        </div>

        <div className="absolute inset-0 pointer-events-none overflow-hidden">
          <div className="absolute top-[18%] start-[8%] md:start-[12%] bg-slate-800/90 backdrop-blur-sm border border-slate-600/50 rounded-xl px-4 py-3 shadow-lg animate-float">
            <p className="text-lg font-bold text-white animate-pulse">٢٤ مريض اليوم</p>
          </div>
          <div className="absolute top-[42%] end-[6%] md:end-[10%] bg-slate-800/90 backdrop-blur-sm border border-slate-600/50 rounded-xl px-4 py-3 shadow-lg animate-slide-stat">
            <p className="text-lg font-bold text-emerald-400">الطابور: ٧ في الانتظار</p>
          </div>
          <div className="absolute bottom-[20%] start-[18%] md:start-[22%] bg-slate-800/90 backdrop-blur-sm border border-slate-600/50 rounded-xl px-4 py-3 shadow-lg animate-bounce-soft">
            <p className="text-lg font-bold text-amber-300">٩٨٪ رضا المرضى</p>
          </div>
        </div>
      </section>

      <section
        dir="rtl"
        className="flex-1 lg:w-[40%] min-h-screen bg-white flex flex-col justify-center px-6 sm:px-10 lg:px-14 py-10 opacity-0 animate-slide-in-panel [animation-fill-mode:forwards] shadow-[-12px_0_40px_-12px_rgba(15,23,42,0.12)]"
      >
        <div className="w-full max-w-md mx-auto space-y-8">
          <div className="text-center lg:text-right">
            <div className="inline-flex items-center gap-2 mb-1">
              <span className="text-xl font-black text-primary">Mawid+</span>
            </div>
            <p className="text-sm font-semibold text-slate-600">بوابة الأطباء</p>
            <h2 className="text-3xl font-bold text-slate-900 mt-6">تسجيل جديد</h2>
            <p className="text-slate-500 text-sm mt-2">أنشئ حسابك كطبيب</p>
          </div>

          {success ? (
            <div
              className="rounded-lg bg-emerald-50 text-emerald-900 text-sm px-4 py-3 border-s-4 border-emerald-500 shadow-sm"
              role="status"
            >
              تم إنشاء حسابك! تحقق من بريدك الإلكتروني لتفعيل الحساب.
            </div>
          ) : (
            <form onSubmit={handleSubmit} className="space-y-5">
              <div className="relative group">
                <label
                  htmlFor="fullName"
                  className="absolute -top-2.5 right-3 px-1 bg-white text-xs font-medium text-slate-500 transition-colors group-focus-within:text-primary z-10"
                >
                  الاسم الكامل
                </label>
                <div className="relative">
                  <User className="absolute right-3 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400 pointer-events-none group-focus-within:text-primary transition-colors" />
                  <input
                    id="fullName"
                    type="text"
                    autoComplete="name"
                    required
                    value={fullName}
                    onChange={(e) => setFullName(e.target.value)}
                    className={inputClassName}
                  />
                </div>
              </div>

              <div className="relative group" ref={specialtyRef}>
                <label
                  htmlFor="specialty"
                  className="absolute -top-2.5 right-3 px-1 bg-white text-xs font-medium text-slate-500 transition-colors group-focus-within:text-primary z-10"
                >
                  التخصص
                </label>
                <div className="relative">
                  <Stethoscope className="absolute right-3 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400 pointer-events-none group-focus-within:text-primary transition-colors" />
                  <input
                    id="specialty"
                    type="text"
                    required
                    autoComplete="off"
                    value={specialty}
                    onChange={(e) => {
                      setSpecialty(e.target.value)
                      setShowSuggestions(true)
                    }}
                    onFocus={() => setShowSuggestions(true)}
                    placeholder="مثال: طب الأطفال، جلدية، عيون..."
                    className={inputClassName}
                  />
                  {showSuggestions && filteredSpecialtySuggestions.length > 0 && (
                    <ul
                      className="absolute z-20 top-full right-0 left-0 mt-1 max-h-48 overflow-y-auto rounded-xl border border-slate-200 bg-white py-1 shadow-lg"
                      role="listbox"
                    >
                      {filteredSpecialtySuggestions.map((s) => (
                        <li key={s} role="option">
                          <button
                            type="button"
                            className="w-full px-4 py-2.5 text-right text-sm text-slate-800 hover:bg-slate-50"
                            onMouseDown={(e) => {
                              e.preventDefault()
                              setSpecialty(s)
                              setShowSuggestions(false)
                            }}
                          >
                            {s}
                          </button>
                        </li>
                      ))}
                    </ul>
                  )}
                </div>
              </div>

              <div className="relative group">
                <label
                  htmlFor="reg-email"
                  className="absolute -top-2.5 right-3 px-1 bg-white text-xs font-medium text-slate-500 transition-colors group-focus-within:text-primary z-10"
                >
                  البريد الإلكتروني
                </label>
                <div className="relative">
                  <Mail className="absolute right-3 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400 pointer-events-none group-focus-within:text-primary transition-colors" />
                  <input
                    id="reg-email"
                    type="email"
                    autoComplete="email"
                    required
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    className={inputClassName}
                    placeholder="doctor@mawidplus.com"
                  />
                </div>
              </div>

              <div className="relative group">
                <label
                  htmlFor="reg-password"
                  className="absolute -top-2.5 right-3 px-1 bg-white text-xs font-medium text-slate-500 transition-colors group-focus-within:text-primary z-10"
                >
                  كلمة المرور
                </label>
                <div className="relative">
                  <Lock className="absolute right-3 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400 pointer-events-none group-focus-within:text-primary transition-colors" />
                  <input
                    id="reg-password"
                    type={showPassword ? 'text' : 'password'}
                    autoComplete="new-password"
                    required
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    className="w-full rounded-xl border border-slate-200 pr-11 pl-12 py-3.5 text-slate-900 transition-all duration-200 focus:outline-none focus:border-primary focus:ring-2 focus:ring-primary/25"
                  />
                  <button
                    type="button"
                    tabIndex={-1}
                    onClick={() => setShowPassword((v) => !v)}
                    className="absolute left-2 top-1/2 -translate-y-1/2 p-2 rounded-lg text-slate-400 hover:text-slate-600 hover:bg-slate-50 transition-colors"
                    aria-label={showPassword ? 'إخفاء كلمة المرور' : 'إظهار كلمة المرور'}
                  >
                    {showPassword ? <EyeOff className="w-5 h-5" /> : <Eye className="w-5 h-5" />}
                  </button>
                </div>
              </div>

              <div className="relative group">
                <label
                  htmlFor="password-confirm"
                  className="absolute -top-2.5 right-3 px-1 bg-white text-xs font-medium text-slate-500 transition-colors group-focus-within:text-primary z-10"
                >
                  تأكيد كلمة المرور
                </label>
                <div className="relative">
                  <Lock className="absolute right-3 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400 pointer-events-none group-focus-within:text-primary transition-colors" />
                  <input
                    id="password-confirm"
                    type={showPasswordConfirm ? 'text' : 'password'}
                    autoComplete="new-password"
                    required
                    value={passwordConfirm}
                    onChange={(e) => setPasswordConfirm(e.target.value)}
                    className="w-full rounded-xl border border-slate-200 pr-11 pl-12 py-3.5 text-slate-900 transition-all duration-200 focus:outline-none focus:border-primary focus:ring-2 focus:ring-primary/25"
                  />
                  <button
                    type="button"
                    tabIndex={-1}
                    onClick={() => setShowPasswordConfirm((v) => !v)}
                    className="absolute left-2 top-1/2 -translate-y-1/2 p-2 rounded-lg text-slate-400 hover:text-slate-600 hover:bg-slate-50 transition-colors"
                    aria-label={showPasswordConfirm ? 'إخفاء كلمة المرور' : 'إظهار كلمة المرور'}
                  >
                    {showPasswordConfirm ? <EyeOff className="w-5 h-5" /> : <Eye className="w-5 h-5" />}
                  </button>
                </div>
              </div>

              {error && (
                <div
                  className="rounded-lg bg-red-50 text-red-800 text-sm px-4 py-3 border-s-4 border-red-500 shadow-sm animate-error-slide"
                  role="alert"
                >
                  {error}
                </div>
              )}

              <button
                type="submit"
                disabled={submitting || loading}
                className="relative w-full py-3.5 rounded-xl font-bold text-white text-lg overflow-hidden transition-all duration-200 hover:scale-[1.02] hover:shadow-lg disabled:opacity-70 disabled:hover:scale-100 disabled:cursor-not-allowed bg-gradient-to-l from-[#1A73E8] to-[#0D47A1] shadow-md"
              >
                {submitting ? (
                  <span className="flex items-center justify-center gap-2">
                    <Loader2 className="w-5 h-5 animate-spin" />
                    جارٍ التحقق...
                  </span>
                ) : (
                  'إنشاء الحساب'
                )}
              </button>
            </form>
          )}

          <p className="text-center text-sm text-slate-600">
            لديك حساب بالفعل؟{' '}
            <Link to="/login" className="text-primary font-medium hover:underline">
              تسجيل الدخول
            </Link>
          </p>
        </div>
      </section>
    </div>
  )
}
