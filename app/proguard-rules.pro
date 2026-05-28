# =============================================================================
# Jumper — R8 / ProGuard rules for the release build
# =============================================================================
# These rules keep the minimum surface needed for reflection-based frameworks
# used in the project. Everything else is free to be shrunk and obfuscated.

# --- Keep line numbers for readable crash reports in the Play Console --------
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# --- Custom Views inflated from XML ------------------------------------------
# JumperGameView is referenced by name in res/layout/fragment_game.xml.
# Any custom View used in XML must keep its (Context, AttributeSet) constructor.
-keepclasseswithmembers class * extends android.view.View {
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}
-keep class com.businessdoomguy.minigamestarter.games.jumper.JumperGameView { *; }

# --- Fragments inflated by the Navigation component --------------------------
# Navigation instantiates fragments by their fully-qualified class name.
-keep public class * extends androidx.fragment.app.Fragment

# --- Application class (referenced by name in AndroidManifest) ---------------
-keep class com.businessdoomguy.minigamestarter.App { *; }

# --- Room --------------------------------------------------------------------
# Room generates implementation classes; entities are accessed via generated
# code. Keep entities and DAO interfaces intact.
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-dontwarn androidx.room.paging.**

# --- Kotlin coroutines -------------------------------------------------------
-dontwarn kotlinx.coroutines.**
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }

# --- Kotlin metadata / enums -------------------------------------------------
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# --- Parcelable / Bundle arguments used by Navigation ------------------------
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}
