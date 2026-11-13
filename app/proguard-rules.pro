# Keep Kotlinx Serialization & OkHttp classes
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.pox.colab.**$$serializer { *; }
-keepclassmembers class com.pox.colab.** {
    *** Companion;
}
-dontwarn org.bouncycastle.**
-dontwarn com.jcraft.jsch.**
