package com.mawidplus.patient.ui.screens.booking

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mawidplus.patient.core.model.Appointment
import com.mawidplus.patient.core.model.Doctor
import com.mawidplus.patient.data.repository.AppointmentRepository
import com.mawidplus.patient.data.repository.AuthRepository
import com.mawidplus.patient.data.repository.DoctorRepository
import com.mawidplus.patient.data.repository.QueueRepository
import com.mawidplus.patient.core.region.MawidRegion
import com.mawidplus.patient.data.repository.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val TAG = "BookingViewModel"
private val APP_ZONE: ZoneId = MawidRegion.timeZone

sealed class BookingDoctorState {
    data object Loading : BookingDoctorState()
    data class Ready(val doctor: Doctor) : BookingDoctorState()
    data class Error(val message: String) : BookingDoctorState()
}

data class TimeSlotRow(
    val time: String,
    val displayArabic: String,
    val isAvailable: Boolean,
    val isBooked: Boolean,
    val isPast: Boolean,
)

data class BookingSlotsUi(
    val workingHoursLine: String,
    val availabilityLine: String,
    val dayClosedMessage: String?,
    val slots: List<TimeSlotRow>,
    val loading: Boolean,
)

sealed class BookingSubmitState {
    data object Idle : BookingSubmitState()
    data object Submitting : BookingSubmitState()
    data class Success(val appointment: Appointment) : BookingSubmitState()
    data class Failed(val message: String) : BookingSubmitState()
}

class BookingViewModel(
    private val doctorId: String,
    private val doctorRepository: DoctorRepository = DoctorRepository(),
    private val appointmentRepository: AppointmentRepository = AppointmentRepository(),
    private val queueRepository: QueueRepository = QueueRepository(),
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _doctorState = MutableStateFlow<BookingDoctorState>(BookingDoctorState.Loading)
    val doctorState: StateFlow<BookingDoctorState> = _doctorState.asStateFlow()

    private val _submitState = MutableStateFlow<BookingSubmitState>(BookingSubmitState.Idle)
    val submitState: StateFlow<BookingSubmitState> = _submitState.asStateFlow()

    private val _slotsUi = MutableStateFlow(
        BookingSlotsUi(
            workingHoursLine = "",
            availabilityLine = "",
            dayClosedMessage = null,
            slots = emptyList(),
            loading = true,
        )
    )
    val slotsUi: StateFlow<BookingSlotsUi> = _slotsUi.asStateFlow()

    private val _selectedDayIndex = MutableStateFlow(0)
    val selectedDayIndex: StateFlow<Int> = _selectedDayIndex.asStateFlow()

    private val _selectedSlotIndex = MutableStateFlow(-1)
    val selectedSlotIndex: StateFlow<Int> = _selectedSlotIndex.asStateFlow()

    val weekDates: List<LocalDate> = run {
        val today = LocalDate.now(APP_ZONE)
        (0..6).map { today.plusDays(it.toLong()) }
    }

    private var cachedDoctor: Doctor? = null

    fun dayEnabledAt(index: Int): Boolean {
        val doctor = cachedDoctor ?: return true
        if (index !in weekDates.indices) return false
        return isDoctorAvailableOnDay(weekDates[index], doctor)
    }

    init {
        viewModelScope.launch {
            when (val r = doctorRepository.getDoctorById(doctorId)) {
                is Result.Success -> {
                    cachedDoctor = r.data
                    _doctorState.value = BookingDoctorState.Ready(r.data)
                    refreshSlotsForSelectedDay()
                }
                is Result.Error -> _doctorState.value = BookingDoctorState.Error(r.message)
                else -> {}
            }
        }
    }

    fun selectDay(index: Int) {
        if (index !in weekDates.indices) return
        _selectedDayIndex.value = index
        _selectedSlotIndex.value = -1
        refreshSlotsForSelectedDay()
    }

    fun selectSlot(index: Int) {
        val slots = _slotsUi.value.slots
        if (index !in slots.indices) return
        val s = slots[index]
        if (!s.isAvailable) return
        _selectedSlotIndex.value = index
    }

    private fun refreshSlotsForSelectedDay() {
        val doctor = cachedDoctor ?: return
        viewModelScope.launch {
            _slotsUi.value = _slotsUi.value.copy(loading = true)
            val date = weekDates[_selectedDayIndex.value]
            val dateIso = date.toString()
            if (!isDoctorAvailableOnDay(date, doctor)) {
                _slotsUi.value = BookingSlotsUi(
                    workingHoursLine = formatWorkingHoursLine(doctor),
                    availabilityLine = "٠ وقت متاح من أصل ٠",
                    dayClosedMessage = "الطبيب لا يعمل في هذا اليوم",
                    slots = emptyList(),
                    loading = false,
                )
                _selectedSlotIndex.value = -1
                return@launch
            }
            val rawStart = parseLocalTime(doctor.startTime) ?: LocalTime.of(9, 0)
            val rawEnd = parseLocalTime(doctor.endTime) ?: LocalTime.of(17, 0)
            val (start, end) = normalizeClinicHours(rawStart, rawEnd)
            val slotMinutes = doctor.slotDurationMinutes.coerceIn(5, 240)
            val bookedResult = appointmentRepository.getBookedSlotsForDoctorAndDate(doctorId, dateIso)
            val booked = when (bookedResult) {
                is Result.Success -> bookedResult.data.toSet()
                is Result.Error -> {
                    Log.e(TAG, "booked slots: ${bookedResult.message}")
                    emptySet()
                }
                else -> emptySet()
            }
            val nowTime = LocalTime.now(APP_ZONE)
            val slots = generateTimeSlots(
                startTime = start,
                endTime = end,
                slotDuration = slotMinutes,
                bookedSlots = booked,
                selectedDate = date,
                today = LocalDate.now(APP_ZONE),
                nowTime = nowTime,
            )
            val availableCount = slots.count { it.isAvailable }
            _slotsUi.value = BookingSlotsUi(
                workingHoursLine = formatWorkingHoursLine(doctor),
                availabilityLine = "$availableCount وقت متاح من أصل ${slots.size}",
                dayClosedMessage = null,
                slots = slots,
                loading = false,
            )
            val firstAvail = slots.indexOfFirst { it.isAvailable }
            _selectedSlotIndex.value = if (firstAvail >= 0) firstAvail else -1
        }
    }

    /**
     * @param appointmentDateIso تاريخ اليوم المختار بصيغة yyyy-MM-dd
     */
    fun submitBooking(appointmentDateIso: String) {
        viewModelScope.launch {
            val uid = authRepository.getCurrentUserIdOrNull()
            if (uid == null) {
                _submitState.value = BookingSubmitState.Failed(
                    "سجّل الدخول أولاً (رقم الهاتف أو Google) لحجز الموعد."
                )
                return@launch
            }
            val idx = _selectedSlotIndex.value
            val slots = _slotsUi.value.slots
            if (idx < 0 || idx !in slots.indices) {
                _submitState.value = BookingSubmitState.Failed("اختر وقتاً متاحاً من الجدول.")
                return@launch
            }
            val slot = slots[idx]
            if (!slot.isAvailable) {
                _submitState.value = BookingSubmitState.Failed("هذا الوقت غير متاح. اختر وقتاً آخر.")
                return@launch
            }
            val timeSlot = slot.time
            _submitState.value = BookingSubmitState.Submitting
            val queueNum = when (val m = queueRepository.maxQueueNumberForDoctorAndDate(doctorId, appointmentDateIso)) {
                is Result.Success -> m.data + 1
                is Result.Error -> {
                    _submitState.value = BookingSubmitState.Failed(
                        m.message.ifBlank { "تعذر حساب رقم الطابور لهذا اليوم. تحقق من الاتصال." }
                    )
                    return@launch
                }
                Result.Loading -> {
                    _submitState.value = BookingSubmitState.Failed("تعذر حساب رقم الطابور.")
                    return@launch
                }
            }
            if (queueNum <= 0) {
                _submitState.value = BookingSubmitState.Failed(
                    "تعذر حساب رقم الطابور لهذا اليوم. تحقق من الاتصال."
                )
                return@launch
            }
            when (
                val r = appointmentRepository.createAppointment(
                    patientId = uid,
                    doctorId = doctorId,
                    appointmentDate = appointmentDateIso,
                    queueNumber = queueNum,
                    timeSlot = timeSlot,
                )
            ) {
                is Result.Success -> {
                    _submitState.value = BookingSubmitState.Success(r.data)
                }
                is Result.Error -> {
                    val msg = r.message
                    if (msg.contains("حجز هذا الوقت", ignoreCase = true)) {
                        refreshSlotsForSelectedDay()
                    }
                    _submitState.value = BookingSubmitState.Failed(msg)
                }
                else -> {}
            }
        }
    }

    fun resetSubmitState() {
        _submitState.value = BookingSubmitState.Idle
    }

    companion object {
        fun factory(doctorId: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return BookingViewModel(doctorId) as T
                }
            }
    }
}

private fun parseLocalTime(raw: String?): LocalTime? {
    if (raw.isNullOrBlank()) return null
    val t = raw.trim()
    return try {
        when {
            t.length >= 8 -> LocalTime.parse(t.substring(0, 8))
            t.length >= 5 -> LocalTime.parse(t.substring(0, 5))
            else -> LocalTime.parse(t)
        }
    } catch (_: Exception) {
        null
    }
}

private fun arabicDayName(day: DayOfWeek): String = when (day) {
    DayOfWeek.SATURDAY -> "السبت"
    DayOfWeek.SUNDAY -> "الأحد"
    DayOfWeek.MONDAY -> "الإثنين"
    DayOfWeek.TUESDAY -> "الثلاثاء"
    DayOfWeek.WEDNESDAY -> "الأربعاء"
    DayOfWeek.THURSDAY -> "الخميس"
    DayOfWeek.FRIDAY -> "الجمعة"
}

private fun isDoctorAvailableOnDay(date: LocalDate, doctor: Doctor): Boolean {
    if (doctor.availableDays.isEmpty()) return true
    val label = arabicDayName(date.dayOfWeek)
    return doctor.availableDays.any { d ->
        d.contains(label, ignoreCase = true) || label.contains(d, ignoreCase = true)
    }
}

private fun formatWorkingHoursLine(doctor: Doctor): String {
    val rawStart = parseLocalTime(doctor.startTime) ?: LocalTime.of(9, 0)
    val rawEnd = parseLocalTime(doctor.endTime) ?: LocalTime.of(17, 0)
    val (start, end) = normalizeClinicHours(rawStart, rawEnd)
    val fmt = DateTimeFormatter.ofPattern("h:mm a")
    val dur = doctor.slotDurationMinutes.coerceAtLeast(5)
    return "مواعيد العمل: ${start.format(fmt)} — ${end.format(fmt)} | كل $dur دقيقة"
}

/**
 * إذا كان وقت البداية في قاعدة البيانات بعد وقت النهاية (خطأ إدخال في لوحة الطبيب)،
 * نعكسهما حتى يُولَّد جدول فترات. يُفضّل تصحيح الجدول من الويب.
 */
private fun normalizeClinicHours(start: LocalTime, end: LocalTime): Pair<LocalTime, LocalTime> {
    return if (start >= end) {
        Log.w(TAG, "clinic hours inverted in DB (start=$start >= end=$end); using swapped range for slots")
        Pair(end, start)
    } else {
        Pair(start, end)
    }
}

private fun generateTimeSlots(
    startTime: LocalTime,
    endTime: LocalTime,
    slotDuration: Int,
    bookedSlots: Set<String>,
    selectedDate: LocalDate,
    today: LocalDate,
    nowTime: LocalTime,
): List<TimeSlotRow> {
    val out = mutableListOf<TimeSlotRow>()
    var current = startTime
    val hm = DateTimeFormatter.ofPattern("HH:mm")
    while (current.plusMinutes(slotDuration.toLong()) <= endTime) {
        val timeStr = current.format(hm)
        val isBooked = bookedSlots.contains(timeStr)
        val isPast = selectedDate == today && current.isBefore(nowTime)
        val isAvailable = !isBooked && !isPast
        out.add(
            TimeSlotRow(
                time = timeStr,
                displayArabic = formatArabicTime(current),
                isAvailable = isAvailable,
                isBooked = isBooked,
                isPast = isPast,
            )
        )
        current = current.plusMinutes(slotDuration.toLong())
    }
    return out
}

private fun formatArabicTime(t: LocalTime): String {
    val h24 = t.hour
    val m = t.minute
    val period = if (h24 < 12) "صباحاً" else "مساءً"
    val h12 = when {
        h24 == 0 -> 12
        h24 > 12 -> h24 - 12
        else -> h24
    }
    return String.format("%d:%02d %s", h12, m, period)
}
