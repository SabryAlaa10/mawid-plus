@file:OptIn(ExperimentalMaterial3Api::class)

package com.mawidplus.patient.ui.screens.appointments

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mawidplus.patient.core.region.MawidRegion
import com.mawidplus.patient.data.SeedDoctorIds
import com.mawidplus.patient.ui.theme.Error
import com.mawidplus.patient.ui.theme.Manrope
import com.mawidplus.patient.ui.theme.OnPrimary
import com.mawidplus.patient.ui.theme.OnSurface
import com.mawidplus.patient.ui.theme.OnSurfaceVariant
import com.mawidplus.patient.ui.theme.Primary
import com.mawidplus.patient.ui.theme.PublicSans
import com.mawidplus.patient.ui.theme.Surface
import com.mawidplus.patient.ui.theme.SurfaceBright
import com.mawidplus.patient.ui.theme.SurfaceContainerLowest
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Composable
fun AppointmentsScreen(
    viewModel: AppointmentsViewModel = viewModel { AppointmentsViewModel() },
    onNavigateToQueue: (doctorId: String) -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToSearch: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val ratingInFlightId by viewModel.ratingInFlightId.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingCancel by remember { mutableStateOf<AppointmentListRow?>(null) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AppointmentsUiEvent.Message -> {
                    snackbarHostState.showSnackbar(
                        message = event.text,
                        withDismissAction = true
                    )
                }
            }
        }
    }

    pendingCancel?.let { row ->
        AlertDialog(
            onDismissRequest = { pendingCancel = null },
            title = {
                Text("إلغاء الموعد", fontFamily = Manrope, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "هل أنت متأكد من إلغاء موعدك مع ${row.doctorName}؟",
                    fontFamily = PublicSans
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val id = row.appointment.id
                        pendingCancel = null
                        viewModel.cancelAppointment(id)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Error)
                ) {
                    Text("نعم، إلغاء", fontFamily = Manrope, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingCancel = null }) {
                    Text("لا، تراجع", fontFamily = PublicSans, color = OnSurfaceVariant)
                }
            }
        )
    }

    Scaffold(
        containerColor = Surface,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Surface)
        ) {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "مواعيدي",
                        fontFamily = Manrope,
                        fontWeight = FontWeight.Bold,
                        color = Primary
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToNotifications) {
                        Icon(
                            Icons.Filled.Notifications,
                            contentDescription = "إشعارات",
                            tint = OnSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = SurfaceBright.copy(alpha = 0.92f)
                )
            )

            when (val s = uiState) {
                is AppointmentsUiState.Loading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = Primary)
                    }
                }
                is AppointmentsUiState.NotSignedIn -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "سجّل الدخول برقم هاتفك أو جوجل لعرض مواعيدك.",
                            fontFamily = PublicSans,
                            color = OnSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                is AppointmentsUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            s.message,
                            fontFamily = PublicSans,
                            color = OnSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(onClick = { viewModel.refresh() }) {
                            Text("إعادة المحاولة", fontFamily = PublicSans)
                        }
                    }
                }
                is AppointmentsUiState.Ready -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        if (s.rows.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(28.dp)
                                    ) {
                                        Icon(
                                            Icons.Filled.CalendarToday,
                                            contentDescription = null,
                                            tint = Primary.copy(alpha = 0.45f),
                                            modifier = Modifier.size(52.dp)
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            "لا توجد مواعيد بعد",
                                            fontFamily = Manrope,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 20.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            "ابدأ بحجز موعد مع طبيب تناسبك من البحث.",
                                            fontFamily = PublicSans,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(top = 10.dp)
                                        )
                                        Text(
                                            "إن كنت قد حجزت مسبقاً ولا يظهر الموعد، من «حسابي» سجّل الخروج ثم أدخل برقم هاتفك مرة أخرى لمزامنة الحساب.",
                                            fontFamily = PublicSans,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.88f),
                                            textAlign = TextAlign.Center,
                                            lineHeight = 18.sp,
                                            modifier = Modifier.padding(top = 14.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            val zone = MawidRegion.timeZone
                            val today = remember { LocalDate.now(zone) }
                            val flatItems = remember(s.rows) {
                                val sections = buildAppointmentSections(s.rows)
                                buildList {
                                    sections.forEachIndexed { idx, section ->
                                        add(AppointmentListEntry.Header(idx, section.title))
                                        section.rows.forEach { row ->
                                            add(AppointmentListEntry.RowItem(row))
                                        }
                                    }
                                }
                            }
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                items(
                                    items = flatItems,
                                    key = { entry ->
                                        when (entry) {
                                            is AppointmentListEntry.Header -> "h_${entry.index}_${entry.title}"
                                            is AppointmentListEntry.RowItem -> entry.row.appointment.id
                                        }
                                    }
                                ) { entry ->
                                    when (entry) {
                                        is AppointmentListEntry.Header -> AppointmentSectionHeader(
                                            title = entry.title,
                                            isFirst = entry.index == 0
                                        )
                                        is AppointmentListEntry.RowItem -> AppointmentSummaryCard(
                                            row = entry.row,
                                            today = today,
                                            ratingInFlightId = ratingInFlightId,
                                            onOpenQueue = { onNavigateToQueue(entry.row.appointment.doctorId) },
                                            onRequestCancel = { pendingCancel = entry.row },
                                            onRateDoctor = { id, stars ->
                                                viewModel.submitDoctorRating(id, stars)
                                            },
                                        )
                                    }
                                }
                            }
                        }
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 3.dp,
                            shadowElevation = 8.dp
                        ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    val id = s.rows.firstOrNull()?.appointment?.doctorId
                                        ?: SeedDoctorIds.FAMILY_AHMED
                                    onNavigateToQueue(id)
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Primary,
                                    contentColor = OnPrimary
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text(
                                    "عرض طابوري",
                                    fontFamily = Manrope,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            OutlinedButton(
                                onClick = onNavigateToSearch,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(Icons.Filled.Search, contentDescription = null, tint = Primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("بحث", fontFamily = Manrope, color = Primary)
                            }
                        }
                        }
                    }
                }
            }
        }
    }
}

private data class AppointmentSection(val title: String, val rows: List<AppointmentListRow>)

private sealed class AppointmentListEntry {
    data class Header(val index: Int, val title: String) : AppointmentListEntry()
    data class RowItem(val row: AppointmentListRow) : AppointmentListEntry()
}

private fun buildAppointmentSections(rows: List<AppointmentListRow>): List<AppointmentSection> {
    val zone = MawidRegion.timeZone
    val today = LocalDate.now(zone)
    val byDate = rows.groupBy { LocalDate.parse(it.appointment.appointmentDate) }.toSortedMap()
    return byDate.map { (date, list) ->
        val title = when (date) {
            today -> "اليوم"
            today.plusDays(1) -> "غداً"
            else -> date.format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", MawidRegion.arabicLocale))
        }
        AppointmentSection(title, list.sortedBy { it.appointment.queueNumber })
    }
}

private fun daysUntilLabelAr(appointmentDate: LocalDate, today: LocalDate): String? {
    val d = ChronoUnit.DAYS.between(today, appointmentDate)
    return when {
        d <= 0 -> null
        d == 1L -> "باقي يوم واحد"
        else -> "باقي $d يوم"
    }
}

private fun statusLabelAr(status: String): String = when (status.lowercase()) {
    "waiting", "scheduled" -> "في الانتظار"
    "in_progress" -> "قيد المعاينة"
    "done" -> "مكتمل"
    "cancelled" -> "ملغى"
    else -> status
}

private fun statusIsCancelledOrDone(status: String): Boolean {
    val s = status.lowercase()
    return s == "cancelled" || s == "done"
}

@Composable
private fun AppointmentSectionHeader(title: String, isFirst: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = if (isFirst) 4.dp else 16.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(22.dp)
                .background(Primary, RoundedCornerShape(2.dp))
        )
        Text(
            title,
            fontFamily = Manrope,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            color = Primary,
            textAlign = TextAlign.Start
        )
    }
}

@Composable
private fun AppointmentSummaryCard(
    row: AppointmentListRow,
    today: LocalDate,
    ratingInFlightId: String?,
    onOpenQueue: () -> Unit,
    onRequestCancel: () -> Unit,
    onRateDoctor: (appointmentId: String, stars: Int) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val date = LocalDate.parse(row.appointment.appointmentDate)
    val dateLine = date.format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", MawidRegion.arabicLocale))
    val daysChip = daysUntilLabelAr(date, today)
    val st = row.appointment.status.lowercase()
    val badgeColor = when {
        st == "cancelled" -> scheme.error
        st == "done" -> scheme.secondary
        st == "in_progress" -> scheme.primary
        else -> scheme.primary
    }
    val canCancel = !statusIsCancelledOrDone(row.appointment.status)
    val timeSlot = row.appointment.timeSlot?.trim()?.takeIf { it.isNotEmpty() }
    val patientRating = row.appointment.patientRating

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenQueue)
            ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        row.doctorName,
                        fontFamily = Manrope,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = scheme.onSurface,
                        textAlign = TextAlign.Start
                    )
                    Text(
                        row.specialty,
                        fontFamily = PublicSans,
                        fontSize = 13.sp,
                        color = scheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                        textAlign = TextAlign.Start
                    )
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = badgeColor.copy(alpha = 0.14f)
                ) {
                    Text(
                        statusLabelAr(row.appointment.status),
                        fontFamily = PublicSans,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = scheme.primaryContainer.copy(alpha = 0.35f)
                ) {
                    Text(
                        "دور #${row.appointment.queueNumber}",
                        fontFamily = Manrope,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = scheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Filled.CalendarToday,
                            contentDescription = null,
                            tint = scheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            dateLine,
                            fontFamily = PublicSans,
                            fontSize = 13.sp,
                            color = scheme.onSurface,
                            textAlign = TextAlign.Start
                        )
                    }
                    if (timeSlot != null) {
                        Text(
                            "الوقت: $timeSlot",
                            fontFamily = PublicSans,
                            fontSize = 12.sp,
                            color = scheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp, start = 24.dp),
                            textAlign = TextAlign.Start
                        )
                    }
                }
            }
            }

            if (st == "done" && !row.appointment.doctorNotes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = scheme.primary.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .background(
                            scheme.primaryContainer.copy(alpha = 0.25f),
                            RoundedCornerShape(14.dp)
                        )
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Filled.MedicalServices,
                        contentDescription = null,
                        tint = scheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "ملاحظات الطبيب",
                            fontFamily = Manrope,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = scheme.primary,
                        )
                        Text(
                            row.appointment.doctorNotes.orEmpty(),
                            fontFamily = PublicSans,
                            fontSize = 14.sp,
                            color = scheme.onSurface,
                            modifier = Modifier.padding(top = 6.dp),
                            textAlign = TextAlign.Start,
                            lineHeight = 20.sp,
                        )
                    }
                }
            }

            if (st == "done" && patientRating == null) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    "قيّم تجربتك مع الطبيب",
                    fontFamily = Manrope,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = scheme.primary,
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (ratingInFlightId == row.appointment.id) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            color = scheme.primary,
                            strokeWidth = 2.dp,
                        )
                    }
                } else {
                    DoctorRatingStarsRow(
                        enabled = ratingInFlightId == null,
                        onPick = { onRateDoctor(row.appointment.id, it) },
                    )
                }
            } else if (st == "done" && patientRating != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    "تقييمك",
                    fontFamily = Manrope,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = scheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(6.dp))
                DoctorRatingStarsDisplay(stars = patientRating)
            }
            if (daysChip != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(100),
                    color = scheme.secondaryContainer.copy(alpha = 0.45f)
                ) {
                    Text(
                        daysChip,
                        fontFamily = PublicSans,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = scheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }
            }
            if (canCancel) {
                Spacer(modifier = Modifier.height(10.dp))
                TextButton(
                    onClick = onRequestCancel,
                    colors = ButtonDefaults.textButtonColors(contentColor = Error)
                ) {
                    Text("إلغاء الموعد", fontFamily = Manrope, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun DoctorRatingStarsRow(
    enabled: Boolean,
    onPick: (Int) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (i in 1..5) {
            Icon(
                imageVector = Icons.Outlined.StarBorder,
                contentDescription = "تقييم $i من 5",
                tint = scheme.primary,
                modifier = Modifier
                    .size(40.dp)
                    .clickable(enabled = enabled) { onPick(i) },
            )
        }
    }
}

@Composable
private fun DoctorRatingStarsDisplay(stars: Int) {
    val scheme = MaterialTheme.colorScheme
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (i in 1..5) {
            Icon(
                imageVector = if (i <= stars) Icons.Filled.Star else Icons.Outlined.StarBorder,
                contentDescription = null,
                tint = if (i <= stars) scheme.primary else scheme.outline,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}
