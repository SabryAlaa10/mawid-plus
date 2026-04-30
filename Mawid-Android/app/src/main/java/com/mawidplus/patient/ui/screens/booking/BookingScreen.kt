@file:OptIn(ExperimentalMaterial3Api::class)

package com.mawidplus.patient.ui.screens.booking

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Check
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.mawidplus.patient.core.notifications.NotificationHelper
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.mawidplus.patient.ui.screens.search.BookingDoctorMeta
import com.mawidplus.patient.ui.screens.search.toBookingMeta
import com.mawidplus.patient.ui.theme.Error
import com.mawidplus.patient.ui.theme.Manrope
import com.mawidplus.patient.ui.theme.OnPrimary
import com.mawidplus.patient.ui.theme.OnSurface
import com.mawidplus.patient.ui.theme.OnSurfaceVariant
import com.mawidplus.patient.ui.theme.Outline
import com.mawidplus.patient.ui.theme.Primary
import com.mawidplus.patient.ui.theme.PrimaryContainer
import com.mawidplus.patient.ui.theme.PublicSans
import com.mawidplus.patient.ui.theme.Secondary
import com.mawidplus.patient.ui.theme.SurfaceBright
import com.mawidplus.patient.ui.theme.SurfaceContainerLow
import com.mawidplus.patient.ui.theme.SurfaceContainerLowest

private enum class ConsultationKind { Video, InPerson }

private fun dayNameAr(d: DayOfWeek): String = when (d) {
    DayOfWeek.SATURDAY -> "سبت"
    DayOfWeek.SUNDAY -> "أحد"
    DayOfWeek.MONDAY -> "اثنين"
    DayOfWeek.TUESDAY -> "ثلاثاء"
    DayOfWeek.WEDNESDAY -> "أربعاء"
    DayOfWeek.THURSDAY -> "خميس"
    DayOfWeek.FRIDAY -> "جمعة"
}

@Composable
fun BookingScreen(
    doctorId: String,
    onBack: () -> Unit,
    onNotifications: () -> Unit = {},
    onConfirm: () -> Unit,
    viewModel: BookingViewModel = viewModel(
        key = doctorId,
        factory = BookingViewModel.factory(doctorId)
    )
) {
    val doctorState by viewModel.doctorState.collectAsStateWithLifecycle()
    val submitState by viewModel.submitState.collectAsStateWithLifecycle()
    val slotsUi by viewModel.slotsUi.collectAsStateWithLifecycle()
    val selectedDayIndex by viewModel.selectedDayIndex.collectAsStateWithLifecycle()
    val selectedSlotIndex by viewModel.selectedSlotIndex.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val meta: BookingDoctorMeta? = remember(doctorState) {
        when (val s = doctorState) {
            is BookingDoctorState.Ready -> s.doctor.toBookingMeta()
            else -> null
        }
    }
    val doctorName: String = remember(doctorState) {
        when (val s = doctorState) {
            is BookingDoctorState.Ready -> s.doctor.fullName
            else -> ""
        }
    }

    LaunchedEffect(submitState, doctorName) {
        when (val s = submitState) {
            is BookingSubmitState.Success -> {
                NotificationHelper.scheduleAppointmentReminder(
                    context = context,
                    appointmentId = s.appointment.id,
                    appointmentDateIso = s.appointment.appointmentDate,
                    doctorName = doctorName.ifEmpty { "الطبيب" },
                    queueNumber = s.appointment.queueNumber
                )
                Toast.makeText(
                    context,
                    "تم الحجز بنجاح! سنذكرك بالموعد قبل يوم وفي صباح اليوم نفسه",
                    Toast.LENGTH_LONG
                ).show()
                onConfirm()
                viewModel.resetSubmitState()
            }
            else -> {}
        }
    }

    val weekDates = viewModel.weekDates
    val weekDays = remember(weekDates) { weekDates.map { dayNameAr(it.dayOfWeek) } }
    val dayLabels = remember(weekDates) { weekDates.map { it.dayOfMonth.toString() } }
    val monthTitleFormatter = remember {
        DateTimeFormatter.ofPattern("MMMM yyyy", Locale("ar", "SA"))
    }

    var consultationKind by remember { mutableStateOf(ConsultationKind.Video) }

    val dayEnabled = remember(doctorState, viewModel) {
        List(viewModel.weekDates.size) { i -> viewModel.dayEnabledAt(i) }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SurfaceBright)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                BookingTopBar(
                    onBack = onBack,
                    onNotifications = onNotifications
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 120.dp)
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
                    when (val ds = doctorState) {
                        is BookingDoctorState.Loading -> {
                            CircularProgressIndicator(
                                color = Primary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp)
                            )
                        }
                        is BookingDoctorState.Error -> {
                            Text(
                                ds.message,
                                fontFamily = PublicSans,
                                color = OnSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                        is BookingDoctorState.Ready -> {
                            if (meta != null) {
                                DoctorBookingCard(
                                    doctorName = doctorName,
                                    meta = meta
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            "اختر التاريخ",
                            fontFamily = Manrope,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            color = OnSurface
                        )
                        Text(
                            weekDates.getOrNull(selectedDayIndex)?.format(monthTitleFormatter).orEmpty(),
                            fontFamily = PublicSans,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = Primary
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    CalendarWeek(
                        weekDays = weekDays,
                        dayLabels = dayLabels,
                        dayEnabled = dayEnabled,
                        selectedIndex = selectedDayIndex,
                        onSelectDay = { i ->
                            if (dayEnabled[i]) viewModel.selectDay(i)
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        slotsUi.workingHoursLine,
                        fontFamily = PublicSans,
                        fontSize = 13.sp,
                        color = OnSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        slotsUi.availabilityLine,
                        fontFamily = PublicSans,
                        fontSize = 12.sp,
                        color = Primary,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    slotsUi.dayClosedMessage?.let { msg ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            msg,
                            fontFamily = PublicSans,
                            fontSize = 14.sp,
                            color = Error,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        "الوقت المتاح",
                        fontFamily = Manrope,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = OnSurface,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    if (slotsUi.loading) {
                        CircularProgressIndicator(
                            color = Primary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                        )
                    } else {
                        TimeSlotGrid(
                            slots = slotsUi.slots,
                            selectedIndex = selectedSlotIndex,
                            onSelect = { i -> viewModel.selectSlot(i) }
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    ConsultationTypeCard(
                        kind = consultationKind,
                        onChange = {
                            consultationKind = if (consultationKind == ConsultationKind.Video) {
                                ConsultationKind.InPerson
                            } else {
                                ConsultationKind.Video
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            if (submitState is BookingSubmitState.Failed) {
                Text(
                    (submitState as BookingSubmitState.Failed).message,
                    color = Error,
                    fontSize = 12.sp,
                    fontFamily = PublicSans,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 100.dp)
                        .padding(horizontal = 16.dp)
                )
            }

            BookingBottomBar(
                priceSar = meta?.priceSar ?: 0,
                onConfirm = {
                    val dateIso = weekDates.getOrNull(selectedDayIndex)?.toString()
                    if (dateIso != null) viewModel.submitBooking(dateIso)
                },
                enabled = doctorState is BookingDoctorState.Ready &&
                    submitState !is BookingSubmitState.Submitting &&
                    !slotsUi.loading &&
                    slotsUi.dayClosedMessage == null &&
                    slotsUi.slots.any { it.isAvailable },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun BookingTopBar(
    onBack: () -> Unit,
    onNotifications: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Filled.ArrowBack,
                    contentDescription = "رجوع",
                    tint = Primary
                )
            }
            Text(
                "حجز موعد",
                fontFamily = Manrope,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = OnSurface
            )
        }
        Text(
            "Mawid+",
            fontFamily = Manrope,
            fontWeight = FontWeight.Black,
            fontSize = 20.sp,
            color = Primary
        )
        IconButton(onClick = onNotifications) {
            Icon(
                Icons.Filled.Notifications,
                contentDescription = "إشعارات",
                tint = OnSurfaceVariant
            )
        }
    }
}

@Composable
private fun DoctorBookingCard(
    doctorName: String,
    meta: BookingDoctorMeta
) {
    val shape = RoundedCornerShape(
        topStart = 48.dp,
        topEnd = 12.dp,
        bottomEnd = 48.dp,
        bottomStart = 12.dp
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(SurfaceContainerLowest)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            AsyncImage(
                model = meta.imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(96.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 32.dp,
                            topEnd = 8.dp,
                            bottomEnd = 32.dp,
                            bottomStart = 8.dp
                        )
                    ),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(Secondary)
                    .border(3.dp, SurfaceContainerLowest, CircleShape)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                meta.specialty.uppercase(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = OnSurfaceVariant,
                fontFamily = PublicSans
            )
            Text(
                doctorName,
                fontFamily = Manrope,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = Primary,
                textAlign = TextAlign.Start
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = null,
                    tint = Color(0xFFFFB300),
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    meta.rating,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = OnSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp)
                )
                Text(
                    meta.reviewShort,
                    fontSize = 12.sp,
                    color = OnSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun CalendarWeek(
    weekDays: List<String>,
    dayLabels: List<String>,
    dayEnabled: List<Boolean>,
    selectedIndex: Int,
    onSelectDay: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceContainerLow)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            weekDays.forEach { d ->
                Text(
                    d,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Outline,
                    fontFamily = PublicSans
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            dayLabels.forEachIndexed { i, label ->
                val enabled = dayEnabled[i]
                val selected = i == selectedIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when {
                                selected -> Primary
                                enabled -> Color.Transparent
                                else -> Color.Transparent
                            }
                        )
                        .clickable(enabled = enabled) { onSelectDay(i) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        fontSize = 13.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                        color = when {
                            selected -> OnPrimary
                            enabled -> OnSurfaceVariant
                            else -> OnSurfaceVariant.copy(alpha = 0.35f)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TimeSlotGrid(
    slots: List<TimeSlotRow>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    slots.chunked(3).forEach { rowSlots ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val start = slots.indexOf(rowSlots.first())
            rowSlots.forEachIndexed { _, slot ->
                val i = start + rowSlots.indexOf(slot)
                val selected = i == selectedIndex && slot.isAvailable
                val clickable = slot.isAvailable
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            when {
                                selected -> Primary
                                slot.isBooked -> SurfaceContainerLow.copy(alpha = 0.55f)
                                slot.isPast -> SurfaceContainerLow.copy(alpha = 0.45f)
                                slot.isAvailable -> SurfaceContainerLowest
                                else -> SurfaceContainerLow.copy(alpha = 0.5f)
                            }
                        )
                        .then(
                            if (slot.isAvailable && !selected) {
                                Modifier.border(1.dp, Primary.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                            } else Modifier
                        )
                        .clickable(enabled = clickable) { onSelect(i) }
                        .padding(vertical = 10.dp, horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (selected) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                tint = OnPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        Text(
                            slot.displayArabic,
                            fontFamily = Manrope,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = when {
                                selected -> OnPrimary
                                slot.isAvailable -> OnSurface
                                else -> OnSurface.copy(alpha = 0.45f)
                            },
                            textAlign = TextAlign.Center
                        )
                        Text(
                            slot.time,
                            fontSize = 11.sp,
                            fontFamily = PublicSans,
                            color = when {
                                selected -> OnPrimary.copy(alpha = 0.9f)
                                slot.isAvailable -> OnSurfaceVariant
                                else -> OnSurfaceVariant.copy(alpha = 0.45f)
                            }
                        )
                        when {
                            slot.isBooked -> Text(
                                "محجوز",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Error.copy(alpha = 0.85f),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            slot.isPast -> Text(
                                "انتهى",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = OnSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConsultationTypeCard(
    kind: ConsultationKind,
    onChange: () -> Unit
) {
    val title = when (kind) {
        ConsultationKind.Video -> "مكالمة فيديو مباشرة"
        ConsultationKind.InPerson -> "زيارة في العيادة"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Primary.copy(alpha = 0.05f))
            .border(1.dp, Primary.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(PrimaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (kind == ConsultationKind.Video) Icons.Filled.Videocam else Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = OnPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column {
                Text(
                    "نوع الاستشارة",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = OnSurface,
                    fontFamily = Manrope
                )
                Text(
                    title,
                    fontSize = 12.sp,
                    color = OnSurfaceVariant,
                    fontFamily = PublicSans
                )
            }
        }
        TextButton(onClick = onChange) {
            Text(
                "تغيير",
                color = Primary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                fontFamily = Manrope
            )
        }
    }
}

@Composable
private fun BookingBottomBar(
    priceSar: Int,
    onConfirm: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 3.dp,
        shadowElevation = 8.dp,
        color = SurfaceContainerLowest.copy(alpha = 0.98f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "إجمالي الرسوم",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Outline,
                    fontFamily = PublicSans
                )
                Text(
                    "$priceSar ر.س",
                    fontFamily = Manrope,
                    fontWeight = FontWeight.Black,
                    fontSize = 26.sp,
                    color = OnSurface
                )
            }
            Button(
                onClick = onConfirm,
                enabled = enabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary,
                    contentColor = OnPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(52.dp)
            ) {
                Text(
                    "تأكيد الموعد",
                    fontFamily = Manrope,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Icon(
                    Icons.Filled.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(22.dp)
                )
            }
        }
    }
}
