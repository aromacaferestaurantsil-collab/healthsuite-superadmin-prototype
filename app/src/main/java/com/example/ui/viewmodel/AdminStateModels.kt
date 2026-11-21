package com.example.ui.viewmodel

import com.example.integrations.PwaReadinessReport
import com.example.model.AdminPage
import com.example.model.AnalyticsMetric
import com.example.model.AuditLogEntry
import com.example.model.DriveUploadResult
import com.example.model.ModuleGroupDoc
import com.example.model.PaymentGatewayConfig
import com.example.model.PaymentTransactionDoc
import com.example.model.PlatformHealthService
import com.example.model.PlatformModuleDoc
import com.example.model.PlatformRole
import com.example.model.PlatformSettingDoc
import com.example.model.PromoOfferDoc
import com.example.model.ReleaseEntry
import com.example.model.SupportTicket
import com.example.model.Vendor
import com.example.model.VendorLicenseDoc
import com.example.model.VendorNotificationDoc
import com.example.model.WhatsAppBridgeStatus
import com.example.security.AdminIdentityPolicy

sealed interface WorkspaceUiState {
    data object Loading : WorkspaceUiState
    data class Success(val data: WorkspaceData) : WorkspaceUiState
    data class Error(val message: String) : WorkspaceUiState
}

data class WorkspaceData(
    val vendors: List<Vendor> = emptyList(),
    val tickets: List<SupportTicket> = emptyList(),
    val auditLogs: List<AuditLogEntry> = emptyList(),
    val settings: List<PlatformSettingDoc> = emptyList(),
    val modules: List<PlatformModuleDoc> = emptyList(),
    val moduleGroups: List<ModuleGroupDoc> = emptyList(),
    val offers: List<PromoOfferDoc> = emptyList(),
    val licenses: List<VendorLicenseDoc> = emptyList(),
    val payments: List<PaymentTransactionDoc> = emptyList(),
    val notifications: List<VendorNotificationDoc> = emptyList()
)

data class CoreWorkspaceSlice(
    val vendors: List<Vendor>,
    val tickets: List<SupportTicket>,
    val auditLogs: List<AuditLogEntry>,
    val settings: List<PlatformSettingDoc>
)

data class AdminControlState(
    val currentPage: AdminPage = AdminPage.Overview,
    val searchQuery: String = "",
    val statusFilter: String = "All",
    val planFilter: String = "All",
    val selectedVendor: Vendor? = null,
    val isAddVendorModalOpen: Boolean = false,
    val newVendorName: String = "",
    val newVendorCity: String = "",
    val newVendorType: String = "Clinic",
    val newVendorPlan: String = "Starter",
    val newVendorAutoRenew: Boolean = true,
    val newVendorAdminEmail: String = "",
    val newVendorBillingEmail: String = "",
    val newVendorPhone: String = "",
    val newVendorAddress: String = "",
    val editingVendor: Vendor? = null,
    val checkoutPreselectedVendorId: String? = null,
    val activeUpiOrder: PaymentTransactionDoc? = null,
    val activeUpiVendor: Vendor? = null,
    val activeUpiScopeModules: String = "",
    val toastMessage: String? = null,
    val isMutating: Boolean = false,
    val isPaymentBusy: Boolean = false,
    val isDriveUploading: Boolean = false,
    val pendingDriveAccessToken: String? = null,
    val lastDriveUpload: DriveUploadResult? = null,
    val driveError: String? = null,
    val whatsAppBridgeUrl: String = "",
    val whatsAppBridgeToken: String = "",
    val whatsAppStatus: WhatsAppBridgeStatus = WhatsAppBridgeStatus(),
    val isQrScannerOpen: Boolean = false,
    val paymentGatewayConfig: PaymentGatewayConfig = PaymentGatewayConfig(),
    val signedInEmail: String = "",
    val isSuperAdmin: Boolean = false,
    val pwaPortalUrl: String = "",
    val isCheckingPwa: Boolean = false,
    val pwaReport: PwaReadinessReport? = null,
    val platformHealth: List<PlatformHealthService> = defaultHealthServices,
    val platformRoles: List<PlatformRole> = defaultPlatformRoles,
    val releases: List<ReleaseEntry> = defaultReleases,
    val analyticsMetrics: List<AnalyticsMetric> = defaultAnalytics
)

val defaultHealthServices = listOf(
    PlatformHealthService("Core application", "Operational"),
    PlatformHealthService("Firebase Auth & 2FA", "Operational"),
    PlatformHealthService("Cloud Firestore", "Operational"),
    PlatformHealthService("Payment & License Guard", "Operational")
)

val defaultPlatformRoles = listOf(
    PlatformRole(AdminIdentityPolicy.REGISTERED_SUPER_ADMIN_EMAIL, "Registered Super Admin"),
    PlatformRole("Authenticated Operators", "Own Workspace & Own Google Drive"),
    PlatformRole("Support Manager", "Support Admin"),
    PlatformRole("Compliance Reviewer", "Read-only Auditor")
)

val defaultReleases = listOf(
    ReleaseEntry("HealthSuite 360 v1.0.0 · Current", "View notes"),
    ReleaseEntry("PharmaSuite v0.9.2 · Staged", "View notes"),
    ReleaseEntry("PatientSuite v0.6.0 · Beta", "View notes")
)

val defaultAnalytics = listOf(
    AnalyticsMetric("Vendor activation", "78%", "Live cohort"),
    AnalyticsMetric("Bundle conversion", "46%", "Modules & groups"),
    AnalyticsMetric("Auto-renewal rate", "68%", "Active tenants"),
    AnalyticsMetric("License compliance", "92%", "Seat guard")
)
