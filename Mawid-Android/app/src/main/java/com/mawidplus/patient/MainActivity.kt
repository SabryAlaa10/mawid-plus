package com.mawidplus.patient

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.mawidplus.patient.core.preferences.ThemePreference
import com.mawidplus.patient.core.preferences.ThemePrefs
import com.mawidplus.patient.ui.navigation.AppNavGraph
import com.mawidplus.patient.ui.theme.MawidTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("ar-EG"))
        setContent {
            MawidRoot()
        }
    }
}

@Composable
private fun MawidRoot() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        MawidPostNotificationsPermission()
    }
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    val themePref = ThemePrefs.get(context.applicationContext)
    val darkTheme = when (themePref) {
        ThemePreference.DARK -> true
        ThemePreference.LIGHT -> false
        ThemePreference.SYSTEM -> systemDark
    }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MawidTheme(darkTheme = darkTheme) {
            AppNavGraph()
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun MawidPostNotificationsPermission() {
    val permissionState = rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    LaunchedEffect(Unit) {
        if (!permissionState.status.isGranted) {
            permissionState.launchPermissionRequest()
        }
    }
}
