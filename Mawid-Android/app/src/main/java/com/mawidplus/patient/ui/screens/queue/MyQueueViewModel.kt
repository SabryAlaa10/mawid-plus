package com.mawidplus.patient.ui.screens.queue

import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mawidplus.patient.PatientApp
import com.mawidplus.patient.core.notifications.NotificationHelper
import com.mawidplus.patient.core.model.Appointment
import com.mawidplus.patient.core.model.Doctor
import com.mawidplus.patient.core.model.QueueSettings
import com.mawidplus.patient.data.repository.AppointmentRepository
import com.mawidplus.patient.data.repository.AuthRepository
import com.mawidplus.patient.data.repository.DoctorRepository
import com.mawidplus.patient.data.repository.QueueRepository
import com.mawidplus.patient.core.region.MawidRegion
import com.mawidplus.patient.data.repository.Result
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

sealed class MyQueueUiState {
    data object Loading : MyQueueUiState()
    data class Ready(
        val doctor: Doctor,
        val queue: QueueSettings,
        val myTicketNumber: Int?,
        val estimatedMinutes: Int,
        val appointmentStatus: String,
        val doctorNotes: String?,
        val isVisitComplete: Boolean,
        /** Active appointment row for this doctor (today or nearest future). */
        val appointmentId: String?,
        val appointmentDateIso: String?,
        /** True when [appointmentDateIso] is today (Africa/Cairo). */
        val isAppointmentToday: Boolean,
        /** Days from today until appointment; 0 if today. */
        val daysUntilAppointment: Long,
        /** Live tracker only for today's non-completed visit. */
        val showLiveQueue: Boolean,
        /** Position in queue ahead (ticket - current); meaningful when [showLiveQueue]. */
        val aheadInQueue: Int,
    ) : MyQueueUiState()

    data class Error(val message: String) : MyQueueUiState()
}

class MyQueueViewModel(
    private val doctorId: String,
    private val doctorRepository: DoctorRepository = DoctorRepository(),
    private val queueRepository: QueueRepository = QueueRepository(),
    private val appointmentRepository: AppointmentRepository = AppointmentRepository(),
    private val authRepository: AuthRepository = AuthRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow<MyQueueUiState>(MyQueueUiState.Loading)
    val uiState: StateFlow<MyQueueUiState> = _uiState.asStateFlow()

    private val completionSoundChannel = Channel<Unit>(Channel.BUFFERED)
    val completionSound = completionSoundChannel.receiveAsFlow()

    private var pollJob: Job? = null
    private var wasVisitCompleteLastPoll: Boolean? = null

    private val zone: ZoneId = MawidRegion.timeZone

    init {
        viewModelScope.launch { load(showLoading = true) }
        pollJob = viewModelScope.launch {
            while (isActive) {
                delay(POLL_INTERVAL_MS)
                if (_uiState.value is MyQueueUiState.Ready) {
                    load(showLoading = false)
                }
            }
        }
    }

    override fun onCleared() {
        pollJob?.cancel()
        completionSoundChannel.close()
        super.onCleared()
    }

    fun refresh() {
        viewModelScope.launch { load(showLoading = true) }
    }

    fun cancelCurrentAppointment() {
        val id = (_uiState.value as? MyQueueUiState.Ready)?.appointmentId ?: return
        val uid = authRepository.getCurrentUserIdOrNull()
        if (uid == null) {
            PatientApp.appContextOrNull()?.let { ctx ->
                Toast.makeText(ctx, "سجّل الدخول لإلغاء الموعد", Toast.LENGTH_LONG).show()
            }
            return
        }
        viewModelScope.launch {
            when (val r = appointmentRepository.cancelAppointment(id, uid)) {
                is Result.Success -> {
                    PatientApp.appContextOrNull()?.let { ctx ->
                        NotificationHelper.cancelAppointmentReminders(ctx, id)
                        Toast.makeText(ctx, "تم إلغاء الموعد بنجاح", Toast.LENGTH_SHORT).show()
                    }
                    load(showLoading = true)
                }
                is Result.Error -> {
                    PatientApp.appContextOrNull()?.let { ctx ->
                        val msg = r.message.ifBlank { "فشل إلغاء الموعد، حاول مرة أخرى" }
                        Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show()
                    }
                    load(showLoading = false)
                }
                else -> {}
            }
        }
    }

    private suspend fun load(showLoading: Boolean) {
        if (showLoading) {
            _uiState.value = MyQueueUiState.Loading
        }
        val doctorResult = doctorRepository.getDoctorById(doctorId)
        val queueResult = queueRepository.getQueueSettings(doctorId)
        if (doctorResult !is Result.Success) {
            _uiState.value = MyQueueUiState.Error(
                (doctorResult as? Result.Error)?.message ?: "تعذر تحميل بيانات الطبيب",
            )
            return
        }
        if (queueResult !is Result.Success) {
            _uiState.value = MyQueueUiState.Error(
                (queueResult as? Result.Error)?.message ?: "تعذر تحميل الطابور",
            )
            return
        }
        val doctor = doctorResult.data
        val queue = queueResult.data
        val uid = authRepository.getCurrentUserIdOrNull()
        var row: Appointment? = null
        if (uid != null) {
            when (val apps = appointmentRepository.getPatientAppointments(uid)) {
                is Result.Success -> {
                    row = resolveActiveAppointment(apps.data, doctorId)
                }
                else -> {}
            }
        }
        val todayIso = LocalDate.now(zone).toString()
        val ticket = row?.queueNumber
        val apptDate = row?.appointmentDate
        val isToday = apptDate != null && apptDate == todayIso
        val daysUntil = if (apptDate != null) {
            val d = LocalDate.parse(apptDate)
            ChronoUnit.DAYS.between(LocalDate.now(zone), d)
        } else {
            0L
        }
        val status = row?.status ?: "waiting"
        val notes = row?.doctorNotes?.trim()?.takeIf { it.isNotEmpty() }
        val complete = isDoneStatus(status)

        val showLive = ticket != null && isToday && !complete &&
            normalizeStatus(status) != "cancelled"

        val ahead = if (showLive && ticket != null) {
            (ticket - queue.currentNumber).coerceAtLeast(0)
        } else {
            0
        }

        if (wasVisitCompleteLastPoll == false && complete) {
            completionSoundChannel.trySend(Unit)
            PatientApp.appContextOrNull()?.let { ctx ->
                NotificationHelper.showNotification(
                    ctx,
                    "انتهت جلستك مع الطبيب",
                    "يمكنك الاطلاع على ملاحظات طبيبك في التطبيق",
                )
            }
        }
        wasVisitCompleteLastPoll = complete

        val estimated = doctor.slotDurationMinutes.coerceAtLeast(5)
        _uiState.value = MyQueueUiState.Ready(
            doctor = doctor,
            queue = queue,
            myTicketNumber = ticket,
            estimatedMinutes = estimated,
            appointmentStatus = status,
            doctorNotes = notes,
            isVisitComplete = complete,
            appointmentId = row?.id,
            appointmentDateIso = apptDate,
            isAppointmentToday = isToday,
            daysUntilAppointment = daysUntil,
            showLiveQueue = showLive,
            aheadInQueue = ahead,
        )
    }

    private fun normalizeStatus(s: String): String =
        when (s.lowercase()) {
            "scheduled" -> "waiting"
            else -> s.lowercase()
        }

    private fun isDoneStatus(s: String): Boolean = normalizeStatus(s) == "done"

    private fun isCancelled(s: String): Boolean = normalizeStatus(s) == "cancelled"

    /**
     * Prefer today's active appointment; else nearest future; else most recent active.
     */
    private fun resolveActiveAppointment(
        apps: List<Appointment>,
        doctorId: String,
    ): Appointment? {
        val forDoctor = apps.filter { it.doctorId == doctorId }
        val active = forDoctor.filter {
            val s = normalizeStatus(it.status)
            !isCancelled(s) && s != "done"
        }
        if (active.isEmpty()) return null

        val todayIso = LocalDate.now(zone).toString()
        val today = LocalDate.now(zone)

        val todayList = active.filter { it.appointmentDate == todayIso }
        if (todayList.isNotEmpty()) {
            return todayList.minBy { it.queueNumber }
        }

        val future = active.filter { LocalDate.parse(it.appointmentDate).isAfter(today) }
        if (future.isNotEmpty()) {
            return future.minBy { LocalDate.parse(it.appointmentDate) }
        }

        return active.maxByOrNull { LocalDate.parse(it.appointmentDate) }
    }

    companion object {
        private const val POLL_INTERVAL_MS = 6_000L

        fun factory(doctorId: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return MyQueueViewModel(doctorId) as T
                }
            }
    }
}
