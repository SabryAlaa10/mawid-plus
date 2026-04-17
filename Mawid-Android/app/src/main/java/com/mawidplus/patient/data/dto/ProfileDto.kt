package com.mawidplus.patient.data.dto

import com.mawidplus.patient.core.model.Profile
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileDto(
    val id: String,
    @SerialName("full_name") val fullName: String,
    val phone: String? = null,
    val role: String,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
) {
    fun toDomain(): Profile = Profile(
        id = id,
        fullName = fullName,
        phone = phone.orEmpty(),
        role = role,
        createdAt = createdAt,
    )
}
