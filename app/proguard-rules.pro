# Keep Kotlinx Serialization & OkHttp classes
-keepattributes *Annotation*, InnerClasses, Signature, RuntimeVisibleAnnotations
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
-keep class com.jcraft.jsch.** { *; }
-dontwarn org.bouncycastle.**
-dontwarn com.jcraft.jsch.**
-dontwarn org.ietf.jgss.**
-dontwarn com.sun.jna.**
-dontwarn org.newsclub.net.unix.**
-dontwarn net.i2p.crypto.**
-dontwarn java.lang.management.**
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.naming.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
