package com.mawidplus.patient.ui.screens.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mawidplus.patient.ui.theme.Manrope
import com.mawidplus.patient.ui.theme.OnPrimary
import com.mawidplus.patient.ui.theme.OnSurfaceVariant
import com.mawidplus.patient.ui.theme.Primary
import com.mawidplus.patient.ui.theme.PrimaryContainer
import com.mawidplus.patient.ui.theme.PublicSans
import com.mawidplus.patient.ui.theme.SecondaryFixed
import com.mawidplus.patient.ui.theme.SurfaceContainerHighest
import kotlinx.coroutines.delay

private const val SPLASH_BG_IMAGE =
    "https://lh3.googleusercontent.com/aida-public/AB6AXuDeDZgm0JqLikMvJTMD138Y2N4EYKKx6aFEOkVEC9oTePZoed_IRn0-ABeOqwO6LEsi37WTrjrcS6B8e-ZIu4_SajBeoE1PJ6Dpz6Dj_8mjSTUTiwXHOfWGPEtTX5aM6T8LiNUMF-zVs-AO0ZcD2AGlt7vMDZXdAIZrakDfHoGyvmsrOowKgY5sdCTDiMh8nHLhcK1kklthaTtxs7Zbn8u3kxCT4guP6zHZrQ1UkcnmF6I4EpC1f4wZu8PTc-JCeS1NanCAMc1DRfI"

@Composable
fun SplashScreen(
    onFinished: suspend () -> Unit,
    displayMs: Long = 2600L
) {
    LaunchedEffect(Unit) {
        delay(displayMs)
        onFinished()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AsyncImage(
            model = SPLASH_BG_IMAGE,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F9FA)),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.92f), Color.White.copy(alpha = 0.55f)),
                        radius = 1200f
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Primary.copy(alpha = 0.06f),
                            Color.Transparent,
                            SecondaryFixed.copy(alpha = 0.08f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(topStart = 48.dp, topEnd = 16.dp, bottomEnd = 48.dp, bottomStart = 16.dp))
                    .background(PrimaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.MedicalServices,
                    contentDescription = null,
                    tint = OnPrimary,
                    modifier = Modifier.size(52.dp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Mawid+",
                fontFamily = Manrope,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 44.sp,
                color = Primary,
                letterSpacing = (-1).sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "احجز بدون انتظار",
                fontFamily = PublicSans,
                fontWeight = FontWeight.Medium,
                fontSize = 20.sp,
                color = OnSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.weight(1f))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth(0.55f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = Primary,
                    trackColor = SurfaceContainerHighest,
                    progress = 0.35f
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "TRUSTED CLINICAL NETWORK",
                        fontFamily = PublicSans,
                        fontWeight = FontWeight.Normal,
                        fontSize = 10.sp,
                        letterSpacing = 2.sp,
                        color = OnSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "© 2024 Mawid Plus Health Systems",
                        fontFamily = PublicSans,
                        fontSize = 10.sp,
                        color = OnSurfaceVariant.copy(alpha = 0.65f)
                    )
                }
            }
        }
    }
}
