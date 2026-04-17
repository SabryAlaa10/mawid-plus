package com.mawidplus.patient.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mawidplus.patient.core.model.Doctor
import com.mawidplus.patient.data.repository.DoctorRepository
import com.mawidplus.patient.data.repository.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class DoctorDetailUiState {
    data object Loading : DoctorDetailUiState()
    data class Ready(val doctor: Doctor) : DoctorDetailUiState()
    data class Error(val message: String) : DoctorDetailUiState()
}

class DoctorDetailViewModel(
    private val doctorId: String,
    private val doctorRepository: DoctorRepository = DoctorRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<DoctorDetailUiState>(DoctorDetailUiState.Loading)
    val uiState: StateFlow<DoctorDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            when (val r = doctorRepository.getDoctorById(doctorId)) {
                is Result.Success -> _uiState.value = DoctorDetailUiState.Ready(r.data)
                is Result.Error -> _uiState.value = DoctorDetailUiState.Error(r.message)
                else -> {}
            }
        }
    }

    companion object {
        fun factory(doctorId: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return DoctorDetailViewModel(doctorId) as T
                }
            }
    }
}
