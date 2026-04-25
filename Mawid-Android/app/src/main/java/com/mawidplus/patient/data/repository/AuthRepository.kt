package com.mawidplus.patient.data.repository

import android.util.Log
import com.mawidplus.patient.PatientApp
import com.mawidplus.patient.core.model.Profile
import com.mawidplus.patient.core.network.SupabaseProvider
import com.mawidplus.patient.core.phone.EgyptPhone
import com.mawidplus.patient.core.session.UiSessionPrefs
import com.mawidplus.patient.data.dto.ProfileDto
import com.mawidplus.patient.data.dto.ProfileFullNameDto
import com.mawidplus.patient.BuildConfig
import com.mawidplus.patient.data.dto.ProfileInsertDto
import com.mawidplus.patient.data.dto.ProfileWriteDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.Google
import io.github.jan.supabase.gotrue.providers.builtin.IDToken
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * تسجيل عبر Anonymous Auth + ملف في [profiles] برقم حقيقي واسم حقيقي للمستخدمين الجدد.
 * التحقق من الرقم عبر RPC ([check_phone_registered]) لأن RLS لا يسمح بقراءة أرقام الآخرين.
 * مسار Google: [signInWithGoogleAndPhone] يمرّر [IDToken] إلى Supabase (مزوّد Google في لوحة التحكم) ثم يكمل ربط الرقم في [profiles].
 */
class AuthRepository {

    private companion object {
        private const val TAG = "AuthRepository"
        private val PROFILE_COLUMNS = Columns.raw(
            "id, full_name, phone, role, created_at, updated_at",
        )
    }

    /** رسالة لمرة واحدة عند تسجيل برقم مسجل مسبقاً (عرض Toast من الشاشة). */
    @Volatile
    private var pendingDuplicateLoginMessage: String? = null

    fun consumePendingDuplicateLoginMessage(): String? {
        val m = pendingDuplicateLoginMessage
        pendingDuplicateLoginMessage = null
        return m
    }

    private val supabase: SupabaseClient
        get() = SupabaseProvider.client

    private val rpcJson = Json { ignoreUnknownKeys = true }

    private val rpcHttpClient by lazy {
        HttpClient(Android) {
            install(ContentNegotiation) {
                json(rpcJson)
            }
        }
    }

    private fun toE164(localDigits: String): String = EgyptPhone.normalizeToE164(localDigits)

    /** PostgREST 2.5.x لا يعرض [rpc] على الواجهة — نستدعي `/rest/v1/rpc/...` مباشرة. */
    private suspend fun postRpcJson(
        function: String,
        body: kotlinx.serialization.json.JsonObject,
        forceAnonBearer: Boolean = false,
    ): String {
        val bearer = when {
            forceAnonBearer -> BuildConfig.SUPABASE_ANON_KEY
            else -> supabase.auth.currentSessionOrNull()?.accessToken ?: BuildConfig.SUPABASE_ANON_KEY
        }
        val url = "${BuildConfig.SUPABASE_URL.trimEnd('/')}/rest/v1/rpc/$function"
        return rpcHttpClient.post(url) {
            header("apikey", BuildConfig.SUPABASE_ANON_KEY)
            header("Authorization", "Bearer $bearer")
            contentType(ContentType.Application.Json)
            setBody(body)
        }.bodyAsText()
    }

    @Serializable
    private data class RpcClaimResult(
        val ok: Boolean,
        val error: String? = null,
        @SerialName("profile_id") val profileId: String? = null,
    )

    suspend fun checkPhoneExists(localPhone: String): Result<Boolean> =
        traceRepositoryCall(TAG, "checkPhoneExists") {
            withContext(Dispatchers.IO) {
                if (!EgyptPhone.isValidLocal(localPhone)) {
                    return@withContext Result.Error("أدخل رقم موبايل مصري صحيح (10 أرقام تبدأ بـ 1)", null)
                }
                safeCall {
                    val normalized = toE164(localPhone)
                    val raw = postRpcJson(
                        "check_phone_registered",
                        buildJsonObject { put("phone_e164", normalized) },
                        forceAnonBearer = true,
                    )
                    rpcJson.parseToJsonElement(raw).jsonPrimitive.booleanOrNull
                        ?: error("استجابة غير متوقعة من الخادم")
                }
            }
        }

    suspend fun registerNewPatient(localPhone: String, fullName: String): Result<Profile> =
        traceRepositoryCall(TAG, "registerNewPatient") {
            withContext(Dispatchers.IO) {
                safeCall {
                    if (!EgyptPhone.isValidLocal(localPhone)) {
                        throw IllegalArgumentException("أدخل رقم موبايل مصري صحيح (10 أرقام تبدأ بـ 1)")
                    }
                    val trimmed = validateFullNameOrThrow(fullName)
                    supabase.auth.awaitInitialization()
                    ensureAuthenticatedUser()
                    val userId = supabase.auth.currentUserOrNull()?.id
                        ?: error("لم تُنشأ جلسة بعد تسجيل الدخول")
                    val normalized = toE164(localPhone)
                    try {
                        writePatientProfileRow(userId, trimmed, normalized)
                    } catch (e: RestException) {
                        val msg = e.message?.lowercase().orEmpty()
                        if (msg.contains("duplicate") || msg.contains("unique") || msg.contains("profiles_phone")) {
                            Log.w(TAG, "registerNewPatient: phone already registered, logging in instead", e)
                            pendingDuplicateLoginMessage =
                                "هذا الرقم مسجل بالفعل، جارٍ تسجيل الدخول…"
                            return@safeCall loginExistingPatientInternal(localPhone, showDuplicateMessage = true)
                        }
                        throw e
                    } catch (e: Exception) {
                        val msg = e.message?.lowercase().orEmpty()
                        if (msg.contains("duplicate") || msg.contains("unique")) {
                            Log.w(TAG, "registerNewPatient: duplicate phone", e)
                            pendingDuplicateLoginMessage =
                                "هذا الرقم مسجل بالفعل، جارٍ تسجيل الدخول…"
                            return@safeCall loginExistingPatientInternal(localPhone, showDuplicateMessage = true)
                        }
                        throw e
                    }
                    PatientApp.appContextOrNull()?.let { ctx ->
                        UiSessionPrefs.markHomeAccess(ctx)
                        UiSessionPrefs.saveLastAuthUserId(ctx, userId)
                    }
                    loadProfile(userId, localPhone, trimmed)
                }
            }
        }

    suspend fun loginExistingPatient(localPhone: String): Result<Profile> =
        traceRepositoryCall(TAG, "loginExistingPatient") {
            withContext(Dispatchers.IO) {
                safeCall {
                    loginExistingPatientInternal(localPhone, showDuplicateMessage = false)
                }
            }
        }

    private suspend fun loginExistingPatientInternal(
        localPhone: String,
        showDuplicateMessage: Boolean,
    ): Profile {
        if (!EgyptPhone.isValidLocal(localPhone)) {
            throw IllegalArgumentException("أدخل رقم موبايل مصري صحيح (10 أرقام تبدأ بـ 1)")
        }
        supabase.auth.awaitInitialization()
        ensureAuthenticatedUser()
        val userId = supabase.auth.currentUserOrNull()?.id
            ?: error("لم تُنشأ جلسة بعد تسجيل الدخول")
        val normalized = toE164(localPhone)
        val raw = postRpcJson(
            "claim_patient_profile_by_phone",
            buildJsonObject { put("phone_e164", normalized) },
        )
        val claim = rpcJson.decodeFromString(RpcClaimResult.serializer(), raw)
        if (!claim.ok) {
            val err = claim.error ?: "unknown"
            Log.e(TAG, "claim failed: $err")
            throw IllegalStateException(
                when (err) {
                    "phone_not_found" -> "لم يُعثر على حساب بهذا الرقم. أنشئ حساباً جديداً."
                    "not_authenticated" -> "انتهت الجلسة. أعد المحاولة."
                    else -> claim.error ?: "تعذر ربط الحساب"
                }
            )
        }
        PatientApp.appContextOrNull()?.let { ctx ->
            UiSessionPrefs.markHomeAccess(ctx)
            UiSessionPrefs.saveLastAuthUserId(ctx, userId)
        }
        val profile = loadProfile(userId, localPhone, "")
        if (showDuplicateMessage) {
            Log.d(TAG, "loginExistingPatient: duplicate phone handled as login")
        }
        return profile
    }

    private suspend fun writePatientProfileRow(userId: String, fullName: String, phoneE164: String) {
        val patch = ProfileWriteDto(fullName = fullName, phone = phoneE164)
        try {
            supabase.from("profiles").update(patch) {
                filter { eq("id", userId) }
            }
            Log.d(TAG, "writePatientProfileRow: update userId=$userId")
        } catch (e: Exception) {
            Log.w(TAG, "writePatientProfileRow: update failed userId=$userId", e)
            throw e
        }
        if (!profileRowMissing(userId)) return
        try {
            supabase.from("profiles").insert(
                ProfileInsertDto(id = userId, fullName = fullName, phone = phoneE164)
            )
            Log.d(TAG, "writePatientProfileRow: insert userId=$userId")
        } catch (e: Exception) {
            Log.w(TAG, "writePatientProfileRow: insert failed", e)
            throw e
        }
    }

    private fun validateFullNameOrThrow(fullName: String): String {
        val t = fullName.trim()
        if (t.length < 3) throw IllegalArgumentException("الاسم يجب أن يكون 3 أحرف على الأقل")
        if (t.any { it.isDigit() }) throw IllegalArgumentException("الاسم لا يجب أن يحتوي على أرقام")
        return t
    }

    /** تسجيل الدخول بالهاتف فقط (مسار قديم) — يُستخدم فقط إن لم يُستبدل بالكامل. */
    fun signInWithPhoneOnly(localPhone: String, fullName: String?): Flow<Result<Profile?>> = flow {
        Log.d(TAG, "signInWithPhoneOnly (legacy) register=${fullName != null}")
        emit(Result.Loading)
        if (!EgyptPhone.isValidLocal(localPhone)) {
            emit(Result.Error("أدخل رقم موبايل مصري صحيح (10 أرقام تبدأ بـ 1)", null))
            return@flow
        }
        try {
            val profile = withContext(Dispatchers.IO) {
                when {
                    fullName != null -> registerNewPatient(localPhone, fullName).let {
                        when (it) {
                            is Result.Success -> it.data
                            is Result.Error -> throw it.exception ?: Exception(it.message)
                            Result.Loading -> error("unexpected")
                        }
                    }
                    else -> loginExistingPatient(localPhone).let {
                        when (it) {
                            is Result.Success -> it.data
                            is Result.Error -> throw it.exception ?: Exception(it.message)
                            Result.Loading -> error("unexpected")
                        }
                    }
                }
            }
            emit(Result.Success(profile))
        } catch (e: CancellationException) {
            throw e
        } catch (e: RestException) {
            emit(Result.Error(e.message ?: "فشل الاتصال بـ Supabase", e))
        } catch (e: Exception) {
            emit(Result.Error(e.message ?: "تعذر إكمال التسجيل", e))
        }
    }.flowOn(Dispatchers.IO)

    /** Google عبر Supabase ([IDToken] + [Google]) ثم تسجيل الدخول أو إنشاء الملف بالرقم كما في المسار الاعتيادي. */
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
                supabase.auth.awaitInitialization()
                supabase.auth.signInWith(IDToken) {
                    this.idToken = idToken
                    provider = Google
                }
                when (val exists = checkPhoneExists(localPhone)) {
                    is Result.Success -> {
                        if (exists.data) {
                            when (val login = loginExistingPatient(localPhone)) {
                                is Result.Success -> login.data
                                is Result.Error -> throw login.exception ?: Exception(login.message)
                                Result.Loading -> error("unexpected")
                            }
                        } else {
                            val name = preferredName ?: throw IllegalArgumentException(
                                "أدخل الاسم الكامل في النموذج أولاً لمتابعة التسجيل بجوجل"
                            )
                            when (val reg = registerNewPatient(localPhone, name)) {
                                is Result.Success -> reg.data
                                is Result.Error -> throw reg.exception ?: Exception(reg.message)
                                Result.Loading -> error("unexpected")
                            }
                        }
                    }
                    is Result.Error -> throw exists.exception ?: Exception(exists.message)
                    Result.Loading -> error("unexpected")
                }
            }
            emit(Result.Success(profile))
        } catch (e: CancellationException) {
            throw e
        } catch (e: RestException) {
            emit(Result.Error(e.message ?: "فشل الاتصال بـ Supabase", e))
        } catch (e: Exception) {
            emit(Result.Error(e.message ?: "تعذر إكمال التسجيل", e))
        }
    }.flowOn(Dispatchers.IO)

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

    private suspend fun profileRowMissing(userId: String): Boolean {
        return try {
            supabase.from("profiles").select(columns = PROFILE_COLUMNS) {
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
            supabase.from("profiles").select(columns = PROFILE_COLUMNS) {
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
                    supabase.from("profiles").select(columns = PROFILE_COLUMNS) {
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
