import { useState, useEffect, useRef } from 'react'

/** Counts from 0 to `target` over `durationMs` when `active` is true. */
export function useCountUp(target, { durationMs = 900, active = true } = {}) {
  const [value, setValue] = useState(0)
  const startRef = useRef(null)
  const fromRef = useRef(0)

  useEffect(() => {
    if (!active || target == null || Number.isNaN(Number(target))) {
      setValue(0)
      return
    }
    const end = Math.max(0, Math.floor(Number(target)))
    if (end === 0) {
      setValue(0)
      return
    }
    fromRef.current = 0
    startRef.current = null
    let frame

    const ease = (t) => 1 - (1 - t) ** 3

    const step = (now) => {
      if (startRef.current == null) startRef.current = now
      const elapsed = now - startRef.current
      const t = Math.min(1, elapsed / durationMs)
      setValue(Math.round(ease(t) * end))
      if (t < 1) frame = requestAnimationFrame(step)
    }

    frame = requestAnimationFrame(step)
    return () => cancelAnimationFrame(frame)
  }, [target, durationMs, active])

  return value
}
