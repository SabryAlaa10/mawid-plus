package com.mawidplus.patient.core.model

data class Appointment(
    val id: String,
    val patientId: String,
    val doctorId: String,
    val queueNumber: Int,
    val status: String,
    val appointmentDate: String,
    val notes: String? = null,
    /** ملاحظات يظهرها للمريض بعد إنهاء الموعد (من لوحة الطبيب). */
    val doctorNotes: String? = null,
    /** وقت الحجز بصيغة HH:mm (مثلاً 09:30). */
    val timeSlot: String? = null,
    /** تقييم المريض للطبيب (1–5) بعد اكتمال الموعد؛ null = لم يُقيَّم بعد. */
    val patientRating: Int? = null,
)
