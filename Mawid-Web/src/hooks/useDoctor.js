import { useEffect, useState, useCallback } from 'react'
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

  return { doctor, loading, error, refresh }
}
