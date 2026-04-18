package com.mawidplus.patient.ui.screens.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mawidplus.patient.data.SeedDoctorIds
import com.mawidplus.patient.data.repository.AppointmentRepository
import com.mawidplus.patient.data.repository.AuthRepository
import com.mawidplus.patient.data.repository.DoctorRepository
import com.mawidplus.patient.core.region.MawidRegion
import com.mawidplus.patient.data.repository.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

private const val HOME_VM_TAG = "HomeVM"
private val HOME_ZONE: ZoneId = MawidRegion.timeZone

data class UpcomingAppointmentCardData(
    val doctorId: String,
    val doctorName: String,
    val clinicLabel: String,
    val specialty: String,
    val appointmentDateIso: String,
    /** اليوم / غداً أو تاريخ كامل */
    val relativeDateLabel: String,
    val displayDateAr: String,
    val queueNumber: Int,
    val doctorPhotoUrl: String?,
    val statusBadgeAr: String,
)

sealed class HomeUiState {
    data object Loading : HomeUiState()
    data class Ready(
        val patientName: String,
        val upcoming: UpcomingAppointmentCardData?,
        val queueShortcutDoctorId: String,
    ) : HomeUiState()

    data class Error(val message: String) : HomeUiState()
}

class HomeViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val appointmentRepository: AppointmentRepository = AppointmentRepository(),
    private val doctorRepository: DoctorRepository = DoctorRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _homeSearchQuery = MutableStateFlow("")
    val homeSearchQuery: StateFlow<String> = _homeSearchQuery.asStateFlow()

    fun setHomeSearchQuery(value: String) {
        _homeSearchQuery.update { value }
    }

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            val uid = authRepository.getCurrentUserIdOrNull()
            if (uid == null) {
                _uiState.value = HomeUiState.Error("سجّل الدخول لعرض بياناتك")
                return@launch
            }
            Log.d(HOME_VM_TAG, "fetching next appointment for patient: $uid")
            when (val prof = authRepository.fetchProfileForCurrentUser()) {
                is Result.Error -> _uiState.value = HomeUiState.Error(prof.message)
                Result.Loading -> Unit
                is Result.Success -> {
                    val patientName = prof.data.fullName.ifBlank { "ضيف موعد+" }
                    when (val nextResult = appointmentRepository.getNextAppointment(uid)) {
                        is Result.Error -> _uiState.value = HomeUiState.Error(nextResult.message)
                        Result.Loading -> Unit
                        is Result.Success -> {
                            val next = nextResult.data
                            Log.d(HOME_VM_TAG, "result: $nextResult")
                            Log.d(HOME_VM_TAG, "appointments count: ${if (next != null) 1 else 0}")
                            val upcoming = next?.let { ap ->
                                val rel = relativeDateLabelAr(ap.appointmentDate)
                                val badge = homeStatusBadgeAr(ap.status)
                                when (val doc = doctorRepository.getDoctorById(ap.doctorId)) {
                                    is Result.Success -> {
                                        val d = doc.data
                                        UpcomingAppointmentCardData(
                                            doctorId = ap.doctorId,
                                            doctorName = d.fullName,
                                            clinicLabel = d.clinicName ?: "عيادة",
                                            specialty = d.specialty,
                                            appointmentDateIso = ap.appointmentDate,
                                            relativeDateLabel = rel,
                                            displayDateAr = formatDateAr(ap.appointmentDate),
                                            queueNumber = ap.queueNumber,
                                            doctorPhotoUrl = d.photoUrl,
                                            statusBadgeAr = badge,
                                        )
                                    }
                                    else -> UpcomingAppointmentCardData(
                                        doctorId = ap.doctorId,
                                        doctorName = "طبيب",
                                        clinicLabel = "عيادة",
                                        specialty = "",
                                        appointmentDateIso = ap.appointmentDate,
                                        relativeDateLabel = rel,
                                        displayDateAr = formatDateAr(ap.appointmentDate),
                                        queueNumber = ap.queueNumber,
                                        doctorPhotoUrl = null,
                                        statusBadgeAr = badge,
                                    )
                                }
                            }
                            val queueDoc = upcoming?.doctorId ?: SeedDoctorIds.FAMILY_AHMED
                            _uiState.value = HomeUiState.Ready(
                                patientName = patientName,
                                upcoming = upcoming,
                                queueShortcutDoctorId = queueDoc,
                            )
                        }
                    }
                }
            }
        }
    }
}

private val arabicMonths = listOf(
    "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
    "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر",
)

private fun formatDateAr(iso: String): String {
    return try {
        val d = LocalDate.parse(iso)
        "${d.dayOfMonth} ${arabicMonths.getOrElse(d.monthValue - 1) { "" }} ${d.year}"
    } catch (_: Exception) {
        iso
    }
}

private fun relativeDateLabelAr(iso: String): String {
    return try {
        val d = LocalDate.parse(iso)
        val today = LocalDate.now(HOME_ZONE)
        when {
            d == today -> "اليوم"
            d == today.plusDays(1) -> "غداً"
            else -> formatDateAr(iso)
        }
    } catch (_: Exception) {
        iso
    }
}

private fun homeStatusBadgeAr(status: String): String = when (status.lowercase().trim()) {
    "waiting", "scheduled" -> "في الانتظار"
    "in_progress" -> "قيد المعاينة"
    else -> "مجدول"
}
