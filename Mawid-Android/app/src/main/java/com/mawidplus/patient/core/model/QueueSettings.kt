package com.mawidplus.patient.core.model

data class QueueSettings(
    val doctorId: String,
    val currentNumber: Int,
    val queueDate: String,
    val isOpen: Boolean
)
