# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.

# Keep Kotlin metadata
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses

# Gson
-keepattributes Signature
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Readium
-keep class org.readium.** { *; }
-dontwarn org.readium.**
-dontwarn org.w3c.**
-dontwarn javax.xml.**
-dontwarn org.xmlpull.v1.**

# Coil
-dontwarn coil.**

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Keep data classes
-keep class com.freeebookbundle.models.** { *; }
-keep class com.freeebookbundle.db.** { *; }

-dontwarn org.joda.convert.**
-keep class org.joda.convert.** { *; }