import { format } from 'date-fns'
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { getWeekRange, normalizeStatus } from './appointmentService'

describe('normalizeStatus', () => {
  it('maps scheduled to waiting', () => {
    expect(normalizeStatus('scheduled')).toBe('waiting')
  })

  it('passes through known statuses', () => {
    expect(normalizeStatus('waiting')).toBe('waiting')
    expect(normalizeStatus('done')).toBe('done')
  })

  it('defaults empty to waiting', () => {
    expect(normalizeStatus('')).toBe('waiting')
    expect(normalizeStatus(null)).toBe('waiting')
  })
})

describe('getWeekRange', () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('returns Saturday–Friday window for a fixed Thursday', () => {
    // 2025-04-10 is Thursday (local calendar)
    vi.setSystemTime(new Date(2025, 3, 10, 12, 0, 0))
    const { weekStart, weekEnd } = getWeekRange()
    expect(format(weekStart, 'yyyy-MM-dd')).toBe('2025-04-05')
    expect(format(weekEnd, 'yyyy-MM-dd')).toBe('2025-04-11')
  })
})
