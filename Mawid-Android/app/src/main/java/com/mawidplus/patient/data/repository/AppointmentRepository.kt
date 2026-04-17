package com.mawidplus.patient.data.repository

import android.util.Log
import com.mawidplus.patient.core.model.Appointment
import com.mawidplus.patient.core.network.SupabaseProvider
import com.mawidplus.patient.data.dto.AppointmentCancelVerifyDto
import com.mawidplus.patient.data.dto.AppointmentDto
import com.mawidplus.patient.data.dto.AppointmentInsertDto
import com.mawidplus.patient.data.dto.AppointmentStatusPatch
import com.mawidplus.patient.data.dto.AppointmentTimeSlotRowDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId

class AppointmentRepository(
    private val supabase: SupabaseClient = SupabaseProvider.client
) {

    private companion object {
        private const val TAG = "AppointmentRepository"
        private val ZONE_RIYADH: ZoneId = ZoneId.of("Asia/Riyadh")
        private val APPOINTMENT_COLUMNS = Columns.raw(
            "id, patient_id, doctor_id, queue_number, status, appointment_date, created_at, notes, doctor_notes, time_slot",
        )
    }

    /**
     * All appointments for My Appointments (today + future), oldest first.
     */
    suspend fun getPatientAppointments(patientId: String): Result<List<Appointment>> =
        traceRepositoryCall(TAG, "getPatientAppointments") {
            withContext(Dispatchers.IO) {
                safeCall {
                    supabase.from("appointments").select(columns = APPOINTMENT_COLUMNS) {
                        filter {
                            eq("patient_id", patientId)
                        }
                    }.decodeList<AppointmentDto>().map { it.toDomain() }
                        .sortedWith(compareBy({ it.appointmentDate }, { it.queueNumber }))
                }
            }
        }

    /**
     * أقرب موعد نشط للمريض (اليوم أو لاحقاً) حسب تقويم آسيا/الرياض.
     */
    suspend fun getNextAppointment(patientId: String): Result<Appointment?> =
        traceRepositoryCall(TAG, "getNextAppointment") {
            withContext(Dispatchers.IO) {
                safeCall {
                    val today = LocalDate.now(ZONE_RIYADH)
                    Log.d(TAG, "getNextAppointment: patientId=$patientId today=$today")
                    val rows = supabase.from("appointments").select(columns = APPOINTMENT_COLUMNS) {
                        filter {
                            eq("patient_id", patientId)
                            gte("appointment_date", today.toString())
                        }
                    }.decodeList<AppointmentDto>().map { it.toDomain() }
                    Log.d(TAG, "getNextAppointment: total rows=${rows.size}")
                    val next = rows
                        .filter { ap ->
                            val st = ap.status.lowercase().trim()
                            st != "cancelled" && st != "done" &&
                                !LocalDate.parse(ap.appointmentDate).isBefore(today)
                        }
                        .minWithOrNull(compareBy({ it.appointmentDate }, { it.queueNumber }))
                    Log.d(TAG, "getNextAppointment: next id=${next?.id} date=${next?.appointmentDate}")
                    next
                }
            }
        }

    /**
     * أوقات الحجز المحجوزة مسبقاً لطبيب في يوم (لا يشمل الملغى).
     */
    suspend fun getBookedSlotsForDoctorAndDate(
        doctorId: String,
        date: String,
    ): Result<List<String>> =
        traceRepositoryCall(TAG, "getBookedSlotsForDoctorAndDate") {
            withContext(Dispatchers.IO) {
                safeCall {
                    val data = supabase.from("appointments").select(
                        columns = Columns.raw("time_slot"),
                    ) {
                        filter {
                            eq("doctor_id", doctorId)
                            eq("appointment_date", date)
                            neq("status", "cancelled")
                        }
                    }.decodeList<AppointmentTimeSlotRowDto>()
                    val slots = data.mapNotNull { it.timeSlot?.trim()?.takeIf { s -> s.isNotEmpty() } }
                    Log.d(TAG, "getBookedSlotsForDoctorAndDate: doctor=$doctorId date=$date count=${slots.size}")
                    slots
                }
            }
        }

    /**
     * Appointments for a patient on a specific calendar day (e.g. today in Asia/Riyadh).
     * For queue / "today only" logic.
     */
    suspend fun getTodayAppointmentsForPatient(
        patientId: String,
        appointmentDateIso: String,
    ): Result<List<Appointment>> =
        traceRepositoryCall(TAG, "getTodayAppointmentsForPatient") {
            withContext(Dispatchers.IO) {
                safeCall {
                    supabase.from("appointments").select(columns = APPOINTMENT_COLUMNS) {
                        filter {
                            eq("patient_id", patientId)
                            eq("appointment_date", appointmentDateIso)
                        }
                    }.decodeList<AppointmentDto>().map { it.toDomain() }
                        .sortedWith(compareBy({ it.queueNumber }))
                }
            }
        }

    suspend fun createAppointment(
        patientId: String,
        doctorId: String,
        appointmentDate: String,
        queueNumber: Int,
        timeSlot: String? = null,
    ): Result<Appointment> = traceRepositoryCall(TAG, "createAppointment") {
        withContext(Dispatchers.IO) {
            safeCall {
                val normalizedSlot = timeSlot?.trim()?.takeIf { it.isNotEmpty() }
                if (!normalizedSlot.isNullOrBlank()) {
                    when (val booked = getBookedSlotsForDoctorAndDate(doctorId, appointmentDate)) {
                        is Result.Success -> {
                            if (normalizedSlot in booked.data) {
                                throw IllegalStateException(
                                    "تم حجز هذا الوقت للتو، اختر وقتاً آخر"
                                )
                            }
                        }
                        is Result.Error -> {
                            throw (booked.exception ?: IllegalStateException(booked.message))
                        }
                        Result.Loading -> Unit
                    }
                }
                val insert = AppointmentInsertDto(
                    patientId = patientId,
                    doctorId = doctorId,
                    queueNumber = queueNumber,
                    appointmentDate = appointmentDate,
                    timeSlot = normalizedSlot,
                )
                supabase.from("appointments").insert(insert) {
                    select()
                }.decodeSingle<AppointmentDto>().toDomain()
            }
        }
    }

    /**
     * Sets [appointmentId] to cancelled. [patientId] must match the row (and RLS); avoids updating wrong rows.
     */
    suspend fun cancelAppointment(appointmentId: String, patientId: String): Result<Unit> =
        traceRepositoryCall(TAG, "cancelAppointment") {
            withContext(Dispatchers.IO) {
                safeCall {
                    val updated = supabase.from("appointments").update(AppointmentStatusPatch("cancelled")) {
                        filter {
                            eq("id", appointmentId)
                            eq("patient_id", patientId)
                        }
                        select(Columns.raw("id, status"))
                    }.decodeList<AppointmentCancelVerifyDto>()
                    if (updated.isEmpty()) {
                        Log.e(TAG, "Cancel: 0 rows updated (id=$appointmentId). Check RLS or session.")
                        throw IllegalStateException(
                            "تعذر إلغاء الموعد. سجّل الخروج ثم الدخول مجدداً، أو تحقق من اتصال الإنترنت."
                        )
                    }
                    val row = updated.first()
                    val st = row.status.lowercase()
                    if (st != "cancelled") {
                        Log.e(TAG, "Cancel verify failed: id=$appointmentId status=${row.status}")
                        throw IllegalStateException("لم يُحدَّث حالة الموعد في الخادم")
                    }
                    Log.d(TAG, "Cancelled appointment: $appointmentId")
                    Unit
                }
            }
        }
}
