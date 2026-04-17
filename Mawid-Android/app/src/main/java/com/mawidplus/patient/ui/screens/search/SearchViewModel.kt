package com.mawidplus.patient.ui.screens.search

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mawidplus.patient.data.repository.DoctorRepository
import com.mawidplus.patient.data.repository.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class SearchUiState {
    data object Loading : SearchUiState()
    data class Ready(val doctors: List<DoctorSearchItem>) : SearchUiState()
    data class Error(val message: String) : SearchUiState()
}

class SearchViewModel(
    private val doctorRepository: DoctorRepository = DoctorRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Loading)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        refresh()
    }

    fun setSearchQuery(value: String) {
        _searchQuery.value = value
    }

    fun refresh() {
        viewModelScope.launch {
            Log.d("SearchVM", "fetching doctors...")
            _uiState.value = SearchUiState.Loading
            when (val r = doctorRepository.listDoctors()) {
                is Result.Success -> {
                    val result = r.data.map { it.toDoctorSearchItem() }
                    Log.d("SearchVM", "result: count=${result.size} items=$result")
                    _uiState.value = SearchUiState.Ready(result)
                }
                is Result.Error -> {
                    Log.d("SearchVM", "result: error=${r.message}")
                    _uiState.value = SearchUiState.Error(r.message)
                }
                else -> {}
            }
        }
    }
}
