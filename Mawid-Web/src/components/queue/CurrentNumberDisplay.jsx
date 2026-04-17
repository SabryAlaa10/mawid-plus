export default function CurrentNumberDisplay({ currentNumber, isOpen }) {
  return (
    <div className="text-center py-6 relative" dir="rtl">
      <div
        className="pointer-events-none absolute inset-x-0 top-1/2 -translate-y-1/2 h-24 mx-auto max-w-[200px] bg-primary/15 blur-3xl rounded-full"
        aria-hidden
      />
      <p className="text-slate-500 text-sm font-semibold mb-2 relative z-10">يتم الآن خدمة</p>
      <div className="relative inline-block min-w-[4ch]">
        {isOpen && (
          <span
            className="absolute inset-0 -m-4 rounded-[2rem] bg-primary/30 blur-3xl animate-pulse pointer-events-none"
            aria-hidden
          />
        )}
        <p className="relative z-10 text-6xl md:text-7xl font-black text-transparent bg-clip-text bg-gradient-to-l from-[#1A73E8] to-[#0D47A1] tabular-nums drop-shadow-sm">
          #{currentNumber}
        </p>
      </div>
    </div>
  )
}
