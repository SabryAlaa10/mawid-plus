import { useState, useEffect, useMemo, useCallback, useRef } from 'react'
import { supabase } from '../lib/supabaseClient'
import { AR_LOCALE } from '../constants/region'

function formatArabicDate(dateStr) {
  if (!dateStr) return ''
  try {
    const d = new Date(dateStr.includes('T') ? dateStr : `${dateStr}T12:00:00`)
    return new Intl.DateTimeFormat(AR_LOCALE, {
      weekday: 'long',
      day: 'numeric',
      month: 'long',
    }).format(d)
  } catch {
    return String(dateStr)
  }
}

function sameDoctorId(a, b) {
  if (a == null || b == null) return false
  return String(a).replace(/-/g, '').toLowerCase() === String(b).replace(/-/g, '').toLowerCase()
}

async function fetchPatientName(patientId) {
  if (!patientId) return 'مريض جديد'
  const { data } = await supabase.from('profiles').select('full_name').eq('id', patientId).maybeSingle()
  return data?.full_name?.trim() || 'مريض جديد'
}

/**
 * إشعارات لوحة الطبيب (في الذاكرة فقط — تُصفّر عند تحديث الصفحة).
 * Realtime: INSERT موعد جديد، UPDATE → إلغاء أو تقييم جديد (patient_rating).
 *
 * لا نستخدم filter على القناة؛ نعتمد على RLS (الطبيب يرى مواعيده فقط) ثم نتحقق من doctorId محلياً.
 * تجنّباً لمشاكل تطابق UUID أو تأخير «أول تحميل» الذي كان يُسقط أحداث الحجز المبكرة.
 * يُستبعد تكرار إشعار التقييم لنفس الموعد (تحديثات لاحقة مثل الملاحظات) عبر Set في الذاكرة.
 */
export function useNotifications(doctorId) {
  const [notifications, setNotifications] = useState([])
  const ratingNotifiedApptIdsRef = useRef(new Set())

  useEffect(() => {
    ratingNotifiedApptIdsRef.current = new Set()
  }, [doctorId])

  const unreadCount = useMemo(
    () => notifications.filter((n) => !n.read).length,
    [notifications]
  )

  useEffect(() => {
    if (!doctorId) return undefined

    const channel = supabase
      .channel(`doctor_notifications:${doctorId}`)
      .on(
        'postgres_changes',
        {
          event: 'INSERT',
          schema: 'public',
          table: 'appointments',
        },
        async (payload) => {
          try {
            const appt = payload.new
            if (!appt?.id || !sameDoctorId(appt.doctor_id, doctorId)) return

            const patientName = await fetchPatientName(appt.patient_id)
            const formattedDate = formatArabicDate(appt.appointment_date)
            const q = appt.queue_number ?? '—'

            const newNotif = {
              id: appt.id,
              type: 'new_booking',
              title: 'حجز موعد جديد',
              message: `${patientName} — رقم #${q} — ${formattedDate}`,
              appointmentId: appt.id,
              appointmentDate: appt.appointment_date,
              queueNumber: appt.queue_number,
              patientName,
              timestamp: new Date(),
              read: false,
            }

            setNotifications((prev) => [newNotif, ...prev].slice(0, 100))
          } catch (e) {
            console.warn('[useNotifications] INSERT handler', e)
          }
        }
      )
      .on(
        'postgres_changes',
        {
          event: 'UPDATE',
          schema: 'public',
          table: 'appointments',
        },
        async (payload) => {
          try {
            const appt = payload.new
            const old = payload.old
            if (!appt?.id || !sameDoctorId(appt.doctor_id, doctorId)) return

            const nowCancelled = appt.status === 'cancelled' || appt.status === 'canceled'
            const wasCancelled = old?.status === 'cancelled' || old?.status === 'canceled'
            if (nowCancelled && !wasCancelled) {
              const cancelNotif = {
                id: `cancel_${appt.id}_${Date.now()}`,
                type: 'cancellation',
                title: 'إلغاء موعد',
                message: `تم إلغاء الموعد رقم #${appt.queue_number ?? '—'}`,
                appointmentId: appt.id,
                appointmentDate: appt.appointment_date,
                timestamp: new Date(),
                read: false,
              }
              setNotifications((prev) => [cancelNotif, ...prev].slice(0, 100))
              return
            }

            const prevRating = old?.patient_rating
            if (prevRating != null && prevRating !== '') return

            const rawStars = appt.patient_rating
            const stars = rawStars != null && rawStars !== '' ? Number(rawStars) : NaN
            if (!Number.isFinite(stars) || stars < 1 || stars > 5) return

            if (ratingNotifiedApptIdsRef.current.has(appt.id)) return
            ratingNotifiedApptIdsRef.current.add(appt.id)

            const patientName = await fetchPatientName(appt.patient_id)
            const formattedDate = formatArabicDate(appt.appointment_date)
            const q = appt.queue_number ?? '—'

            const ratingNotif = {
              id: `rating_${appt.id}_${Date.now()}`,
              type: 'new_rating',
              title: 'تقييم جديد من مريض',
              message: `${patientName} قيّمك بـ ${stars} من 5 — دور #${q} — ${formattedDate}`,
              appointmentId: appt.id,
              appointmentDate: appt.appointment_date,
              queueNumber: appt.queue_number,
              patientName,
              stars,
              timestamp: new Date(),
              read: false,
            }
            setNotifications((prev) => [ratingNotif, ...prev].slice(0, 100))
          } catch (e) {
            console.warn('[useNotifications] UPDATE handler', e)
          }
        }
      )
      .subscribe((status, err) => {
        if (status === 'CHANNEL_ERROR' || status === 'TIMED_OUT') {
          console.warn('[useNotifications] channel status:', status, err)
        }
      })

    return () => {
      supabase.removeChannel(channel)
    }
  }, [doctorId])

  const markAllRead = useCallback(() => {
    setNotifications((prev) => prev.map((n) => ({ ...n, read: true })))
  }, [])

  const markOneRead = useCallback((id) => {
    setNotifications((prev) =>
      prev.map((n) => (n.id === id ? { ...n, read: true } : n))
    )
  }, [])

  const clearAll = useCallback(() => {
    setNotifications([])
  }, [])

  const timeAgo = useCallback((timestamp) => {
    const d = timestamp instanceof Date ? timestamp : new Date(timestamp)
    const diff = Math.floor((Date.now() - d.getTime()) / 1000)
    if (diff < 60) return 'الآن'
    if (diff < 3600) return `منذ ${Math.floor(diff / 60)} دقيقة`
    if (diff < 86400) return `منذ ${Math.floor(diff / 3600)} ساعة`
    return `منذ ${Math.floor(diff / 86400)} يوم`
  }, [])

  return {
    notifications,
    unreadCount,
    markAllRead,
    markOneRead,
    clearAll,
    timeAgo,
  }
}
