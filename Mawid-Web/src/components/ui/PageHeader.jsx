import { useEffect, useState } from 'react'

/** Title + rotating Arabic subtitles + fade-in. */
export default function PageHeader({ title, subtitles = [] }) {
  const [i, setI] = useState(0)
  const lines = subtitles.filter(Boolean)

  useEffect(() => {
    if (lines.length <= 1) return undefined
    const t = window.setInterval(() => setI((x) => (x + 1) % lines.length), 4500)
    return () => window.clearInterval(t)
  }, [lines.length])

  const line = lines.length ? lines[i] : null

  return (
    <div className="opacity-0 animate-fade-in-up [animation-fill-mode:forwards] mb-2">
      <h1 className="text-3xl font-black tracking-tight text-slate-900">{title}</h1>
      {line && (
        <p
          key={line}
          className="text-slate-500 text-sm mt-2 font-medium min-h-[1.25rem] animate-subtitle-pulse"
        >
          {line}
        </p>
      )}
    </div>
  )
}
