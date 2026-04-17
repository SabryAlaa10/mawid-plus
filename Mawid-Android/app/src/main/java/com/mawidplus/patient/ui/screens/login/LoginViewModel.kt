package com.mawidplus.patient.ui.screens.login

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mawidplus.patient.core.model.Profile
import com.mawidplus.patient.data.repository.AuthRepository
import com.mawidplus.patient.data.repository.Result
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class LoginViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private companion object {
        private const val TAG = "LoginViewModel"
    }

    sealed class UiState {
        data object Idle : UiState()
        data object Submitting : UiState()
        /** الرقم غير مسجل — يجب التوجيه لشاشة التسجيل مع الرقم. */
        data object PhoneNotFound : UiState()
        data object Authenticated : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _pendingPhoneLocalDigits = MutableStateFlow<String?>(null)
    val pendingPhoneLocalDigits: StateFlow<String?> = _pendingPhoneLocalDigits.asStateFlow()

    private var authJob: Job? = null

    fun submitPhoneLogin(phoneLocalDigits: String) {
        authJob?.cancel()
        authJob = viewModelScope.launch {
            _uiState.value = UiState.Submitting
            _pendingPhoneLocalDigits.value = phoneLocalDigits
            when (val exists = authRepository.checkPhoneExists(phoneLocalDigits)) {
                is Result.Success -> {
                    if (exists.data) {
                        when (val login = authRepository.loginExistingPatient(phoneLocalDigits)) {
                            is Result.Success -> {
                                Log.d(TAG, "submitPhoneLogin: existing user ${login.data.id}")
                                _uiState.value = UiState.Authenticated
                            }
                            is Result.Error -> {
                                _uiState.value = UiState.Error(
                                    login.message.ifBlank { "فشل تسجيل الدخول" }
                                )
                            }
                            Result.Loading -> Unit
                        }
                    } else {
                        _uiState.value = UiState.PhoneNotFound
                    }
                }
                is Result.Error -> {
                    _uiState.value = UiState.Error(
                        exists.message.ifBlank { "خطأ في التحقق من الرقم" }
                    )
                }
                Result.Loading -> Unit
            }
        }
    }

    fun clearPhoneNotFound() {
        if (_uiState.value is UiState.PhoneNotFound) {
            _uiState.value = UiState.Idle
            _pendingPhoneLocalDigits.value = null
        }
    }

    fun submitPhoneAuth(phone: String, @Suppress("UNUSED_PARAMETER") fullName: String, isRegister: Boolean) {
        if (isRegister) {
            _uiState.value = UiState.Error("استخدم شاشة إنشاء الحساب لإدخال الاسم")
            return
        }
        submitPhoneLogin(phone)
    }

    fun signInWithGoogle(
        localPhone: String,
        idToken: String,
        email: String?,
        displayName: String?,
        fullNameFromForm: String?
    ) {
        authJob?.cancel()
        authJob = authRepository
            .signInWithGoogleAndPhone(localPhone, idToken, email, displayName, fullNameFromForm)
            .catch { e ->
                if (e is CancellationException) throw e
                emit(Result.Error(e.message ?: "فشل الاتصال", null))
            }
            .onEach { result ->
                try {
                    when (result) {
                        Result.Loading -> _uiState.value = UiState.Submitting
                        is Result.Success<*> -> {
                            val profile = result.data as? Profile
                            _uiState.value = if (profile != null) {
                                Log.d(TAG, "signInWithGoogle success profileId=${profile.id}")
                                UiState.Authenticated
                            } else {
                                UiState.Error("تعذر تسجيل الدخول")
                            }
                        }
                        is Result.Error -> _uiState.value = UiState.Error(result.message)
                    }
                } catch (t: Throwable) {
                    if (t is CancellationException) throw t
                    _uiState.value = UiState.Error(t.message ?: "حدث خطأ غير متوقع")
                }
            }
            .launchIn(viewModelScope)
    }

    fun reportError(message: String) {
        _uiState.value = UiState.Error(message)
    }

    fun clearError() {
        _uiState.value = UiState.Idle
    }
}
