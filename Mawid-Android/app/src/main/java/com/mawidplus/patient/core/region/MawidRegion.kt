package com.mawidplus.patient.core.region

import java.time.ZoneId
import java.util.Locale

/** إعدادات المشروع لمصر: التوقيت، العملة، ومحلي التواريخ. */
object MawidRegion {
    val timeZone: ZoneId = ZoneId.of("Africa/Cairo")
    const val currencySuffix: String = "ج.م"
    val arabicLocale: Locale = Locale("ar", "EG")
}
