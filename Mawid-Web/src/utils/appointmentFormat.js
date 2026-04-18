import { format, parseISO, isValid } from 'date-fns'
import { arEG } from 'date-fns/locale/ar-EG'

export function formatArabicAppointmentDate(dateStr) {
  if (!dateStr) return '—'
  const d = parseISO(`${dateStr}T12:00:00`)
  return isValid(d) ? format(d, 'EEEE d MMMM', { locale: arEG }) : dateStr
}
