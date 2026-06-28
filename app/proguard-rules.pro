# Keep kotlinx.serialization metadata for serialized models
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.gymbuddy.** {
    *** Companion;
}
-keepclasseswithmembers class com.gymbuddy.** {
    kotlinx.serialization.KSerializer serializer(...);
}
