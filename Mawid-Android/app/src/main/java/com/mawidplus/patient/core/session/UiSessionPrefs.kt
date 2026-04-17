package com.mawidplus.patient.core.session

import android.content.Context

/**
 * يحفظ أن المستخدم أكمل شاشة الدخول حتى لا يُعاد توجيهه من الـ Splash في كل مرة (يُزامن مع جلسة Supabase).
 * يُمسح عند [com.mawidplus.patient.data.repository.AuthRepository.signOut].
 */
object UiSessionPrefs {
    private const val NAME = "mawid_ui_session"
    private const val KEY_HOME = "granted_home_access"
    /** آخر auth.users.id بعد تسجيل الدخول بالهاتف/التسجيل — لاكتشاف جلسة مجهولة جديدة بعد فقدان التخزين. */
    private const val KEY_LAST_AUTH_USER_ID = "last_auth_user_id"

    fun markHomeAccess(context: Context) {
        context.applicationContext.getSharedPreferences(NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_HOME, true)
            .apply()
    }

    fun saveLastAuthUserId(context: Context, userId: String) {
        context.applicationContext.getSharedPreferences(NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LAST_AUTH_USER_ID, userId.trim())
            .apply()
    }

    fun getLastAuthUserId(context: Context): String? =
        context.applicationContext.getSharedPreferences(NAME, Context.MODE_PRIVATE)
            .getString(KEY_LAST_AUTH_USER_ID, null)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

    fun clear(context: Context) {
        context.applicationContext.getSharedPreferences(NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }

    fun hasHomeAccess(context: Context): Boolean =
        context.applicationContext.getSharedPreferences(NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_HOME, false)
}
