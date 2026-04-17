@file:OptIn(ExperimentalMaterial3Api::class)

package com.mawidplus.patient.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mawidplus.patient.ui.components.DoctorPhotoDisplay
import com.mawidplus.patient.ui.theme.Manrope
import com.mawidplus.patient.ui.theme.OnSurfaceVariant
import com.mawidplus.patient.ui.theme.Primary
import com.mawidplus.patient.ui.theme.Surface
import com.mawidplus.patient.ui.theme.SurfaceBright

@Composable
fun MapViewScreen(onBack: () -> Unit) {
    SearchSubScreenScaffold(
        title = "عرض الخريطة",
        onBack = onBack,
        body = {
            Text(
                "قريباً: خريطة العيادات والأطباء بالقرب منك.",
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

@Composable
fun SearchFiltersScreen(onBack: () -> Unit) {
    SearchSubScreenScaffold(
        title = "تصفية البحث",
        onBack = onBack,
        body = {
            Text(
                "اختر التخصص، التأمين، والمواعيد المتاحة (قيد التطوير).",
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

@Composable
internal fun SearchSubScreenScaffold(
    title: String,
    onBack: () -> Unit,
    body: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Surface)
        ) {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = { Text(title, fontFamily = Manrope) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "رجوع", tint = Primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceBright.copy(alpha = 0.92f)
                )
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 8.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                body()
            }
        }
    }
}
