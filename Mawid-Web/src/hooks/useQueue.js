import { useEffect, useState, useCallback, useRef, useMemo } from 'react'
import * as queueService from '../services/queueService'
import * as appointmentService from '../services/appointmentService'

export function useQueue(doctorId) {
  const [queue, setQueue] = useState(null)
  const [waitingList, setWaitingList] = useState([])
  const waitingListRef = useRef([])
  const queueRef = useRef(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [maxQueueNumber, setMaxQueueNumber] = useState(0)
  const [flash, setFlash] = useState(null)

  useEffect(() => {
    waitingListRef.current = waitingList
  }, [waitingList])

  useEffect(() => {
    queueRef.current = queue
  }, [queue])

  const load = useCallback(async () => {
    if (!doctorId) {
      setQueue(null)
      setWaitingList([])
      setMaxQueueNumber(0)
      setLoading(false)
      return
    }
    setLoading(true)
    setError(null)
    try {
      const [q, w, maxQ] = await Promise.all([
        queueService.getQueueSettings(doctorId),
        queueService.fetchWaitingAppointments(doctorId),
        queueService.getMaxQueueNumber(doctorId),
      ])
      setQueue(q)
      setWaitingList(w)
      setMaxQueueNumber(maxQ)
    } catch (e) {
      setError(e.message || 'خطأ في الطابور')
    } finally {
      setLoading(false)
    }
  }, [doctorId])

  useEffect(() => {
    load()
  }, [load])

  useEffect(() => {
    if (!doctorId) return undefined
    const unsubQ = queueService.subscribeToQueue(doctorId, () => {
      load()
    })
    const unsubA = appointmentService.subscribeToAppointments(
      doctorId,
      () => {
        load()
      },
      'queue'
    )
    return () => {
      unsubQ()
      unsubA()
    }
  }, [doctorId, load])

  const currentNumber = queue?.current_number ?? 0

  const canCallNext = useMemo(
    () => waitingList.some((p) => (p.queue_number ?? 0) > currentNumber),
    [waitingList, currentNumber]
  )

  const callNext = useCallback(async () => {
    if (!doctorId) return
    const cur = queueRef.current?.current_number ?? 0
    if (!waitingListRef.current.some((p) => (p.queue_number ?? 0) > cur)) return
    try {
      const result = await queueService.callNextPatient(doctorId)
      if (!result.success) {
        setFlash({ type: 'error', text: result.message })
        window.setTimeout(() => setFlash((f) => (f?.text === result.message ? null : f)), 3000)
        return
      }
      setFlash({ type: 'success', text: result.message })
      window.setTimeout(() => setFlash((f) => (f?.text === result.message ? null : f)), 2000)
      await load()
    } catch {
      setFlash({ type: 'error', text: 'حدث خطأ، حاول مرة أخرى' })
      window.setTimeout(() => setFlash(null), 3000)
    }
  }, [doctorId, load])

  const reset = useCallback(async () => {
    if (!doctorId) return
    await queueService.resetQueue(doctorId)
    await load()
  }, [doctorId, load])

  const toggle = useCallback(
    async (isOpen) => {
      if (!doctorId) return
      await queueService.toggleQueueOpen(doctorId, isOpen)
      await load()
    },
    [doctorId, load]
  )

  return {
    currentNumber,
    isOpen: queue?.is_open ?? false,
    queueDate: queue?.queue_date,
    waitingList,
    maxQueueNumber,
    canCallNext,
    flash,
    loading,
    error,
    refresh: load,
    callNext,
    reset,
    toggle,
  }
}
