import html2canvas from 'html2canvas'
import { jsPDF } from 'jspdf'

/**
 * Renders a DOM node to a single-page A4 PDF (scaled to fit).
 * @param {HTMLElement} element
 * @returns {Promise<Blob>}
 */
export async function elementToPdfBlob(element) {
  const canvas = await html2canvas(element, {
    scale: 2,
    useCORS: true,
    logging: false,
    backgroundColor: '#ffffff',
  })

  const imgData = canvas.toDataURL('image/png')
  const pdf = new jsPDF({ orientation: 'p', unit: 'mm', format: 'a4' })
  const pageW = pdf.internal.pageSize.getWidth()
  const pageH = pdf.internal.pageSize.getHeight()
  const margin = 10
  const maxW = pageW - margin * 2
  const maxH = pageH - margin * 2
  let w = maxW
  let h = (canvas.height * w) / canvas.width
  if (h > maxH) {
    h = maxH
    w = (canvas.width * h) / canvas.height
  }
  const x = (pageW - w) / 2
  pdf.addImage(imgData, 'PNG', x, margin, w, h)
  return pdf.output('blob')
}

export function downloadBlob(blob, filename) {
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.click()
  URL.revokeObjectURL(url)
}

export async function sharePdfBlob(blob, filename, { title, text } = {}) {
  const file = new File([blob], filename, { type: 'application/pdf' })
  const shareData = { files: [file], title: title ?? 'جدول المواعيد', text: text ?? '' }
  if (navigator.share && navigator.canShare?.(shareData)) {
    await navigator.share(shareData)
    return true
  }
  return false
}
