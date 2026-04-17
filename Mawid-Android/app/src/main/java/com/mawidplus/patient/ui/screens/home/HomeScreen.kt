@file:OptIn(ExperimentalMaterial3Api::class)

package com.mawidplus.patient.ui.screens.home

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface as M3Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.mawidplus.patient.data.SeedDoctorIds
import com.mawidplus.patient.ui.components.DoctorPhotoDisplay
import com.mawidplus.patient.ui.components.rememberCrossfadeImageRequest
import com.mawidplus.patient.ui.theme.Manrope
import com.mawidplus.patient.ui.theme.OnPrimary
import com.mawidplus.patient.ui.theme.OnPrimaryContainer
import com.mawidplus.patient.ui.theme.OnSecondaryContainer
import com.mawidplus.patient.ui.theme.OnSurface
import com.mawidplus.patient.ui.theme.OnSurfaceVariant
import com.mawidplus.patient.ui.theme.Outline
import com.mawidplus.patient.ui.theme.Primary
import com.mawidplus.patient.ui.theme.PrimaryContainer
import com.mawidplus.patient.ui.theme.PrimaryFixed
import com.mawidplus.patient.ui.theme.PublicSans
import com.mawidplus.patient.ui.theme.SecondaryContainer
import com.mawidplus.patient.ui.theme.Surface
import com.mawidplus.patient.ui.theme.SurfaceBright
import com.mawidplus.patient.ui.theme.SurfaceContainerLow
import com.mawidplus.patient.ui.theme.SurfaceContainerLowest
import com.mawidplus.patient.ui.theme.Tertiary

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
    onNavigateToQueue: (doctorId: String) -> Unit = {},
    /** ينتقل إلى شاشة البحث مع النص الحالي في شريط البحث. */
    onNavigateToSearch: (query: String) -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNewBooking: () -> Unit = {},
    onNearMe: () -> Unit = {},
    onVideoConsult: () -> Unit = {},
    /** زر «مساعد موعد+» — اربط التنقل أو المنطق من `MainTabContainer` / `AppNavGraph` عند تجهيز الميزة. */
    onSmartAssistant: () -> Unit = {},
    onHealthTipClick: () -> Unit = {}
) {
    val homeState by viewModel.uiState.collectAsStateWithLifecycle()
    val homeSearchQuery by viewModel.homeSearchQuery.collectAsStateWithLifecycle()
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
                            val id = when (val s = homeState) {
                                is HomeUiState.Ready -> s.queueShortcutDoctorId
                                else -> SeedDoctorIds.FAMILY_AHMED
                            }
                            onNavigateToQueue(id)
                        }
                    ) {
                        Icon(
                            Icons.Filled.ArrowBack,
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
                        Text(
                            text = "مرحباً، ${s.patientName}",
                            fontFamily = Manrope,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = OnSurface,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = if (s.upcoming == null) {
                                "نسعد بخدمتك — احجز موعدك من البحث أو الإجراءات السريعة."
                            } else {
                                "إليك أقرب موعد مسجّل لك."
                            },
                            fontFamily = PublicSans,
                            fontSize = 13.sp,
                            color = OnSurfaceVariant,
                            textAlign = TextAlign.Start,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                        )
                    }
                }
            }
        }

        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = homeSearchQuery,
                    onValueChange = viewModel::setHomeSearchQuery,
                    readOnly = false,
                    placeholder = {
                        Text(
                            "ابحث عن عيادة أو طبيب...",
                            color = Outline.copy(alpha = 0.8f),
                            fontFamily = PublicSans,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = "بحث",
                            tint = OnSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { onNavigateToSearch(homeSearchQuery) }) {
                            Icon(Icons.Filled.Search, contentDescription = "تنفيذ البحث", tint = Primary)
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = { onNavigateToSearch(homeSearchQuery) }
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = SurfaceContainerLowest,
                        unfocusedContainerColor = SurfaceContainerLowest,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = OnSurface,
                        unfocusedTextColor = OnSurface,
                        disabledTextColor = OnSurface,
                        cursorColor = Primary
                    ),
                    shape = RoundedCornerShape(16.dp),
                    textStyle = TextStyle(
                        textAlign = TextAlign.Start,
                        fontFamily = PublicSans,
                        color = Color(0xFF0A0A0A),
                        fontSize = 16.sp
                    )
                )
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
                    onClick = {
                        val id = when (val s = homeState) {
                            is HomeUiState.Ready -> s.queueShortcutDoctorId
                            else -> SeedDoctorIds.FAMILY_AHMED
                        }
                        onNavigateToQueue(id)
                    }
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
                            onViewDetails = { onNavigateToQueue(s.upcoming.doctorId) },
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
                            Icons.Filled.ArrowBack,
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
                            Icons.Filled.ArrowBack,
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
                    NearMeCard(
                        modifier = Modifier.weight(1f),
                        onClick = onNearMe
                    )
                    VideoConsultCard(
                        modifier = Modifier.weight(1f),
                        onClick = onVideoConsult
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

@Composable
private fun UpcomingAppointmentCard(
    data: UpcomingAppointmentCardData,
    onViewDetails: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .heightIn(min = 220.dp)
            .clip(EditorialShape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(Primary, PrimaryContainer),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "العيادة",
                    color = OnPrimaryContainer.copy(alpha = 0.9f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Start
                )
                M3Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.White.copy(alpha = 0.22f)
                ) {
                    Text(
                        data.statusBadgeAr,
                        fontFamily = PublicSans,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
            Text(
                data.clinicLabel,
                fontFamily = Manrope,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
                color = Color.White,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DoctorPhotoDisplay(
                    photoUrl = data.doctorPhotoUrl,
                    name = data.doctorName,
                    modifier = Modifier.size(52.dp),
                    shape = CircleShape,
                    initialsSize = 18.sp,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        data.doctorName,
                        fontFamily = Manrope,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White,
                        textAlign = TextAlign.Start
                    )
                    if (data.specialty.isNotBlank()) {
                        Text(
                            data.specialty,
                            fontFamily = PublicSans,
                            fontSize = 12.sp,
                            color = PrimaryFixed.copy(alpha = 0.95f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Filled.CalendarToday,
                    contentDescription = null,
                    tint = PrimaryFixed.copy(alpha = 0.95f),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    data.relativeDateLabel,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontFamily = Manrope,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Start
                )
            }
            Text(
                data.displayDateAr,
                color = PrimaryFixed.copy(alpha = 0.9f),
                fontSize = 12.sp,
                fontFamily = PublicSans,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "رقم #${data.queueNumber} في الطابور",
                color = Color.White.copy(alpha = 0.95f),
                fontSize = 14.sp,
                fontFamily = Manrope,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = onViewDetails,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Primary
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    "عرض التفاصيل",
                    fontFamily = Manrope,
                    fontWeight = FontWeight.Bold
                )
            }
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
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainerLowest)
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.NearMe,
                    contentDescription = null,
                    tint = Primary
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
fun VideoConsultCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .height(140.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainerLowest)
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Tertiary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Videocam,
                    contentDescription = null,
                    tint = Tertiary
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
