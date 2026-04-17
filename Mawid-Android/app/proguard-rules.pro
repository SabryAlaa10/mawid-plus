# Add project specific ProGuard rules here.
# By default, the flags in android-sdk/tools/proguard/proguard-android.txt
# are already added in the gradle build file.

# Kotlin serialization (DTOs used with Supabase / Ktor)
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.mawidplus.patient.data.dto.**$$serializer { *; }
-keepclassmembers class com.mawidplus.patient.data.dto.** {
    *** Companion;
}

# Ktor client
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# Coil
-dontwarn coil.**

# Supabase / OkHttp (used transitively)
-dontwarn okhttp3.**
-dontwarn okio.**

# SLF4J (optional binding — Ktor / transitive)
-dontwarn org.slf4j.impl.StaticLoggerBinder
-dontwarn org.slf4j.**
