import { useEffect, useState, useCallback } from 'react'
import { supabase } from '../lib/supabaseClient'
import * as doctorService from '../services/doctorService'

export function useDoctor(userId) {
  const [doctor, setDoctor] = useState(null)
  const [loading, setLoading] = useState(!!userId)
  const [error, setError] = useState(null)

  const refresh = useCallback(async () => {
    if (!userId) {
      setDoctor(null)
      setLoading(false)
      return
    }
    setLoading(true)
    setError(null)
    try {
      const row = await doctorService.getDoctorProfile(userId)
      setDoctor(row)
      if (!row) setError('لم يُعثر على ملف طبيب مرتبط بهذا الحساب. تأكد من ربط auth.users.id مع doctors.id في Supabase.')
    } catch (e) {
      setError(e.message || 'تعذر تحميل بيانات الطبيب')
      setDoctor(null)
    } finally {
      setLoading(false)
    }
  }, [userId])

  useEffect(() => {
    refresh()
  }, [refresh])

  /** عند تحديث صف الطبيب (مثلاً بعد تقييم مريض) نحدّث المتوسط والعداد بدون إعادة تحميل الصفحة. */
  useEffect(() => {
    if (!userId) return undefined
    const channel = supabase
      .channel(`doctor_profile:${userId}`)
      .on(
        'postgres_changes',
        {
          event: 'UPDATE',
          schema: 'public',
          table: 'doctors',
          filter: `id=eq.${userId}`,
        },
        () => {
          refresh()
        },
      )
      .subscribe((status, err) => {
        if (status === 'CHANNEL_ERROR' || status === 'TIMED_OUT') {
          console.warn('[useDoctor] doctors realtime:', status, err)
        }
      })

    return () => {
      supabase.removeChannel(channel)
    }
  }, [userId, refresh])

  return { doctor, loading, error, refresh }
}
