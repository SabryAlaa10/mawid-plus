import { useEffect, useState, useCallback } from 'react'
import { format } from 'date-fns'
import * as appointmentService from '../services/appointmentService'

/**
 * @param {'today'|'week'|'custom'} viewMode
 */
export function useAppointments(doctorId, date, status, search, viewMode = 'custom') {
  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const load = useCallback(async () => {
    if (!doctorId) {
      setRows([])
      setLoading(false)
      return
    }
    setLoading(true)
    setError(null)
    try {
      let data
      if (viewMode === 'today') {
        const today = format(new Date(), 'yyyy-MM-dd')
        data = await appointmentService.fetchAppointmentsForDoctor(doctorId, {
          date: today,
          status,
          search,
        })
      } else if (viewMode === 'week') {
        data = await appointmentService.getWeekAppointments(doctorId, { status, search })
      } else {
        data = await appointmentService.fetchAppointmentsForDoctor(doctorId, { date, status, search })
      }
      setRows(data)
    } catch (e) {
      setError(e.message || 'خطأ في المواعيد')
      setRows([])
    } finally {
      setLoading(false)
    }
  }, [doctorId, date, status, search, viewMode])

  useEffect(() => {
    load()
  }, [load])

  useEffect(() => {
    if (!doctorId) return undefined
    const unsub = appointmentService.subscribeToAppointments(
      doctorId,
      () => {
        load()
      },
      'list'
    )
    return unsub
  }, [doctorId, load])

  return { rows, loading, error, refresh: load }
}
