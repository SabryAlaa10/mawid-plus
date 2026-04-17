package com.mawidplus.patient.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.mawidplus.patient.ui.theme.Manrope
import com.mawidplus.patient.ui.theme.Primary
import com.mawidplus.patient.ui.theme.PrimaryContainer

fun initialsFromDisplayName(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (parts.size >= 2) {
        val a = parts[0].firstOrNull() ?: return "؟"
        val b = parts[1].firstOrNull() ?: return a.toString()
        return "$a$b"
    }
    val single = parts.firstOrNull().orEmpty()
    return when {
        single.length >= 2 -> single.take(2)
        single.isNotEmpty() -> single.take(1)
        else -> "؟"
    }
}

@Composable
fun DoctorPhotoDisplay(
    photoUrl: String?,
    name: String,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    initialsSize: TextUnit = 20.sp,
) {
    val context = LocalContext.current
    val initials = remember(name) { initialsFromDisplayName(name) }
    val imageRequest = remember(photoUrl, context) {
        val u = photoUrl?.trim()
        if (u.isNullOrEmpty()) null
        else ImageRequest.Builder(context).data(u).crossfade(300).build()
    }
    val gradient = remember {
        Brush.linearGradient(
            colors = listOf(Primary, PrimaryContainer),
            start = Offset(0f, 0f),
            end = Offset(200f, 200f),
        )
    }
    Box(
        modifier = modifier
            .clip(shape)
            .background(gradient),
        contentAlignment = Alignment.Center,
    ) {
        if (photoUrl.isNullOrBlank()) {
            Text(
                text = initials,
                fontFamily = Manrope,
                fontWeight = FontWeight.Black,
                fontSize = initialsSize,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
        } else {
            SubcomposeAsyncImage(
                model = imageRequest ?: photoUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                loading = {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            color = Color.White,
                            strokeWidth = 2.dp,
                        )
                    }
                },
                error = {
                    Text(
                        text = initials,
                        fontFamily = Manrope,
                        fontWeight = FontWeight.Black,
                        fontSize = initialsSize,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                    )
                },
            )
        }
    }
}
