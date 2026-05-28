# TaskPulse ProGuard Rules
-keep class com.chastechgroup.taskpulse.data.entities.** { *; }
-keep class com.chastechgroup.taskpulse.data.models.** { *; }
-keep class com.chastechgroup.taskpulse.engine.** { *; }
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** { volatile <fields>; }

# Billing
-keep class com.android.billingclient.** { *; }

# Compose
-keep class androidx.compose.** { *; }
