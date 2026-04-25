package com.mawidplus.patient.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatTurnDto(
    val role: String,
    val content: String,
)

@Serializable
data class ChatRequestDto(
    val message: String,
    @SerialName("session_id") val sessionId: String? = null,
    val history: List<ChatTurnDto> = emptyList(),
)

@Serializable
data class ChatResponseDto(
    @SerialName("session_id") val sessionId: String? = null,
    @SerialName("assistant_message") val assistantMessage: String,
    @SerialName("quick_replies") val quickReplies: List<String> = emptyList(),
    @SerialName("detected_specialty") val detectedSpecialty: String? = null,
    @SerialName("recommended_doctor") val recommendedDoctor: List<RecommendedDoctorDto>? = null,
    val severity: String? = null,
    val summary: String? = null,
    @SerialName("ready_for_doctors") val readyForDoctors: Boolean = false,
    @SerialName("specialty_available") val specialtyAvailable: Boolean = false,
)

@Serializable
data class RecommendedDoctorDto(
    val id: String? = null,
    @SerialName("full_name") val fullName: String? = null,
    val specialty: String? = null,
    val rating: Double? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("consultation_fee_sar") val consultationFeeSar: Int? = null,
    @SerialName("clinic_address") val clinicAddress: String? = null,
)
