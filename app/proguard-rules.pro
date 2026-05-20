# Keep Room entities, DAOs, and the generated database implementation so R8
# does not strip fields/methods that Room's annotation processor binds to
# at runtime.
-keep class com.ideasinc.followthrough.data.** { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keep @androidx.room.Database class * { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-keepclassmembers class * {
    @androidx.room.* <methods>;
}

# Standard Room safety
-keep class androidx.room.** { *; }
-keepclassmembers class androidx.room.RoomDatabase {
    public <init>(...);
}

# Kotlin metadata — keep so reflection-based libraries (Compose, lifecycle)
# can resolve types correctly in release builds.
-keep class kotlin.Metadata { *; }
-keepattributes *Annotation*, InnerClasses, EnclosingMethod, Signature
