@file:OptIn(ExperimentalMaterial3Api::class)

package com.mawidplus.patient.ui.screens.search

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.mawidplus.patient.core.model.Doctor
import com.mawidplus.patient.ui.components.DoctorPhotoDisplay
import com.mawidplus.patient.ui.theme.Manrope
import com.mawidplus.patient.ui.theme.OnSurface
import com.mawidplus.patient.ui.theme.OnSurfaceVariant
import com.mawidplus.patient.ui.theme.Primary
import com.mawidplus.patient.ui.theme.PublicSans
import com.mawidplus.patient.ui.theme.Secondary
import com.mawidplus.patient.ui.theme.Surface
import com.mawidplus.patient.ui.theme.SurfaceBright
import com.mawidplus.patient.ui.theme.SurfaceContainerHigh
import com.mawidplus.patient.ui.theme.SurfaceContainerLow
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Locale

@Composable
fun DoctorDetailScreen(
    doctorId: String,
    onBack: () -> Unit,
    onBook: () -> Unit,
    onOpenMap: (doctorId: String) -> Unit = {},
    viewModel: DoctorDetailViewModel = viewModel(
        key = doctorId,
        factory = DoctorDetailViewModel.factory(doctorId)
    ),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        when (val s = state) {
            is DoctorDetailUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Surface),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Primary)
                }
            }
            is DoctorDetailUiState.Error -> {
                SearchSubScreenScaffold(
                    title = "ملف الطبيب",
                    onBack = onBack,
                    body = {
                        Text(
                            text = s.message,
                            fontFamily = Manrope,
                            fontSize = 15.sp,
                            color = OnSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                        )
                    }
                )
            }
            is DoctorDetailUiState.Ready -> {
                DoctorDetailReadyContent(
                    doctor = s.doctor,
                    onBack = onBack,
                    onBook = onBook,
                    onOpenMap = { onOpenMap(doctorId) },
                )
            }
        }
    }
}

@Composable
private fun DoctorDetailReadyContent(
    doctor: Doctor,
    onBack: () -> Unit,
    onBook: () -> Unit,
    onOpenMap: () -> Unit,
) {
    val scroll = rememberScrollState()
    val todayName = arabicDayName(LocalDate.now().dayOfWeek)
    val availableToday = doctor.availableDays.isEmpty() ||
        doctor.availableDays.any { it.contains(todayName, ignoreCase = true) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface),
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = { Text("ملف الطبيب", fontFamily = Manrope) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "رجوع", tint = Primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceBright.copy(alpha = 0.95f)
                )
            )
        },
        bottomBar = {
            val gradient = Brush.linearGradient(
                colors = listOf(Primary, Color(0xFF1557B0)),
                start = Offset(0f, 0f),
                end = Offset(Float.POSITIVE_INFINITY, 0f)
            )
            Button(
                onClick = onBook,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .background(gradient, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "احجز موعد الآن",
                        fontFamily = Manrope,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(bottom = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                DoctorPhotoDisplay(
                    photoUrl = doctor.photoUrl,
                    name = doctor.fullName,
                    modifier = Modifier.size(100.dp),
                    shape = CircleShape,
                    initialsSize = 32.sp,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    doctor.fullName,
                    fontFamily = Manrope,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = OnSurface,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = doctor.specialty,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Primary.copy(alpha = 0.12f))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    color = Primary,
                    fontFamily = PublicSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                )
                Spacer(modifier = Modifier.height(12.dp))
                StarRatingRow(rating = doctor.rating)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (doctor.reviewCount > 0) {
                        "(${doctor.reviewCount} تقييم)"
                    } else {
                        "(لا توجد مراجعات بعد)"
                    },
                    fontFamily = PublicSans,
                    fontSize = 13.sp,
                    color = OnSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                InfoStatCard(
                    title = "سنوات الخبرة",
                    value = "${doctor.experienceYears} سنة",
                )
                InfoStatCard(
                    title = "رسوم الكشف",
                    value = "${doctor.consultationFeeSar} ر.س",
                )
                InfoStatCard(
                    title = "مدة الموعد",
                    value = "${doctor.slotDurationMinutes} دقيقة",
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            Column(Modifier.padding(horizontal = 20.dp)) {
                Text(
                    "نبذة عن الطبيب",
                    fontFamily = Manrope,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = OnSurface,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = doctor.about?.takeIf { it.isNotBlank() } ?: "لا توجد نبذة متاحة.",
                    fontFamily = PublicSans,
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    color = OnSurfaceVariant,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            Column(Modifier.padding(horizontal = 20.dp)) {
                Text(
                    "العيادة",
                    fontFamily = Manrope,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = OnSurface,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    doctor.clinicName ?: "—",
                    fontFamily = PublicSans,
                    fontSize = 15.sp,
                    color = OnSurface,
                )
                val addr = doctor.clinicAddress
                if (!addr.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            addr,
                            fontFamily = PublicSans,
                            fontSize = 14.sp,
                            color = OnSurfaceVariant,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Start,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onOpenMap,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.Map, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("عرض على الخريطة", fontFamily = Manrope)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Column(Modifier.padding(horizontal = 20.dp)) {
                Text(
                    "مواعيد العمل",
                    fontFamily = Manrope,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = OnSurface,
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (doctor.availableDays.isEmpty()) {
                    Text(
                        "لم يُحدد جدول الأيام في الملف.",
                        fontFamily = PublicSans,
                        fontSize = 13.sp,
                        color = OnSurfaceVariant,
                    )
                } else {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        doctor.availableDays.forEach { day ->
                            Text(
                                day,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SurfaceContainerHigh)
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                fontSize = 12.sp,
                                fontFamily = PublicSans,
                                color = OnSurface,
                            )
                        }
                    }
                }
                val timeLine = listOfNotNull(doctor.startTime, doctor.endTime).joinToString(" — ")
                if (timeLine.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = timeLine,
                        fontFamily = PublicSans,
                        fontSize = 14.sp,
                        color = OnSurfaceVariant,
                    )
                }
                if (availableToday && doctor.availableDays.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "متاح اليوم",
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Secondary.copy(alpha = 0.15f))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        color = Secondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        fontFamily = Manrope,
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun InfoStatCard(title: String, value: String) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceContainerLow)
            .padding(12.dp),
    ) {
        Text(
            title,
            fontSize = 11.sp,
            color = OnSurfaceVariant,
            fontFamily = PublicSans,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            value,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = Manrope,
            color = OnSurface,
        )
    }
}

@Composable
private fun StarRatingRow(rating: Double?) {
    val r = (rating ?: 0.0).coerceIn(0.0, 5.0)
    val full = kotlin.math.floor(r).toInt().coerceIn(0, 5)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        repeat(5) { i ->
            Icon(
                Icons.Filled.Star,
                contentDescription = null,
                tint = if (i < full) Color(0xFFFFB300) else Color(0xFFE0E0E0),
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = String.format(Locale.US, "%.1f", r),
            fontFamily = Manrope,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Primary,
        )
    }
}

private fun arabicDayName(day: DayOfWeek): String = when (day) {
    DayOfWeek.SUNDAY -> "الأحد"
    DayOfWeek.MONDAY -> "الإثنين"
    DayOfWeek.TUESDAY -> "الثلاثاء"
    DayOfWeek.WEDNESDAY -> "الأربعاء"
    DayOfWeek.THURSDAY -> "الخميس"
    DayOfWeek.FRIDAY -> "الجمعة"
    DayOfWeek.SATURDAY -> "السبت"
}
