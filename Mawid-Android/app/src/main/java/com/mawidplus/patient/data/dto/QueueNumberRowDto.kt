package com.mawidplus.patient.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class QueueNumberRowDto(
    @SerialName("queue_number") val queueNumber: Int,
)
