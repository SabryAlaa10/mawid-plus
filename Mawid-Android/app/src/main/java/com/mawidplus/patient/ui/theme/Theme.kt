package com.mawidplus.patient.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    primaryContainer = PrimaryContainer,
    onPrimary = OnPrimary,
    secondary = Secondary,
    secondaryContainer = SecondaryContainer,
    onSecondary = OnSecondary,
    tertiary = Tertiary,
    tertiaryContainer = TertiaryContainer,
    error = Error,
    errorContainer = ErrorContainer,
    onError = OnError,
    background = Surface,
    onBackground = OnSurface,
    surface = Surface,
    surfaceVariant = SurfaceVariant,
    onSurface = OnSurface,
    onSurfaceVariant = OnSurfaceVariant,
    outline = Outline,
    outlineVariant = OutlineVariant,
    inverseSurface = InverseSurface,
    inverseOnSurface = InverseOnSurface,
    inversePrimary = InversePrimary
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryFixedDim,
    primaryContainer = Primary,
    onPrimary = OnPrimaryFixed,
    secondary = SecondaryFixedDim,
    secondaryContainer = OnSecondaryFixed,
    onSecondary = OnSecondaryFixedVariant,
    error = ErrorContainer,
    onError = OnErrorContainer,
    background = InverseSurface,
    onBackground = InverseOnSurface,
    surface = InverseSurface,
    onSurface = InverseOnSurface
)

@Composable
fun MawidTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
