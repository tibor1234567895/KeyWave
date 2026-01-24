# Add project-specific ProGuard rules here.

# Keep data classes for JSON serialization in DataStore
-keep class com.tiborlaszlo.keywave.data.** { *; }
-keepclassmembers class com.tiborlaszlo.keywave.data.** { *; }

# Keep enum classes
-keepclassmembers enum com.tiborlaszlo.keywave.data.** { *; }

# Kotlin coroutines
-keepclassmembers class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**
