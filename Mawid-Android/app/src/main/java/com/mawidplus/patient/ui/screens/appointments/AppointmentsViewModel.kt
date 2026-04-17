package com.mawidplus.patient.ui.screens.appointments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mawidplus.patient.PatientApp
import com.mawidplus.patient.core.model.Appointment
import com.mawidplus.patient.core.notifications.NotificationHelper
import com.mawidplus.patient.data.repository.AppointmentRepository
import com.mawidplus.patient.data.repository.AuthRepository
import com.mawidplus.patient.data.repository.DoctorRepository
import com.mawidplus.patient.data.repository.Result
import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AppointmentListRow(
    val appointment: Appointment,
    val doctorName: String,
    val specialty: String,
)

sealed class AppointmentsUiState {
    data object Loading : AppointmentsUiState()
    data object NotSignedIn : AppointmentsUiState()
    data class Ready(val rows: List<AppointmentListRow>) : AppointmentsUiState()
    data class Error(val message: String) : AppointmentsUiState()
}

sealed class AppointmentsUiEvent {
    data class Message(val text: String, val isError: Boolean) : AppointmentsUiEvent()
}

class AppointmentsViewModel(
    private val appointmentRepository: AppointmentRepository = AppointmentRepository(),
    private val doctorRepository: DoctorRepository = DoctorRepository(),
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private companion object {
        private const val TAG = "AppointmentsVM"
    }

    private val _uiState = MutableStateFlow<AppointmentsUiState>(AppointmentsUiState.Loading)
    val uiState: StateFlow<AppointmentsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AppointmentsUiEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<AppointmentsUiEvent> = _events.asSharedFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = AppointmentsUiState.Loading
            val uid = authRepository.getCurrentUserIdOrNull()
            if (uid == null) {
                _uiState.value = AppointmentsUiState.NotSignedIn
                return@launch
            }
            when (val apps = appointmentRepository.getPatientAppointments(uid)) {
                is Result.Success -> {
                    val visible = apps.data.filter { !isCancelledStatus(it.status) }
                    Log.d(TAG, "refresh: userId=$uid appointments=${apps.data.size} visible=${visible.size}")
                    val ids = visible.map { it.doctorId }.distinct()
                    when (val docs = doctorRepository.getDoctorsByIds(ids)) {
                        is Result.Success -> {
                            val byId = docs.data.associateBy { it.id }
                            val rows = visible.map { ap ->
                                val d = byId[ap.doctorId]
                                AppointmentListRow(
                                    appointment = ap,
                                    doctorName = d?.fullName ?: "طبيب",
                                    specialty = d?.specialty ?: "—",
                                )
                            }
                            _uiState.value = AppointmentsUiState.Ready(rows)
                        }
                        is Result.Error -> {
                            Log.e(TAG, "getDoctorsByIds failed, showing appointments without names", docs.exception)
                            val rows = visible.map { ap ->
                                AppointmentListRow(
                                    appointment = ap,
                                    doctorName = "طبيب",
                                    specialty = "—",
                                )
                            }
                            _uiState.value = AppointmentsUiState.Ready(rows)
                        }
                        else -> {}
                    }
                }
                is Result.Error -> _uiState.value = AppointmentsUiState.Error(apps.message)
                else -> {}
            }
        }
    }

    /** Optimistic remove, then server cancel; revert list on failure. */
    fun cancelAppointment(appointmentId: String) {
        val ready = _uiState.value as? AppointmentsUiState.Ready ?: return
        val uid = authRepository.getCurrentUserIdOrNull()
        if (uid == null) {
            viewModelScope.launch {
                _events.emit(AppointmentsUiEvent.Message("سجّل الدخول لإلغاء الموعد", isError = true))
            }
            return
        }
        val previousRows = ready.rows
        val optimistic = previousRows.filter { it.appointment.id != appointmentId }
        _uiState.value = AppointmentsUiState.Ready(optimistic)

        viewModelScope.launch {
            when (val r = appointmentRepository.cancelAppointment(appointmentId, uid)) {
                is Result.Success -> {
                    PatientApp.appContextOrNull()?.let { ctx ->
                        NotificationHelper.cancelAppointmentReminders(ctx, appointmentId)
                    }
                    _events.emit(AppointmentsUiEvent.Message("تم إلغاء الموعد", isError = false))
                    refresh()
                }
                is Result.Error -> {
                    _uiState.value = AppointmentsUiState.Ready(previousRows)
                    _events.emit(
                        AppointmentsUiEvent.Message(
                            r.message.ifBlank { "فشل إلغاء الموعد، حاول مرة أخرى" },
                            isError = true
                        )
                    )
                }
                else -> {
                    _uiState.value = AppointmentsUiState.Ready(previousRows)
                    _events.emit(
                        AppointmentsUiEvent.Message("فشل إلغاء الموعد، حاول مرة أخرى", isError = true)
                    )
                }
            }
        }
    }

    private fun isCancelledStatus(status: String): Boolean =
        status.lowercase().trim() == "cancelled"
}
