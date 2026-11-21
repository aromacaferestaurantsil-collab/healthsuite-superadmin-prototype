package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.AdminPage
import com.example.model.AnalyticsMetric
import com.example.model.AuditLogEntry
import com.example.model.ModuleStage
import com.example.model.PlatformHealthService
import com.example.model.PlatformModule
import com.example.model.PlatformRole
import com.example.model.PlatformSettingItem
import com.example.model.ReleaseEntry
import com.example.model.SupportTicket
import com.example.model.Vendor
import com.example.model.VendorStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SuperAdminUiState(
    val currentPage: AdminPage = AdminPage.Overview,
    val vendors: List<Vendor> = seedVendors,
    val tickets: List<SupportTicket> = seedTickets,
    val searchQuery: String = "",
    val statusFilter: String = "All",
    val selectedVendor: Vendor? = null,
    val isAddVendorModalOpen: Boolean = false,
    val newVendorName: String = "",
    val newVendorEmail: String = "",
    val auditLogs: List<AuditLogEntry> = emptyList(),
    val toastMessage: String? = null,
    val modules: List<PlatformModule> = seedModules,
    val platformHealth: List<PlatformHealthService> = seedHealthServices,
    val platformRoles: List<PlatformRole> = seedPlatformRoles,
    val releases: List<ReleaseEntry> = seedReleases,
    val analyticsMetrics: List<AnalyticsMetric> = seedAnalytics,
    val settings: List<PlatformSettingItem> = seedSettings
) {
    val visibleVendors: List<Vendor>
        get() = vendors.filter { vendor ->
            val matchesFilter = statusFilter == "All" || vendor.status.label == statusFilter
            val query = searchQuery.trim()
            val matchesQuery = query.isEmpty() || listOf(
                vendor.id,
                vendor.name,
                vendor.city,
                vendor.type,
                vendor.plan,
                vendor.status.label
            ).joinToString(" ").contains(query, ignoreCase = true)
            matchesFilter && matchesQuery
        }
}

class SuperAdminViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(SuperAdminUiState())
    val uiState: StateFlow<SuperAdminUiState> = _uiState.asStateFlow()

    private var toastJob: Job? = null

    fun selectPage(page: AdminPage) {
        _uiState.update { it.copy(currentPage = page) }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun updateStatusFilter(filter: String) {
        _uiState.update { it.copy(statusFilter = filter) }
    }

    fun openVendorProfile(vendor: Vendor) {
        _uiState.update { it.copy(selectedVendor = vendor) }
    }

    fun closeVendorProfile() {
        _uiState.update { it.copy(selectedVendor = null) }
    }

    fun updateVendorStatus(vendorId: String, newStatus: VendorStatus) {
        _uiState.update { state ->
            val updatedVendors = state.vendors.map { vendor ->
                if (vendor.id == vendorId) vendor.copy(status = newStatus) else vendor
            }
            val newLog = AuditLogEntry("${newStatus.label} vendor $vendorId")
            state.copy(
                vendors = updatedVendors,
                auditLogs = listOf(newLog) + state.auditLogs,
                selectedVendor = null
            )
        }
        flash("Demo vendor status updated")
    }

    fun openAddVendorModal() {
        _uiState.update { it.copy(isAddVendorModalOpen = true) }
    }

    fun closeAddVendorModal() {
        _uiState.update { it.copy(isAddVendorModalOpen = false) }
    }

    fun updateNewVendorName(name: String) {
        _uiState.update { it.copy(newVendorName = name) }
    }

    fun updateNewVendorEmail(email: String) {
        _uiState.update { it.copy(newVendorEmail = email) }
    }

    fun submitNewVendor() {
        val current = _uiState.value
        val trimmedName = current.newVendorName.trim()
        val trimmedEmail = current.newVendorEmail.trim()
        if (trimmedName.isEmpty() || !trimmedEmail.contains("@")) {
            flash("Enter a valid name and email")
            return
        }
        _uiState.update { state ->
            val newVendor = Vendor(
                id = "VEN-${1001 + state.vendors.size}",
                name = trimmedName,
                city = "Not specified",
                type = "Clinic",
                plan = "Starter",
                status = VendorStatus.Pending
            )
            val newLog = AuditLogEntry("Registered vendor $trimmedName")
            state.copy(
                vendors = listOf(newVendor) + state.vendors,
                auditLogs = listOf(newLog) + state.auditLogs,
                newVendorName = "",
                newVendorEmail = "",
                isAddVendorModalOpen = false
            )
        }
        flash("Vendor added to pending queue")
    }

    fun resolveTicket(ticketId: String) {
        _uiState.update { state ->
            val updatedTickets = state.tickets.map { ticket ->
                if (ticket.id == ticketId) ticket.copy(status = "Resolved") else ticket
            }
            val newLog = AuditLogEntry("Resolved ticket $ticketId")
            state.copy(
                tickets = updatedTickets,
                auditLogs = listOf(newLog) + state.auditLogs
            )
        }
        flash("Ticket resolved in demo")
    }

    fun toggleSetting(key: String) {
        _uiState.update { state ->
            state.copy(
                settings = state.settings.map { item ->
                    if (item.key == key) item.copy(enabled = !item.enabled) else item
                }
            )
        }
    }

    fun buildVendorsCsv(): String {
        val rows = _uiState.value.visibleVendors
        val header = "ID,Name,City,Type,Plan,Status"
        val body = rows.joinToString("\n") { v ->
            listOf(v.id, v.name, v.city, v.type, v.plan, v.status.label)
                .joinToString(",") { cell -> "\"${cell.replace("\"", "\"\"")}\"" }
        }
        return if (body.isEmpty()) header else "$header\n$body"
    }

    fun flash(message: String) {
        toastJob?.cancel()
        _uiState.update { it.copy(toastMessage = message) }
        toastJob = viewModelScope.launch {
            delay(2500L)
            _uiState.update { it.copy(toastMessage = null) }
        }
    }
}

private val seedVendors = listOf(
    Vendor("VEN-1001", "Aarogya Medical Centre", "Guwahati", "Hospital", "Enterprise", VendorStatus.Active),
    Vendor("VEN-1002", "North East Diagnostics", "Silchar", "Laboratory", "Growth", VendorStatus.Pending),
    Vendor("VEN-1003", "CarePlus Pharmacy", "Dibrugarh", "Pharmacy", "Starter", VendorStatus.Active),
    Vendor("VEN-1004", "LifeSpring Clinic", "Jorhat", "Clinic", "Growth", VendorStatus.Suspended)
)

private val seedTickets = listOf(
    SupportTicket("TKT-2048", "Unable to renew subscription", "CarePlus Pharmacy", "Open", "High"),
    SupportTicket("TKT-2047", "Lab report export formatting", "North East Diagnostics", "In progress", "Normal")
)

private val seedModules = listOf(
    PlatformModule("PharmaSuite", "Pharmacy operations", ModuleStage.Live),
    PlatformModule("LabSuite", "Laboratory management", ModuleStage.Live),
    PlatformModule("PatientSuite", "Patient services", ModuleStage.Beta),
    PlatformModule("DoctorSuite", "Clinical workspace", ModuleStage.Beta),
    PlatformModule("HealthAnalytics", "Insights and reporting", ModuleStage.Planned),
    PlatformModule("Care Connect", "Care coordination", ModuleStage.Planned)
)

private val seedHealthServices = listOf(
    PlatformHealthService("Core application", "Operational"),
    PlatformHealthService("Authentication", "Operational"),
    PlatformHealthService("Database services", "Operational"),
    PlatformHealthService("Notification service", "Operational")
)

private val seedPlatformRoles = listOf(
    PlatformRole("Platform Owner", "Super Admin"),
    PlatformRole("Operations Lead", "Platform Operations"),
    PlatformRole("Support Manager", "Support Admin"),
    PlatformRole("Compliance Reviewer", "Read-only Auditor")
)

private val seedReleases = listOf(
    ReleaseEntry("HealthSuite 360 v0.9.4 · Current", "View notes"),
    ReleaseEntry("PharmaSuite v0.8.2 · Staged", "View notes"),
    ReleaseEntry("PatientSuite v0.5.0 · Beta", "View notes")
)

private val seedAnalytics = listOf(
    AnalyticsMetric("Vendor activation", "78%", "Illustrative"),
    AnalyticsMetric("Trial conversion", "42%", "Illustrative"),
    AnalyticsMetric("Support SLA", "91%", "Illustrative"),
    AnalyticsMetric("Module adoption", "64%", "Illustrative")
)

private val seedSettings = listOf(
    PlatformSettingItem("mfa", "Require multi-factor authentication", true),
    PlatformSettingItem("self_reg", "Allow vendor self-registration", false),
    PlatformSettingItem("maintenance", "Maintenance mode", false),
    PlatformSettingItem("alerts", "Critical platform alerts", true)
)
