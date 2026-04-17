package com.mawidplus.patient.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileFullNameDto(
    @SerialName("full_name") val fullName: String,
)
