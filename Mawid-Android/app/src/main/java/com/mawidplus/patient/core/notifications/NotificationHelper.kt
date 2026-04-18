package com.mawidplus.patient.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.mawidplus.patient.core.region.MawidRegion
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

object NotificationHelper {

    /** قناة جديدة بأولوية أعلى (تغيير المعرف يطبّق الإعدادات على الأجهزة التي كانت لديها قناة قديمة). */
    const val CHANNEL_ID = "mawid_appointments_v2"
    private const val NOTIFICATION_ID_BASE = 7100

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "مواعيد Mawid+",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "تذكيرات بالمواعيد الطبية (قبل 6 ساعات، ساعة، ونصف ساعة من الموعد)"
            enableVibration(true)
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
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .build()
        nm.notify((NOTIFICATION_ID_BASE + (title + message).hashCode() % 1000), notification)
    }

    /**
     * يجدول 3 تذكيرات بتوقيت مصر/القاهرة حسب [appointmentDateIso] + [timeSlot]:
     * قبل الموعد بـ 6 ساعات، بساعة، وبـ 30 دقيقة (الوصول للعيادة/الطبيب).
     */
    fun scheduleAppointmentReminder(
        context: Context,
        appointmentId: String,
        appointmentDateIso: String,
        timeSlot: String?,
        doctorName: String,
        queueNumber: Int,
        locationHint: String? = null,
    ) {
        createNotificationChannel(context)
        val zone = MawidRegion.timeZone
        val apptDate = LocalDate.parse(appointmentDateIso)
        val apptTime = parseAppointmentTime(timeSlot)
        val appointmentStart = ZonedDateTime.of(apptDate, apptTime, zone)
        val now = ZonedDateTime.now(zone)
        val loc = locationHint?.trim()?.takeIf { it.isNotEmpty() }.orEmpty()

        val t6h = appointmentStart.minusHours(6)
        enqueueIfFuture(
            context = context,
            tag = reminderTag(appointmentId, AppointmentReminderWorker.REMINDER_6H),
            runAt = t6h,
            now = now,
            doctorName = doctorName,
            queueNumber = queueNumber,
            reminderType = AppointmentReminderWorker.REMINDER_6H,
            locationHint = loc,
        )
        val t1h = appointmentStart.minusHours(1)
        enqueueIfFuture(
            context = context,
            tag = reminderTag(appointmentId, AppointmentReminderWorker.REMINDER_1H),
            runAt = t1h,
            now = now,
            doctorName = doctorName,
            queueNumber = queueNumber,
            reminderType = AppointmentReminderWorker.REMINDER_1H,
            locationHint = loc,
        )
        val t30m = appointmentStart.minusMinutes(30)
        enqueueIfFuture(
            context = context,
            tag = reminderTag(appointmentId, AppointmentReminderWorker.REMINDER_30M),
            runAt = t30m,
            now = now,
            doctorName = doctorName,
            queueNumber = queueNumber,
            reminderType = AppointmentReminderWorker.REMINDER_30M,
            locationHint = loc,
        )
    }

    fun cancelAppointmentReminders(context: Context, appointmentId: String) {
        val wm = WorkManager.getInstance(context)
        wm.cancelAllWorkByTag(reminderTag(appointmentId, AppointmentReminderWorker.REMINDER_6H))
        wm.cancelAllWorkByTag(reminderTag(appointmentId, AppointmentReminderWorker.REMINDER_1H))
        wm.cancelAllWorkByTag(reminderTag(appointmentId, AppointmentReminderWorker.REMINDER_30M))
        // مهام قديمة قبل التحديث
        wm.cancelAllWorkByTag(reminderTag(appointmentId, "day_before"))
        wm.cancelAllWorkByTag(reminderTag(appointmentId, "same_day"))
    }

    private fun reminderTag(appointmentId: String, kind: String): String =
        "reminder_${appointmentId}_$kind"

    /** وقت الموعد من الحجز (HH:mm)؛ إن وُجد تنسيق أطول يُقتطع أول 5 أحرف. */
    private fun parseAppointmentTime(timeSlot: String?): LocalTime {
        val raw = timeSlot?.trim().orEmpty()
        if (raw.isEmpty()) return LocalTime.of(9, 0)
        return try {
            when {
                raw.length >= 8 -> LocalTime.parse(raw.substring(0, 8)) // HH:mm:ss
                raw.length >= 5 -> LocalTime.parse(raw.substring(0, 5)) // HH:mm
                else -> LocalTime.parse(raw)
            }
        } catch (_: Exception) {
            LocalTime.of(9, 0)
        }
    }

    private fun enqueueIfFuture(
        context: Context,
        tag: String,
        runAt: ZonedDateTime,
        now: ZonedDateTime,
        doctorName: String,
        queueNumber: Int,
        reminderType: String,
        locationHint: String,
    ) {
        val delayMs = ChronoUnit.MILLIS.between(now, runAt)
        if (delayMs <= 0L) return

        val data = Data.Builder()
            .putString(AppointmentReminderWorker.KEY_DOCTOR_NAME, doctorName)
            .putInt(AppointmentReminderWorker.KEY_QUEUE_NUMBER, queueNumber)
            .putString(AppointmentReminderWorker.KEY_REMINDER_TYPE, reminderType)
            .putString(AppointmentReminderWorker.KEY_LOCATION_HINT, locationHint)
            .build()

        val request = OneTimeWorkRequestBuilder<AppointmentReminderWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .addTag(tag)
            .build()

        WorkManager.getInstance(context).enqueue(request)
    }
}
