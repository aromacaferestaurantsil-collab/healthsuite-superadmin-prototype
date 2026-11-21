# Super Admin security remediation checklist

This document records the remaining server-side work required before production integration.
It is intentionally separate from the Android client because a client application cannot securely grant itself platform privileges.

## Current bootstrap identity

The Android client currently has a temporary email allowlist in `AdminIdentityPolicy`.
It is a sign-in convenience gate only. It is **not** an authorization boundary and must never be treated as proof of Super Admin privileges.

Keep the personal email as a temporary bootstrap account during development. Do not add passwords, OAuth tokens, service-account JSON, TOTP seeds, or private keys to this repository.

## Required trusted authorization design

1. Create a trusted administrator-provisioning path (Firebase Admin SDK in a secured Cloud Function or controlled server).
2. Verify the bootstrap account's Firebase UID and verified email out of band.
3. Assign a server-managed custom claim such as `platformRole: "super_admin"`. The Android app must not be able to set or modify this claim.
4. Require a valid, refreshed ID token containing that claim for every platform-wide operation.
5. Define vendor membership and roles separately (for example `vendor_admin`, `doctor`, `patient`, `agent`, `employee`) with an explicit vendor/facility identifier.
6. Add Firestore rules that authorize Super Admin operations only from the trusted claim and constrain vendor users to their assigned vendor/facility. Do not rely on a document path containing the caller's own UID as proof of platform role.
7. Keep payment callbacks, settlement verification, subscription transitions, licence issuance/revocation and audit events in trusted server code. Verify gateway signatures server-side and make webhook processing idempotent.
8. Make audit events append-only for clients; privileged audit records should be written by trusted server functions.
9. Add emulator tests proving that an ordinary authenticated user, vendor admin, and each subordinate role cannot read or mutate another vendor's data or perform platform administration.

## Firestore rules review finding

The current rules scope records under `/users/{userId}/...` and authorize access with `request.auth.uid == userId`. This protects one user's subtree from another ordinary user, but it does not create a platform Super Admin role or a vendor hierarchy. Do not deploy these rules as the final multi-tenant authorization model.

Do not replace the rules with a broad `isSuperAdmin` allow-all shortcut without also testing collection paths, permitted fields, immutable identifiers, and role-specific restrictions.

## Release signing

The Android release build must never use the debug keystore. The security-hardening branch now configures release signing only when all four protected environment variables are supplied:

- `ANDROID_KEYSTORE_PATH`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

Store the keystore as a protected CI secret/file and the passwords as protected secrets. Never commit the keystore or passwords. A release built without these values is not a distributable signed release.

## Acceptance gates before production

- [ ] Trusted Firebase Admin provisioning of the initial Super Admin custom claim
- [ ] Custom-claim enforcement in Firestore rules and trusted backend endpoints
- [ ] Vendor and subordinate-role isolation tests using the Firebase Emulator Suite
- [ ] Server-side payment verification and idempotent licence lifecycle
- [ ] Append-only server-authored security audit events
- [ ] Firebase project/environment IDs verified against the intended non-production project
- [ ] Signed release built with protected signing secrets
- [ ] Android device tests for login, second factor, recovery, session revocation and role denial
- [ ] Backup restore and external integrations verified end-to-end

## Scope and safety

This branch contains a release-signing configuration hardening change and this remediation record. It does not alter Firestore production rules, Firebase users/claims, payment integrations, or any live project. Those changes require a configured trusted backend and emulator-backed tests; changing rules speculatively could lock out the current administrator or expose tenant data.
