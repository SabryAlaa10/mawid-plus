package com.mawidplus.patient.core.model

data class Profile(
    val id: String,
    val fullName: String,
    val phone: String,
    val role: String,
    /** ISO timestamp من profiles.created_at */
    val createdAt: String? = null,
)
