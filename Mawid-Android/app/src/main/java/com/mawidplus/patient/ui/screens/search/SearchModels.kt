package com.mawidplus.patient.ui.screens.search

import com.mawidplus.patient.core.model.Doctor
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Locale

private const val DEFAULT_DOCTOR_IMAGE =
    "https://images.unsplash.com/photo-1612349317150-e413f6a5b16d?w=400&h=400&fit=crop&q=80"

data class DoctorSearchItem(
    val id: String,
    val name: String,
    val specialty: String,
    val clinicName: String?,
    val clinicAddress: String? = null,
    val rating: String,
    val reviews: String,
    val nextSlot: String,
    val availableToday: Boolean,
    val imageUrl: String? = null
)

data class BookingDoctorMeta(
    val specialty: String,
    val imageUrl: String,
    val rating: String,
    val reviewShort: String,
    /** رسوم الكشف بالجنيه المصري (يُخزَّن في العمود consultation_fee_sar كرقم). */
    val consultationFeeEgp: Int,
)

fun Doctor.toDoctorSearchItem(): DoctorSearchItem {
    val ratingStr = rating?.let { String.format(Locale.US, "%.1f", it) } ?: "—"
    val reviewsStr = if (reviewCount > 0) "($reviewCount مراجعة)" else "(لا مراجعات بعد)"
    return DoctorSearchItem(
        id = id,
        name = fullName,
        specialty = specialty,
        clinicName = clinicName,
        clinicAddress = clinicAddress,
        rating = ratingStr,
        reviews = reviewsStr,
        nextSlot = "احجز لعرض المواعيد",
        availableToday = doctorAvailableToday(this),
        imageUrl = photoUrl
    )
}

private fun doctorAvailableToday(d: Doctor): Boolean {
    if (d.availableDays.isEmpty()) return true
    val today = arabicDayName(LocalDate.now().dayOfWeek)
    return d.availableDays.any { day -> day.contains(today, ignoreCase = true) || today.contains(day, ignoreCase = true) }
}

private fun arabicDayName(day: DayOfWeek): String = when (day) {
    DayOfWeek.SUNDAY -> "الأحد"
    DayOfWeek.MONDAY -> "الإثنين"
    DayOfWeek.TUESDAY -> "الثلاثاء"
    DayOfWeek.WEDNESDAY -> "الأربعاء"
    DayOfWeek.THURSDAY -> "الخميس"
    DayOfWeek.FRIDAY -> "الجمعة"
    DayOfWeek.SATURDAY -> "السبت"
}

/** Client-side filter: empty query returns all rows; matches name, specialty, or clinic. */
fun List<DoctorSearchItem>.filteredBySearchQuery(query: String): List<DoctorSearchItem> {
    val q = query.trim()
    if (q.isEmpty()) return this
    return filter { item ->
        listOf(item.name, item.specialty, item.clinicName.orEmpty(), item.clinicAddress.orEmpty())
            .any { it.contains(q, ignoreCase = true) }
    }
}

fun Doctor.toBookingMeta(): BookingDoctorMeta = BookingDoctorMeta(
    specialty = specialty,
    imageUrl = photoUrl ?: DEFAULT_DOCTOR_IMAGE,
    rating = rating?.let { String.format(Locale.US, "%.1f", it) } ?: "—",
    reviewShort = if (reviewCount > 0) "($reviewCount مراجعة)" else "",
    consultationFeeEgp = consultationFeeSar
)
