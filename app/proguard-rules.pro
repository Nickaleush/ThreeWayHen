# =============================================================================
# Three Way Hen template — R8 / ProGuard rules
# =============================================================================

-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Custom Views inflated from XML.
-keepclasseswithmembers class * extends android.view.View {
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}
-keep class cob.woodlab.forestarcher.games.archer.ArcherGameView { *; }

# Navigation instantiates fragments by fully-qualified class name.
-keep public class * extends androidx.fragment.app.Fragment

# Application class referenced by AndroidManifest.
-keep class cob.woodlab.forestarcher.App { *; }

# Room.
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-dontwarn androidx.room.paging.**

# Kotlin coroutines.
-dontwarn kotlinx.coroutines.**
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }

# Kotlin metadata / enums.
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Parcelable / Bundle arguments used by Navigation.
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}
