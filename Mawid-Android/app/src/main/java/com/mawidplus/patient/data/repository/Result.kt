package com.mawidplus.patient.data.repository

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String, val exception: Throwable? = null) : Result<Nothing>()
    data object Loading : Result<Nothing>()
}

fun <T> Flow<T>.asResult(): Flow<Result<T>> = this
    .map<T, Result<T>> { Result.Success(it) }
    .catch { e -> emit(Result.Error(e.message ?: "Unknown error", e)) }
    .onStart { emit(Result.Loading) }

suspend fun <T> safeCall(action: suspend () -> T): Result<T> = try {
    Result.Success(action())
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Result.Error(e.message ?: "Unknown error", e)
}

/**
 * Logs start / outcome of a repository suspend call (Logcat tag = [tag]).
 */
suspend fun <T> traceRepositoryCall(tag: String, operation: String, block: suspend () -> Result<T>): Result<T> {
    Log.d(tag, "$operation: start")
    return try {
        val result = block()
        when (result) {
            is Result.Success -> Log.d(tag, "$operation: success")
            is Result.Error -> {
                val ex = result.exception
                if (ex != null) Log.w(tag, "$operation: error ${result.message}", ex)
                else Log.d(tag, "$operation: error ${result.message}")
            }
            Result.Loading -> Log.d(tag, "$operation: loading")
        }
        result
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(tag, "$operation: unexpected exception", e)
        Result.Error(e.message ?: "Unknown error", e)
    }
}
