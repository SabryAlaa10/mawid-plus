package com.mawidplus.patient.data.repository

import com.mawidplus.patient.core.model.Doctor
import com.mawidplus.patient.core.network.SupabaseProvider
import com.mawidplus.patient.data.dto.DoctorDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DoctorRepository(
    private val supabase: SupabaseClient = SupabaseProvider.client
) {

    private companion object {
        private const val TAG = "DoctorRepository"
        /** Clinic name + address embed; all doctor columns from *. */
        private val DOCTOR_WITH_CLINIC = Columns.raw("*, clinics(name, address)")
    }

    suspend fun listDoctors(): Result<List<Doctor>> = traceRepositoryCall(TAG, "listDoctors") {
        withContext(Dispatchers.IO) {
            safeCall {
                supabase.from("doctors").select(columns = DOCTOR_WITH_CLINIC).decodeList<DoctorDto>()
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
