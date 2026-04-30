package com.mawidplus.patient

import android.app.Application
import com.mawidplus.patient.core.notifications.NotificationHelper

class PatientApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        NotificationHelper.createNotificationChannel(this)
    }

    companion object {
        @Volatile
        private var instance: Application? = null

        fun appContextOrNull(): Application? = instance
    }
}
