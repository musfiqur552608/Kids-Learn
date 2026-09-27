# ---------------------------------------------------------------- Room
# Generated DAO implementations and the `KidsDatabase_Impl` are found reflectively
# by Room's runtime; R8 must not rename or strip them.
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep @androidx.room.Entity class * { *; }
-dontwarn androidx.room.paging.**

# Hilt / Dagger generate components that are looked up reflectively at startup.
-keep,allowobfuscation @interface dagger.hilt.**
-keep class dagger.hilt.internal.aggregatedroot.codegen.** { *; }
-keep class * extends dagger.hilt.internal.GeneratedComponent { *; }

# ---------------------------------------------------------------- Lottie
# Lottie deserialises the animation JSON reflectively into model classes named in
# the JSON ("ty", "ks", "sh"), so the model classes and their fields must survive
# obfuscation or the confetti renders as an empty composition.
-keep class com.airbnb.lottie.** { *; }
-keepclassmembers class com.airbnb.lottie.** { *; }
-dontwarn com.airbnb.lottie.**

# ---------------------------------------------------------------- Kotlin
# Enum `valueOf`/`values` are used by the Room type mapping and by JSON parsing of
# saved state; R8 can otherwise strip the synthetic members it generates.
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Coroutines internals are reflected upon by the debug agent and by
# `kotlinx.coroutines.debug`, neither of which ships in the release build.
-dontwarn kotlinx.coroutines.debug.**

# ---------------------------------------------------------------- App
# Keep line numbers so a Play Console crash report from a parent is readable,
# but hide the original file names.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
