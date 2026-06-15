# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class com.tertiaryinfotech.hrportal.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.tertiaryinfotech.hrportal.data.**$$serializer { *; }
-keep @kotlinx.serialization.Serializable class com.tertiaryinfotech.hrportal.data.** { *; }
