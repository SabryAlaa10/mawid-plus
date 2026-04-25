@file:OptIn(ExperimentalMaterial3Api::class)

package com.mawidplus.patient.ui.screens.home

import android.widget.Toast
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.mawidplus.patient.data.SeedDoctorIds
import com.mawidplus.patient.ui.components.DoctorPhotoDisplay
import com.mawidplus.patient.ui.components.rememberCrossfadeImageRequest
import com.mawidplus.patient.ui.theme.Manrope
import com.mawidplus.patient.ui.theme.OnPrimary
import com.mawidplus.patient.ui.theme.OnSecondaryContainer
import com.mawidplus.patient.ui.theme.OnSurface
import com.mawidplus.patient.ui.theme.OnSurfaceVariant
import com.mawidplus.patient.ui.theme.Primary
import com.mawidplus.patient.ui.theme.PrimaryContainer
import com.mawidplus.patient.ui.theme.PublicSans
import com.mawidplus.patient.ui.theme.SecondaryContainer
import com.mawidplus.patient.ui.theme.Surface
import com.mawidplus.patient.ui.theme.SurfaceBright
import com.mawidplus.patient.ui.theme.SurfaceContainerLow
import com.mawidplus.patient.ui.theme.SurfaceContainerLowest

private const val IMG_TIP_YOGA =
    "https://images.unsplash.com/photo-1544367567-0f2fcb009e0b?w=600&h=300&fit=crop&q=80"
private const val IMG_TIP_FOOD =
    "https://images.unsplash.com/photo-1490645935967-10de6ba17061?w=600&h=300&fit=crop&q=80"

private data class HealthTip(
    val imageUrl: String,
    val badge: String,
    val title: String,
    val excerpt: String
)

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    onNavigateToQueue: (doctorId: String, appointmentId: String?) -> Unit = { _, _ -> },
    /** ينتقل إلى شاشة البحث مع النص الحالي في شريط البحث. */
    onNavigateToSearch: (query: String) -> Unit = {},
    onNavigateToAppointments: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNewBooking: () -> Unit = {},
    onNearMe: () -> Unit = {},
    /** زر «مساعد موعد+» — اربط التنقل أو المنطق من `MainTabContainer` / `AppNavGraph` عند تجهيز الميزة. */
    onSmartAssistant: () -> Unit = {},
    onHealthTipClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val homeState by viewModel.uiState.collectAsStateWithLifecycle()
    var showGreetingEnter by remember { mutableStateOf(false) }
    var showSubtitleEnter by remember { mutableStateOf(false) }
    LaunchedEffect(homeState) {
        if (homeState is HomeUiState.Ready) {
            android.util.Log.d("HomeDebug", "homeState ready, starting animation")
            delay(200)
            showGreetingEnter = true
        }
    }
    LaunchedEffect(homeState) {
        if (homeState is HomeUiState.Ready) {
            delay(350)
            showSubtitleEnter = true
        }
    }
    val greetingAlpha by animateFloatAsState(
        targetValue = if (showGreetingEnter) 1f else 0f,
        animationSpec = tween(600, easing = EaseOutCubic),
        label = "greetingAlpha",
    )
    val greetingOffset by animateFloatAsState(
        targetValue = if (showGreetingEnter) 0f else 60f,
        animationSpec = tween(600, easing = EaseOutCubic),
        label = "greetingOffset",
    )
    val subtitleAlpha by animateFloatAsState(
        targetValue = if (showSubtitleEnter) 1f else 0f,
        animationSpec = tween(500, easing = EaseOutCubic),
        label = "subtitleAlpha",
    )
    val tips = listOf(
        HealthTip(
            IMG_TIP_YOGA,
            "نمط حياة",
            "فوائد التمارين الصباحية",
            "اكتشف كيف يمكن لعشر دقائق فقط أن تغير مزاجك طوال اليوم..."
        ),
        HealthTip(
            IMG_TIP_FOOD,
            "تغذية",
            "أطعمة تدعم المناعة",
            "نصائح بسيطة لوجبة متوازنة تناسب يومك..."
        )
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface)
    ) {
        item {
            CenterAlignedTopAppBar(
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
                    IconButton(
                        onClick = {
                            when (val s = homeState) {
                                is HomeUiState.Ready -> onNavigateToQueue(
                                    s.queueShortcutDoctorId,
                                    s.upcoming?.appointmentId,
                                )
                                else -> onNavigateToQueue(SeedDoctorIds.FAMILY_AHMED, null)
                            }
                        }
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "طابوري",
                            tint = Primary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToNotifications) {
                        Icon(Icons.Filled.Notifications, contentDescription = "إشعارات", tint = Primary)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = SurfaceBright.copy(alpha = 0.92f)
                )
            )
        }

        item {
            when (val s = homeState) {
                is HomeUiState.Loading -> {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        color = Primary
                    )
                }
                is HomeUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            s.message,
                            fontFamily = PublicSans,
                            color = OnSurfaceVariant,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Start
                        )
                        TextButton(onClick = { viewModel.refresh() }) {
                            Text("إعادة المحاولة", color = Primary, fontFamily = PublicSans)
                        }
                    }
                }
                is HomeUiState.Ready -> {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    alpha = greetingAlpha
                                    translationY = greetingOffset
                                },
                        ) {
                            Text(
                                text = "مرحباً،",
                                fontFamily = Manrope,
                                fontWeight = FontWeight.Normal,
                                fontSize = 16.sp,
                                color = OnSurfaceVariant,
                                textAlign = TextAlign.Start,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Text(
                                text = s.patientName,
                                fontFamily = Manrope,
                                fontWeight = FontWeight.Bold,
                                fontSize = 28.sp,
                                color = OnSurface,
                                textAlign = TextAlign.Start,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        if (s.upcoming == null) {
                            Text(
                                text = "نسعد بخدمتك — احجز موعدك من البحث أو الإجراءات السريعة.",
                                fontFamily = PublicSans,
                                fontSize = 13.sp,
                                color = OnSurfaceVariant,
                                textAlign = TextAlign.Start,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                            )
                        } else {
                            Text(
                                text = "إليك أقرب موعد مسجّل لك.",
                                fontFamily = PublicSans,
                                fontSize = 13.sp,
                                color = OnSurfaceVariant,
                                textAlign = TextAlign.Start,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 2.dp)
                                    .graphicsLayer { alpha = subtitleAlpha },
                            )
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "موعدك القادم",
                    fontFamily = Manrope,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = OnSurface,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    onClick = { onNavigateToAppointments() }
                ) {
                    Text(
                        "عرض الكل",
                        color = Primary,
                        fontFamily = PublicSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }
        }

        item {
            when (val s = homeState) {
                is HomeUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Primary)
                    }
                }
                is HomeUiState.Error -> {
                    HomeUpcomingEmptyCard(onBook = { onNavigateToSearch("") })
                }
                is HomeUiState.Ready -> {
                    if (s.upcoming == null) {
                        HomeUpcomingEmptyCard(onBook = { onNavigateToSearch("") })
                    } else {
                        UpcomingAppointmentCard(
                            data = s.upcoming,
                            onViewDetails = {
                                onNavigateToQueue(s.upcoming.doctorId, s.upcoming.appointmentId)
                            },
                        )
                    }
                }
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    "إجراءات سريعة",
                    fontFamily = Manrope,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = OnSurface,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onNewBooking,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SecondaryContainer,
                        contentColor = OnSecondaryContainer
                    ),
                    shape = RoundedCornerShape(36.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            NewBookingPlusIcon()
                            Column(horizontalAlignment = Alignment.Start) {
                                Text(
                                    "حجز جديد",
                                    fontFamily = Manrope,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = OnSecondaryContainer,
                                    textAlign = TextAlign.Start
                                )
                                Text(
                                    "احجز موعدك في ثوانٍ",
                                    fontSize = 10.sp,
                                    fontFamily = PublicSans,
                                    color = OnSecondaryContainer.copy(alpha = 0.65f),
                                    textAlign = TextAlign.Start
                                )
                            }
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "متابعة",
                            tint = OnSecondaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onSmartAssistant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OnSecondaryContainer,
                        contentColor = OnPrimary
                    ),
                    shape = RoundedCornerShape(36.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(OnPrimary.copy(alpha = 0.2f))
                                    .padding(10.dp)
                            ) {
                                Icon(
                                    Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    tint = OnPrimary,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Column(horizontalAlignment = Alignment.Start) {
                                Text(
                                    "مساعد موعد+",
                                    fontFamily = Manrope,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = OnPrimary,
                                    textAlign = TextAlign.Start
                                )
                                Text(
                                    "صف أعراضك لنجد لك الطبيب المناسب",
                                    fontSize = 10.sp,
                                    fontFamily = PublicSans,
                                    color = OnPrimary.copy(alpha = 0.85f),
                                    textAlign = TextAlign.Start
                                )
                            }
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "متابعة",
                            tint = OnPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    VisualConsultationCard(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            Toast.makeText(
                                context,
                                "قريباً — الاستشارة المرئية هتكون متاحة",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                    NearMeCard(
                        modifier = Modifier.weight(1f),
                        onClick = onNearMe
                    )
                }
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    "نصائح صحية لك",
                    fontFamily = Manrope,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = OnSurface,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(tips, key = { it.imageUrl }) { tip ->
                        HealthTipCard(tip = tip, onClick = onHealthTipClick)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun HomeUpcomingEmptyCard(onBook: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(200.dp)
            .clip(EditorialShape)
            .background(SurfaceContainerLow)
            .clickable(onClick = onBook)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "لا مواعيد قادمة مسجّلة",
                fontFamily = Manrope,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = OnSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = "ابحث عن طبيب واحجز موعدك من تبويب البحث.",
                fontFamily = PublicSans,
                fontSize = 13.sp,
                color = OnSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "الانتقال للبحث",
                fontFamily = Manrope,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Primary
            )
        }
    }
}

private val CompletedVisitGreen = Color(0xFF2E7D32)
/** تدرج أزرق: أعمق يساراً/أعلى — كما في مرجع التصميم */
private val AppointmentCardGradientA = Color(0xFF0A5FCC)
private val AppointmentCardGradientB = Color(0xFF1A8CFF)
private val AppointmentCardGradientC = Color(0xFF1A73E8)

@Composable
private fun UpcomingAppointmentCard(
    data: UpcomingAppointmentCardData,
    onViewDetails: () -> Unit,
) {
    val cardShape = RoundedCornerShape(28.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .shadow(
                elevation = 10.dp,
                shape = cardShape,
                spotColor = Color(0xFF1A73E8).copy(alpha = 0.45f),
                ambientColor = Color(0xFF000000).copy(alpha = 0.2f),
            )
            .clip(cardShape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(AppointmentCardGradientA, AppointmentCardGradientB, AppointmentCardGradientC),
                    start = Offset(0f, 0f),
                    end = Offset(900f, 500f),
                ),
            )
            .then(
                if (data.statusBadgeAr == "مكتمل") {
                    Modifier.border(2.dp, CompletedVisitGreen.copy(alpha = 0.85f), cardShape)
                } else {
                    Modifier
                },
            )
            .clickable(onClick = onViewDetails)
            .padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                // —— الصف العلوي: عيادة (يمين) + دائرة رقم الانتظار (يسار) ——
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(
                        modifier = Modifier.weight(1f, fill = false),
                    ) {
                        Text(
                            text = "العيادة",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.88f),
                            fontFamily = PublicSans,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = data.clinicLabel,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = Manrope,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "حالة الحجز",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            fontFamily = PublicSans,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.2f))
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                        ) {
                            Text(
                                text = data.statusBadgeAr,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                fontFamily = PublicSans,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.22f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "رقم الانتظار",
                                fontSize = 10.sp,
                                lineHeight = 12.sp,
                                color = Color.White.copy(alpha = 0.9f),
                                fontFamily = PublicSans,
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "#${data.queueNumber}",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontFamily = Manrope,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // —— الدكتور: صورة دائرية يمين، "الدكتور" + الاسم + التخصص يسار من الصورة ——
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DoctorPhotoDisplay(
                        photoUrl = data.doctorPhotoUrl,
                        name = data.doctorName,
                        modifier = Modifier
                            .size(64.dp)
                            .border(2.dp, Color.White.copy(alpha = 0.5f), CircleShape),
                        shape = CircleShape,
                        initialsSize = 18.sp,
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "الدكتور",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.88f),
                            fontFamily = PublicSans,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = data.doctorName,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = Manrope,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (data.specialty.isNotBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                data.specialty,
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.9f),
                                fontFamily = PublicSans,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(
                    color = Color.White.copy(alpha = 0.45f),
                    thickness = 1.dp,
                )
                Spacer(modifier = Modifier.height(12.dp))

                // —— التذييل: تاريخ (يمين) + وقت (يسار) ——
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.CalendarToday,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = data.displayDateAr,
                            fontSize = 14.sp,
                            color = Color.White,
                            fontFamily = PublicSans,
                            maxLines = 1,
                        )
                    }
                    if (data.appointmentTime.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = data.appointmentTime,
                                fontSize = 14.sp,
                                color = Color.White,
                                fontFamily = PublicSans,
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                Icons.Filled.Schedule,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    } else {
                        Text(
                            text = "—",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 14.sp,
                        )
                    }
                }
            }
        }
    }
}

/** حد فاتح وبطاقة بيضاء كما في التصميم */
private val QuickActionCardBorder = Color(0xFFE2E8F0)
private val VisualConsultIconCircle = Color(0xFFF5E6D3)
private val VisualConsultIconTint = Color(0xFF6D4C41)
private val NearMeIconCircle = Color(0xFFE3F2FD)

@Composable
fun VisualConsultationCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .height(140.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .border(1.dp, QuickActionCardBorder, RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(VisualConsultIconCircle),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Videocam,
                    contentDescription = "استشارة مرئية",
                    tint = VisualConsultIconTint,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "استشارة مرئية",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                fontFamily = Manrope,
                color = OnSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun NearMeCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .height(140.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .border(1.dp, QuickActionCardBorder, RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(NearMeIconCircle),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.NearMe,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "العيادات القريبة",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                fontFamily = Manrope,
                color = OnSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun HealthTipCard(
    tip: HealthTip,
    onClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .width(280.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainerLow)
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(12.dp))
        ) {
            val tipImageReq = rememberCrossfadeImageRequest(tip.imageUrl)
            AsyncImage(
                model = tipImageReq ?: tip.imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Text(
                text = tip.badge,
                fontFamily = PublicSans,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SecondaryContainer.copy(alpha = 0.95f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            tip.title,
            fontFamily = Manrope,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = OnSurface,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            tip.excerpt,
            fontSize = 10.sp,
            fontFamily = PublicSans,
            color = OnSurfaceVariant,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** زر حجز جديد: دائرة + خارجية فاتحة وداخلية داكنة مع + أبيض (يمين)، نص، سهم يسار. */
@Composable
private fun NewBookingPlusIcon() {
    Box(
        modifier = Modifier.size(48.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White.copy(alpha = 0.32f), CircleShape)
        )
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(34.dp)
                .background(OnSecondaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Add,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

private val EditorialShape = RoundedCornerShape(
    topStart = 16.dp,
    topEnd = 48.dp,
    bottomStart = 48.dp,
    bottomEnd = 16.dp
)
