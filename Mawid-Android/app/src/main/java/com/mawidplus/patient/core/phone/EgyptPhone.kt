package com.mawidplus.patient.core.phone

/** مصر: بدون الصفر الأول، 10 أرقام تبدأ بـ 1 (مثال: 10xxxxxxx، 11xxxxxxx، 15xxxxxxx). */
object EgyptPhone {
    const val DIAL_CODE = "+20"
    const val PLACEHOLDER = "1xxxxxxxxx"

    fun digitsOnly(raw: String): String {
        val d = raw.filter { it.isDigit() }
        // Users often type 01xxxxxxxxx (11 digits); local format is 10 digits starting with 1
        val normalized = if (d.length >= 11 && d.startsWith('0')) d.drop(1) else d
        return normalized.take(10)
    }

    fun isValidLocal(localDigits: String): Boolean =
        localDigits.length == 10 && localDigits.startsWith('1')
}
