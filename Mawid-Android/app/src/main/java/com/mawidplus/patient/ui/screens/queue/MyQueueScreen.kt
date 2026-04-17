@file:OptIn(ExperimentalMaterial3Api::class)

package com.mawidplus.patient.ui.screens.queue

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.mawidplus.patient.core.model.QueueSettings
import com.mawidplus.patient.ui.components.DoctorPhotoDisplay
import com.mawidplus.patient.ui.components.BottomNavigationBar
import com.mawidplus.patient.ui.components.EditorialCard
import com.mawidplus.patient.ui.navigation.Routes
import com.mawidplus.patient.ui.theme.Error
import com.mawidplus.patient.ui.theme.Manrope
import com.mawidplus.patient.ui.theme.OnSecondary
import com.mawidplus.patient.ui.theme.OnSurface
import com.mawidplus.patient.ui.theme.OnSurfaceVariant
import com.mawidplus.patient.ui.theme.OutlineVariant
import com.mawidplus.patient.ui.theme.Primary
import com.mawidplus.patient.ui.theme.PrimaryContainer
import com.mawidplus.patient.ui.theme.PrimaryFixed
import com.mawidplus.patient.ui.theme.PublicSans
import com.mawidplus.patient.ui.theme.Secondary
import com.mawidplus.patient.ui.theme.SecondaryContainer
import com.mawidplus.patient.ui.theme.SecondaryFixed
import com.mawidplus.patient.ui.theme.Surface
import com.mawidplus.patient.ui.theme.SurfaceBright
import com.mawidplus.patient.ui.theme.SurfaceContainer
import com.mawidplus.patient.ui.theme.SurfaceContainerHigh
import com.mawidplus.patient.ui.theme.SurfaceContainerLow

private val QueueHeroShape = RoundedCornerShape(
    topEnd = 52.dp,
    bottomStart = 52.dp,
    topStart = 18.dp,
    bottomEnd = 18.dp,
)

private fun formatArabicAppointmentDate(iso: String): String {
    if (iso.isBlank()) return "—"
    return runCatching {
        val d = LocalDate.parse(iso)
        val formatter = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale("ar", "SA"))
        d.format(formatter)
    }.getOrElse { iso }
}

@Composable
fun MyQueueScreen(
    doctorId: String,
    viewModel: MyQueueViewModel = viewModel(
        key = doctorId,
        factory = MyQueueViewModel.factory(doctorId)
    ),
    onBack: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToTab: (String) -> Unit = {},
    onDoctorProfile: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.completionSound.collect {
            runCatching {
                val tg = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90)
                tg.startTone(ToneGenerator.TONE_PROP_BEEP, 320)
                tg.release()
            }
        }
    }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Surface)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 88.dp)
            ) {
                CenterAlignedTopAppBar(
                    modifier = Modifier.statusBarsPadding(),
                    title = {
                        Text(
                            "Mawid+",
                            fontFamily = Manrope,
                            fontWeight = FontWeight.Black,
                            color = Primary,
                            fontSize = 20.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.Filled.ArrowBack,
                                contentDescription = "رجوع",
                                tint = Primary
                            )
                        }
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

                Spacer(modifier = Modifier.height(16.dp))

                when (val state = uiState) {
                    is MyQueueUiState.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Primary)
                        }
                    }
                    is MyQueueUiState.Error -> {
                        Text(
                            state.message,
                            fontFamily = PublicSans,
                            color = OnSurfaceVariant,
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                    is MyQueueUiState.Ready -> {
                        val doctor = state.doctor
                        val queue = state.queue
                        val ticket = state.myTicketNumber
                        val progress = ticket?.let { t ->
                            (queue.currentNumber.toFloat() / t.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
                        } ?: 0f
                        val totalAhead = ticket?.let { (it - queue.currentNumber).coerceAtLeast(0) } ?: 0
                        val isYourTurn =
                            ticket != null && queue.currentNumber == ticket && !state.isVisitComplete
                        val showSoonBadge =
                            state.showLiveQueue && state.aheadInQueue > 0 && state.aheadInQueue <= 3

                        if (ticket == null) {
                            Text(
                                "لا يوجد موعد نشط مع هذا الطبيب. احجز موعداً من تبويب البحث.",
                                fontFamily = PublicSans,
                                color = OnSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 32.dp)
                            )
                        } else {
                            if (state.isVisitComplete) {
                                CompletedVisitHeroCard(
                                    ticketNumber = ticket,
                                    queueDate = state.appointmentDateIso ?: queue.queueDate,
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                )
                                DoctorNotesCard(
                                    notes = state.doctorNotes,
                                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
                                )
                            } else if (state.showLiveQueue) {
                                ActiveQueueHeroCard(
                                    ticketNumber = ticket,
                                    queue = queue,
                                    estimatedMinutes = state.estimatedMinutes,
                                    progress = progress,
                                    totalAhead = totalAhead,
                                    isYourTurn = isYourTurn,
                                    showSoonBadge = showSoonBadge,
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                )
                            } else {
                                FutureAppointmentHeroCard(
                                    ticketNumber = ticket,
                                    appointmentDateFormatted = formatArabicAppointmentDate(
                                        state.appointmentDateIso.orEmpty()
                                    ),
                                    daysUntil = state.daysUntilAppointment,
                                    doctorName = doctor.fullName,
                                    specialty = doctor.specialty,
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                )
                            }
                        }

                        Text(
                            "تفاصيل العيادة",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = Manrope,
                                fontWeight = FontWeight.Bold,
                                color = OnSurfaceVariant
                            ),
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                        )

                        EditorialCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(onClick = onDoctorProfile),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    DoctorPhotoDisplay(
                                        photoUrl = doctor.photoUrl,
                                        name = doctor.fullName,
                                        modifier = Modifier.size(64.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        initialsSize = 22.sp,
                                    )
                                    Column(
                                        modifier = Modifier.padding(start = 16.dp)
                                    ) {
                                        Text(
                                            doctor.specialty,
                                            color = OnSurfaceVariant.copy(alpha = 0.6f),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            doctor.fullName,
                                            fontFamily = Manrope,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp,
                                            color = OnSurface,
                                            textAlign = TextAlign.Start
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    QueueInfoTile(
                                        icon = Icons.Filled.LocationOn,
                                        label = "الموقع",
                                        value = doctor.clinicName ?: "الموقع غير مُحدَّد",
                                        modifier = Modifier.weight(1f)
                                    )
                                    QueueInfoTile(
                                        icon = Icons.Filled.CalendarToday,
                                        label = "التاريخ",
                                        value = state.appointmentDateIso?.let { formatArabicAppointmentDate(it) }
                                            ?: queue.queueDate,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(SecondaryContainer.copy(alpha = 0.2f))
                                .padding(16.dp)
                        ) {
                            Row {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Secondary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Filled.Info,
                                        contentDescription = null,
                                        tint = OnSecondary
                                    )
                                }
                                Column(modifier = Modifier.padding(start = 12.dp)) {
                                    Text(
                                        if (state.isVisitComplete && ticket != null) "تمت الزيارة" else "يرجى ملاحظة:",
                                        color = Secondary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        fontFamily = Manrope
                                    )
                                    Text(
                                        if (state.isVisitComplete && ticket != null) {
                                            "سجّل الطبيب هذه الجلسة كمكتملة. يمكنك الاحتفاظ بملاحظاته أعلاه للمراجعة."
                                        } else {
                                            "يرجى التواجد بالقرب من غرفة الفحص عند اقتراب دورك. سيتم تحديث الطابور تلقائياً كل بضع ثوانٍ."
                                        },
                                        color = OnSurfaceVariant,
                                        fontSize = 12.sp,
                                        fontFamily = PublicSans,
                                        textAlign = TextAlign.Start
                                    )
                                }
                            }
                        }

                        if (ticket != null && !state.isVisitComplete) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.cancelCurrentAppointment() },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.Transparent,
                                        contentColor = Error
                                    ),
                                    border = BorderStroke(1.dp, Error.copy(alpha = 0.2f)),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Filled.Cancel,
                                            contentDescription = null,
                                            tint = Error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            "إلغاء الموعد",
                                            color = Error,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = Manrope
                                        )
                                    }
                                }
                                Text(
                                    "قد يتم تطبيق رسوم إلغاء إذا قمت بالإلغاء قبل أقل من ساعتين من الموعد.",
                                    color = OutlineVariant,
                                    fontSize = 10.sp,
                                    fontFamily = PublicSans,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp)
                                )
                            }
                        }
                    }
                }
            }

            BottomNavigationBar(
                modifier = Modifier.align(Alignment.BottomCenter),
                selectedRoute = Routes.APPOINTMENTS,
                onItemSelected = onNavigateToTab
            )
        }
    }
}

@Composable
private fun FutureAppointmentHeroCard(
    ticketNumber: Int,
    appointmentDateFormatted: String,
    daysUntil: Long,
    doctorName: String,
    specialty: String,
    modifier: Modifier = Modifier,
) {
    val daysLabel = when {
        daysUntil <= 0L -> "موعدك اليوم"
        daysUntil == 1L -> "باقي يوم واحد على موعدك"
        else -> "باقي $daysUntil يوم على موعدك"
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = QueueHeroShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 220.dp)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(PrimaryFixed, Primary, PrimaryContainer),
                        start = Offset(0f, 0f),
                        end = Offset(700f, 500f),
                    )
                )
                .padding(horizontal = 20.dp, vertical = 22.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "موعدك القادم",
                    color = Color.White.copy(alpha = 0.92f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = Manrope,
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "#$ticketNumber",
                    fontFamily = Manrope,
                    fontWeight = FontWeight.Black,
                    fontSize = 48.sp,
                    color = Color.White,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "موعدك يوم $appointmentDateFormatted وأنت رقم $ticketNumber في الطابور",
                    color = Color.White.copy(alpha = 0.95f),
                    fontSize = 14.sp,
                    fontFamily = PublicSans,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "$doctorName · $specialty",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    fontFamily = PublicSans,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = daysLabel,
                    color = SecondaryFixed,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = Manrope,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.2f))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun ActiveQueueHeroCard(
    ticketNumber: Int,
    queue: QueueSettings,
    estimatedMinutes: Int,
    progress: Float,
    totalAhead: Int,
    isYourTurn: Boolean,
    showSoonBadge: Boolean,
    modifier: Modifier = Modifier,
) {
    val accentOrb = Color.White.copy(alpha = 0.12f)
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = QueueHeroShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 232.dp)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(Primary, PrimaryContainer, Color(0xFF0D47A1)),
                        start = Offset(0f, 0f),
                        end = Offset(800f, 900f),
                    )
                )
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 24.dp, y = (-28).dp)
                    .clip(CircleShape)
                    .background(accentOrb)
            )
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .align(Alignment.BottomStart)
                    .offset(x = (-20).dp, y = 20.dp)
                    .clip(CircleShape)
                    .background(accentOrb.copy(alpha = 0.08f))
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "رقمك في الانتظار",
                    color = Color.White.copy(alpha = 0.92f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = Manrope,
                    modifier = Modifier
                        .clip(RoundedCornerShape(100))
                        .background(Color.White.copy(alpha = 0.14f))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "#$ticketNumber",
                    fontFamily = Manrope,
                    fontWeight = FontWeight.Black,
                    fontSize = 56.sp,
                    lineHeight = 58.sp,
                    color = Color.White,
                )
                if (isYourTurn) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "دورك الآن",
                        fontFamily = Manrope,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = SecondaryFixed,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.15f))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                } else if (showSoonBadge) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "دورك قريب",
                        fontFamily = Manrope,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color(0xFFB9F6CA),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.2f))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Filled.Schedule,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.95f),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "الوقت المقدر لكل موعد: $estimatedMinutes دقيقة",
                        color = Color.White.copy(alpha = 0.88f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = PublicSans,
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "يتم الآن خدمة: #${queue.currentNumber}",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = PublicSans,
                            textAlign = TextAlign.Start
                        )
                        Text(
                            text = "باقٍ أمامك: $totalAhead",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = PublicSans,
                            textAlign = TextAlign.Start
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = SecondaryFixed,
                        trackColor = Color.White.copy(alpha = 0.22f),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "تاريخ الطابور: ${queue.queueDate}",
                        color = Color.White.copy(alpha = 0.72f),
                        fontSize = 11.sp,
                        fontFamily = PublicSans,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun CompletedVisitHeroCard(
    ticketNumber: Int,
    queueDate: String,
    modifier: Modifier = Modifier,
) {
    val deepTeal = Color(0xFF00332E)
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = QueueHeroShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 200.dp)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(Secondary, deepTeal),
                        start = Offset(0f, 0f),
                        end = Offset(700f, 500f),
                    )
                )
                .padding(horizontal = 20.dp, vertical = 22.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 16.dp, y = (-20).dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f))
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Done,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "اكتملت الجلسة",
                    fontFamily = Manrope,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color.White,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "شكراً لزيارتك",
                    color = SecondaryFixed,
                    fontSize = 15.sp,
                    fontFamily = Manrope,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "سجّل الطبيب اكتمال موعدك.",
                    color = Color.White.copy(alpha = 0.88f),
                    fontSize = 13.sp,
                    fontFamily = PublicSans,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "رقمك كان #$ticketNumber",
                    fontFamily = Manrope,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 28.sp,
                    color = SecondaryFixed,
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "تاريخ الطابور: $queueDate",
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 11.sp,
                    fontFamily = PublicSans,
                )
            }
        }
    }
}

@Composable
private fun DoctorNotesCard(
    notes: String?,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(4.dp, Primary, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                Icons.Filled.MedicalServices,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(26.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "ملاحظات طبيبك",
                    fontFamily = Manrope,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Primary,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = notes?.takeIf { it.isNotBlank() }
                        ?: "لم يُضف الطبيب ملاحظات لهذه الزيارة.",
                    fontFamily = PublicSans,
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    color = if (notes.isNullOrBlank()) OnSurfaceVariant.copy(alpha = 0.75f) else OnSurface,
                    textAlign = TextAlign.Start,
                )
            }
        }
    }
}

@Composable
private fun QueueInfoTile(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceContainerLow)
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Primary,
                fontFamily = PublicSans
            )
        }
        Text(
            value,
            fontFamily = PublicSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            color = OnSurface,
            textAlign = TextAlign.Start,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
