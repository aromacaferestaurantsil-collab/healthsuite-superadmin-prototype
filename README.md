# HealthSuite 360 — Super Admin Prototype (Android)

Android Kotlin and Jetpack Compose implementation of the HealthSuite 360 Super Admin console prototype.

- **Synthetic demo data only**: All vendors, support tickets, platform modules, releases, analytics metrics, and audit logs live in in-memory `ViewModel` state (`SuperAdminViewModel`).
- **Zero production connections**: No backend APIs, credentials, authentication, billing, or deployment actions are executed.
- **Least-privilege permissions**: Runs offline on synthetic state and exports CSV files via Android's Storage Access Framework (`ActivityResultContracts.CreateDocument`) without requesting storage permissions.

## Tech Stack
- Kotlin & Jetpack Compose (Material 3)
- Adaptive navigation (`PermanentNavigationDrawer` on expanded screens, `ModalNavigationDrawer` on compact/medium screens)
- Gradle Kotlin DSL (`compileSdk = 36`, `minSdk = 26`, `targetSdk = 36`)