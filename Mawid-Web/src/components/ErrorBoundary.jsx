import { Component } from 'react'

export default class ErrorBoundary extends Component {
  constructor(props) {
    super(props)
    this.state = { hasError: false, error: null }
  }

  static getDerivedStateFromError(error) {
    return { hasError: true, error }
  }

  componentDidCatch(error, errorInfo) {
    console.error('ErrorBoundary:', error, errorInfo)
  }

  render() {
    if (this.state.hasError) {
      return (
        <div
          className="min-h-screen flex flex-col items-center justify-center gap-4 p-6 bg-slate-50 text-slate-800"
          dir="rtl"
        >
          <h1 className="text-xl font-semibold">حدث خطأ غير متوقع</h1>
          <p className="text-sm text-slate-600 text-center max-w-md">
            حدّث الصفحة أو جرّب لاحقاً. إذا تكرر الخطأ، راجع وحدة التحكم في المتصفح (F12).
          </p>
          <button
            type="button"
            className="rounded-lg bg-[#1A73E8] px-4 py-2 text-white text-sm"
            onClick={() => window.location.reload()}
          >
            تحديث الصفحة
          </button>
        </div>
      )
    }
    return this.props.children
  }
}
