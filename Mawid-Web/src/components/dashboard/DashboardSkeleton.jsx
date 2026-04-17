export default function DashboardSkeleton() {
  return (
    <div className="space-y-8 max-w-[1600px] mx-auto animate-fade-in-up [animation-fill-mode:forwards]" dir="rtl">
      <div className="space-y-2">
        <div className="h-9 w-48 rounded-lg skeleton-shimmer" />
        <div className="h-4 w-72 rounded-md skeleton-shimmer opacity-80" />
      </div>
      <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4 md:gap-5">
        {[0, 1, 2, 3].map((i) => (
          <div
            key={i}
            className="dashboard-card p-5 h-[120px] skeleton-shimmer opacity-0 animate-fade-in-up [animation-fill-mode:forwards]"
            style={{ animationDelay: `${i * 100}ms` }}
          />
        ))}
      </div>
      <div className="dashboard-card h-56 skeleton-shimmer" />
      <div className="dashboard-card h-80 skeleton-shimmer" />
    </div>
  )
}
