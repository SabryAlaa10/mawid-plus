package com.mawidplus.patient.core.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class AppointmentReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val doctorName = inputData.getString(KEY_DOCTOR_NAME) ?: return Result.failure()
        val queueNumber = inputData.getInt(KEY_QUEUE_NUMBER, 0)
        val reminderType = inputData.getString(KEY_REMINDER_TYPE) ?: "day_before"

        val title = "تذكير بموعدك"
        val message = when (reminderType) {
            "day_before" -> "غداً لديك موعد مع $doctorName — رقم $queueNumber في الطابور"
            "same_day" -> "اليوم لديك موعد مع $doctorName — رقم $queueNumber في الطابور"
            else -> "لديك موعد مع $doctorName"
        }

        NotificationHelper.showNotification(applicationContext, title, message)
        return Result.success()
    }

    companion object {
        const val KEY_DOCTOR_NAME = "doctor_name"
        const val KEY_QUEUE_NUMBER = "queue_number"
        const val KEY_REMINDER_TYPE = "reminder_type"
    }
}
