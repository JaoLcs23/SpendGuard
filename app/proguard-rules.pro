-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod

-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class **$$serializer { static **$$serializer INSTANCE; }
-keep @kotlinx.serialization.Serializable class com.joaolucas.spendguard.** { *; }
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

-keep class io.github.jan.supabase.** { *; }
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**
-dontwarn io.github.jan.supabase.**

-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase {
    abstract !static *;
}

-keep class com.google.ai.client.generativeai.** { *; }
-dontwarn com.google.ai.client.**

-keep class com.android.billingclient.** { *; }
-dontwarn com.android.billingclient.**

-keep class androidx.security.crypto.** { *; }
-keep class androidx.biometric.** { *; }

-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class androidx.work.** { *; }

-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.**

-keep class com.joaolucas.spendguard.PurchaseEntity { *; }
-keep class com.joaolucas.spendguard.FinancialProfile { *; }
-keep class com.joaolucas.spendguard.WeeklyInsight { *; }
-keep class com.joaolucas.spendguard.InterventionResult { *; }
-keep class com.joaolucas.spendguard.PurchaseInfo { *; }
-keep class com.joaolucas.spendguard.SpendingCategory { *; }
-keep class com.joaolucas.spendguard.EmotionalState { *; }

-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

-keep class org.apache.poi.** { *; }
-dontwarn org.apache.poi.**
-dontwarn org.openxmlformats.**
-dontwarn org.etsi.**
-dontwarn org.w3c.**

-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

-keep class com.joaolucas.spendguard.BuildConfig { *; }

-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
}
