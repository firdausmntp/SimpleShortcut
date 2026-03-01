# Add project specific ProGuard rules here.

# Keep data model
-keep class com.josski.simpleshortcut.data.** { *; }

# Keep widget provider & service class names (referenced by OS via manifest)
-keep public class com.josski.simpleshortcut.widget.* { *; }

# Room
-keep @androidx.room.Entity class * { *; }
-keepclassmembers @androidx.room.Dao class * { *; }

# Strip logging in release
-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
    public static int i(...);
}

# Remove kotlin metadata not needed at runtime
-dontwarn kotlin.reflect.jvm.internal.**
-keep class kotlin.Metadata { *; }

# General optimizations
-optimizationpasses 5
-dontusemixedcaseclassnames
-allowaccessmodification