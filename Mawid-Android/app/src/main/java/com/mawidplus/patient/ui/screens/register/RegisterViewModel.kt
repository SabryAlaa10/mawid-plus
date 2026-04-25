package com.mawidplus.patient.ui.screens.register

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mawidplus.patient.data.repository.AuthRepository
import com.mawidplus.patient.data.repository.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RegisterViewModel(
    private val authRepository: AuthRepository,
    val initialPhoneLocalDigits: String,
) : ViewModel() {

    sealed class UiState {
        data object Idle : UiState()
        data object Submitting : UiState()
        /** duplicateMessage: رسالة لمرة واحدة إذا كان الرقم مسجلاً وتم تسجيل الدخول تلقائياً */
        data class Success(val duplicateLoginMessage: String?) : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun clearError() {
        _uiState.value = UiState.Idle
    }

    fun submitRegister(phoneLocalDigits: String, fullName: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Submitting
            try {
                val phone = phoneLocalDigits.trim()
                if (phone.length != 10 || !phone.startsWith("1")) {
                    _uiState.value = UiState.Error("أدخل رقم موبايل مصري صحيح")
                    return@launch
                }
                val name = fullName.trim()
                if (name.length < 3) {
                    _uiState.value = UiState.Error("الاسم يجب أن يكون 3 أحرف على الأقل")
                    return@launch
                }
                if (name.any { it.isDigit() }) {
                    _uiState.value = UiState.Error("الاسم لا يجب أن يحتوي على أرقام")
                    return@launch
                }
                when (val r = authRepository.registerNewPatient(phone, name)) {
                    is Result.Success -> {
                        val dup = authRepository.consumePendingDuplicateLoginMessage()
                        Log.d(TAG, "submitRegister success profileId=${r.data.id}")
                        _uiState.value = UiState.Success(dup)
                    }
                    is Result.Error -> {
                        _uiState.value = UiState.Error(r.message.ifBlank { "فشل إنشاء الحساب" })
                    }
                    Result.Loading -> Unit
                }
            } catch (e: Exception) {
                Log.e(TAG, "submitRegister", e)
                _uiState.value = UiState.Error(e.message ?: "فشل إنشاء الحساب")
            }
        }
    }

    companion object {
        private const val TAG = "RegisterViewModel"

        fun factory(initialPhone: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return RegisterViewModel(AuthRepository(), initialPhone) as T
                }
            }
    }
}
