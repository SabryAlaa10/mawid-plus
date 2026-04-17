package com.mawidplus.patient.core.network

import android.util.Log
import com.mawidplus.patient.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest

/**
 * عميل Supabase واحد للتطبيق. المفاتيح من [BuildConfig] (anon key).
 * لا نضبط Json داخل Postgrest هنا — بعض إصدارات supabase-kt تسبب تعطلًا وقت التشغيل مع كتلة Json {};
 * الحقول الزائدة من PostgREST تُعرّف في DTOs (مثل created_at).
 */
object SupabaseProvider {

    private const val TAG = "SupabaseProvider"

    val client: SupabaseClient by lazy {
        Log.d(TAG, "createSupabaseClient url=${BuildConfig.SUPABASE_URL}")
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            install(Auth)
            install(Postgrest)
        }.also {
            Log.d(TAG, "SupabaseClient initialized")
        }
    }
}
