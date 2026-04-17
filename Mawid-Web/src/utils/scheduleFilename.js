/** Maps Arabic letters to a readable Latin slug for download filenames (ASCII-safe on Windows). */
const AR_TO_LATIN = {
  ا: 'a',
  أ: 'a',
  إ: 'i',
  آ: 'a',
  ٱ: 'a',
  ب: 'b',
  ت: 't',
  ث: 'th',
  ج: 'j',
  ح: 'h',
  خ: 'kh',
  د: 'd',
  ذ: 'dh',
  ر: 'r',
  ز: 'z',
  س: 's',
  ش: 'sh',
  ص: 's',
  ض: 'd',
  ط: 't',
  ظ: 'z',
  ع: 'a',
  غ: 'gh',
  ف: 'f',
  ق: 'q',
  ك: 'k',
  ل: 'l',
  م: 'm',
  ن: 'n',
  ه: 'h',
  و: 'w',
  ي: 'y',
  ى: 'a',
  ة: 'h',
  ء: '',
  ئ: 'y',
  ؤ: 'w',
}

const AR_DIGITS = '٠١٢٣٤٥٦٧٨٩'

function arabicDigitToAscii(ch) {
  const i = AR_DIGITS.indexOf(ch)
  return i >= 0 ? String(i) : ch
}

/**
 * @param {string} fullName
 * @param {string} [doctorId]
 * @returns {string} e.g. jadwal-ahmad-khaled.pdf
 */
export function buildSchedulePdfFilename(fullName, doctorId) {
  let raw = String(fullName || '')
    .normalize('NFKC')
    .trim()
  raw = raw.replace(/^(د\.?|د\s*\.|doctor\.?|dr\.?)\s*/iu, 'dr ')

  let out = ''
  for (const ch of raw) {
    if (ch === '\u0640' || ch === '\u200c' || ch === '\u200d' || ch === '\ufeff') continue
    if (ch === '\ufefb' || ch === '\ufefc') {
      out += 'la'
      continue
    }
    const cp = ch.codePointAt(0)
    if (cp >= 0x61 && cp <= 0x7a) {
      out += ch
      continue
    }
    if (cp >= 0x41 && cp <= 0x5a) {
      out += ch.toLowerCase()
      continue
    }
    if (cp >= 0x30 && cp <= 0x39) {
      out += ch
      continue
    }
    if (AR_DIGITS.includes(ch)) {
      out += arabicDigitToAscii(ch)
      continue
    }
    if (ch === ' ' || ch === '\t' || ch === '.' || ch === '،' || ch === ',' || ch === '-' || ch === '_') {
      out += '-'
      continue
    }
    const lat = AR_TO_LATIN[ch]
    if (lat !== undefined) {
      out += lat
      continue
    }
    if (cp >= 0x0600 && cp <= 0x06ff) {
      continue
    }
  }

  let slug = out
    .replace(/-+/g, '-')
    .replace(/^-+|-+$/g, '')
    .toLowerCase()
    .slice(0, 72)

  if (!slug || slug.length < 2) {
    const id = String(doctorId || 'doctor').replace(/[^a-f0-9]/gi, '').slice(0, 12) || 'doctor'
    slug = `doctor-${id}`
  }

  return `jadwal-${slug}.pdf`
}
