package com.mawidplus.patient.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileWriteDto(
    @SerialName("full_name") val fullName: String,
    val phone: String,
    val role: String = "patient"
)

@Serializable
data class ProfileInsertDto(
    val id: String,
    @SerialName("full_name") val fullName: String,
    val phone: String,
    val role: String = "patient"
)
