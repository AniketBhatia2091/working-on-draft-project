# ProGuard & R8 rules for DayKhata

# Layer 2 Privacy: Strip all Android Log statements in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
    public static int wtf(...);
}

# Room Database
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# DataStore Preferences
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite {
    <fields>;
}

# AndroidX Biometric
-keep class androidx.biometric.** { *; }
-dontwarn androidx.biometric.**

# Keep DayKhata data models, backup DTOs, and entities
-keep class com.daykhata.app.data.local.Expense { *; }
-keep class com.daykhata.app.domain.model.** { *; }
-keep class com.daykhata.app.domain.backup.** { *; }

# Jetpack Compose
-keepclassmembers class * extends androidx.compose.ui.Modifier { *; }
