import { format } from 'date-fns'
import { supabase } from '../lib/supabaseClient'

export async function getQueueSettings(doctorId) {
  const { data, error } = await supabase
    .from('queue_settings')
    .select('doctor_id, current_number, queue_date, is_open, updated_at')
    .eq('doctor_id', doctorId)
    .maybeSingle()
  if (error) throw error
  return data
}

export async function updateCurrentNumber(doctorId, newNumber) {
  const { data, error } = await supabase
    .from('queue_settings')
    .update({ current_number: newNumber, updated_at: new Date().toISOString() })
    .eq('doctor_id', doctorId)
    .select()
    .maybeSingle()
  if (error) throw error
  return data
}

/**
 * أقصى رقم طابور اليوم — يشمل الملغاة ليتوافق مع تطبيق أندرويد (max على كل الصفوف)
 * حتى لا يُعاد استخدام رقم ما زال موجوداً في صف ملغى.
 */
export async function getMaxQueueNumber(doctorId) {
  const todayIso = format(new Date(), 'yyyy-MM-dd')
  const { data, error } = await supabase
    .from('appointments')
    .select('queue_number')
    .eq('doctor_id', doctorId)
    .eq('appointment_date', todayIso)
    .order('queue_number', { ascending: false })
    .limit(1)
  if (error) return 0
  return data?.[0]?.queue_number ?? 0
}

/**
 * يستدعي أول مريض في الانتظار برقم طابور أكبر من الرقم الحالي فقط (لا يزيد العداد بدون موعد).
 * يتخطى الفجوات (مثلاً موعد ملغى في المنتصف).
 */
export async function callNextPatient(doctorId) {
  const todayIso = format(new Date(), 'yyyy-MM-dd')

  const row = await getQueueSettings(doctorId)
  if (!row) {
    return {
      success: false,
      message: 'إعدادات الطابور غير متوفرة',
      currentNumber: 0,
    }
  }

  const currentNumber = row.current_number ?? 0

  const { data: rows, error: fetchErr } = await supabase
    .from('appointments')
    .select('id, queue_number, status')
    .eq('doctor_id', doctorId)
    .eq('appointment_date', todayIso)
    .neq('status', 'cancelled')
    .order('queue_number', { ascending: true })

  if (fetchErr) throw fetchErr

  const waiting = (rows || []).filter((a) => a.status === 'waiting' || a.status === 'scheduled')

  const nextPatient = waiting.find((a) => (a.queue_number ?? 0) > currentNumber)

  if (!nextPatient) {
    const noWaiting = waiting.length === 0
    return {
      success: false,
      message: noWaiting ? 'لا يوجد مرضى في قائمة الانتظار' : 'لا يوجد مريض تالٍ في الطابور',
      currentNumber,
    }
  }

  const nextNum = nextPatient.queue_number

  if (currentNumber > 0) {
    await supabase
      .from('appointments')
      .update({ status: 'done' })
      .eq('doctor_id', doctorId)
      .eq('appointment_date', todayIso)
      .eq('queue_number', currentNumber)
      .eq('status', 'in_progress')
  }

  const { error: upNextErr } = await supabase.from('appointments').update({ status: 'in_progress' }).eq('id', nextPatient.id)

  if (upNextErr) throw upNextErr

  const { error: updateErr } = await supabase
    .from('queue_settings')
    .update({ current_number: nextNum, updated_at: new Date().toISOString() })
    .eq('doctor_id', doctorId)

  if (updateErr) throw updateErr

  return {
    success: true,
    message: `جارٍ استدعاء المريض رقم #${nextNum}`,
    currentNumber: nextNum,
    patientId: nextPatient.id,
  }
}

export async function resetQueue(doctorId) {
  return updateCurrentNumber(doctorId, 0)
}

export async function toggleQueueOpen(doctorId, isOpen) {
  const { data, error } = await supabase
    .from('queue_settings')
    .update({ is_open: isOpen, updated_at: new Date().toISOString() })
    .eq('doctor_id', doctorId)
    .select('doctor_id, current_number, queue_date, is_open, updated_at')
    .maybeSingle()
  if (error) throw error
  return data
}

export async function fetchWaitingAppointments(doctorId) {
  const todayIso = format(new Date(), 'yyyy-MM-dd')
  const { data: apps, error } = await supabase
    .from('appointments')
    .select('id, queue_number, status, appointment_date, created_at, patient_id')
    .eq('doctor_id', doctorId)
    .eq('appointment_date', todayIso)
    .order('queue_number', { ascending: true })
  if (error) throw error

  const filtered = (apps || []).filter((a) => a.status === 'waiting' || a.status === 'scheduled')
  const ids = [...new Set(filtered.map((a) => a.patient_id))]
  let pmap = {}
  if (ids.length) {
    const { data: profs, error: e2 } = await supabase.from('profiles').select('id, full_name').in('id', ids)
    if (e2) throw e2
    pmap = Object.fromEntries((profs || []).map((p) => [p.id, p]))
  }

  return filtered.map((r) => ({
    ...r,
    status: r.status === 'scheduled' ? 'waiting' : r.status,
    patient_name: pmap[r.patient_id]?.full_name ?? '—',
  }))
}

export function subscribeToQueue(doctorId, callback) {
  const channel = supabase
    .channel(`queue-doctor-${doctorId}`)
    .on(
      'postgres_changes',
      { event: '*', schema: 'public', table: 'queue_settings', filter: `doctor_id=eq.${doctorId}` },
      (payload) => {
        console.log('Realtime: queue updated', payload)
        callback(payload)
      }
    )
    .subscribe()

  return () => {
    supabase.removeChannel(channel)
  }
}
