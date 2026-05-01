# ---- Kotlin Serialization ----
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class **$$serializer {
    static **$$serializer INSTANCE;
}
-keep @kotlinx.serialization.Serializable class com.joaolucas.spendguard.** { *; }

# ---- Supabase / Ktor ----
-keep class io.github.jan.supabase.** { *; }
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# ---- Room ----
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *

# ---- Gemini AI SDK ----
-keep class com.google.ai.client.generativeai.** { *; }
-dontwarn com.google.ai.client.**

# ---- Google Play Billing ----
-keep class com.android.billingclient.** { *; }

# ---- Encrypted SharedPreferences ----
-keep class androidx.security.crypto.** { *; }

# ---- WorkManager ----
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# ---- Coroutines ----
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.**

# ---- App models ----
-keep class com.joaolucas.spendguard.PurchaseEntity { *; }
-keep class com.joaolucas.spendguard.UserProfile { *; }
-keep class com.joaolucas.spendguard.WeeklyUsage { *; }
-keep class com.joaolucas.spendguard.PurchaseRemote { *; }

# ---- Compose ----
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**