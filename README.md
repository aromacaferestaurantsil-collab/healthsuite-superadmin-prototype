# HealthSuite 360 — Super Admin (Android)

Production-ready Kotlin and Jetpack Compose implementation of the **HealthSuite 360 · Super Admin** console for healthcare tenant governance, billing plans, security auditing, cloud backup, and WhatsApp operations.

## Implemented Features

- **Enforced Authentication & Two-Step Verification**:
  - **Step 1**: Google Sign-In via Jetpack `CredentialManager` (`GetSignInWithGoogleOption`) backed by Firebase Authentication.
  - **Step 2**: Hardware-backed `AndroidKeyStore` (AES-256-GCM) encrypted RFC 6238 TOTP authenticator with QR setup (`ZXing`) and `BiometricPrompt` (`BIOMETRIC_STRONG` / `DEVICE_CREDENTIAL`) verification.
  - **Window & Clipboard Protection**: Enforces `WindowManager.LayoutParams.FLAG_SECURE` across both Two-Step Verification enrollment/login and the main console (when enabled in settings), and marks copied 2FA setup keys as sensitive on Android 13+.
- **Cloud Firestore Workspace**:
  - Per-admin scoped collections (`users/{uid}/vendors`, `modules`, `moduleGroups`, `offers`, `licenses`, `payments`, `notifications`, `tickets`, `auditLogs`, `settings`) with real-time `.snapshots()`, server timestamps, and strict zero-trust security rules (`firestore.rules`).
- **Online Payment Gateway & Automated License Issuance**:
  - Supports vendor module/group/all-module purchases via HTTPS Merchant Checkout (`/orders` creation & verification) and Android UPI Intent (`upi://pay`), automatically issuing Keystore-signed HMAC license keys and updating vendor active seats and expiry upon verified settlement.
- **Module Pricing, Groups & All-Modules Licensing**:
  - Editable monthly/annual prices and discount percentages per module, custom module licensing groups, and All-Modules Enterprise Licensing packages.
- **Offers, Subscription Remaining & Push Notifications**:
  - Manage promo offers/discounts, track remaining subscription days and auto-renewal status, scan expiring/manual-renewal vendors, and dispatch Android `NotificationCompat` push alerts (with `POST_NOTIFICATIONS` permission), optional HTTPS push webhook relays, and `mailto:` email drafts.
- **Single-License Anti-Sharing & Audit Guard**:
  - Evaluates device fingerprints and distinct IP counts to flag vendors who purchase 1 license seat with multi-device usage, lock licenses back to a single primary device, push security warnings, or revoke and suspend violating accounts.
- **Extended Vendor Directory & Safe CSV Export**:
  - Capture and edit each vendor's Admin Email ID, Billing Email ID, Phone Number, Facility Address, City, Facility Type, License Seats, and Auto-Renewal toggle, with formula-neutralized CSV exports.
- **Per-User Google Drive CSV Backup**:
  - Requests `https://www.googleapis.com/auth/drive.file` consent via Google Play Services `AuthorizationClient` bound to the signed-in user's account, verifies Drive ownership via `/drive/v3/about`, and prevents other users from accessing the Super Admin's Google Drive.
  - Also supports local device CSV export via Android's Storage Access Framework (`CreateDocument`).
- **WhatsApp Integration via Link-Device QR**:
  - Connects over HTTPS (with strict public-HTTPS validation and redirect blocking) to a self-hosted WhatsApp multi-device bridge (`/status`, `/qr`, `/pair`, `/send`), renders live pairing QR codes (`ZXing`), scans external QR payloads using CameraX + ML Kit Barcode Scanning, and supports direct `https://wa.me/...` dispatch.
- **Android Home Screen Shortcuts & PWA Portal Installability**:
  - Supports one-tap Home Screen shortcut pinning (`ShortcutManagerCompat`) launching `healthsuite360://console` via deep-link filter (`singleTop`), plus live HTTPS web portal PWA manifest (`name`, `start_url`, `display=standalone`, `icons`) and Service Worker verification with browser install launch.
- **Release Optimization & ProGuard**:
  - Configured with `isMinifyEnabled = true`, `isShrinkResources = true`, and comprehensive keep rules in `app/proguard-rules.pro`.

## Tech Stack

- Kotlin & Jetpack Compose (Material 3, edge-to-edge, adaptive `PermanentNavigationDrawer` / `ModalNavigationDrawer`)
- Firebase Authentication & Cloud Firestore (`firebase-bom:34.0.0`)
- Jetpack `CredentialManager`, `androidx.biometric`, `AndroidKeyStore`, CameraX, ML Kit Barcode Scanning, OkHttp, ZXing, `NotificationCompat`
- Gradle Kotlin DSL (`compileSdk = 36`, `minSdk = 26`, `targetSdk = 36`)