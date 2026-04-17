package com.mawidplus.patient.data.dto

import com.mawidplus.patient.core.model.Doctor
import com.mawidplus.patient.core.model.parseAvailableDaysFromJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ClinicEmbedDto(
    val name: String,
    val address: String? = null,
)

@Serializable
data class DoctorDto(
    val id: String,
    @SerialName("clinic_id") val clinicId: String? = null,
    /** Populated when selecting with `clinics(name, address)` embed. */
    val clinics: ClinicEmbedDto? = null,
    @SerialName("full_name") val fullName: String,
    val specialty: String,
    @SerialName("slot_duration_minutes") val slotDurationMinutes: Int,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("experience_years") val experienceYears: Int? = null,
    val about: String? = null,
    val rating: Double? = null,
    @SerialName("review_count") val reviewCount: Int = 0,
    @SerialName("consultation_fee_sar") val consultationFeeSar: Int = 0,
    @SerialName("available_days") val availableDays: JsonElement? = null,
    @SerialName("start_time") val startTime: String? = null,
    @SerialName("end_time") val endTime: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @SerialName("clinic_address") val clinicAddress: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
) {
    fun toDomain(): Doctor {
        val days = parseAvailableDaysFromJson(availableDays)
        val addr = clinicAddress?.takeIf { it.isNotBlank() } ?: clinics?.address?.takeIf { !it.isNullOrBlank() }
        return Doctor(
            id = id,
            clinicId = clinicId,
            clinicName = clinics?.name,
            fullName = fullName,
            specialty = specialty,
            slotDurationMinutes = slotDurationMinutes,
            imageUrl = imageUrl,
            avatarUrl = avatarUrl,
            experienceYears = experienceYears ?: 0,
            about = about?.takeIf { it.isNotBlank() },
            rating = rating,
            reviewCount = reviewCount,
            consultationFeeSar = consultationFeeSar,
            availableDays = days,
            startTime = startTime,
            endTime = endTime,
            latitude = latitude,
            longitude = longitude,
            clinicAddress = addr,
        )
    }
}
