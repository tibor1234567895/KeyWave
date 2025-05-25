# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep source file names and line numbers for better crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep accessibility service
-keep class eu.tiborlaszlo.keywave.service.KeyWaveAccessibilityService {
    public void onCreate();
    public void onServiceConnected();
    public void onAccessibilityEvent(android.view.accessibility.AccessibilityEvent);
    public void onInterrupt();
    public boolean onKeyEvent(android.view.KeyEvent);
}

# Keep SettingsManager
-keep class eu.tiborlaszlo.keywave.service.SettingsManager { *; }

# Keep AndroidManifest accessibility service configuration
-keepattributes ServiceInfo

# Keep MediaSession related classes
-keep class android.support.v4.media.** { *; }
-keep class android.media.** { *; }

# Keep DataStore preferences
-keep class androidx.datastore.** { *; }

# Keep Compose related classes
-keep class androidx.compose.** { *; }