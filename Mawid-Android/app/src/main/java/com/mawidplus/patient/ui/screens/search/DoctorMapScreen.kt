@file:OptIn(ExperimentalMaterial3Api::class)

package com.mawidplus.patient.ui.screens.search

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.mawidplus.patient.core.model.Doctor
import com.mawidplus.patient.data.repository.DoctorRepository
import com.mawidplus.patient.data.repository.Result
import com.mawidplus.patient.ui.components.DoctorPhotoDisplay
import com.mawidplus.patient.ui.theme.Manrope
import com.mawidplus.patient.ui.theme.OnSurface
import com.mawidplus.patient.ui.theme.OnSurfaceVariant
import com.mawidplus.patient.ui.theme.Primary
import com.mawidplus.patient.ui.theme.PublicSans
import com.mawidplus.patient.ui.theme.Surface
import com.mawidplus.patient.ui.theme.SurfaceBright
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

@Composable
fun DoctorMapScreen(
    doctorId: String,
    onBack: () -> Unit,
    onBook: () -> Unit,
    repository: DoctorRepository = remember { DoctorRepository() },
) {
    var doctor by remember { mutableStateOf<Doctor?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(doctorId) {
        loading = true
        loadError = null
        when (val r = withContext(Dispatchers.IO) { repository.getDoctorById(doctorId) }) {
            is Result.Success -> {
                doctor = r.data
                loading = false
            }
            is Result.Error -> {
                loadError = r.message
                loading = false
            }
            else -> loading = false
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        when {
            loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Surface),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Primary)
                }
            }
            loadError != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Surface)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TopAppBar(
                        title = { Text("الخريطة", fontFamily = Manrope) },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = Primary)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceBright)
                    )
                    Text(
                        loadError ?: "",
                        fontFamily = Manrope,
                        color = OnSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            doctor != null -> {
                val d = doctor!!
                DoctorMapBody(
                    doctor = d,
                    onBack = onBack,
                    onBook = onBook,
                )
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
private fun DoctorMapBody(
    doctor: Doctor,
    onBack: () -> Unit,
    onBook: () -> Unit,
) {
    val context = LocalContext.current
    val lat = doctor.latitude
    val lng = doctor.longitude
    val hasCoords = lat != null && lng != null

    var userLat by remember { mutableStateOf<Double?>(null) }
    var userLng by remember { mutableStateOf<Double?>(null) }
    var hasFineLocation by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasFineLocation = granted
    }

    LaunchedEffect(hasFineLocation) {
        if (!hasFineLocation) return@LaunchedEffect
        val loc = fusedLastLocation(context)
        if (loc != null) {
            userLat = loc.latitude
            userLng = loc.longitude
        }
    }

    LaunchedEffect(Unit) {
        if (!hasFineLocation) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    val distanceText = remember(userLat, userLng, lat, lng) {
        if (userLat == null || userLng == null || lat == null || lng == null) return@remember null
        val r = FloatArray(1)
        Location.distanceBetween(userLat!!, userLng!!, lat!!, lng!!, r)
        val m = r[0]
        if (m < 1000) "${m.toInt()} م"
        else String.format(java.util.Locale.US, "%.1f كم", m / 1000f)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (hasCoords) {
            val pos = LatLng(lat!!, lng!!)
            val cameraPositionState = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(pos, 14f)
            }
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = hasFineLocation),
                uiSettings = MapUiSettings(zoomControlsEnabled = true, myLocationButtonEnabled = hasFineLocation),
            ) {
                Marker(
                    state = MarkerState(position = pos),
                    title = doctor.fullName,
                    snippet = doctor.specialty,
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Surface),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "لم يتم تحديد موقع العيادة بعد",
                    fontFamily = Manrope,
                    fontSize = 16.sp,
                    color = OnSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(24.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            TopAppBar(
                title = { Text(doctor.fullName, fontFamily = Manrope, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = Primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceBright.copy(alpha = 0.95f)
                )
            )
        }

        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(12.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DoctorPhotoDisplay(
                        photoUrl = doctor.photoUrl,
                        name = doctor.fullName,
                        modifier = Modifier.size(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        initialsSize = 18.sp,
                    )
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text(
                            doctor.fullName,
                            fontFamily = Manrope,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = OnSurface,
                        )
                        Text(
                            doctor.specialty,
                            fontSize = 13.sp,
                            color = OnSurfaceVariant,
                        )
                    }
                }
                doctor.clinicName?.let { name ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(name, fontFamily = PublicSans, fontSize = 14.sp, color = OnSurface)
                }
                val addr = doctor.clinicAddress
                if (!addr.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            addr,
                            modifier = Modifier.padding(start = 6.dp),
                            fontSize = 13.sp,
                            color = OnSurfaceVariant,
                            fontFamily = PublicSans,
                        )
                    }
                }
                distanceText?.let { dist ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "المسافة التقريبية: $dist",
                        fontSize = 13.sp,
                        color = Primary,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = PublicSans,
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onBook,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("احجز موعد", fontFamily = Manrope)
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
private suspend fun fusedLastLocation(context: android.content.Context): Location? =
    suspendCancellableCoroutine { cont ->
        val client = LocationServices.getFusedLocationProviderClient(context)
        client.lastLocation
            .addOnSuccessListener { cont.resume(it) }
            .addOnFailureListener { cont.resume(null) }
    }
