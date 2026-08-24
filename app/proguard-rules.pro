# Keep Room Entities & DAOs
-keep class com.cashflow.ai.data.local.entity.** { *; }
-keep class com.cashflow.ai.data.local.dao.** { *; }
-keep class * extends androidx.room.RoomDatabase

# Keep Domain Models
-keep class com.cashflow.ai.domain.model.** { *; }

# Google APIs
-keepclassmembers class * {
    @com.google.api.client.util.Key <fields>;
}

# MPAndroidChart
-keep class com.github.mikephil.charting.** { *; }
