package com.mawidplus.patient.core.preferences

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

enum class ThemePreference {
    /** يتبع إعداد الوضع الداكن في النظام */
    SYSTEM,
    LIGHT,
    DARK;

    companion object {
        fun fromStored(value: String?): ThemePreference =
            entries.find { it.name == value } ?: SYSTEM
    }
}

object ThemePrefs {
    private const val NAME = "mawid_theme"
    private const val KEY = "theme_preference"

    fun get(context: Context): ThemePreference =
        ThemePreference.fromStored(
            context.applicationContext
                .getSharedPreferences(NAME, Context.MODE_PRIVATE)
                .getString(KEY, null),
        )

    fun set(context: Context, preference: ThemePreference) {
        context.applicationContext
            .getSharedPreferences(NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, preference.name)
            .apply()
    }

    fun applyToAppCompat(preference: ThemePreference) {
        AppCompatDelegate.setDefaultNightMode(
            when (preference) {
                ThemePreference.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                ThemePreference.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                ThemePreference.DARK -> AppCompatDelegate.MODE_NIGHT_YES
            },
        )
    }
}
