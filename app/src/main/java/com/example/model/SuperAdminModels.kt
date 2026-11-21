package com.example.model

enum class AdminPage(val title: String, val heading: String) {
    Overview("Overview", "Platform overview"),
    Vendors("Vendors", "Vendors"),
    Subscriptions("Subscriptions", "Subscriptions"),
    Modules("Modules", "Modules"),
    PlatformUsers("Platform users", "Platform users"),
    SupportDesk("Support desk", "Support desk"),
    Releases("Releases", "Releases"),
    Analytics("Analytics", "Analytics"),
    AuditLog("Audit log", "Audit log"),
    Settings("Settings", "Settings")
}

enum class VendorStatus(val label: String) {
    Active("Active"),
    Pending("Pending"),
    Suspended("Suspended")
}

enum class ModuleStage(val label: String) {
    Live("Live"),
    Beta("Beta"),
    Planned("Planned")
}

data class Vendor(
    val id: String,
    val name: String,
    val city: String,
    val type: String,
    val plan: String,
    val status: VendorStatus
)

data class SupportTicket(
    val id: String,
    val title: String,
    val vendor: String,
    val status: String,
    val priority: String
)

data class PlatformModule(
    val name: String,
    val description: String,
    val stage: ModuleStage
)

data class PlatformRole(
    val title: String,
    val role: String
)

data class PlatformHealthService(
    val name: String,
    val status: String
)

data class ReleaseEntry(
    val title: String,
    val actionLabel: String
)

data class AnalyticsMetric(
    val label: String,
    val value: String,
    val tag: String
)

data class AuditLogEntry(
    val message: String,
    val status: String = "Success"
)

data class PlatformSettingItem(
    val key: String,
    val label: String,
    val enabled: Boolean
)
