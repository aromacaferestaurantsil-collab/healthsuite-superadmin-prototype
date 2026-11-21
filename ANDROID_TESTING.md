# Android test readiness

## Automated checks

The Android CI workflow runs JVM unit tests and assembles a debug APK. CI creates a clearly non-production Firebase configuration only so the Google Services Gradle plugin can complete configuration. The resulting APK is a compile/smoke artifact: Firebase Authentication, Firestore, Google sign-in, Drive, payment, WhatsApp and push services will not connect to a real project.

## Installable test build with real Firebase

1. In Firebase Console, register Android package `com.aistudio.healthsuite.kxmpzq` and download the matching `google-services.json`.
2. Place it at `app/google-services.json`. This file is intentionally not committed.
3. Build with Android Studio or run `gradle assembleDebug` from the project root.
4. Install `app/build/outputs/apk/debug/app-debug.apk` on a test device running Android 8.0 (API 26) or later.
5. Use a dedicated test Firebase project and synthetic vendor records. Do not use production patient data.

## Required device test matrix

- Google sign-in success, cancellation, offline failure, unverified email, and sign-out.
- Two-step enrollment, valid/invalid TOTP, replay/clock drift, biometric cancellation, device restart and recovery.
- Firestore read/write denial for an untrusted account and cross-user access attempts.
- Vendor create/edit/status, module pricing/groups, offers, licenses, payments, support tickets and audit trail.
- CSV export, Android file picker, Drive permission denial, QR camera permission denial/grant, WhatsApp bridge offline/invalid URL.
- Android 8 through current Android, small phone, large phone and tablet layouts; rotation, process death and accessibility font scaling.

## Security blockers before production

- Email allowlisting in the Android client is not an authorization boundary. The console must require a server-issued Firebase custom claim such as `superAdmin: true`, and Firestore rules must independently enforce it.
- Client-side payment status, license issuance, audit writes and role assignment must be replaced or verified by trusted backend functions.
- Do not promote the CI placeholder APK to a user-facing or production build.
