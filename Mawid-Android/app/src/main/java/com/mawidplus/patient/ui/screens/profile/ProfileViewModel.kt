package com.mawidplus.patient.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mawidplus.patient.core.model.Profile
import com.mawidplus.patient.data.repository.AuthRepository
import com.mawidplus.patient.data.repository.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ProfileUiState {
    data object Loading : ProfileUiState()
    data object NotSignedIn : ProfileUiState()
    data class Ready(
        val profile: Profile,
        val isEditMode: Boolean = false,
        val editedName: String = "",
        val isSaving: Boolean = false,
    ) : ProfileUiState()

    data class Error(val message: String) : ProfileUiState()
}

class ProfileViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private companion object {
        private const val MAX_NAME_LEN = 80
    }

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            when (val r = authRepository.fetchProfileForCurrentUser()) {
                is Result.Success -> {
                    val p = r.data
                    _uiState.value = ProfileUiState.Ready(
                        profile = p,
                        editedName = p.fullName,
                    )
                }
                is Result.Error -> {
                    if (r.message.contains("غير مسجل")) {
                        _uiState.value = ProfileUiState.NotSignedIn
                    } else {
                        _uiState.value = ProfileUiState.Error(r.message)
                    }
                }
                else -> {}
            }
        }
    }

    fun toggleEditMode() {
        val s = _uiState.value as? ProfileUiState.Ready ?: return
        if (s.isEditMode) {
            _uiState.value = s.copy(isEditMode = false, editedName = s.profile.fullName)
        } else {
            _uiState.value = s.copy(isEditMode = true, editedName = s.profile.fullName)
        }
    }

    fun updateEditedName(value: String) {
        val s = _uiState.value as? ProfileUiState.Ready ?: return
        val clipped = value.take(MAX_NAME_LEN)
        _uiState.value = s.copy(editedName = clipped)
    }

    fun cancelEdit() {
        val s = _uiState.value as? ProfileUiState.Ready ?: return
        _uiState.value = s.copy(isEditMode = false, editedName = s.profile.fullName)
    }

    fun saveProfile(onMessage: (String, Boolean) -> Unit) {
        val s = _uiState.value as? ProfileUiState.Ready ?: return
        val name = s.editedName.replace(Regex("\\s+"), " ").trim()
        if (name.length < 3) {
            onMessage("الاسم يجب أن يكون 3 أحرفاً على الأقل", true)
            return
        }
        viewModelScope.launch {
            _uiState.value = s.copy(isSaving = true)
            val uid = authRepository.getCurrentUserIdOrNull()
            if (uid == null) {
                onMessage("سجّل الدخول أولاً", true)
                _uiState.value = s.copy(isSaving = false)
                return@launch
            }
            when (val r = authRepository.updatePatientProfile(uid, name)) {
                is Result.Success -> {
                    val updated = s.profile.copy(fullName = name)
                    _uiState.value = ProfileUiState.Ready(
                        profile = updated,
                        isEditMode = false,
                        editedName = name,
                        isSaving = false,
                    )
                    onMessage("تم حفظ اسمك بنجاح", false)
                }
                is Result.Error -> {
                    _uiState.value = s.copy(isSaving = false)
                    onMessage(r.message.ifBlank { "فشل تحديث البيانات" }, true)
                }
                else -> {
                    _uiState.value = s.copy(isSaving = false)
                }
            }
        }
    }
}
