package com.mawidplus.patient.ui.navigation

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object Routes {
    const val SPLASH = "splash"
    // Auth
    const val LOGIN = "login"
    const val REGISTER = "register"
    /** وسيط اختياري: أرقام محلية 10 (بدون +20) */
    const val REGISTER_WITH_PHONE_PATTERN = "register?phone={phone}"

    fun registerRoute(phoneLocalDigits: String = ""): String =
        if (phoneLocalDigits.isEmpty()) "register?phone=" else "register?phone=$phoneLocalDigits"
    // Main container
    const val MAIN = "main"
    // Tabs
    const val HOME = "home"
    /** مفتاح تبويب البحث في الشريط السفلي */
    const val SEARCH = "search"
    /** مسار التنقل مع استعلام */
    const val SEARCH_WITH_QUERY_PATTERN = "search?query={query}"

    const val DOCTOR_MAP_PATTERN = "doctor_map/{doctorId}"
    const val APPOINTMENTS = "appointments"
    const val PROFILE = "profile"
    // Others
    /** [appointmentFocus] = `all` (أي موعد نشط) أو [uuid] لصف حجز محدد (بما فيه done/cancelled). */
    const val MY_QUEUE_PATTERN = "my_queue/{doctorId}/{appointmentFocus}"
    const val NOTIFICATIONS = "notifications"
    /** نتائج البحث — شاشات فرعية */
    const val MAP_VIEW = "map_view"
    const val SEARCH_FILTERS = "search_filters"

    const val DOCTOR_DETAIL_PATTERN = "doctor_detail/{doctorId}"

    const val BOOKING_PATTERN = "booking/{doctorId}"

    /** مساعد الذكاء الاصطناعي (FastAPI) */
    const val AI_ASSISTANT = "ai_assistant"

    fun doctorDetailRoute(id: String) = "doctor_detail/$id"
    fun bookingRoute(id: String) = "booking/$id"
    fun myQueueRoute(doctorId: String, appointmentId: String? = null) =
        if (appointmentId.isNullOrBlank()) "my_queue/$doctorId/all"
        else "my_queue/$doctorId/${appointmentId.trim()}"

    fun doctorMapRoute(doctorId: String) = "doctor_map/$doctorId"

    fun searchRoute(query: String = ""): String {
        val q = URLEncoder.encode(query, StandardCharsets.UTF_8.toString())
        return "search?query=$q"
    }
}
