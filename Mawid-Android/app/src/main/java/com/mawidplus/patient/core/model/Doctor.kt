package com.mawidplus.patient.core.model

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

data class Doctor(
    val id: String,
    val clinicId: String?,
    /** From `clinics` embed when loaded with `select=*,clinics(name, address)`. */
    val clinicName: String?,
    val fullName: String,
    val specialty: String,
    val slotDurationMinutes: Int,
    val imageUrl: String?,
    /** Uploaded from doctor dashboard (Supabase Storage); overrides [imageUrl] for display when set. */
    val avatarUrl: String? = null,
    /** Years of experience (from `experience_years`). */
    val experienceYears: Int = 0,
    /** Doctor bio / about text. */
    val about: String? = null,
    /** Average rating; null if not set. */
    val rating: Double? = null,
    val reviewCount: Int = 0,
    /** رسوم الكشف بالجنيه المصري (مخزَّنة في العمود `consultation_fee_sar` تاريخياً). */
    val consultationFeeSar: Int = 0,
    /** Which weekdays are available — normalized Arabic labels (e.g. الأحد). */
    val availableDays: List<String> = emptyList(),
    val startTime: String? = null,
    val endTime: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    /** Address on doctor row; falls back to clinic embed when null. */
    val clinicAddress: String? = null,
) {
    val photoUrl: String?
        get() = avatarUrl?.takeIf { it.isNotBlank() } ?: imageUrl?.takeIf { it.isNotBlank() }

    /** Display fee (same as [consultationFeeSar]). */
    val consultationFee: Int get() = consultationFeeSar

    /** Rating as float for UI, 0f when null. */
    val ratingFloat: Float get() = (rating ?: 0.0).toFloat()
}

fun parseAvailableDaysFromJson(element: JsonElement?): List<String> {
    if (element == null || element is JsonNull) return emptyList()
    return try {
        when (element) {
            is JsonArray -> element.mapNotNull { el ->
                when (el) {
                    is JsonPrimitive -> mapDayTokenToArabic(el.content)
                    else -> null
                }
            }
            is JsonPrimitive -> listOfNotNull(mapDayTokenToArabic(element.content))
            else -> emptyList()
        }
    } catch (_: Exception) {
        emptyList()
    }
}

/** Maps stored tokens (0–6, English short names, Arabic) to short Arabic labels. */
private fun mapDayTokenToArabic(raw: String): String? {
    val t = raw.trim().lowercase()
    return when (t) {
        "0", "sun", "sunday", "الأحد" -> "الأحد"
        "1", "mon", "monday", "الإثنين" -> "الإثنين"
        "2", "tue", "tuesday", "الثلاثاء" -> "الثلاثاء"
        "3", "wed", "wednesday", "الأربعاء" -> "الأربعاء"
        "4", "thu", "thursday", "الخميس" -> "الخميس"
        "5", "fri", "friday", "الجمعة" -> "الجمعة"
        "6", "sat", "saturday", "السبت" -> "السبت"
        else -> if (t.isNotEmpty()) raw.trim() else null
    }
}
