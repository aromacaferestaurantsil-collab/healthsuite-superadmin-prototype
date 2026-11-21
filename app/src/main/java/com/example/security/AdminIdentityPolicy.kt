package com.example.security

/**
 * Defines the registered Super Admin identity and enforces per-user Google Drive ownership.
 * No passwords, tokens, OAuth secrets, or 2FA keys are stored in source code.
 */
object AdminIdentityPolicy {
    const val REGISTERED_SUPER_ADMIN_EMAIL = "rooznammanzoor@gmail.com"

    fun isRegisteredSuperAdmin(
        email: String?,
        isEmailVerified: Boolean = true
    ): Boolean {
        if (!isEmailVerified || email.isNullOrBlank()) return false
        return email.trim().equals(REGISTERED_SUPER_ADMIN_EMAIL, ignoreCase = true)
    }

    /**
     * Ensures every user uploads only to their own Google Drive account and prevents
     * other signed-in users from accessing the Super Admin's Google Drive.
     */
    fun validateDriveOwnerForSignedInUser(
        signedInEmail: String,
        driveOwnerEmail: String
    ): String? {
        val cleanSignedIn = signedInEmail.trim()
        val cleanDriveOwner = driveOwnerEmail.trim()
        if (cleanSignedIn.isBlank()) {
            return "A signed-in Google account email is required before uploading to Google Drive."
        }
        if (cleanDriveOwner.isBlank()) {
            return "Unable to verify the connected Google Drive owner account."
        }
        if (!isRegisteredSuperAdmin(cleanSignedIn) &&
            cleanDriveOwner.equals(REGISTERED_SUPER_ADMIN_EMAIL, ignoreCase = true)
        ) {
            return "Access denied: you cannot use the Super Admin's Google Drive. Connect your own Google Drive ($cleanSignedIn)."
        }
        if (!cleanDriveOwner.equals(cleanSignedIn, ignoreCase = true)) {
            return "Google Drive account mismatch ($cleanDriveOwner). Each user must use their own Google Drive ($cleanSignedIn)."
        }
        return null
    }
}
