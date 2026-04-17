package com.mawidplus.patient.data.dto

import com.mawidplus.patient.core.model.QueueSettings
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class QueueCurrentNumberPatch(
    @SerialName("current_number") val currentNumber: Int
)

@Serializable
data class QueueSettingsDto(
    @SerialName("doctor_id") val doctorId: String,
    @SerialName("current_number") val currentNumber: Int,
    @SerialName("queue_date") val queueDate: String,
    @SerialName("is_open") val isOpen: Boolean,
    @SerialName("updated_at") val updatedAt: String? = null
) {
    fun toDomain(): QueueSettings = QueueSettings(
        doctorId = doctorId,
        currentNumber = currentNumber,
        queueDate = queueDate,
        isOpen = isOpen
    )
}
