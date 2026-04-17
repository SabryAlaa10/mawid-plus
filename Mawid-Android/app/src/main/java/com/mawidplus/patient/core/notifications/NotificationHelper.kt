package com.mawidplus.patient.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

object NotificationHelper {

    const val CHANNEL_ID = "mawid_appointments"
    private const val NOTIFICATION_ID_BASE = 7100

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "مواعيد Mawid+",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "تذكيرات بالمواعيد الطبية"
        }
        val nm = context.getSystemService(NotificationManager::class.java)
        nm?.createNotificationChannel(channel)
    }

    fun showNotification(context: Context, title: String, message: String) {
        createNotificationChannel(context)
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        nm.notify((NOTIFICATION_ID_BASE + (title + message).hashCode() % 1000), notification)
    }

    /**
     * Schedules day-before (09:00) and same-day (08:00) reminders in Asia/Riyadh.
     */
    fun scheduleAppointmentReminder(
        context: Context,
        appointmentId: String,
        appointmentDateIso: String,
        doctorName: String,
        queueNumber: Int,
    ) {
        createNotificationChannel(context)
        val zone = ZoneId.of("Asia/Riyadh")
        val apptDate = LocalDate.parse(appointmentDateIso)
        val now = ZonedDateTime.now(zone)

        val dayBeforeAt9 = ZonedDateTime.of(apptDate.minusDays(1), LocalTime.of(9, 0), zone)
        enqueueIfFuture(
            context = context,
            tag = reminderTag(appointmentId, "day_before"),
            runAt = dayBeforeAt9,
            now = now,
            doctorName = doctorName,
            queueNumber = queueNumber,
            reminderType = "day_before",
        )

        val sameDayAt8 = ZonedDateTime.of(apptDate, LocalTime.of(8, 0), zone)
        enqueueIfFuture(
            context = context,
            tag = reminderTag(appointmentId, "same_day"),
            runAt = sameDayAt8,
            now = now,
            doctorName = doctorName,
            queueNumber = queueNumber,
            reminderType = "same_day",
        )
    }

    fun cancelAppointmentReminders(context: Context, appointmentId: String) {
        val wm = WorkManager.getInstance(context)
        wm.cancelAllWorkByTag(reminderTag(appointmentId, "day_before"))
        wm.cancelAllWorkByTag(reminderTag(appointmentId, "same_day"))
    }

    private fun reminderTag(appointmentId: String, kind: String): String =
        "reminder_${appointmentId}_$kind"

    private fun enqueueIfFuture(
        context: Context,
        tag: String,
        runAt: ZonedDateTime,
        now: ZonedDateTime,
        doctorName: String,
        queueNumber: Int,
        reminderType: String,
    ) {
        val delayMs = ChronoUnit.MILLIS.between(now, runAt)
        if (delayMs <= 0L) return

        val data = Data.Builder()
            .putString(AppointmentReminderWorker.KEY_DOCTOR_NAME, doctorName)
            .putInt(AppointmentReminderWorker.KEY_QUEUE_NUMBER, queueNumber)
            .putString(AppointmentReminderWorker.KEY_REMINDER_TYPE, reminderType)
            .build()

        val request = OneTimeWorkRequestBuilder<AppointmentReminderWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .addTag(tag)
            .build()

        WorkManager.getInstance(context).enqueue(request)
    }
}
