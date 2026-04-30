import { format, parseISO, isValid } from 'date-fns'
import { ar } from 'date-fns/locale'

export function formatArabicAppointmentDate(dateStr) {
  if (!dateStr) return '—'
  const d = parseISO(`${dateStr}T12:00:00`)
  return isValid(d) ? format(d, 'EEEE d MMMM', { locale: ar }) : dateStr
}
