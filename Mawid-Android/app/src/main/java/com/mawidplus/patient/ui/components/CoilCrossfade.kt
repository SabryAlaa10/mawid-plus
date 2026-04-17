package com.mawidplus.patient.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import coil.request.ImageRequest

private const val CROSSFADE_MS = 300

/**
 * Coil [ImageRequest] with crossfade for smoother image loads (lists + hero images).
 */
@Composable
fun rememberCrossfadeImageRequest(data: Any?): ImageRequest? {
    val context = LocalContext.current
    return remember(data, context) {
        when (data) {
            null -> null
            is String -> {
                val u = data.trim()
                if (u.isEmpty()) null
                else ImageRequest.Builder(context).data(u).crossfade(CROSSFADE_MS).build()
            }
            else -> ImageRequest.Builder(context).data(data).crossfade(CROSSFADE_MS).build()
        }
    }
}
