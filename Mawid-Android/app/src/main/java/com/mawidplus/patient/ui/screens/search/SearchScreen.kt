@file:OptIn(ExperimentalMaterial3Api::class)

package com.mawidplus.patient.ui.screens.search

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mawidplus.patient.ui.components.DoctorPhotoDisplay
import com.mawidplus.patient.ui.theme.Manrope
import com.mawidplus.patient.ui.theme.OnPrimary
import com.mawidplus.patient.ui.theme.OnSurface
import com.mawidplus.patient.ui.theme.OnSurfaceVariant
import com.mawidplus.patient.ui.theme.Outline
import com.mawidplus.patient.ui.theme.Primary
import com.mawidplus.patient.ui.theme.PublicSans
import com.mawidplus.patient.ui.theme.Secondary
import com.mawidplus.patient.ui.theme.Surface
import com.mawidplus.patient.ui.theme.SurfaceBright
import com.mawidplus.patient.ui.theme.SurfaceContainerHigh
import com.mawidplus.patient.ui.theme.SurfaceContainerLow
import com.mawidplus.patient.ui.theme.SurfaceContainerLowest

private val doctorCardShape = RoundedCornerShape(
    topStart = 48.dp,
    topEnd = 12.dp,
    bottomEnd = 48.dp,
    bottomStart = 12.dp
)

@Composable
fun SearchScreen(
    viewModel: SearchViewModel = viewModel { SearchViewModel() },
    onBack: () -> Unit = {},
    onNotifications: () -> Unit = {},
    onSearchFieldClick: () -> Unit = {},
    onSearchFilters: () -> Unit = {},
    onViewMap: () -> Unit = {},
    onDoctorClick: (doctorId: String) -> Unit = {},
    onBookAppointment: (doctorId: String) -> Unit = {}
) {
    var filterIndex by remember { mutableIntStateOf(0) }
    val filters = listOf("الكل", "متاح اليوم", "الأعلى تقييماً", "الأقرب لي")
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val doctors = remember(uiState, filterIndex, searchQuery) {
        when (val s = uiState) {
            is SearchUiState.Ready -> {
                val textFiltered = s.doctors.filteredBySearchQuery(searchQuery)
                when (filterIndex) {
                    1 -> textFiltered.filter { it.availableToday }
                    2 -> textFiltered.sortedByDescending {
                        it.rating.replace(",", ".").toDoubleOrNull() ?: 0.0
                    }
                    else -> textFiltered
                }
            }
            else -> emptyList()
        }
    }
    val resultCount = doctors.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface)
    ) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        "Mawid+",
                        fontFamily = Manrope,
                        fontWeight = FontWeight.Black,
                        color = Primary,
                        fontSize = 20.sp
                    )
                    Text(
                        "طب الأسرة",
                        fontSize = 11.sp,
                        fontFamily = PublicSans,
                        fontWeight = FontWeight.SemiBold,
                        color = OnSurfaceVariant
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "رجوع", tint = Primary)
                }
            },
            actions = {
                IconButton(onClick = onNotifications) {
                    Icon(Icons.Filled.Notifications, contentDescription = "إشعارات", tint = OnSurfaceVariant)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceBright.copy(alpha = 0.92f))
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextField(
                value = searchQuery,
                onValueChange = viewModel::setSearchQuery,
                readOnly = false,
                placeholder = {
                    Text(
                        "ابحث عن طبيب أو تخصص...",
                        color = Outline.copy(alpha = 0.75f),
                        fontFamily = PublicSans,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                leadingIcon = {
                    IconButton(onClick = onSearchFieldClick) {
                        Icon(Icons.Filled.Search, contentDescription = "بحث", tint = Primary)
                    }
                },
                trailingIcon = {
                    IconButton(onClick = onSearchFilters) {
                        Icon(Icons.Filled.Settings, contentDescription = "تصفية", tint = Outline.copy(alpha = 0.6f))
                    }
                },
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filters.forEachIndexed { index, label ->
                    val selected = filterIndex == index
                    FilterChip(
                        selected = selected,
                        onClick = { filterIndex = index },
                        label = {
                            Text(
                                label,
                                fontFamily = PublicSans,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        leadingIcon = if (index == 0) {
                            {
                                Icon(
                                    Icons.Filled.List,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = if (selected) Primary else SurfaceContainerLow,
                            labelColor = if (selected) OnPrimary else OnSurfaceVariant,
                            selectedContainerColor = Primary,
                            selectedLabelColor = OnPrimary
                        )
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "الأطباء المتاحون ($resultCount)",
                fontFamily = PublicSans,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = OnSurfaceVariant,
                textAlign = TextAlign.Start
            )
            TextButton(onClick = onViewMap) {
                Text(
                    "عرض الخريطة",
                    fontFamily = PublicSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Primary
                )
            }
        }

        when (uiState) {
            is SearchUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .padding(horizontal = 24.dp),
                        color = Primary
                    )
                }
            }
            is SearchUiState.Error -> {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        (uiState as SearchUiState.Error).message,
                        fontFamily = PublicSans,
                        color = OnSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(onClick = { viewModel.refresh() }) {
                        Text("إعادة المحاولة", color = Primary, fontFamily = PublicSans)
                    }
                }
            }
            is SearchUiState.Ready -> {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(doctors, key = { it.id }) { doctor ->
                        DoctorResultCard(
                            doctor = doctor,
                            onCardClick = { onDoctorClick(doctor.id) },
                            onBookClick = { onBookAppointment(doctor.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DoctorResultCard(
    doctor: DoctorSearchItem,
    onCardClick: () -> Unit,
    onBookClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(doctorCardShape)
            .background(SurfaceContainerLowest)
            .clickable(onClick = onCardClick)
            .padding(16.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(modifier = Modifier.size(width = 96.dp, height = 128.dp)) {
                val imgShape = RoundedCornerShape(
                    topStart = 40.dp,
                    topEnd = 12.dp,
                    bottomEnd = 40.dp,
                    bottomStart = 12.dp
                )
                DoctorPhotoDisplay(
                    photoUrl = doctor.imageUrl,
                    name = doctor.name,
                    modifier = Modifier.fillMaxSize(),
                    shape = imgShape,
                    initialsSize = 28.sp,
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(28.dp)
                        .background(SurfaceContainerLowest, CircleShape)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = Secondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        doctor.specialty.uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = Outline,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(SurfaceContainerHigh)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Text(
                    doctor.name,
                    fontFamily = Manrope,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = OnSurface,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.padding(top = 4.dp)
                )
                doctor.clinicName?.let { clinic ->
                    Text(
                        clinic,
                        fontFamily = PublicSans,
                        fontSize = 13.sp,
                        color = OnSurfaceVariant,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(18.dp))
                    Text(
                        doctor.rating,
                        fontFamily = PublicSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                    Text(
                        doctor.reviews,
                        fontFamily = PublicSans,
                        fontSize = 13.sp,
                        color = OnSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
                if (doctor.availableToday) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Secondary.copy(alpha = 0.1f))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            Icons.Filled.CalendarToday,
                            contentDescription = null,
                            tint = Secondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            "متاح اليوم",
                            fontFamily = PublicSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Secondary,
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "أقرب موعد",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    color = OnSurfaceVariant,
                    textAlign = TextAlign.Start
                )
                Text(
                    doctor.nextSlot,
                    fontFamily = PublicSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = OnSurfaceVariant,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Button(
                onClick = onBookClick,
                colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = OnPrimary),
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    "حجز موعد",
                    fontFamily = Manrope,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}
