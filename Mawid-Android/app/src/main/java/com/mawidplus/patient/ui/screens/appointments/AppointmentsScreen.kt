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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
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
import com.mawidplus.patient.ui.theme.SecondaryContainer
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

@Composable
fun AppointmentsScreen(
    viewModel: AppointmentsViewModel = viewModel { AppointmentsViewModel() },
    onNavigateToQueue: (doctorId: String) -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToSearch: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingCancel by remember { mutableStateOf<AppointmentListRow?>(null) }

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
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(24.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.CalendarToday,
                                        contentDescription = null,
                                        tint = Primary.copy(alpha = 0.35f),
                                        modifier = Modifier.size(56.dp)
                                    )
                                    Spacer(modifier = Modifier.height(20.dp))
                                    Text(
                                        "لا توجد مواعيد بعد",
                                        fontFamily = Manrope,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp,
                                        color = OnSurface,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        "احجز موعداً من البحث عن الأطباء.",
                                        fontFamily = PublicSans,
                                        fontSize = 14.sp,
                                        color = OnSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(top = 12.dp)
                                    )
                                }
                            }
                        } else {
                            val zone = ZoneId.of("Asia/Riyadh")
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
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
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
                                        is AppointmentListEntry.Header -> Text(
                                            entry.title,
                                            fontFamily = Manrope,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = Primary,
                                            modifier = Modifier.padding(top = if (entry.index == 0) 0.dp else 8.dp, bottom = 4.dp)
                                        )
                                        is AppointmentListEntry.RowItem -> AppointmentSummaryCard(
                                            row = entry.row,
                                            today = today,
                                            onOpenQueue = { onNavigateToQueue(entry.row.appointment.doctorId) },
                                            onRequestCancel = { pendingCancel = entry.row }
                                        )
                                    }
                                }
                            }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
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

private data class AppointmentSection(val title: String, val rows: List<AppointmentListRow>)

private sealed class AppointmentListEntry {
    data class Header(val index: Int, val title: String) : AppointmentListEntry()
    data class RowItem(val row: AppointmentListRow) : AppointmentListEntry()
}

private fun buildAppointmentSections(rows: List<AppointmentListRow>): List<AppointmentSection> {
    val zone = ZoneId.of("Asia/Riyadh")
    val today = LocalDate.now(zone)
    val byDate = rows.groupBy { LocalDate.parse(it.appointment.appointmentDate) }.toSortedMap()
    return byDate.map { (date, list) ->
        val title = when (date) {
            today -> "اليوم"
            today.plusDays(1) -> "غداً"
            else -> date.format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale("ar", "SA")))
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
private fun AppointmentSummaryCard(
    row: AppointmentListRow,
    today: LocalDate,
    onOpenQueue: () -> Unit,
    onRequestCancel: () -> Unit,
) {
    val date = LocalDate.parse(row.appointment.appointmentDate)
    val dateLine = date.format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale("ar", "SA")))
    val daysChip = daysUntilLabelAr(date, today)
    val st = row.appointment.status.lowercase()
    val badgeColor = when {
        st == "cancelled" -> Error
        st == "done" -> Color(0xFF2E7D32)
        else -> Primary
    }
    val canCancel = !statusIsCancelledOrDone(row.appointment.status)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceContainerLowest, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenQueue),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                row.doctorName,
                fontFamily = Manrope,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = OnSurface,
                modifier = Modifier.weight(1f)
            )
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = badgeColor.copy(alpha = 0.12f)
            ) {
                Text(
                    statusLabelAr(row.appointment.status),
                    fontFamily = PublicSans,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = badgeColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
        Text(
            row.specialty,
            fontFamily = PublicSans,
            fontSize = 12.sp,
            color = OnSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
        Text(
            "التاريخ: $dateLine",
            fontFamily = PublicSans,
            fontSize = 14.sp,
            color = OnSurfaceVariant,
            modifier = Modifier
                .padding(top = 6.dp)
                .clickable(onClick = onOpenQueue)
        )
        Text(
            "رقم الطابور: ${row.appointment.queueNumber}",
            fontFamily = PublicSans,
            fontSize = 13.sp,
            color = OnSurface,
            modifier = Modifier
                .padding(top = 4.dp)
                .clickable(onClick = onOpenQueue)
        )
        if (st == "done" && !row.appointment.doctorNotes.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(width = 4.dp, color = Primary, shape = RoundedCornerShape(12.dp))
                    .background(Color(0xFFE3F2FD), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    Icons.Filled.MedicalServices,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(22.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "ملاحظات الطبيب",
                        fontFamily = Manrope,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Primary,
                    )
                    Text(
                        row.appointment.doctorNotes.orEmpty(),
                        fontFamily = PublicSans,
                        fontSize = 14.sp,
                        color = OnSurface,
                        modifier = Modifier.padding(top = 4.dp),
                        textAlign = TextAlign.Start,
                    )
                }
            }
        }
        if (daysChip != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(100),
                color = SecondaryContainer.copy(alpha = 0.35f)
            ) {
                Text(
                    daysChip,
                    fontFamily = PublicSans,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF00695C),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
        if (canCancel) {
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(
                onClick = onRequestCancel,
                colors = ButtonDefaults.textButtonColors(contentColor = Error)
            ) {
                Text("إلغاء الموعد", fontFamily = Manrope, fontWeight = FontWeight.Bold)
            }
        }
    }
}
