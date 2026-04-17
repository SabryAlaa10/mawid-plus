package com.mawidplus.patient.data.repository

import com.mawidplus.patient.core.model.QueueSettings
import com.mawidplus.patient.core.network.SupabaseProvider
import com.mawidplus.patient.data.dto.QueueCurrentNumberPatch
import com.mawidplus.patient.data.dto.QueueNumberRowDto
import com.mawidplus.patient.data.dto.QueueSettingsDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class QueueRepository(
    private val supabase: SupabaseClient = SupabaseProvider.client
) {

    private companion object {
        private const val TAG = "QueueRepository"
        private val QUEUE_SETTINGS_COLUMNS = Columns.raw(
            "doctor_id, current_number, queue_date, is_open, updated_at",
        )
    }

    suspend fun getQueueSettings(doctorId: String): Result<QueueSettings> =
        traceRepositoryCall(TAG, "getQueueSettings") {
            withContext(Dispatchers.IO) {
                safeCall {
                    supabase.from("queue_settings").select(columns = QUEUE_SETTINGS_COLUMNS) {
                        filter {
                            eq("doctor_id", doctorId)
                        }
                    }.decodeSingle<QueueSettingsDto>().toDomain()
                }
            }
        }

    /**
     * Next queue ticket for [appointmentDateIso] only (sequence is per doctor per day).
     */
    suspend fun maxQueueNumberForDoctorAndDate(
        doctorId: String,
        appointmentDateIso: String,
    ): Result<Int> =
        traceRepositoryCall(TAG, "maxQueueNumberForDoctorAndDate") {
            withContext(Dispatchers.IO) {
                safeCall {
                    val rows = supabase.from("appointments").select(
                        columns = Columns.raw("queue_number"),
                    ) {
                        filter {
                            eq("doctor_id", doctorId)
                            eq("appointment_date", appointmentDateIso)
                        }
                        order(column = "queue_number", order = Order.DESCENDING)
                        limit(1)
                    }.decodeList<QueueNumberRowDto>()
                    rows.firstOrNull()?.queueNumber ?: 0
                }
            }
        }

    /** Doctor dashboard: advance "now serving" counter (not used when patients book). */
    suspend fun updateCurrentNumber(doctorId: String, newCurrent: Int): Result<Unit> =
        traceRepositoryCall(TAG, "updateCurrentNumber") {
            withContext(Dispatchers.IO) {
                safeCall {
                    supabase.from("queue_settings").update(QueueCurrentNumberPatch(newCurrent)) {
                        filter {
                            eq("doctor_id", doctorId)
                        }
                    }
                    Unit
                }
            }
        }
}
