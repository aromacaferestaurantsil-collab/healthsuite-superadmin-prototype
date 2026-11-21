package com.example.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test

class AdminIdentityPolicyTest {
    @Test
    fun registeredVerifiedAdminEmailIsAcceptedByCurrentIdentityPolicy() {
        assertEquals(
            true,
            AdminIdentityPolicy.isRegisteredSuperAdmin(
                email = AdminIdentityPolicy.REGISTERED_SUPER_ADMIN_EMAIL,
                isEmailVerified = true
            )
        )
    }

    @Test
    fun emailComparisonIsCaseInsensitiveAndTrimsWhitespace() {
        assertEquals(
            true,
            AdminIdentityPolicy.isRegisteredSuperAdmin(
                email = "  ${AdminIdentityPolicy.REGISTERED_SUPER_ADMIN_EMAIL.uppercase()}  ",
                isEmailVerified = true
            )
        )
    }

    @Test
    fun unverifiedOrBlankEmailIsRejected() {
        assertEquals(
            false,
            AdminIdentityPolicy.isRegisteredSuperAdmin(
                email = AdminIdentityPolicy.REGISTERED_SUPER_ADMIN_EMAIL,
                isEmailVerified = false
            )
        )
        assertEquals(false, AdminIdentityPolicy.isRegisteredSuperAdmin(email = " "))
        assertEquals(false, AdminIdentityPolicy.isRegisteredSuperAdmin(email = null))
    }

    @Test
    fun driveUploadRequiresSignedInOwnerMatch() {
        assertNull(
            AdminIdentityPolicy.validateDriveOwnerForSignedInUser(
                signedInEmail = "operator@example.com",
                driveOwnerEmail = "operator@example.com"
            )
        )
        assertNotNull(
            AdminIdentityPolicy.validateDriveOwnerForSignedInUser(
                signedInEmail = "operator@example.com",
                driveOwnerEmail = "another@example.com"
            )
        )
        assertNotNull(
            AdminIdentityPolicy.validateDriveOwnerForSignedInUser(
                signedInEmail = "",
                driveOwnerEmail = "operator@example.com"
            )
        )
    }

    @Test
    fun nonAdminCannotUseTheRegisteredAdminDrive() {
        assertNotNull(
            AdminIdentityPolicy.validateDriveOwnerForSignedInUser(
                signedInEmail = "vendor@example.com",
                driveOwnerEmail = AdminIdentityPolicy.REGISTERED_SUPER_ADMIN_EMAIL
            )
        )
    }
}
