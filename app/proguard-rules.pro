# Basic ProGuard rules for security
-keepclassmembers class * {
    public private *;
}

# Keep - Applications keep classes that are entry points
-keep public class * extends android.app.Application
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider

# Keep - Native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep - Enum classes
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Security - Obfuscate code
-useuniqueclassmembernames
-dontoptimize

# Logging - Remove debug logs in release
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
