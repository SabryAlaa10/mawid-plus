@file:OptIn(ExperimentalMaterial3Api::class)

package com.mawidplus.patient.ui.screens.appointments

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
    onNavigateToQueue: (doctorId: String, appointmentId: String?) -> Unit = { _, _ -> },
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToSearch: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val statusFilter by viewModel.statusFilter.collectAsStateWithLifecycle()
    val periodFilter by viewModel.periodFilter.collectAsStateWithLifecycle()
    val filteredRows by viewModel.filteredRows.collectAsStateWithLifecycle()
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
                            Spacer(modifier = Modifier.height(12.dp))
                            FilterChipRow(
                                label = "حالة الموعد",
                                options = listOf("الكل", "قادم", "مكتمل", "ملغي"),
                                selected = statusFilter,
                                onSelect = { viewModel.setStatusFilter(it) },
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            FilterChipRow(
                                label = "الفترة الزمنية",
                                options = listOf("الكل", "هذا الشهر", "آخر 3 شهور", "هذه السنة"),
                                selected = periodFilter,
                                onSelect = { viewModel.setPeriodFilter(it) },
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            if (filteredRows.isEmpty() && s.rows.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .padding(top = 48.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            Icons.Filled.CalendarToday,
                                            contentDescription = null,
                                            tint = OnSurfaceVariant.copy(alpha = 0.5f),
                                            modifier = Modifier.size(48.dp),
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            "لا توجد مواعيد تطابق الفلتر المحدد",
                                            fontSize = 14.sp,
                                            color = OnSurfaceVariant,
                                            fontFamily = PublicSans,
                                            textAlign = TextAlign.Center,
                                        )
                                    }
                                }
                            } else {
                                val flatItems = remember(filteredRows) {
                                    val sections = buildAppointmentSections(filteredRows)
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
                                    verticalArrangement = Arrangement.spacedBy(14.dp),
                                ) {
                                    items(
                                        items = flatItems,
                                        key = { entry ->
                                            when (entry) {
                                                is AppointmentListEntry.Header -> "h_${entry.index}_${entry.title}"
                                                is AppointmentListEntry.RowItem -> entry.row.appointment.id
                                            }
                                        },
                                    ) { entry ->
                                        when (entry) {
                                            is AppointmentListEntry.Header -> AppointmentSectionHeader(
                                                title = entry.title,
                                                isFirst = entry.index == 0,
                                            )
                                            is AppointmentListEntry.RowItem -> AppointmentSummaryCard(
                                                row = entry.row,
                                                today = today,
                                                ratingInFlightId = ratingInFlightId,
                                                onOpenQueue = {
                                                    onNavigateToQueue(
                                                        entry.row.appointment.doctorId,
                                                        entry.row.appointment.id,
                                                    )
                                                },
                                                onRequestCancel = { pendingCancel = entry.row },
                                                onRateDoctor = { id, stars ->
                                                    viewModel.submitDoctorRating(id, stars)
                                                },
                                            )
                                        }
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
                                    val first = filteredRows.firstOrNull()
                                    val id = first?.appointment?.doctorId ?: SeedDoctorIds.FAMILY_AHMED
                                    onNavigateToQueue(id, first?.appointment?.id)
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

@Composable
private fun FilterChipRow(
    label: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = OnSurfaceVariant,
            fontFamily = PublicSans,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { option ->
                val isSelected = option == selected
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) Primary else Color.Transparent)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) Primary else OnSurfaceVariant.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(20.dp),
                        )
                        .clickable { onSelect(option) }
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                ) {
                    Text(
                        text = option,
                        fontSize = 13.sp,
                        fontFamily = PublicSans,
                        color = if (isSelected) Color.White else OnSurfaceVariant,
                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                    )
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
    val dateCompact = date.format(DateTimeFormatter.ofPattern("d MMMM yyyy", MawidRegion.arabicLocale))
    val daysChip = daysUntilLabelAr(date, today)
    val st = row.appointment.status.lowercase()
    val canCancel = !statusIsCancelledOrDone(row.appointment.status)
    val timeSlot = row.appointment.timeSlot?.trim()?.takeIf { it.isNotEmpty() } ?: "—"
    val patientRating = row.appointment.patientRating

    val cardWhite = Color(0xFFFFFFFF)
    val pillGray = Color(0xFFF1F3F4)
    val completedBg = Color(0xFFE0F2F1)
    val completedDot = Color(0xFF2E7D32)
    val completedText = Color(0xFF1B5E20)
    val notesBg = Color(0xFFE3F2FD)
    val starOrange = Color(0xFFFF9800)
    val secondaryBtnBg = Color(0xFFE8EAED)
    val subtleBorder = Color(0xFFE0E0E0)

    val statusPillBg: Color
    val statusPillFg: Color
    val statusDot: Color
    when {
        st == "done" -> {
            statusPillBg = completedBg
            statusPillFg = completedText
            statusDot = completedDot
        }
        st == "cancelled" -> {
            statusPillBg = scheme.errorContainer.copy(alpha = 0.5f)
            statusPillFg = scheme.onErrorContainer
            statusDot = scheme.error
        }
        else -> {
            statusPillBg = scheme.primaryContainer.copy(alpha = 0.45f)
            statusPillFg = scheme.onPrimaryContainer
            statusDot = scheme.primary
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        row.doctorName,
                        fontFamily = Manrope,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF1A1A1A),
                        textAlign = TextAlign.Start
                    )
                    Text(
                        row.specialty,
                        fontFamily = PublicSans,
                        fontSize = 12.sp,
                        color = Color(0xFF757575),
                        letterSpacing = 0.2.sp,
                        modifier = Modifier.padding(top = 4.dp),
                        textAlign = TextAlign.Start
                    )
                }
                if (row.doctorPhotoUrl != null) {
                    AsyncImage(
                        model = row.doctorPhotoUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .border(1.dp, subtleBorder, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(pillGray)
                            .border(1.dp, subtleBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Person,
                            contentDescription = null,
                            tint = Color(0xFF9E9E9E),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(shape = RoundedCornerShape(24.dp), color = pillGray) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "الدور",
                            fontFamily = PublicSans,
                            fontSize = 11.sp,
                            color = Color(0xFF616161)
                        )
                        Text(
                            "#${row.appointment.queueNumber}",
                            fontFamily = Manrope,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFF1A1A1A)
                        )
                    }
                }
                Surface(shape = RoundedCornerShape(24.dp), color = statusPillBg) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(statusDot)
                        )
                        Text(
                            statusLabelAr(row.appointment.status),
                            fontFamily = PublicSans,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = statusPillFg
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = pillGray
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "الوقت",
                                fontFamily = PublicSans,
                                fontSize = 11.sp,
                                color = Color(0xFF757575)
                            )
                            Text(
                                timeSlot,
                                fontFamily = Manrope,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = Color(0xFF1A1A1A)
                            )
                        }
                        Icon(
                            Icons.Filled.Schedule,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = pillGray
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "التاريخ",
                                fontFamily = PublicSans,
                                fontSize = 11.sp,
                                color = Color(0xFF757575)
                            )
                            Text(
                                dateCompact,
                                fontFamily = Manrope,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = Color(0xFF1A1A1A)
                            )
                        }
                        Icon(
                            Icons.Filled.CalendarToday,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            if (st == "done" && !row.appointment.doctorNotes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(notesBg)
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "ملاحظات الطبيب",
                            fontFamily = Manrope,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Primary
                        )
                        Icon(
                            Icons.AutoMirrored.Filled.Assignment,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        row.appointment.doctorNotes.orEmpty(),
                        fontFamily = PublicSans,
                        fontSize = 14.sp,
                        color = Color(0xFF424242),
                        textAlign = TextAlign.Start,
                        lineHeight = 22.sp
                    )
                }
            }

            if (st == "done" && patientRating != null) {
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = subtleBorder, thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "${patientRating}.0",
                        fontFamily = Manrope,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF1A1A1A)
                    )
                    DoctorRatingStarsDisplayOrange(
                        stars = patientRating,
                        starColor = starOrange
                    )
                }
            } else if (st == "done" && patientRating == null) {
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = subtleBorder, thickness = 1.dp)
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    "قيّم تجربتك مع الطبيب",
                    fontFamily = Manrope,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (ratingInFlightId == row.appointment.id) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            color = Primary,
                            strokeWidth = 2.dp
                        )
                    }
                } else {
                    DoctorRatingStarsRow(
                        enabled = ratingInFlightId == null,
                        onPick = { onRateDoctor(row.appointment.id, it) },
                    )
                }
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

            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onOpenQueue,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Primary,
                        contentColor = OnPrimary
                    )
                ) {
                    Text(
                        "التفاصيل",
                        fontFamily = Manrope,
                        fontWeight = FontWeight.Bold
                    )
                }
                Button(
                    onClick = onOpenQueue,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = secondaryBtnBg,
                        contentColor = Color(0xFF424242)
                    )
                ) {
                    Text(
                        "عرض الطابور",
                        fontFamily = Manrope,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (canCancel) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = onRequestCancel,
                    colors = ButtonDefaults.textButtonColors(contentColor = Error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("إلغاء الموعد", fontFamily = Manrope, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun DoctorRatingStarsDisplayOrange(stars: Int, starColor: Color) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 1..5) {
            Icon(
                imageVector = if (i <= stars) Icons.Filled.Star else Icons.Outlined.StarBorder,
                contentDescription = null,
                tint = if (i <= stars) starColor else Color(0xFFE0E0E0),
                modifier = Modifier.size(22.dp)
            )
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

