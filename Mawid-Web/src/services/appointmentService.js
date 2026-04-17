import { format } from 'date-fns'
import { supabase } from '../lib/supabaseClient'

/** Matches PostgREST default max rows; keeps doctor queries predictable. */
const MAX_APPOINTMENT_ROWS = 1000

/** Arabic week: Saturday → Friday */
export function getWeekRange() {
  const today = new Date()
  const dayOfWeek = today.getDay()
  const daysFromSaturday = (dayOfWeek + 1) % 7
  const weekStart = new Date(today)
  weekStart.setDate(today.getDate() - daysFromSaturday)
  weekStart.setHours(0, 0, 0, 0)
  const weekEnd = new Date(weekStart)
  weekEnd.setDate(weekStart.getDate() + 6)
  return { weekStart, weekEnd }
}

/** Normalize legacy Android status */
export function normalizeStatus(s) {
  if (!s) return 'waiting'
  if (s === 'scheduled') return 'waiting'
  return s
}

async function mapProfiles(appointments) {
  const ids = [...new Set((appointments || []).map((a) => a.patient_id).filter(Boolean))]
  if (!ids.length) return {}
  const { data, error } = await supabase.from('profiles').select('id, full_name, phone').in('id', ids)
  if (error) throw error
  return Object.fromEntries((data || []).map((p) => [p.id, p]))
}

/** Today's appointments only (local calendar date). */
export async function getTodayAppointments(doctorId) {
  const today = format(new Date(), 'yyyy-MM-dd')
  return fetchAppointmentsForDoctor(doctorId, { date: today, status: 'all', search: '' })
}

export async function getWeekAppointments(doctorId, { status, search } = {}) {
  const { weekStart, weekEnd } = getWeekRange()
  const startStr = format(weekStart, 'yyyy-MM-dd')
  const endStr = format(weekEnd, 'yyyy-MM-dd')
  const { data: apps, error } = await supabase
    .from('appointments')
    .select(
      'id, patient_id, doctor_id, queue_number, status, appointment_date, created_at, notes, doctor_notes, time_slot',
    )
    .eq('doctor_id', doctorId)
    .gte('appointment_date', startStr)
    .lte('appointment_date', endStr)
    .order('appointment_date', { ascending: true })
    .limit(MAX_APPOINTMENT_ROWS)
  if (error) throw error

  const pmap = await mapProfiles(apps)

  let rows = (apps || []).map((row) => ({
    ...row,
    status: normalizeStatus(row.status),
    patient_name: pmap[row.patient_id]?.full_name ?? '—',
    patient_phone: pmap[row.patient_id]?.phone ?? '',
  }))

  rows.sort((a, b) => {
    const da = a.appointment_date.localeCompare(b.appointment_date)
    if (da !== 0) return da
    return (a.queue_number ?? 0) - (b.queue_number ?? 0)
  })

  if (status && status !== 'all') {
    rows = rows.filter((r) => r.status === status)
  }
  if (search?.trim()) {
    const t = search.trim().toLowerCase()
    rows = rows.filter((r) => (r.patient_name || '').toLowerCase().includes(t))
  }

  return rows
}

export async function fetchAppointmentsForDoctor(doctorId, { date, status, search } = {}) {
  let q = supabase
    .from('appointments')
    .select(
      'id, patient_id, doctor_id, queue_number, status, appointment_date, created_at, notes, doctor_notes, time_slot',
    )
    .eq('doctor_id', doctorId)
    .order('queue_number', { ascending: true })

  if (date) q = q.eq('appointment_date', date)
  q = q.limit(MAX_APPOINTMENT_ROWS)

  const { data: apps, error } = await q
  if (error) throw error

  const pmap = await mapProfiles(apps)

  let rows = (apps || []).map((row) => ({
    ...row,
    status: normalizeStatus(row.status),
    patient_name: pmap[row.patient_id]?.full_name ?? '—',
    patient_phone: pmap[row.patient_id]?.phone ?? '',
  }))

  if (status && status !== 'all') {
    rows = rows.filter((r) => r.status === status)
  }
  if (search?.trim()) {
    const t = search.trim().toLowerCase()
    rows = rows.filter((r) => (r.patient_name || '').toLowerCase().includes(t))
  }

  return rows
}

/**
 * إحصائيات يوم محدد (نفس تاريخ التقويم المحلي عبر isoDate من date-fns).
 * الإلغاء يُحسب من حقل status الخام أولاً (cancelled / canceled) حتى لا يختلط مع normalizeStatus.
 */
export async function fetchAppointmentStatsForDay(doctorId, isoDate) {
  const { data, error } = await supabase
    .from('appointments')
    .select('status')
    .eq('doctor_id', doctorId)
    .eq('appointment_date', isoDate)
  if (error) throw error

  const counts = { today: 0, waiting: 0, in_progress: 0, done: 0, cancelled: 0 }
  for (const row of data || []) {
    counts.today += 1
    const raw = (row.status ?? '').toString().trim().toLowerCase()
    if (raw === 'cancelled' || raw === 'canceled') {
      counts.cancelled += 1
      continue
    }
    const s = normalizeStatus(row.status)
    if (s === 'waiting') counts.waiting += 1
    else if (s === 'in_progress') counts.in_progress += 1
    else if (s === 'done') counts.done += 1
  }
  return counts
}

/** اسم بديل واضح لاستدعاء إحصائيات اليوم الحالي */
export async function getTodayStats(doctorId) {
  const today = format(new Date(), 'yyyy-MM-dd')
  return fetchAppointmentStatsForDay(doctorId, today)
}

export async function fetchAppointmentsLast7Days(doctorId) {
  const end = new Date()
  const start = new Date(end)
  start.setDate(start.getDate() - 6)
  const startStr = format(start, 'yyyy-MM-dd')
  const endStr = format(end, 'yyyy-MM-dd')
  const { data, error } = await supabase
    .from('appointments')
    .select('appointment_date')
    .eq('doctor_id', doctorId)
    .gte('appointment_date', startStr)
    .lte('appointment_date', endStr)
  if (error) throw error

  const byDay = {}
  for (const row of data || []) {
    const d = row.appointment_date
    byDay[d] = (byDay[d] || 0) + 1
  }
  return byDay
}

export async function updateAppointment(appointmentId, patch) {
  const { data, error } = await supabase
    .from('appointments')
    .update(patch)
    .eq('id', appointmentId)
    .select()
    .maybeSingle()
  if (error) throw error
  return data
}

/** إنهاء الموعد مع ملاحظات للمريض (عمود doctor_notes). */
export async function completeAppointment(appointmentId, notes) {
  const { error } = await supabase
    .from('appointments')
    .update({
      status: 'done',
      doctor_notes: notes ?? null,
    })
    .eq('id', appointmentId)
  if (error) throw error
}

/**
 * Each caller must use a unique `scope` (e.g. 'list', 'queue'). Supabase reuses
 * channels by name — two hooks with the same name would call `.on()` after `subscribe()`.
 */
export function subscribeToAppointments(doctorId, callback, scope = 'default') {
  const channel = supabase
    .channel(`appointments-doctor-${doctorId}-${scope}`)
    .on(
      'postgres_changes',
      { event: '*', schema: 'public', table: 'appointments', filter: `doctor_id=eq.${doctorId}` },
      (payload) => {
        console.log('Realtime: new appointment received', payload)
        callback(payload)
      }
    )
    .subscribe()

  return () => {
    supabase.removeChannel(channel)
  }
}

/** INSERT + UPDATE on appointments for this doctor (notifications UI). */
export function subscribeAppointmentNotifications(doctorId, onPayload) {
  const channel = supabase
    .channel(`appointments-notify-${doctorId}`)
    .on(
      'postgres_changes',
      {
        event: 'INSERT',
        schema: 'public',
        table: 'appointments',
        filter: `doctor_id=eq.${doctorId}`,
      },
      onPayload
    )
    .on(
      'postgres_changes',
      {
        event: 'UPDATE',
        schema: 'public',
        table: 'appointments',
        filter: `doctor_id=eq.${doctorId}`,
      },
      onPayload
    )
    .subscribe()

  return () => {
    supabase.removeChannel(channel)
  }
}
