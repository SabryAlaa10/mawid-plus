/** Floating gradient blobs — decorative only (matches login depth). */
export default function DashboardDecor() {
  return (
    <div className="pointer-events-none fixed inset-0 overflow-hidden z-0" aria-hidden>
      <div className="absolute -top-32 -right-24 w-[420px] h-[420px] rounded-full bg-primary/[0.07] blur-3xl animate-float-blob" />
      <div
        className="absolute top-1/3 -left-32 w-[360px] h-[360px] rounded-full bg-slate-400/[0.06] blur-3xl animate-float-slow"
        style={{ animationDelay: '2s' }}
      />
      <div
        className="absolute bottom-0 right-1/4 w-[280px] h-[280px] rounded-full bg-[#0D47A1]/[0.05] blur-3xl animate-float"
        style={{ animationDelay: '1s' }}
      />
    </div>
  )
}
