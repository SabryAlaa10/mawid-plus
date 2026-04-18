import { useEffect, useState, useRef } from 'react'
import { useOutletContext } from 'react-router-dom'
import { Loader2, RefreshCw, UserCircle, Camera } from 'lucide-react'
import { useAuth } from '../hooks/useAuth'
import { useToast } from '../context/ToastContext'
import PageHeader from '../components/ui/PageHeader'
import * as doctorService from '../services/doctorService'

export default function ProfilePage() {
  const { user } = useAuth()
  const { doctor, doctorLoading, doctorError, refreshDoctor } = useOutletContext()
  const { showToast } = useToast()
  const [fullName, setFullName] = useState('')
  const [specialty, setSpecialty] = useState('')
  const [about, setAbout] = useState('')
  const [exp, setExp] = useState(0)
  const [latitude, setLatitude] = useState('')
  const [longitude, setLongitude] = useState('')
  const [clinicAddress, setClinicAddress] = useState('')
  const [saving, setSaving] = useState(false)
  const [msg, setMsg] = useState(null)
  const [avatarPreview, setAvatarPreview] = useState(null)
  const [avatarUploading, setAvatarUploading] = useState(false)
  const fileInputRef = useRef(null)

  const avatarDisplayUrl =
    avatarPreview || doctor?.avatar_url || doctor?.image_url || null

  const doctorInitials = () => {
    const parts = (fullName || doctor?.full_name || 'د')
      .trim()
      .split(/\s+/)
      .filter(Boolean)
    if (parts.length >= 2) return `${parts[0][0]}${parts[1][0]}`
    if (parts[0]?.length >= 2) return parts[0].slice(0, 2)
    return parts[0]?.[0] || 'د'
  }

  useEffect(() => {
    if (!doctor) return
    setFullName(doctor.full_name ?? '')
    setSpecialty(doctor.specialty ?? '')
    setAbout(doctor.about ?? '')
    setExp(doctor.experience_years ?? 0)
    setLatitude(doctor.latitude != null ? String(doctor.latitude) : '')
    setLongitude(doctor.longitude != null ? String(doctor.longitude) : '')
    setClinicAddress(doctor.clinic_address ?? '')
  }, [doctor])

  useEffect(() => {
    return () => {
      if (avatarPreview?.startsWith('blob:')) URL.revokeObjectURL(avatarPreview)
    }
  }, [avatarPreview])

  const handleAvatarFile = async (e) => {
    const file = e.target.files?.[0]
    if (!file || !doctor?.id) return
    if (file.size > 5 * 1024 * 1024) {
      showToast('حجم الصورة يتجاوز 5 ميجابايت', 'error')
      e.target.value = ''
      return
    }
    const preview = URL.createObjectURL(file)
    setAvatarPreview(preview)
    setAvatarUploading(true)
    try {
      const url = await doctorService.uploadDoctorAvatar(doctor.id, file)
      await doctorService.updateAvatarUrl(doctor.id, url)
      setAvatarPreview(null)
      URL.revokeObjectURL(preview)
      await refreshDoctor?.()
      showToast('تم تحديث الصورة بنجاح')
    } catch (err) {
      showToast(err.message || 'فشل رفع الصورة', 'error')
      URL.revokeObjectURL(preview)
      setAvatarPreview(null)
    } finally {
      setAvatarUploading(false)
      e.target.value = ''
    }
  }

  const save = async () => {
    if (!doctor?.id) return
    setSaving(true)
    setMsg(null)
    try {
      await doctorService.updateDoctorProfile(doctor.id, {
        full_name: fullName,
        specialty,
        about,
        experience_years: Number(exp),
      })
      const latNum = latitude.trim() === '' ? null : parseFloat(latitude)
      const lngNum = longitude.trim() === '' ? null : parseFloat(longitude)
      if (latNum != null && (latNum < -90 || latNum > 90)) throw new Error('خط العرض يجب أن يكون بين -90 و 90')
      if (lngNum != null && (lngNum < -180 || lngNum > 180)) throw new Error('خط الطول يجب أن يكون بين -180 و 180')
      await doctorService.updateLocation(doctor.id, {
        latitude: latNum,
        longitude: lngNum,
        clinicAddress: clinicAddress.trim() || null,
      })
      await refreshDoctor?.()
      showToast('تم حفظ الملف بنجاح')
    } catch (e) {
      const m = e.message || 'فشل الحفظ'
      setMsg(m)
      showToast(m, 'error')
    } finally {
      setSaving(false)
    }
  }

  if (doctorLoading && !doctor) {
    return (
      <div className="max-w-xl space-y-6 mx-auto animate-fade-in-up [animation-fill-mode:forwards]" dir="rtl">
        <div className="h-10 w-56 skeleton-shimmer rounded-lg" />
        <div className="dashboard-card h-28 skeleton-shimmer" />
        <div className="dashboard-card min-h-[320px] skeleton-shimmer" />
      </div>
    )
  }

  if (!doctor && doctorError) {
    return (
      <div className="dashboard-card p-8 max-w-lg mx-auto text-center opacity-0 animate-fade-in-up [animation-fill-mode:forwards]" dir="rtl">
        <p className="text-red-800 font-bold mb-2">تعذر تحميل بيانات الطبيب</p>
        <p className="text-sm text-red-700/90 mb-4">{doctorError}</p>
        <button
          type="button"
          onClick={() => refreshDoctor?.()}
          className="btn-gradient-primary inline-flex items-center gap-2 px-6 py-2.5 rounded-xl text-sm"
        >
          <RefreshCw className="w-4 h-4" />
          إعادة المحاولة
        </button>
      </div>
    )
  }

  if (!doctor) {
    return (
      <div className="dashboard-card p-8 max-w-md mx-auto text-center opacity-0 animate-fade-in-up [animation-fill-mode:forwards]" dir="rtl">
        <UserCircle className="w-12 h-12 text-slate-300 mx-auto mb-3" />
        <p className="text-slate-600 font-bold">لا يوجد ملف طبيب.</p>
      </div>
    )
  }

  return (
    <div className="max-w-xl space-y-6 mx-auto" dir="rtl">
      <PageHeader
        title="الملف الشخصي"
        subtitles={['بياناتك الظاهرة للمرضى', 'صورة احترافية لعيادتك', 'متزامن مع Supabase']}
      />

      <input
        ref={fileInputRef}
        type="file"
        accept="image/jpeg,image/png,image/webp,image/*"
        className="hidden"
        onChange={handleAvatarFile}
      />

      <div className="dashboard-card p-6 opacity-0 animate-fade-in-up [animation-fill-mode:forwards] [animation-delay:0.05s]">
        <h3 className="text-sm font-bold text-slate-800 mb-3">تقييمات المرضى</h3>
        {doctor.review_count > 0 || doctor.rating != null ? (
          <div className="flex flex-wrap items-end gap-3 mb-1">
            <p className="text-3xl font-black text-slate-900 tabular-nums m-0">
              {doctor.rating != null ? Number(doctor.rating).toFixed(1) : '—'}
              <span className="text-base font-bold text-slate-500 ms-1">/ 5</span>
            </p>
            <p className="text-sm text-slate-600 m-0 pb-1">
              بناءً على{' '}
              <span className="font-bold tabular-nums">{doctor.review_count ?? 0}</span> تقييم
            </p>
          </div>
        ) : (
          <p className="text-sm text-slate-500 m-0 mb-1">
            سيظهر هنا متوسط تقييمك وعدد التقييمات بعد أن يقيّمك المرضى من التطبيق (بعد اكتمال الموعد).
          </p>
        )}
        <p className="text-xs text-slate-400 mt-2 m-0">
          يُحدَّث المتوسط تلقائياً عند كل تقييم جديد ويظهر أيضاً في جرس الإشعارات.
        </p>
      </div>

      <div className="dashboard-card p-6 opacity-0 animate-fade-in-up [animation-fill-mode:forwards] [animation-delay:0.06s]">
        <div className="flex flex-col sm:flex-row items-center gap-6">
          <button
            type="button"
            onClick={() => fileInputRef.current?.click()}
            disabled={avatarUploading}
            className="relative shrink-0 rounded-full focus:outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2 disabled:opacity-70"
            aria-label="تغيير صورة الملف الشخصي"
          >
            <div className="w-[120px] h-[120px] rounded-full overflow-hidden ring-2 ring-primary/25 shadow-lg bg-slate-100">
              {avatarDisplayUrl ? (
                <img
                  src={avatarDisplayUrl}
                  alt=""
                  className="w-full h-full object-cover"
                />
              ) : (
                <div className="w-full h-full bg-gradient-to-br from-[#1A73E8] to-[#1557b0] flex items-center justify-center text-white text-4xl font-black">
                  {doctorInitials()}
                </div>
              )}
            </div>
            <span className="absolute bottom-1 right-1 w-10 h-10 rounded-full bg-primary text-white shadow-md flex items-center justify-center border-2 border-white">
              {avatarUploading ? (
                <Loader2 className="w-5 h-5 animate-spin" aria-hidden />
              ) : (
                <Camera className="w-5 h-5" aria-hidden />
              )}
            </span>
          </button>
          <div className="min-w-0 text-center sm:text-start flex-1">
            <p className="text-xs font-semibold text-slate-500 uppercase tracking-wide">البريد (للقراءة فقط)</p>
            <p className="font-bold text-slate-800 truncate mt-1">{user?.email ?? '—'}</p>
            <p className="text-sm text-slate-500 mt-2">اضغط على الصورة لرفع صورة جديدة (حتى 5 ميجابايت)</p>
          </div>
        </div>
      </div>

      <div className="space-y-4 dashboard-card p-6 md:p-7 opacity-0 animate-fade-in-up [animation-fill-mode:forwards] [animation-delay:0.1s]">
        <div>
          <label className="text-xs font-semibold text-slate-500 block mb-1.5">الاسم الكامل</label>
          <input
            className="dashboard-input"
            value={fullName}
            onChange={(e) => setFullName(e.target.value)}
          />
        </div>
        <div>
          <label className="text-xs font-semibold text-slate-500 block mb-1.5">التخصص</label>
          <input
            className="dashboard-input"
            value={specialty}
            onChange={(e) => setSpecialty(e.target.value)}
          />
        </div>
        <div>
          <label className="text-xs font-semibold text-slate-500 block mb-1.5">نبذة</label>
          <textarea
            className="dashboard-input min-h-[120px] resize-y"
            value={about}
            onChange={(e) => setAbout(e.target.value)}
          />
        </div>
        <div>
          <label className="text-xs font-semibold text-slate-500 block mb-1.5">سنوات الخبرة</label>
          <input
            type="number"
            min={0}
            className="dashboard-input"
            value={exp}
            onChange={(e) => {
              const v = e.target.value
              setExp(v === '' ? 0 : Number(v))
            }}
          />
        </div>
      </div>

      <div className="space-y-4 dashboard-card p-6 md:p-7 opacity-0 animate-fade-in-up [animation-fill-mode:forwards] [animation-delay:0.12s]">
        <h3 className="text-sm font-bold text-slate-800 mb-2">موقع العيادة</h3>
        <p className="text-xs text-slate-500 mb-3">
          يمكنك الحصول على الإحداثيات من{' '}
          <a
            href="https://www.google.com/maps"
            target="_blank"
            rel="noreferrer"
            className="text-primary font-semibold underline"
          >
            Google Maps
          </a>{' '}
          (انقر بزر الماوس الأيمن على المكان → الإحداثيات).
        </p>
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div>
            <label className="text-xs font-semibold text-slate-500 block mb-1.5">خط العرض (latitude)</label>
            <input
              type="number"
              step="any"
              className="dashboard-input"
              placeholder="24.7136"
              value={latitude}
              onChange={(e) => setLatitude(e.target.value)}
            />
          </div>
          <div>
            <label className="text-xs font-semibold text-slate-500 block mb-1.5">خط الطول (longitude)</label>
            <input
              type="number"
              step="any"
              className="dashboard-input"
              placeholder="46.6753"
              value={longitude}
              onChange={(e) => setLongitude(e.target.value)}
            />
          </div>
        </div>
        <div>
          <label className="text-xs font-semibold text-slate-500 block mb-1.5">عنوان العيادة (نص)</label>
          <input
            className="dashboard-input"
            value={clinicAddress}
            onChange={(e) => setClinicAddress(e.target.value)}
            placeholder="القاهرة، حي ..."
          />
        </div>
      </div>

      {msg && (
        <p
          className={`text-sm px-4 py-3 rounded-xl border animate-error-slide ${
            msg.includes('فشل') ? 'text-red-800 bg-red-50 border-red-200' : 'text-emerald-800 bg-emerald-50 border-emerald-200'
          }`}
        >
          {msg}
        </p>
      )}

      <button
        type="button"
        disabled={saving}
        onClick={save}
        className="btn-gradient-primary inline-flex items-center justify-center gap-2 px-8 py-3.5 rounded-xl text-base"
      >
        {saving ? 'جاري الحفظ…' : 'حفظ التغييرات'}
        {saving && <Loader2 className="w-5 h-5 animate-spin" />}
      </button>
    </div>
  )
}
