package com.mawidplus.patient.data.repository

import android.util.Log
import com.mawidplus.patient.BuildConfig
import com.mawidplus.patient.core.model.Appointment
import com.mawidplus.patient.core.region.MawidRegion
import com.mawidplus.patient.core.network.SupabaseProvider
import com.mawidplus.patient.data.dto.AppointmentDto
import com.mawidplus.patient.data.dto.AppointmentInsertDto
import com.mawidplus.patient.data.dto.AppointmentStatusPatch
import com.mawidplus.patient.data.dto.AppointmentTimeSlotRowDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.LocalDate
import java.time.ZoneId

class AppointmentRepository(
    private val supabase: SupabaseClient = SupabaseProvider.client
) {

    private companion object {
        private const val TAG = "AppointmentRepository"
        private val APP_ZONE: ZoneId = MawidRegion.timeZone
        private val APPOINTMENT_COLUMNS = Columns.raw(
            "id, patient_id, doctor_id, queue_number, status, appointment_date, created_at, notes, doctor_notes, time_slot, patient_rating",
        )
    }

    private val rpcJson = Json { ignoreUnknownKeys = true }

    private val rpcHttpClient by lazy {
        HttpClient(Android) {
            install(ContentNegotiation) {
                json(rpcJson)
            }
        }
    }

    @Serializable
    private data class RpcCancelAppointmentResult(
        val ok: Boolean,
        val error: String? = null,
    )

    @Serializable
    private data class RpcSubmitDoctorRatingResult(
        val ok: Boolean,
        val error: String? = null,
    )

    /** PostgREST RPC عبر HTTP (نفس أسلوب [AuthRepository]). */
    private suspend fun postRpcJson(
        function: String,
        body: kotlinx.serialization.json.JsonObject,
    ): String {
        val bearer =
            supabase.auth.currentSessionOrNull()?.accessToken ?: BuildConfig.SUPABASE_ANON_KEY
        val url = "${BuildConfig.SUPABASE_URL.trimEnd('/')}/rest/v1/rpc/$function"
        return rpcHttpClient.post(url) {
            header("apikey", BuildConfig.SUPABASE_ANON_KEY)
            header("Authorization", "Bearer $bearer")
            contentType(ContentType.Application.Json)
            setBody(body)
        }.bodyAsText()
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
     * أقرب موعد نشط للمريض (اليوم أو لاحقاً) حسب تقويم مصر/القاهرة.
     */
    suspend fun getNextAppointment(patientId: String): Result<Appointment?> =
        traceRepositoryCall(TAG, "getNextAppointment") {
            withContext(Dispatchers.IO) {
                safeCall {
                    val today = LocalDate.now(APP_ZONE)
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
     * آخر موعد مكتمل للمريض (حسب التاريخ ثم رقم الطابور).
     */
    suspend fun getLastDoneAppointment(patientId: String): Result<Appointment?> =
        traceRepositoryCall(TAG, "getLastDoneAppointment") {
            withContext(Dispatchers.IO) {
                safeCall {
                    val rows = supabase.from("appointments").select(columns = APPOINTMENT_COLUMNS) {
                        filter {
                            eq("patient_id", patientId)
                            eq("status", "done")
                        }
                        order(column = "appointment_date", order = Order.DESCENDING)
                        order(column = "queue_number", order = Order.DESCENDING)
                        limit(1)
                    }.decodeList<AppointmentDto>().map { it.toDomain() }
                    rows.firstOrNull()
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
     * Appointments for a patient on a specific calendar day (e.g. today in Africa/Cairo).
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
     * يلغي الموعد عبر [cancel_my_appointment] في Supabase (SECURITY DEFINER + تحقق patient_id = auth.uid()).
     * يتفادى فشل تحديث PostgREST عندما لا تُرجع السياسات صفاً بعد PATCH.
     */
    suspend fun cancelAppointment(appointmentId: String, patientId: String): Result<Unit> =
        traceRepositoryCall(TAG, "cancelAppointment") {
            withContext(Dispatchers.IO) {
                safeCall {
                    val sessionId = supabase.auth.currentUserOrNull()?.id
                    if (sessionId == null) {
                        throw IllegalStateException("سجّل الدخول لإلغاء الموعد.")
                    }
                    if (sessionId != patientId) {
                        Log.w(
                            TAG,
                            "cancelAppointment: session uid differs from arg (session=$sessionId patientId=$patientId)",
                        )
                    }
                    val raw = postRpcJson(
                        "cancel_my_appointment",
                        buildJsonObject { put("p_appointment_id", appointmentId) },
                    )
                    val result = rpcJson.decodeFromString(
                        RpcCancelAppointmentResult.serializer(),
                        raw,
                    )
                    if (result.ok) {
                        Log.d(TAG, "Cancelled appointment via RPC: $appointmentId")
                        return@safeCall Unit
                    }
                    Log.e(TAG, "cancel_my_appointment failed: ${result.error} (appointmentId=$appointmentId)")
                    throw IllegalStateException(
                        when (result.error) {
                            "not_authenticated" -> "سجّل الدخول لإلغاء الموعد."
                            "not_found_or_forbidden" ->
                                "تعذر إلغاء الموعد. إن استمرّت المشكلة، حدّث القائمة أو تواصل مع الدعم."
                            else -> "تعذر إلغاء الموعد. تحقق من الاتصال وحاول مرة أخرى."
                        },
                    )
                }
            }
        }

    /**
     * تقييم الطبيب بعد اكتمال الموعد عبر [submit_doctor_rating] (SECURITY DEFINER).
     */
    suspend fun submitDoctorRating(
        appointmentId: String,
        patientId: String,
        stars: Int,
    ): Result<Unit> =
        traceRepositoryCall(TAG, "submitDoctorRating") {
            withContext(Dispatchers.IO) {
                safeCall {
                    val sessionId = supabase.auth.currentUserOrNull()?.id
                    if (sessionId == null) {
                        throw IllegalStateException("سجّل الدخول لإرسال التقييم.")
                    }
                    if (sessionId != patientId) {
                        Log.w(
                            TAG,
                            "submitDoctorRating: session uid differs from arg (session=$sessionId patientId=$patientId)",
                        )
                    }
                    val raw = postRpcJson(
                        "submit_doctor_rating",
                        buildJsonObject {
                            put("p_appointment_id", appointmentId)
                            put("p_stars", stars)
                        },
                    )
                    val result = rpcJson.decodeFromString(
                        RpcSubmitDoctorRatingResult.serializer(),
                        raw,
                    )
                    if (result.ok) {
                        Log.d(TAG, "submit_doctor_rating ok appointmentId=$appointmentId stars=$stars")
                        return@safeCall Unit
                    }
                    Log.e(TAG, "submit_doctor_rating failed: ${result.error} (appointmentId=$appointmentId)")
                    throw IllegalStateException(
                        when (result.error) {
                            "not_authenticated" -> "سجّل الدخول لإرسال التقييم."
                            "not_found_or_forbidden" ->
                                "تعذّر إرسال التقييم. حدّث القائمة أو تواصل مع الدعم."
                            "invalid_status" -> "يمكنك التقييم بعد اكتمال الموعد فقط."
                            "already_rated" -> "سبق تقييم هذا الموعد."
                            "invalid_rating" -> "اختر من نجمة إلى خمس نجمات."
                            else -> "تعذّر إرسال التقييم. تحقق من الاتصال وحاول مرة أخرى."
                        },
                    )
                }
            }
        }
}
