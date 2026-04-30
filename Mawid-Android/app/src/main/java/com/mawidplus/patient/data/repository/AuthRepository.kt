package com.mawidplus.patient.data.repository

import android.util.Log
import com.mawidplus.patient.PatientApp
import com.mawidplus.patient.core.model.Profile
import com.mawidplus.patient.core.network.SupabaseProvider
import com.mawidplus.patient.core.phone.EgyptPhone
import com.mawidplus.patient.core.session.UiSessionPrefs
import com.mawidplus.patient.data.dto.ProfileDto
import com.mawidplus.patient.data.dto.ProfileFullNameDto
import com.mawidplus.patient.data.dto.ProfileInsertDto
import com.mawidplus.patient.data.dto.ProfileWriteDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * بدون OTP: تسجيل دخول عبر Anonymous Auth في Supabase ثم حفظ الهاتف والاسم في [profiles].
 * تسجيل Google يستخدم نفس المسار بعد التحقق من Google على الجهاز، مع ربط رقم الهاتف من النموذج.
 */
class AuthRepository {

    private companion object {
        private const val TAG = "AuthRepository"
    }

    /** رسالة لمرة واحدة عند تسجيل دخول تلقائي لرقم مسجّل مسبقاً (إن وُجدت مستقبلاً). */
    private var pendingDuplicateLoginMessage: String? = null

    fun consumePendingDuplicateLoginMessage(): String? {
        val m = pendingDuplicateLoginMessage
        pendingDuplicateLoginMessage = null
        return m
    }

    suspend fun registerNewPatient(phoneLocal: String, fullName: String): Result<Profile> =
        traceRepositoryCall(TAG, "registerNewPatient") {
            withContext(Dispatchers.IO) {
                if (!EgyptPhone.isValidLocal(phoneLocal)) {
                    return@withContext Result.Error("أدخل رقم موبايل مصري صحيح (10 أرقام تبدأ بـ 1)", null)
                }
                val name = fullName.trim()
                if (name.length < 3) {
                    return@withContext Result.Error("الاسم يجب أن يكون 3 أحرف على الأقل", null)
                }
                try {
                    val profile = runPhoneSignIn(phoneLocal, name)
                    Result.Success(profile)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: RestException) {
                    Result.Error(e.message ?: "فشل الاتصال بـ Supabase", e)
                } catch (e: Exception) {
                    Result.Error(e.message ?: "تعذر إكمال التسجيل", e)
                }
            }
        }

    private val supabase: SupabaseClient
        get() = SupabaseProvider.client

    private fun toE164(localDigits: String): String = "${EgyptPhone.DIAL_CODE}$localDigits"

    fun signInWithPhoneOnly(localPhone: String, fullName: String?): Flow<Result<Profile?>> = flow {
        Log.d(TAG, "signInWithPhoneOnly start register=${fullName != null}")
        emit(Result.Loading)
        if (!EgyptPhone.isValidLocal(localPhone)) {
            Log.d(TAG, "signInWithPhoneOnly invalid phone")
            emit(Result.Error("أدخل رقم موبايل مصري صحيح (10 أرقام تبدأ بـ 1)", null))
            return@flow
        }
        try {
            val profile = withContext(Dispatchers.IO) {
                runPhoneSignIn(localPhone, fullName)
            }
            Log.d(TAG, "signInWithPhoneOnly success userId=${profile.id}")
            emit(Result.Success(profile))
        } catch (e: CancellationException) {
            throw e
        } catch (e: RestException) {
            Log.w(TAG, "signInWithPhoneOnly RestException", e)
            emit(Result.Error(e.message ?: "فشل الاتصال بـ Supabase", e))
        } catch (e: Exception) {
            Log.e(TAG, "signInWithPhoneOnly error", e)
            emit(Result.Error(e.message ?: "تعذر إكمال التسجيل", e))
        } catch (t: Throwable) {
            Log.e(TAG, "signInWithPhoneOnly throwable", t)
            emit(Result.Error(t.message ?: "تعذر إكمال التسجيل", null))
        }
    }.flowOn(Dispatchers.IO)

    fun signInWithGoogleAndPhone(
        localPhone: String,
        idToken: String,
        email: String?,
        displayName: String?,
        fullNameFromForm: String?
    ): Flow<Result<Profile?>> = flow {
        Log.d(TAG, "signInWithGoogleAndPhone start")
        emit(Result.Loading)
        if (idToken.isBlank()) {
            Log.d(TAG, "signInWithGoogleAndPhone blank idToken")
            emit(Result.Error("تعذر إكمال تسجيل الدخول بجوجل", null))
            return@flow
        }
        if (!EgyptPhone.isValidLocal(localPhone)) {
            emit(Result.Error("أدخل رقم موبايل مصري صحيح قبل المتابعة مع Google", null))
            return@flow
        }
        val preferredName = fullNameFromForm?.trim()?.takeIf { it.isNotEmpty() }
            ?: displayName?.takeIf { it.isNotBlank() }
            ?: email?.substringBefore("@")?.takeIf { it.isNotBlank() }
        try {
            val profile = withContext(Dispatchers.IO) {
                runPhoneSignIn(localPhone, preferredName)
            }
            Log.d(TAG, "signInWithGoogleAndPhone success userId=${profile.id}")
            emit(Result.Success(profile))
        } catch (e: CancellationException) {
            throw e
        } catch (e: RestException) {
            Log.w(TAG, "signInWithGoogleAndPhone RestException", e)
            emit(Result.Error(e.message ?: "فشل الاتصال بـ Supabase", e))
        } catch (e: Exception) {
            Log.e(TAG, "signInWithGoogleAndPhone error", e)
            emit(Result.Error(e.message ?: "تعذر إكمال التسجيل", e))
        } catch (t: Throwable) {
            Log.e(TAG, "signInWithGoogleAndPhone throwable", t)
            emit(Result.Error(t.message ?: "تعذر إكمال التسجيل", null))
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun runPhoneSignIn(localPhone: String, fullName: String?): Profile {
        supabase.auth.awaitInitialization()
        ensureAuthenticatedUser()
        val userId = supabase.auth.currentUserOrNull()?.id
            ?: error("لم تُنشأ جلسة بعد تسجيل الدخول")
        val phoneStored = toE164(localPhone)
        val resolvedName = resolveFullName(userId, fullName)
        upsertProfilePhoneAndName(userId, resolvedName, phoneStored)
        val profile = loadProfile(userId, localPhone, resolvedName)
        PatientApp.appContextOrNull()?.let { UiSessionPrefs.markHomeAccess(it) }
        return profile
    }

    private suspend fun ensureAuthenticatedUser() {
        if (supabase.auth.currentUserOrNull() != null) return
        try {
            supabase.auth.signInAnonymously()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            throw Exception(
                "فعّل «Anonymous» في Supabase (Authentication → Providers → Anonymous) أو تحقق من الإنترنت.",
                e
            )
        }
    }

    private suspend fun resolveFullName(userId: String, incoming: String?): String {
        val trimmed = incoming?.trim().orEmpty()
        if (trimmed.isNotEmpty()) return trimmed
        val existing = readFullName(userId)
        if (!existing.isNullOrBlank()) return existing
        return "مستخدم موعد+"
    }

    private suspend fun readFullName(userId: String): String? {
        return try {
            val row = supabase.from("profiles").select(columns = Columns.ALL) {
                filter { eq("id", userId) }
            }.decodeSingle<ProfileDto>()
            row.fullName
        } catch (e: Exception) {
            Log.w(TAG, "readFullName: no row or error userId=$userId", e)
            null
        }
    }

    private suspend fun upsertProfilePhoneAndName(userId: String, fullName: String, phoneE164: String) {
        val patch = ProfileWriteDto(fullName = fullName, phone = phoneE164)
        try {
            supabase.from("profiles").update(patch) {
                filter { eq("id", userId) }
            }
            Log.d(TAG, "upsertProfile: update attempted userId=$userId")
        } catch (e: Exception) {
            Log.w(TAG, "upsertProfile: update failed (may be new user) userId=$userId", e)
        }
        if (!profileRowMissing(userId)) {
            Log.d(TAG, "upsertProfile: row exists after update userId=$userId")
            return
        }
        try {
            supabase.from("profiles").insert(
                ProfileInsertDto(id = userId, fullName = fullName, phone = phoneE164)
            )
            Log.d(TAG, "upsertProfile: insert success userId=$userId")
        } catch (e: Exception) {
            Log.w(TAG, "upsertProfile: insert failed, retry update userId=$userId", e)
            try {
                supabase.from("profiles").update(patch) {
                    filter { eq("id", userId) }
                }
            } catch (e2: Exception) {
                Log.e(TAG, "upsertProfile: retry update failed userId=$userId", e2)
            }
        }
    }

    private suspend fun profileRowMissing(userId: String): Boolean {
        return try {
            supabase.from("profiles").select(columns = Columns.ALL) {
                filter { eq("id", userId) }
            }.decodeSingle<ProfileDto>()
            false
        } catch (e: Exception) {
            Log.d(TAG, "profileRowMissing: true userId=$userId (${e.message})")
            true
        }
    }

    private suspend fun loadProfile(userId: String, fallbackLocalPhone: String, fallbackName: String): Profile {
        return try {
            supabase.from("profiles").select(columns = Columns.ALL) {
                filter { eq("id", userId) }
            }.decodeSingle<ProfileDto>().toDomain()
        } catch (e: Exception) {
            Log.w(TAG, "loadProfile: using fallback (select failed) userId=$userId", e)
            Profile(
                id = userId,
                fullName = fallbackName,
                phone = fallbackLocalPhone,
                role = "patient"
            )
        }
    }

    fun signOut(): Flow<Result<Unit>> = flow {
        Log.d(TAG, "signOut start")
        emit(Result.Loading)
        try {
            withContext(Dispatchers.IO) {
                try {
                    supabase.auth.signOut()
                } finally {
                    PatientApp.appContextOrNull()?.let { UiSessionPrefs.clear(it) }
                }
            }
            Log.d(TAG, "signOut success")
            emit(Result.Success(Unit))
        } catch (e: Exception) {
            Log.e(TAG, "signOut error", e)
            PatientApp.appContextOrNull()?.let { UiSessionPrefs.clear(it) }
            emit(Result.Error(e.message ?: "فشل تسجيل الخروج", e))
        }
    }.flowOn(Dispatchers.IO)

    fun getCurrentProfile(): Profile? = null

    suspend fun fetchProfileForCurrentUser(): Result<Profile> =
        traceRepositoryCall(TAG, "fetchProfileForCurrentUser") {
            withContext(Dispatchers.IO) {
                val uid = getCurrentUserIdOrNull()
                if (uid == null) {
                    return@withContext Result.Error("غير مسجل", null)
                }
                safeCall {
                    supabase.from("profiles").select(columns = Columns.ALL) {
                        filter { eq("id", uid) }
                    }.decodeSingle<ProfileDto>().toDomain()
                }
            }
        }

    suspend fun updatePatientProfile(userId: String, fullName: String): Result<Unit> =
        traceRepositoryCall(TAG, "updatePatientProfile") {
            withContext(Dispatchers.IO) {
                safeCall {
                    val trimmed = fullName.trim()
                    if (trimmed.length < 3) {
                        throw IllegalArgumentException("الاسم يجب أن يكون 3 أحرف على الأقل")
                    }
                    supabase.from("profiles").update(ProfileFullNameDto(trimmed)) {
                        filter { eq("id", userId) }
                    }
                    Log.d(TAG, "Profile updated for: $userId")
                    Unit
                }
            }
        }

    fun getCurrentUserIdOrNull(): String? = try {
        supabase.auth.currentUserOrNull()?.id
    } catch (_: Throwable) {
        null
    }
}
