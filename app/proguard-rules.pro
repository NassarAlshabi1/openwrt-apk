# Add project specific ProGuard rules here.
-keep class com.openwrtmanager.app.data.model.** { *; }
-keepattributes *Annotation*

# Error Prone annotations are compile-time metadata referenced by Tink.
# They are not required at runtime and should not be bundled in the APK.
-dontwarn com.google.errorprone.annotations.**
