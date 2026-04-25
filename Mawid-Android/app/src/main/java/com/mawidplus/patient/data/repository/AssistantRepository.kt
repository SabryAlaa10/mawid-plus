package com.mawidplus.patient.data.repository

import android.util.Log
import com.mawidplus.patient.BuildConfig
import com.mawidplus.patient.core.network.SupabaseProvider
import com.mawidplus.patient.data.dto.ChatRequestDto
import com.mawidplus.patient.data.dto.ChatResponseDto
import com.mawidplus.patient.data.dto.ChatTurnDto
import io.github.jan.supabase.gotrue.auth
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
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
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class AssistantRepository(
    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    },
) {

    private companion object {
        private const val TAG = "AssistantRepository"
    }

    private val httpClient: HttpClient by lazy {
        HttpClient(Android) {
            install(HttpTimeout) {
                requestTimeoutMillis = 120_000
                connectTimeoutMillis = 30_000
            }
            install(ContentNegotiation) {
                json(json)
            }
        }
    }

    suspend fun chat(
        message: String,
        sessionId: String?,
        history: List<ChatTurnDto> = emptyList(),
    ): Result<ChatResponseDto> = traceRepositoryCall(TAG, "chat") {
        withContext(Dispatchers.IO) {
            val auth = SupabaseProvider.client.auth
            if (auth.currentSessionOrNull() == null) {
                return@withContext Result.Error("يرجى تسجيل الدخول أولاً", null)
            }
            // accessToken عادة يساعة؛ بدون تحديث يفشل GET /auth/v1/user والسيرفر يردّ «جلسة منتهية»
            runCatching { auth.refreshCurrentSession() }
                .onFailure { e ->
                    Log.w(TAG, "refreshCurrentSession", e)
                    return@withContext Result.Error(
                        "تعذر تحديث جلسة الأمان. سجّل خروجاً ثم دخولاً من جديد.",
                        e,
                    )
                }
            val token = auth.currentSessionOrNull()?.accessToken
                ?: return@withContext Result.Error("يرجى تسجيل الدخول أولاً", null)
            try {
                val base = BuildConfig.ASSISTANT_API_BASE_URL.trimEnd('/')
                val response = httpClient.post("$base/api/chat") {
                    header("Authorization", "Bearer $token")
                    contentType(ContentType.Application.Json)
                    setBody(
                        ChatRequestDto(
                            message = message,
                            sessionId = sessionId,
                            history = history,
                        ),
                    )
                }
                val code = response.status.value
                if (code !in 200..299) {
                    val body = runCatching { response.bodyAsText() }.getOrNull().orEmpty()
                    Log.w(TAG, "chat http $code $body")
                    return@withContext Result.Error(
                        formatAssistantApiError(body, code),
                        null,
                    )
                }
                Result.Success(response.body<ChatResponseDto>())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "chat", e)
                Result.Error(e.message ?: "حدث خطأ غير متوقع", e)
            }
        }
    }
}

/** يردّ { "detail": "..." } من FastAPI كنص واضح للمستخدم. */
private val errorBodyJson = Json { ignoreUnknownKeys = true; isLenient = true }

private fun formatAssistantApiError(body: String, code: Int): String {
    if (body.isBlank()) return "تعذر الاتصال بالمساعد ($code)"
    return runCatching {
        val root = errorBodyJson.parseToJsonElement(body).jsonObject
        val detail = root["detail"]?.jsonPrimitive?.contentOrNull
        if (!detail.isNullOrBlank()) detail else body
    }.getOrElse { body }
}
