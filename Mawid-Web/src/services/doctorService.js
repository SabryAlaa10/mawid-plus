import { supabase } from '../lib/supabaseClient'

const MAX_AVATAR_BYTES = 5 * 1024 * 1024

/**
 * ضغط الصورة إلى JPEG (حد أقصى للبعد الأطول maxDim، جودة تقريبية quality).
 */
export async function compressImageToJpeg(file, maxDim = 1024, quality = 0.85) {
  if (file.size > MAX_AVATAR_BYTES) {
    throw new Error('حجم الصورة يتجاوز 5 ميجابايت')
  }
  const bitmap = await createImageBitmap(file)
  try {
    let w = bitmap.width
    let h = bitmap.height
    const scale = Math.min(1, maxDim / Math.max(w, h))
    w = Math.round(w * scale)
    h = Math.round(h * scale)
    const canvas = document.createElement('canvas')
    canvas.width = w
    canvas.height = h
    const ctx = canvas.getContext('2d')
    if (!ctx) throw new Error('تعذر معالجة الصورة')
    ctx.drawImage(bitmap, 0, 0, w, h)
    const blob = await new Promise((resolve, reject) => {
      canvas.toBlob(
        (b) => (b ? resolve(b) : reject(new Error('تعذر ضغط الصورة'))),
        'image/jpeg',
        quality,
      )
    })
    return blob
  } finally {
    bitmap.close()
  }
}

/** رفع إلى doctor-avatars/{doctorId}.jpg وإرجاع الرابط العام */
export async function uploadDoctorAvatar(doctorId, file) {
  const blob = await compressImageToJpeg(file)
  const path = `${doctorId}.jpg`
  const { error: upErr } = await supabase.storage
    .from('doctor-avatars')
    .upload(path, blob, { upsert: true, contentType: 'image/jpeg' })
  if (upErr) throw upErr
  const { data } = supabase.storage.from('doctor-avatars').getPublicUrl(path)
  return data.publicUrl
}

export async function updateAvatarUrl(doctorId, publicUrl) {
  // نفس مسار الملف في Storage يبقى ثابتاً بعد upsert؛ المتصفح وCoil يكاشان حسب الرابط الكامل.
  const base = publicUrl.split('?')[0]
  const busted = `${base}?v=${Date.now()}`
  const { data, error } = await supabase
    .from('doctors')
    .update({ avatar_url: busted })
    .eq('id', doctorId)
    .select()
    .maybeSingle()
  if (error) throw error
  return data
}

const DOCTOR_PROFILE_COLUMNS =
  'id, clinic_id, full_name, specialty, slot_duration_minutes, image_url, avatar_url, experience_years, about, rating, review_count, consultation_fee_sar, available_days, start_time, end_time, latitude, longitude, clinic_address, created_at'

export async function getDoctorProfile(doctorId) {
  const { data, error } = await supabase.from('doctors').select(DOCTOR_PROFILE_COLUMNS).eq('id', doctorId).maybeSingle()
  if (error) throw error
  return data
}

export async function updateSchedule(doctorId, scheduleData) {
  const patch = {}
  if (scheduleData.available_days !== undefined) patch.available_days = scheduleData.available_days
  if (scheduleData.start_time !== undefined) patch.start_time = scheduleData.start_time
  if (scheduleData.end_time !== undefined) patch.end_time = scheduleData.end_time
  if (scheduleData.slot_duration_minutes !== undefined) patch.slot_duration_minutes = scheduleData.slot_duration_minutes

  const { data, error } = await supabase.from('doctors').update(patch).eq('id', doctorId).select().maybeSingle()
  if (error) throw error
  return data
}

export async function updateFee(doctorId, feeSar) {
  const { data, error } = await supabase
    .from('doctors')
    .update({ consultation_fee_sar: feeSar })
    .eq('id', doctorId)
    .select()
    .maybeSingle()
  if (error) throw error
  return data
}

export async function updateLocation(doctorId, { latitude, longitude, clinicAddress }) {
  const patch = {}
  if (latitude !== undefined && latitude !== null && !Number.isNaN(latitude)) patch.latitude = latitude
  if (longitude !== undefined && longitude !== null && !Number.isNaN(longitude)) patch.longitude = longitude
  if (clinicAddress !== undefined) patch.clinic_address = clinicAddress || null
  const { data, error } = await supabase.from('doctors').update(patch).eq('id', doctorId).select().maybeSingle()
  if (error) throw error
  return data
}

export async function updateDoctorProfile(doctorId, { specialty, about, experience_years, full_name }) {
  const docPatch = {}
  if (specialty !== undefined) docPatch.specialty = specialty
  if (about !== undefined) docPatch.about = about
  if (experience_years !== undefined) docPatch.experience_years = experience_years
  if (full_name !== undefined) docPatch.full_name = full_name

  const { data: doc, error: e1 } = await supabase.from('doctors').update(docPatch).eq('id', doctorId).select().maybeSingle()
  if (e1) throw e1

  if (full_name !== undefined) {
    const { error: e2 } = await supabase.from('profiles').update({ full_name }).eq('id', doctorId)
    if (e2) throw e2
  }

  return doc
}
