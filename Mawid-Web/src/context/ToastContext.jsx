import { createContext, useCallback, useContext, useMemo, useRef, useState } from 'react'

const ToastContext = createContext(null)

export function ToastProvider({ children }) {
  const [toast, setToast] = useState(null)
  const hideTimer = useRef(null)

  const showToast = useCallback((message, type = 'success') => {
    if (hideTimer.current) window.clearTimeout(hideTimer.current)
    setToast({ id: Date.now(), message, type })
    hideTimer.current = window.setTimeout(() => setToast(null), 4200)
  }, [])

  const value = useMemo(() => ({ showToast }), [showToast])

  return (
    <ToastContext.Provider value={value}>
      {children}
      {toast && (
        <div
          className="fixed top-4 left-1/2 -translate-x-1/2 z-[100] max-w-md w-[calc(100%-2rem)] pointer-events-none"
          dir="rtl"
          role="status"
        >
          <div
            className={`pointer-events-auto rounded-xl px-4 py-3 shadow-xl border text-sm font-semibold animate-toast-slide ${
              toast.type === 'error'
                ? 'bg-red-50 text-red-900 border-red-200'
                : 'bg-emerald-50 text-emerald-900 border-emerald-200'
            }`}
          >
            {toast.message}
          </div>
        </div>
      )}
    </ToastContext.Provider>
  )
}

export function useToast() {
  const ctx = useContext(ToastContext)
  if (!ctx) return { showToast: () => {} }
  return ctx
}
