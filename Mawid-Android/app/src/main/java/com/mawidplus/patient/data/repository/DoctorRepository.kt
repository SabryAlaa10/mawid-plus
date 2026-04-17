package com.mawidplus.patient.data.repository

import com.mawidplus.patient.core.model.Doctor
import com.mawidplus.patient.core.network.SupabaseProvider
import com.mawidplus.patient.data.dto.DoctorDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DoctorRepository(
    private val supabase: SupabaseClient = SupabaseProvider.client
) {

    private companion object {
        private const val TAG = "DoctorRepository"
        /** Clinic embed; explicit columns (no SELECT *). */
        private val DOCTOR_WITH_CLINIC = Columns.raw(
            "id, clinic_id, full_name, specialty, slot_duration_minutes, image_url, avatar_url, " +
                "experience_years, about, rating, review_count, consultation_fee_sar, available_days, " +
                "start_time, end_time, latitude, longitude, clinic_address, created_at, " +
                "clinics(name, address)",
        )
        /** PostgREST default max rows is 1000; cap list explicitly for predictable behavior. */
        private const val MAX_DOCTOR_LIST = 1000L
    }

    suspend fun listDoctors(): Result<List<Doctor>> = traceRepositoryCall(TAG, "listDoctors") {
        withContext(Dispatchers.IO) {
            safeCall {
                supabase.from("doctors").select(columns = DOCTOR_WITH_CLINIC) {
                    order(column = "full_name", order = Order.ASCENDING)
                    limit(MAX_DOCTOR_LIST)
                }.decodeList<DoctorDto>()
                    .map { it.toDomain() }
            }
        }
    }

    suspend fun getDoctorById(id: String): Result<Doctor> = traceRepositoryCall(TAG, "getDoctorById") {
        withContext(Dispatchers.IO) {
            safeCall {
                supabase.from("doctors").select(columns = DOCTOR_WITH_CLINIC) {
                    filter {
                        eq("id", id)
                    }
                }.decodeSingle<DoctorDto>().toDomain()
            }
        }
    }

    suspend fun getDoctorsByIds(ids: List<String>): Result<List<Doctor>> = traceRepositoryCall(TAG, "getDoctorsByIds") {
        if (ids.isEmpty()) {
            Result.Success(emptyList())
        } else {
            withContext(Dispatchers.IO) {
                safeCall {
                    supabase.from("doctors").select(columns = DOCTOR_WITH_CLINIC) {
                        filter {
                            isIn("id", ids)
                        }
                    }.decodeList<DoctorDto>().map { it.toDomain() }
                }
            }
        }
    }
}
