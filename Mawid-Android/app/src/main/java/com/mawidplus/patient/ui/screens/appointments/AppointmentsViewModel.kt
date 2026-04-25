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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

data class AppointmentListRow(
    val appointment: Appointment,
    val doctorName: String,
    val specialty: String,
    /** لصورة الدائرة في بطاقة الموعد */
    val doctorPhotoUrl: String? = null,
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

    private val _allRows: MutableStateFlow<List<AppointmentListRow>> = MutableStateFlow(emptyList())
    val allRows: StateFlow<List<AppointmentListRow>> = _allRows.asStateFlow()

    private val _statusFilter = MutableStateFlow("الكل")
    val statusFilter: StateFlow<String> = _statusFilter.asStateFlow()

    private val _periodFilter = MutableStateFlow("الكل")
    val periodFilter: StateFlow<String> = _periodFilter.asStateFlow()

    val filteredRows: StateFlow<List<AppointmentListRow>> = combine(
        _allRows,
        _statusFilter,
        _periodFilter,
    ) { rows, status, period ->
        var result = when (status) {
            "قادم" -> rows.filter { it.appointment.status.lowercase() in listOf("scheduled", "waiting", "in_progress") }
            "مكتمل" -> rows.filter { it.appointment.status.lowercase() == "done" }
            "ملغي" -> rows.filter { it.appointment.status.lowercase() in listOf("cancelled", "canceled") }
            else -> rows.filter { !isCancelledStatus(it.appointment.status) }
        }
        val today = LocalDate.now(ZoneId.systemDefault())
        result = when (period) {
            "هذا الشهر" -> result.filter {
                val d = LocalDate.parse(it.appointment.appointmentDate.take(10))
                d.year == today.year && d.monthValue == today.monthValue
            }
            "آخر 3 شهور" -> result.filter {
                val d = LocalDate.parse(it.appointment.appointmentDate.take(10))
                !d.isBefore(today.minusMonths(3))
            }
            "هذه السنة" -> result.filter {
                val d = LocalDate.parse(it.appointment.appointmentDate.take(10))
                d.year == today.year
            }
            else -> result
        }
        result
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _events = MutableSharedFlow<AppointmentsUiEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<AppointmentsUiEvent> = _events.asSharedFlow()

    private val _ratingInFlightId = MutableStateFlow<String?>(null)
    val ratingInFlightId: StateFlow<String?> = _ratingInFlightId.asStateFlow()

    init {
        refresh()
    }

    fun setStatusFilter(value: String) {
        _statusFilter.value = value
    }

    fun setPeriodFilter(value: String) {
        _periodFilter.value = value
    }

    fun refresh(showLoading: Boolean = true) {
        viewModelScope.launch {
            if (showLoading) {
                _uiState.value = AppointmentsUiState.Loading
            }
            val uid = authRepository.getCurrentUserIdOrNull()
            if (uid == null) {
                _allRows.value = emptyList()
                _uiState.value = AppointmentsUiState.NotSignedIn
                return@launch
            }
            when (val apps = appointmentRepository.getPatientAppointments(uid)) {
                is Result.Success -> {
                    val all = apps.data
                    Log.d(TAG, "refresh: userId=$uid appointments=${all.size}")
                    val ids = all.map { it.doctorId }.distinct()
                    when (val docs = doctorRepository.getDoctorsByIds(ids)) {
                        is Result.Success -> {
                            val byId = docs.data.associateBy { it.id }
                            val rows = all.map { ap ->
                                val d = byId[ap.doctorId]
                                AppointmentListRow(
                                    appointment = ap,
                                    doctorName = d?.fullName ?: "طبيب",
                                    specialty = d?.specialty ?: "—",
                                    doctorPhotoUrl = d?.photoUrl,
                                )
                            }
                            _allRows.value = rows
                            _uiState.value = AppointmentsUiState.Ready(rows)
                        }
                        is Result.Error -> {
                            Log.e(TAG, "getDoctorsByIds failed, showing appointments without names", docs.exception)
                            val rows = all.map { ap ->
                                AppointmentListRow(
                                    appointment = ap,
                                    doctorName = "طبيب",
                                    specialty = "—",
                                    doctorPhotoUrl = null,
                                )
                            }
                            _allRows.value = rows
                            _uiState.value = AppointmentsUiState.Ready(rows)
                        }
                        else -> {}
                    }
                }
                is Result.Error -> {
                    _allRows.value = emptyList()
                    _uiState.value = AppointmentsUiState.Error(apps.message)
                }
                else -> {}
            }
        }
    }

    /** Optimistic remove, then server cancel; revert list on failure. */
    fun cancelAppointment(appointmentId: String) {
        val previousRows = _allRows.value
        if (previousRows.isEmpty()) {
            return
        }
        val uid = authRepository.getCurrentUserIdOrNull()
        if (uid == null) {
            viewModelScope.launch {
                _events.emit(AppointmentsUiEvent.Message("سجّل الدخول لإلغاء الموعد", isError = true))
            }
            return
        }
        val optimistic = previousRows.filter { it.appointment.id != appointmentId }
        _allRows.value = optimistic
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
                    _allRows.value = previousRows
                    _uiState.value = AppointmentsUiState.Ready(previousRows)
                    _events.emit(
                        AppointmentsUiEvent.Message(
                            r.message.ifBlank { "فشل إلغاء الموعد، حاول مرة أخرى" },
                            isError = true
                        )
                    )
                }
                else -> {
                    _allRows.value = previousRows
                    _uiState.value = AppointmentsUiState.Ready(previousRows)
                    _events.emit(
                        AppointmentsUiEvent.Message("فشل إلغاء الموعد، حاول مرة أخرى", isError = true)
                    )
                }
            }
        }
    }

    fun submitDoctorRating(appointmentId: String, stars: Int) {
        if (_ratingInFlightId.value != null) return
        val uid = authRepository.getCurrentUserIdOrNull()
        if (uid == null) {
            viewModelScope.launch {
                _events.emit(
                    AppointmentsUiEvent.Message("سجّل الدخول لإرسال التقييم", isError = true),
                )
            }
            return
        }
        viewModelScope.launch {
            _ratingInFlightId.value = appointmentId
            when (val r = appointmentRepository.submitDoctorRating(appointmentId, uid, stars)) {
                is Result.Success -> {
                    refresh(showLoading = false)
                    _events.emit(AppointmentsUiEvent.Message("شكراً لتقييمك", isError = false))
                }
                is Result.Error -> {
                    _events.emit(
                        AppointmentsUiEvent.Message(
                            r.message.ifBlank { "تعذّر إرسال التقييم" },
                            isError = true,
                        ),
                    )
                }
                else -> {
                    _events.emit(
                        AppointmentsUiEvent.Message("تعذّر إرسال التقييم، حاول مرة أخرى", isError = true),
                    )
                }
            }
            _ratingInFlightId.value = null
        }
    }

    private fun isCancelledStatus(status: String): Boolean =
        status.lowercase().trim() == "cancelled"
}
