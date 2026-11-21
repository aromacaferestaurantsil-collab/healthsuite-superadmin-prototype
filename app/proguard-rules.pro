# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in the Android SDK tools proguard-android-optimize.txt file.

# Preserve source file names and line numbers for crash stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Preserve annotations, signatures, and generic metadata
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Keep classes/members explicitly annotated with @Keep
-keep class androidx.annotation.Keep
-keep @androidx.annotation.Keep class * { *; }
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}

# App domain models & Firestore POJOs
-keepclassmembers class com.example.model.** {
    <init>(...);
    <fields>;
}
-keepnames class com.example.model.**

# Android ViewModels & SavedState
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keepclassmembers class * extends androidx.lifecycle.AndroidViewModel {
    <init>(...);
}

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.flow.**

# Firebase Auth & Cloud Firestore
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName <fields>;
    @com.google.firebase.firestore.PropertyName <methods>;
    @com.google.firebase.firestore.DocumentId <fields>;
    @com.google.firebase.firestore.ServerTimestamp <fields>;
    @com.google.firebase.firestore.IgnoreExtraProperties <fields>;
}
-dontwarn com.google.firebase.**
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**

# Google Play Services & Jetpack Credential Manager
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }
-dontwarn androidx.credentials.**

# AndroidX Biometric
-keep class androidx.biometric.** { *; }
-dontwarn androidx.biometric.**

# OkHttp3 & Okio
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.Nullable
-dontwarn javax.annotation.ParametersAreNonnullByDefault
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# CameraX & ML Kit Barcode Scanning
-keep class androidx.camera.** { *; }
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# ZXing QR Code Core
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**
