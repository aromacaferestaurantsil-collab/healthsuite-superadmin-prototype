package com.example.security

import com.example.model.VendorLicenseDoc
import java.security.SecureRandom
import java.util.Locale
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

data class LicenseEvaluation(
    val status: String,
    val isSingleLicenseAbuse: Boolean,
    val riskSummary: String
)

object LicenseSecurityEngine {
    private const val SIGNING_PREF_KEY = "hs360_license_hmac_secret"

    private fun getOrCreateSigningSecret(vault: KeystoreVault): String {
        val existing = vault.getEncryptedString(SIGNING_PREF_KEY)
        if (!existing.isNullOrBlank()) return existing
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        val hex = bytes.joinToString("") { "%02x".format(it) }
        vault.putEncryptedString(SIGNING_PREF_KEY, hex)
        return hex
    }

    fun generateSignedLicenseKey(
        vault: KeystoreVault,
        vendorCode: String,
        scopeCode: String,
        seats: Int
    ): String {
        val secret = getOrCreateSigningSecret(vault)
        val nonce = "%04X".format(SecureRandom().nextInt(0x10000))
        val payload = "${vendorCode.uppercase(Locale.ROOT)}|${scopeCode.uppercase(Locale.ROOT)}|$seats|$nonce"
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        val sig = mac.doFinal(payload.toByteArray(Charsets.UTF_8))
            .take(4)
            .joinToString("") { "%02X".format(it) }
        val cleanVendor = vendorCode.replace("-", "").take(8).uppercase(Locale.ROOT)
        return "HS360-$cleanVendor-$nonce-$sig"
    }

    fun evaluateTelemetry(
        purchasedSeats: Int,
        activeDeviceCount: Int,
        distinctIpCount: Int,
        currentlyRevoked: Boolean = false
    ): LicenseEvaluation {
        if (currentlyRevoked) {
            return LicenseEvaluation(
                status = "Revoked",
                isSingleLicenseAbuse = purchasedSeats == 1 && activeDeviceCount > 1,
                riskSummary = "License revoked due to policy violation."
            )
        }
        return when {
            purchasedSeats == 1 && activeDeviceCount > 1 -> LicenseEvaluation(
                status = "Unauthorized Sharing",
                isSingleLicenseAbuse = true,
                riskSummary = "Bought 1 license but active on $activeDeviceCount devices ($distinctIpCount IPs)."
            )
            purchasedSeats == 1 && distinctIpCount > 2 -> LicenseEvaluation(
                status = "Unauthorized Sharing",
                isSingleLicenseAbuse = true,
                riskSummary = "Single license accessed from $distinctIpCount distinct network locations."
            )
            activeDeviceCount > purchasedSeats -> LicenseEvaluation(
                status = "Unauthorized Sharing",
                isSingleLicenseAbuse = false,
                riskSummary = "Active devices ($activeDeviceCount) exceed purchased seats ($purchasedSeats)."
            )
            distinctIpCount > purchasedSeats * 2 -> LicenseEvaluation(
                status = "Warning",
                isSingleLicenseAbuse = false,
                riskSummary = "High IP spread ($distinctIpCount IPs across $purchasedSeats seats)."
            )
            else -> LicenseEvaluation(
                status = "Compliant",
                isSingleLicenseAbuse = false,
                riskSummary = "$activeDeviceCount / $purchasedSeats seats in use."
            )
        }
    }

    fun evaluateDoc(license: VendorLicenseDoc): LicenseEvaluation =
        evaluateTelemetry(
            purchasedSeats = license.purchasedSeats,
            activeDeviceCount = license.activeDeviceCount,
            distinctIpCount = license.distinctIpCount,
            currentlyRevoked = license.violationStatus.equals("Revoked", ignoreCase = true)
        )
}
