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
        val reminderType = inputData.getString(KEY_REMINDER_TYPE) ?: return Result.failure()
        val locationHint = inputData.getString(KEY_LOCATION_HINT).orEmpty()

        val title = "تذكير بموعدك"
        val message = buildMessage(reminderType, doctorName, queueNumber, locationHint)

        NotificationHelper.showNotification(applicationContext, title, message)
        return Result.success()
    }

    private fun buildMessage(
        reminderType: String,
        doctorName: String,
        queueNumber: Int,
        locationHint: String,
    ): String {
        val loc = locationHint.trim().takeIf { it.isNotEmpty() }
        return when (reminderType) {
            REMINDER_6H ->
                "باقي 6 ساعات على موعدك مع $doctorName — رقم الطابور $queueNumber. جهّز المستندات إن احتجتها."
            REMINDER_1H ->
                "باقي ساعة واحدة على موعدك مع $doctorName — رقم الطابور $queueNumber."
            REMINDER_30M -> {
                val place = when {
                    loc != null -> " كن في العيادة أو عند الطبيب (المكان: $loc)."
                    else -> " كن في العيادة أو عند الطبيب قبل الموعد."
                }
                "باقي نصف ساعة على موعدك مع $doctorName — رقم الطابور $queueNumber.$place"
            }
            // قد تبقى مهام قديمة بعد التحديث
            "day_before" ->
                "غداً لديك موعد مع $doctorName — رقم $queueNumber في الطابور"
            "same_day" ->
                "اليوم لديك موعد مع $doctorName — رقم $queueNumber في الطابور"
            else -> "لديك موعد مع $doctorName — رقم الطابور $queueNumber"
        }
    }

    companion object {
        const val KEY_DOCTOR_NAME = "doctor_name"
        const val KEY_QUEUE_NUMBER = "queue_number"
        const val KEY_REMINDER_TYPE = "reminder_type"
        const val KEY_LOCATION_HINT = "location_hint"

        const val REMINDER_6H = "6h"
        const val REMINDER_1H = "1h"
        const val REMINDER_30M = "30m"
    }
}
