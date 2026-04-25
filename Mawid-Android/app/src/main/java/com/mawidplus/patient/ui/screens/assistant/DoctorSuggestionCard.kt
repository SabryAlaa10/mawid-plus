package com.mawidplus.patient.ui.screens.assistant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mawidplus.patient.data.dto.RecommendedDoctorDto
import com.mawidplus.patient.ui.components.rememberCrossfadeImageRequest
import com.mawidplus.patient.ui.theme.OnPrimary
import com.mawidplus.patient.ui.theme.OnSurface
import com.mawidplus.patient.ui.theme.OnSurfaceVariant
import com.mawidplus.patient.ui.theme.PublicSans
import com.mawidplus.patient.ui.theme.Manrope
import com.mawidplus.patient.ui.theme.Outline
import com.mawidplus.patient.ui.theme.Primary
import com.mawidplus.patient.ui.theme.SurfaceContainerLowest
import com.mawidplus.patient.ui.theme.SurfaceContainerLow

@Composable
fun DoctorSuggestionCard(
    doctor: RecommendedDoctorDto,
    onBook: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val name = doctor.fullName.orEmpty()
    val spec = doctor.specialty.orEmpty()
    val rating = doctor.rating ?: 0.0
    val fee = doctor.consultationFeeSar
    val url = doctor.imageUrl
    val address = doctor.clinicAddress

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            // صورة الدكتور
            if (!url.isNullOrBlank()) {
                AsyncImage(
                    model = rememberCrossfadeImageRequest(url),
                    contentDescription = name,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceContainerLow),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Spacer(
                    modifier = Modifier
                        .size(64.dp)
                        .background(SurfaceContainerLow, RoundedCornerShape(16.dp)),
                )
            }
            Spacer(modifier = Modifier.width(14.dp))

            // معلومات الدكتور
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    name,
                    fontFamily = Manrope,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = OnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (spec.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        spec,
                        fontFamily = PublicSans,
                        fontSize = 13.sp,
                        color = OnSurfaceVariant,
                        maxLines = 1,
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // التقييم
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = Outline,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            "%.1f".format(rating),
                            fontFamily = PublicSans,
                            fontSize = 13.sp,
                            color = OnSurfaceVariant,
                        )
                    }
                    // السعر
                    if (fee != null) {
                        Text(
                            "$fee ر.س",
                            fontFamily = PublicSans,
                            fontSize = 14.sp,
                            color = OnSurface,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                // عنوان العيادة
                if (!address.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        address,
                        fontFamily = PublicSans,
                        fontSize = 12.sp,
                        color = OnSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        // زر الحجز — عرض كامل
        Button(
            onClick = onBook,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 14.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Primary,
                contentColor = OnPrimary,
            ),
        ) {
            Text(
                "احجز موعد",
                fontFamily = PublicSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                modifier = Modifier.padding(vertical = 4.dp),
            )
        }
    }
}
