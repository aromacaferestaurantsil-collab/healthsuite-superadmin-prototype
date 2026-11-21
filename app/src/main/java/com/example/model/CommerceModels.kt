package com.example.model

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import kotlin.math.roundToLong

@Keep
@IgnoreExtraProperties
@Immutable
data class PlatformModuleDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val moduleCode: String = "",
    val name: String = "",
    val description: String = "",
    val stage: String = ModuleStage.Live.label,
    val monthlyPrice: Double = 0.0,
    val annualPrice: Double = 0.0,
    val currency: String = "INR",
    val discountPercent: Int = 0,
    val licenseAllEligible: Boolean = true,
    val isActive: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun effectiveMonthlyPrice(): Double =
        ((monthlyPrice * (100 - discountPercent.coerceIn(0, 95)) / 100.0) * 100.0).roundToLong() / 100.0

    fun effectiveAnnualPrice(): Double =
        ((annualPrice * (100 - discountPercent.coerceIn(0, 95)) / 100.0) * 100.0).roundToLong() / 100.0
}

@Keep
@IgnoreExtraProperties
@Immutable
data class ModuleGroupDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val groupCode: String = "",
    val name: String = "",
    val description: String = "",
    val moduleCodesCsv: String = "",
    val bundlePrice: Double = 0.0,
    val discountPercent: Int = 0,
    val includesAllModules: Boolean = false,
    val maxSeatsDefault: Int = 1,
    val isActive: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun effectiveBundlePrice(): Double =
        ((bundlePrice * (100 - discountPercent.coerceIn(0, 95)) / 100.0) * 100.0).roundToLong() / 100.0

    fun moduleList(): List<String> =
        moduleCodesCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
}

@Keep
@IgnoreExtraProperties
@Immutable
data class PromoOfferDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val code: String = "",
    val title: String = "",
    val description: String = "",
    val discountPercent: Int = 10,
    val targetScope: String = "All Modules",
    val validUntilMillis: Long = 0L,
    val isActive: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
@Immutable
data class VendorLicenseDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val licenseKey: String = "",
    val vendorId: String = "",
    val vendorName: String = "",
    val vendorEmail: String = "",
    val scopeName: String = "",
    val purchasedSeats: Int = 1,
    val activeDevicesCsv: String = "",
    val activeDeviceCount: Int = 0,
    val distinctIpCount: Int = 0,
    val violationStatus: String = "Compliant",
    val lastHeartbeatMillis: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun activeDeviceList(): List<String> =
        activeDevicesCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }

    val isSingleLicenseAbuse: Boolean
        get() = purchasedSeats == 1 && (activeDeviceCount > 1 || distinctIpCount > 2)
}

@Keep
@IgnoreExtraProperties
@Immutable
data class PaymentTransactionDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val orderRef: String = "",
    val vendorId: String = "",
    val vendorName: String = "",
    val itemTitle: String = "",
    val scopeType: String = "Module",
    val seats: Int = 1,
    val billingCycle: String = "Monthly",
    val promoCode: String = "",
    val subtotalAmount: Double = 0.0,
    val discountAmount: Double = 0.0,
    val finalAmount: Double = 0.0,
    val currency: String = "INR",
    val gatewayMode: String = "HTTPS Gateway",
    val status: String = "Pending",
    val gatewayRef: String = "",
    val checkoutUrl: String = "",
    val autoRenewEnabled: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
@Immutable
data class VendorNotificationDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val vendorId: String = "",
    val vendorName: String = "",
    val recipientEmail: String = "",
    val recipientPhone: String = "",
    val category: String = "Offer",
    val title: String = "",
    val message: String = "",
    val deliveryChannel: String = "System Push + Log",
    val status: String = "Sent",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Immutable
data class PaymentGatewayConfig(
    val gatewayApiUrl: String = "",
    val gatewayBearerToken: String = "",
    val upiVpa: String = "",
    val merchantName: String = "HealthSuite 360",
    val pushWebhookUrl: String = "",
    val pushWebhookToken: String = ""
)
