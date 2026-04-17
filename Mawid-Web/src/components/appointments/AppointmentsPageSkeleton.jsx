export default function AppointmentsPageSkeleton() {
  return (
    <div className="space-y-6 max-w-[1600px] mx-auto" dir="rtl">
      <div className="space-y-2">
        <div className="h-9 w-40 rounded-lg skeleton-shimmer" />
        <div className="h-4 w-64 rounded-md skeleton-shimmer" />
      </div>
      <div className="dashboard-card p-5 h-24 skeleton-shimmer" />
      <div className="dashboard-card min-h-[320px] skeleton-shimmer" />
    </div>
  )
}
