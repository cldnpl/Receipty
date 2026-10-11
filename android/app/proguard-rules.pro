# kotlinx.serialization: i @Serializable dei conti salvati.
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class com.cldnpl.receipty.** {
    *** Companion;
}
-keepclasseswithmembers class com.cldnpl.receipty.** {
    kotlinx.serialization.KSerializer serializer(...);
}
