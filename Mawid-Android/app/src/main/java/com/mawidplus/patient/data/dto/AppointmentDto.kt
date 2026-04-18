package com.mawidplus.patient.data.dto

import com.mawidplus.patient.core.model.Appointment
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AppointmentDto(
    val id: String,
    @SerialName("patient_id") val patientId: String,
    @SerialName("doctor_id") val doctorId: String,
    @SerialName("queue_number") val queueNumber: Int,
    val status: String,
    @SerialName("appointment_date") val appointmentDate: String,
    @SerialName("created_at") val createdAt: String? = null,
    val notes: String? = null,
    @SerialName("doctor_notes") val doctorNotes: String? = null,
    @SerialName("time_slot") val timeSlot: String? = null,
    @SerialName("patient_rating") val patientRating: Int? = null,
) {
    fun toDomain(): Appointment = Appointment(
        id = id,
        patientId = patientId,
        doctorId = doctorId,
        queueNumber = queueNumber,
        status = status,
        appointmentDate = appointmentDate,
        notes = notes,
        doctorNotes = doctorNotes,
        timeSlot = timeSlot,
        patientRating = patientRating,
    )
}

@Serializable
data class AppointmentTimeSlotRowDto(
    @SerialName("time_slot") val timeSlot: String? = null,
)

/** Minimal row returned after cancel update (verify status only). */
@Serializable
data class AppointmentCancelVerifyDto(
    val id: String,
    val status: String,
)

@Serializable
data class AppointmentStatusPatch(
    val status: String = "cancelled",
)

@Serializable
data class AppointmentInsertDto(
    @SerialName("patient_id") val patientId: String,
    @SerialName("doctor_id") val doctorId: String,
    @SerialName("queue_number") val queueNumber: Int = 0,
    val status: String = "scheduled",
    @SerialName("appointment_date") val appointmentDate: String,
    @SerialName("time_slot") val timeSlot: String? = null,
)
