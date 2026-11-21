package com.example.model

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import kotlin.math.ceil

enum class AdminPage(val title: String, val heading: String) {
    Overview("Overview", "Platform overview"),
    Vendors("Vendors", "Vendors"),
    Modules("Modules & pricing", "Modules & pricing"),
    ModuleGroups("Module groups", "Licensing groups"),
    Offers("Offers & push", "Offers & push alerts"),
    PaymentCheckout("Buy modules", "Online payment gateway"),
    LicenseGuard("License guard", "License sharing & audit"),
    Subscriptions("Subscriptions", "Subscriptions & renewals"),
    SupportDesk("Support desk", "Support desk"),
    AuditLog("Audit log", "Audit log"),
    WhatsAppLink("WhatsApp QR", "WhatsApp integration"),
    PlatformUsers("Platform users", "Platform users"),
    Releases("Releases", "Releases"),
    Analytics("Analytics", "Analytics"),
    Settings("Settings", "Settings")
}

enum class VendorStatus(val label: String) {
    Active("Active"),
    Pending("Pending"),
    Suspended("Suspended");

    companion object {
        fun fromRaw(raw: String?): VendorStatus =
            entries.firstOrNull { it.label.equals(raw, ignoreCase = true) || it.name.equals(raw, ignoreCase = true) }
                ?: Pending
    }
}

enum class ModuleStage(val label: String) {
    Live("Live"),
    Beta("Beta"),
    Planned("Planned")
}

@Keep
@IgnoreExtraProperties
@Immutable
data class Vendor(
    @DocumentId val id: String = "",
    val userId: String = "",
    val vendorCode: String = "",
    val name: String = "",
    val city: String = "",
    val type: String = "Clinic",
    val plan: String = "Starter",
    val status: String = VendorStatus.Pending.label,
    val adminEmail: String = "",
    val billingEmail: String = "",
    val phone: String = "",
    val address: String = "",
    val purchasedLicenses: Int = 1,
    val activeSeats: Int = 1,
    val autoRenew: Boolean = true,
    val subscriptionExpiresAtMillis: Long = 0L,
    val assignedModulesCsv: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val statusEnum: VendorStatus get() = VendorStatus.fromRaw(status)

    fun remainingDays(nowMillis: Long = System.currentTimeMillis()): Int {
        if (subscriptionExpiresAtMillis <= 0L) return 0
        val diff = subscriptionExpiresAtMillis - nowMillis
        return ceil(diff / 86_400_000.0).toInt()
    }
}

@Keep
@IgnoreExtraProperties
@Immutable
data class SupportTicket(
    @DocumentId val id: String = "",
    val userId: String = "",
    val ticketCode: String = "",
    val title: String = "",
    val vendor: String = "",
    val status: String = "Open",
    val priority: String = "Normal",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
@Immutable
data class AuditLogEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val message: String = "",
    val status: String = "Success",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
@Immutable
data class PlatformSettingDoc(
    @DocumentId val key: String = "",
    val userId: String = "",
    val label: String = "",
    val enabled: Boolean = false,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Immutable
data class PlatformModule(
    val name: String,
    val description: String,
    val stage: ModuleStage
)

@Immutable
data class PlatformRole(
    val title: String,
    val role: String
)

@Immutable
data class PlatformHealthService(
    val name: String,
    val status: String
)

@Immutable
data class ReleaseEntry(
    val title: String,
    val actionLabel: String
)

@Immutable
data class AnalyticsMetric(
    val label: String,
    val value: String,
    val tag: String
)

@Immutable
data class DriveUploadResult(
    val fileId: String,
    val fileName: String,
    val webViewLink: String?,
    val uploadedAtMillis: Long,
    val ownerEmail: String = ""
)

@Immutable
data class WhatsAppBridgeStatus(
    val isConnected: Boolean = false,
    val accountLabel: String? = null,
    val qrPayload: String? = null,
    val statusMessage: String = "Bridge not configured",
    val isLoading: Boolean = false
)
