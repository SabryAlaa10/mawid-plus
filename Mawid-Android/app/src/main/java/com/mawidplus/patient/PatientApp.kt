package com.mawidplus.patient

import android.app.Application
import android.util.Log
import com.mawidplus.patient.core.preferences.ThemePrefs
import com.mawidplus.patient.core.network.SupabaseProvider
import com.mawidplus.patient.core.notifications.NotificationHelper
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PatientApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        instance = this
        ThemePrefs.applyToAppCompat(ThemePrefs.get(this))
        NotificationHelper.createNotificationChannel(this)
        // تحميل جلسة Supabase مبكراً لتقليل سباقات قبل الشاشة الأولى
        appScope.launch(Dispatchers.IO) {
            try {
                SupabaseProvider.client.auth.awaitInitialization()
            } catch (e: Exception) {
                Log.w("PatientApp", "Early Supabase auth init", e)
            }
        }
    }

    companion object {
        @Volatile
        private var instance: Application? = null

        fun appContextOrNull(): Application? = instance
    }
}
