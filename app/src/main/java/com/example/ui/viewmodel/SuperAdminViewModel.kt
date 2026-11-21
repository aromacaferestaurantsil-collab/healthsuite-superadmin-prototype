package com.example.ui.viewmodel

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.FirestoreAdminRepository
import com.example.data.FirestoreCommerceRepository
import com.example.integrations.DriveAuthorizationOutcome
import com.example.integrations.GoogleDriveUploader
import com.example.integrations.PaymentGatewayClient
import com.example.integrations.PwaInstallHelper
import com.example.integrations.WhatsAppBridgeClient
import com.example.model.AdminPage
import com.example.model.ModuleGroupDoc
import com.example.model.PaymentGatewayConfig
import com.example.model.PaymentTransactionDoc
import com.example.model.PlatformModuleDoc
import com.example.model.PlatformSettingDoc
import com.example.model.PromoOfferDoc
import com.example.model.Vendor
import com.example.model.VendorLicenseDoc
import com.example.model.VendorStatus
import com.example.model.WhatsAppBridgeStatus
import com.example.notifications.VendorNotificationHelper
import com.example.security.AdminIdentityPolicy
import com.example.security.KeystoreVault
import com.example.security.NetworkUrlValidator
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SuperAdminViewModel(
    private val repository: FirestoreAdminRepository,
    private val commerceRepository: FirestoreCommerceRepository,
    private val vault: KeystoreVault,
    private val driveUploader: GoogleDriveUploader,
    private val whatsAppClient: WhatsAppBridgeClient,
    private val paymentGatewayClient: PaymentGatewayClient,
    private val currentUserId: String,
    private val currentUserEmail: String,
    private val isEmailVerified: Boolean = true,
    private val pwaInstallHelper: PwaInstallHelper = PwaInstallHelper()
) : ViewModel() {

    private val _controlState = MutableStateFlow(
        AdminControlState(
            signedInEmail = currentUserEmail.trim(),
            isSuperAdmin = AdminIdentityPolicy.isRegisteredSuperAdmin(currentUserEmail, isEmailVerified),
            whatsAppBridgeUrl = vault.getEncryptedString(bridgeUrlKey(currentUserId)).orEmpty(),
            whatsAppBridgeToken = vault.getEncryptedString(bridgeTokenKey(currentUserId)).orEmpty(),
            paymentGatewayConfig = PaymentGatewayConfig(
                gatewayApiUrl = vault.getEncryptedString(gwUrlKey(currentUserId)).orEmpty(),
                gatewayBearerToken = vault.getEncryptedString(gwTokenKey(currentUserId)).orEmpty(),
                upiVpa = vault.getEncryptedString(gwUpiKey(currentUserId)).orEmpty(),
                merchantName = vault.getEncryptedString(gwMerchantKey(currentUserId)).orEmpty()
                    .ifBlank { "HealthSuite 360" },
                pushWebhookUrl = vault.getEncryptedString(pushUrlKey(currentUserId)).orEmpty(),
                pushWebhookToken = vault.getEncryptedString(pushTokenKey(currentUserId)).orEmpty()
            )
        )
    )
    val controlState: StateFlow<AdminControlState> = _controlState.asStateFlow()

    private val coreFlow = combine(
        repository.observeVendors(currentUserId),
        repository.observeTickets(currentUserId),
        repository.observeAuditLogs(currentUserId),
        repository.observeSettings(currentUserId)
    ) { vendors, tickets, logs, settings ->
        CoreWorkspaceSlice(vendors, tickets, logs, settings)
    }

    private val catalogFlow = combine(
        commerceRepository.observeModules(currentUserId),
        commerceRepository.observeModuleGroups(currentUserId),
        commerceRepository.observeOffers(currentUserId)
    ) { modules, groups, offers ->
        Triple(modules, groups, offers)
    }

    private val billingSecurityFlow = combine(
        commerceRepository.observeLicenses(currentUserId),
        commerceRepository.observePayments(currentUserId),
        commerceRepository.observeNotifications(currentUserId)
    ) { licenses, payments, notifications ->
        Triple(licenses, payments, notifications)
    }

    val workspaceState: StateFlow<WorkspaceUiState> = combine(
        coreFlow,
        catalogFlow,
        billingSecurityFlow
    ) { core, catalog, billing ->
        WorkspaceData(
            vendors = core.vendors,
            tickets = core.tickets,
            auditLogs = core.auditLogs,
            settings = core.settings,
            modules = catalog.first,
            moduleGroups = catalog.second,
            offers = catalog.third,
            licenses = billing.first,
            payments = billing.second,
            notifications = billing.third
        )
    }
        .map<WorkspaceData, WorkspaceUiState> { WorkspaceUiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing HealthSuite workspace", rootError)
            emit(WorkspaceUiState.Error(rootError.message ?: "Failed to sync platform workspace"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = WorkspaceUiState.Loading
        )

    private var toastJob: Job? = null

    init {
        viewModelScope.launch {
            repository.seedWorkspaceIfEmpty().onFailure { err ->
                Log.w(TAG, "Workspace seed check failed: ${err.message}")
            }
        }
    }

    fun selectPage(page: AdminPage) {
        _controlState.update { it.copy(currentPage = page) }
    }

    fun openVendorCheckout(vendorId: String) {
        _controlState.update {
            it.copy(
                selectedVendor = null,
                checkoutPreselectedVendorId = vendorId,
                currentPage = AdminPage.PaymentCheckout
            )
        }
    }

    fun updateSearchQuery(query: String) {
        _controlState.update { it.copy(searchQuery = query) }
    }

    fun updateStatusFilter(filter: String) {
        _controlState.update { it.copy(statusFilter = filter) }
    }

    fun updatePlanFilter(filter: String) {
        _controlState.update { it.copy(planFilter = filter) }
    }

    fun openVendorProfile(vendor: Vendor) {
        _controlState.update { it.copy(selectedVendor = vendor) }
    }

    fun closeVendorProfile() {
        _controlState.update { it.copy(selectedVendor = null) }
    }

    fun openEditVendorContact(vendor: Vendor) {
        _controlState.update { it.copy(selectedVendor = null, editingVendor = vendor) }
    }

    fun closeEditVendorContact() {
        _controlState.update { it.copy(editingVendor = null) }
    }

    fun openAddVendorModal() {
        _controlState.update { it.copy(isAddVendorModalOpen = true) }
    }

    fun closeAddVendorModal() {
        _controlState.update { it.copy(isAddVendorModalOpen = false) }
    }

    fun updateNewVendorField(
        name: String? = null,
        city: String? = null,
        type: String? = null,
        plan: String? = null,
        autoRenew: Boolean? = null,
        adminEmail: String? = null,
        billingEmail: String? = null,
        phone: String? = null,
        address: String? = null
    ) {
        _controlState.update { cur ->
            cur.copy(
                newVendorName = name ?: cur.newVendorName,
                newVendorCity = city ?: cur.newVendorCity,
                newVendorType = type ?: cur.newVendorType,
                newVendorPlan = plan ?: cur.newVendorPlan,
                newVendorAutoRenew = autoRenew ?: cur.newVendorAutoRenew,
                newVendorAdminEmail = adminEmail ?: cur.newVendorAdminEmail,
                newVendorBillingEmail = billingEmail ?: cur.newVendorBillingEmail,
                newVendorPhone = phone ?: cur.newVendorPhone,
                newVendorAddress = address ?: cur.newVendorAddress
            )
        }
    }

    fun submitNewVendor(currentVendorCount: Int) {
        val state = _controlState.value
        viewModelScope.launch {
            _controlState.update { it.copy(isMutating = true) }
            val result = repository.addVendor(
                name = state.newVendorName,
                city = state.newVendorCity,
                type = state.newVendorType,
                plan = state.newVendorPlan,
                autoRenew = state.newVendorAutoRenew,
                adminEmail = state.newVendorAdminEmail,
                billingEmail = state.newVendorBillingEmail,
                phone = state.newVendorPhone,
                address = state.newVendorAddress,
                currentVendorCount = currentVendorCount
            )
            _controlState.update { current ->
                if (result.isSuccess) {
                    current.copy(
                        isMutating = false,
                        newVendorName = "",
                        newVendorCity = "",
                        newVendorType = "Clinic",
                        newVendorPlan = "Starter",
                        newVendorAutoRenew = true,
                        newVendorAdminEmail = "",
                        newVendorBillingEmail = "",
                        newVendorPhone = "",
                        newVendorAddress = "",
                        isAddVendorModalOpen = false
                    )
                } else {
                    current.copy(isMutating = false)
                }
            }
            flash(
                if (result.isSuccess) "Vendor registered with subscription & contact details"
                else result.exceptionOrNull()?.localizedMessage ?: "Failed to register vendor"
            )
        }
    }

    fun saveVendorContactDetails(
        vendorId: String,
        name: String,
        type: String,
        plan: String,
        city: String,
        adminEmail: String,
        billingEmail: String,
        phone: String,
        address: String,
        autoRenew: Boolean
    ) {
        viewModelScope.launch {
            _controlState.update { it.copy(isMutating = true) }
            val res = repository.updateVendorContactDetails(
                vendorId, name, type, plan, city, adminEmail, billingEmail, phone, address, autoRenew
            )
            _controlState.update { it.copy(isMutating = false, editingVendor = if (res.isSuccess) null else it.editingVendor) }
            flash(if (res.isSuccess) "Vendor details & subscription updated" else res.exceptionOrNull()?.localizedMessage ?: "Update failed")
        }
    }

    fun toggleVendorAutoRenew(vendor: Vendor) {
        viewModelScope.launch {
            val res = repository.toggleVendorAutoRenew(vendor.id, !vendor.autoRenew)
            flash(if (res.isSuccess) "Auto-renewal ${if (!vendor.autoRenew) "enabled" else "paused"} for ${vendor.name}" else "Failed to update auto-renew")
        }
    }

    fun updateVendorStatus(vendorId: String, newStatus: VendorStatus) {
        viewModelScope.launch {
            _controlState.update { it.copy(isMutating = true) }
            val result = repository.updateVendorStatus(vendorId, newStatus)
            _controlState.update { it.copy(isMutating = false, selectedVendor = null) }
            flash(if (result.isSuccess) "Vendor status updated to ${newStatus.label}" else result.exceptionOrNull()?.localizedMessage ?: "Failed to update status")
        }
    }

    // --- Modules Pricing, Groups & Offers ---

    fun updateModulePricing(
        moduleId: String,
        monthlyPrice: Double,
        annualPrice: Double,
        discountPercent: Int,
        licenseAllEligible: Boolean,
        stage: String
    ) {
        viewModelScope.launch {
            val res = commerceRepository.updateModulePricing(
                moduleId, monthlyPrice, annualPrice, discountPercent, licenseAllEligible, stage
            )
            flash(if (res.isSuccess) "Module pricing & offer saved" else res.exceptionOrNull()?.localizedMessage ?: "Failed to update module")
        }
    }

    fun createModule(
        name: String,
        description: String,
        monthlyPrice: Double,
        annualPrice: Double,
        discountPercent: Int,
        licenseAllEligible: Boolean
    ) {
        viewModelScope.launch {
            val res = commerceRepository.createModule(
                name, description, monthlyPrice, annualPrice, discountPercent, licenseAllEligible
            )
            flash(if (res.isSuccess) "Module added for licensing" else res.exceptionOrNull()?.localizedMessage ?: "Failed to add module")
        }
    }

    fun createModuleGroup(
        name: String,
        description: String,
        selectedModules: List<String>,
        includesAllModules: Boolean,
        bundlePrice: Double,
        discountPercent: Int,
        maxSeatsDefault: Int
    ) {
        viewModelScope.launch {
            val res = commerceRepository.createModuleGroup(
                name, description, selectedModules, includesAllModules, bundlePrice, discountPercent, maxSeatsDefault
            )
            flash(if (res.isSuccess) "Module licensing group created" else res.exceptionOrNull()?.localizedMessage ?: "Failed to create group")
        }
    }

    fun createPromoOffer(
        code: String,
        title: String,
        description: String,
        discountPercent: Int,
        targetScope: String,
        validDays: Int
    ) {
        viewModelScope.launch {
            val res = commerceRepository.createOffer(
                code, title, description, discountPercent, targetScope, validDays
            )
            flash(if (res.isSuccess) "Offer $code created" else res.exceptionOrNull()?.localizedMessage ?: "Failed to create offer")
        }
    }

    fun toggleOffer(offer: PromoOfferDoc) {
        viewModelScope.launch {
            val res = commerceRepository.toggleOfferActive(offer)
            flash(if (res.isSuccess) "Offer ${offer.code} updated" else "Failed to toggle offer")
        }
    }

    // --- Payment Gateway Configuration & Checkout ---

    fun savePaymentGatewayConfig(config: PaymentGatewayConfig) {
        vault.putEncryptedString(gwUrlKey(currentUserId), config.gatewayApiUrl.trim())
        vault.putEncryptedString(gwTokenKey(currentUserId), config.gatewayBearerToken.trim())
        vault.putEncryptedString(gwUpiKey(currentUserId), config.upiVpa.trim())
        vault.putEncryptedString(gwMerchantKey(currentUserId), config.merchantName.trim().ifBlank { "HealthSuite 360" })
        vault.putEncryptedString(pushUrlKey(currentUserId), config.pushWebhookUrl.trim())
        vault.putEncryptedString(pushTokenKey(currentUserId), config.pushWebhookToken.trim())
        _controlState.update {
            it.copy(
                paymentGatewayConfig = config.copy(
                    gatewayApiUrl = config.gatewayApiUrl.trim(),
                    gatewayBearerToken = config.gatewayBearerToken.trim(),
                    upiVpa = config.upiVpa.trim(),
                    merchantName = config.merchantName.trim().ifBlank { "HealthSuite 360" },
                    pushWebhookUrl = config.pushWebhookUrl.trim(),
                    pushWebhookToken = config.pushWebhookToken.trim()
                )
            )
        }
        flash("Gateway & push settings encrypted in Keystore")
    }

    fun initiateHttpsGatewayCheckout(
        context: Context,
        vendor: Vendor,
        itemTitle: String,
        scopeType: String,
        assignedScopeModules: String,
        seats: Int,
        billingCycle: String,
        promoCode: String,
        subtotalAmount: Double,
        discountAmount: Double,
        finalAmount: Double,
        autoRenew: Boolean
    ) {
        val cfg = _controlState.value.paymentGatewayConfig
        val orderRef = "ORD-${System.currentTimeMillis()}"
        viewModelScope.launch {
            _controlState.update { it.copy(isPaymentBusy = true) }
            val gatewayRes = paymentGatewayClient.createCheckoutOrder(
                gatewayBaseUrl = cfg.gatewayApiUrl,
                bearerToken = cfg.gatewayBearerToken,
                orderRef = orderRef,
                vendorId = vendor.id,
                vendorName = vendor.name,
                vendorEmail = vendor.adminEmail,
                itemTitle = itemTitle,
                amount = finalAmount,
                currency = "INR",
                autoRenew = autoRenew
            )
            gatewayRes.onSuccess { created ->
                commerceRepository.createPendingPayment(
                    orderRef = created.orderRef,
                    vendor = vendor,
                    itemTitle = itemTitle,
                    scopeType = scopeType,
                    seats = seats,
                    billingCycle = billingCycle,
                    promoCode = promoCode,
                    subtotalAmount = subtotalAmount,
                    discountAmount = discountAmount,
                    finalAmount = finalAmount,
                    currency = "INR",
                    gatewayMode = "HTTPS Gateway",
                    gatewayRef = created.gatewayRef,
                    checkoutUrl = created.checkoutUrl,
                    autoRenewEnabled = autoRenew
                )
                _controlState.update { it.copy(isPaymentBusy = false) }
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(created.checkoutUrl)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                }.onFailure {
                    flash("Order ${created.orderRef} created; open checkout URL in browser")
                }
            }.onFailure { err ->
                _controlState.update { it.copy(isPaymentBusy = false) }
                flash(err.localizedMessage ?: "HTTPS Payment Gateway unavailable")
            }
        }
    }

    fun verifyHttpsGatewayPayment(
        payment: PaymentTransactionDoc,
        vendor: Vendor?,
        allModules: List<PlatformModuleDoc>,
        moduleGroups: List<ModuleGroupDoc>
    ) {
        val cfg = _controlState.value.paymentGatewayConfig
        if (vendor == null) {
            flash("Vendor record not found for order ${payment.orderRef}")
            return
        }
        val resolvedModules = when (payment.scopeType) {
            "All Modules" -> allModules
                .filter { it.licenseAllEligible }
                .joinToString(",") { it.name }
                .ifBlank { payment.itemTitle }
            "Module Group" -> {
                val matched = moduleGroups.firstOrNull { it.name.equals(payment.itemTitle, ignoreCase = true) }
                when {
                    matched?.includesAllModules == true ->
                        allModules.filter { it.licenseAllEligible }.joinToString(",") { it.name }
                    matched != null -> matched.moduleCodesCsv
                    else -> payment.itemTitle
                }
            }
            else -> payment.itemTitle
        }
        viewModelScope.launch {
            _controlState.update { it.copy(isPaymentBusy = true) }
            val res = paymentGatewayClient.verifyOrderStatus(
                gatewayBaseUrl = cfg.gatewayApiUrl,
                bearerToken = cfg.gatewayBearerToken,
                orderRef = payment.orderRef
            )
            res.onSuccess { verified ->
                if (verified.isPaid) {
                    val licRes = commerceRepository.finalizePaidOrderAndIssueLicense(
                        payment = payment,
                        vendor = vendor,
                        gatewayRef = verified.gatewayRef,
                        assignedScopeModules = resolvedModules,
                        vault = vault
                    )
                    flash(
                        licRes.fold(
                            onSuccess = { key -> "Payment verified! Issued license $key" },
                            onFailure = { it.localizedMessage ?: "Failed to record license" }
                        )
                    )
                } else if (verified.status == "Failed") {
                    commerceRepository.markPaymentFailed(payment.id, verified.message)
                    flash("Payment marked Failed: ${verified.message}")
                } else {
                    flash(verified.message)
                }
            }.onFailure { err ->
                flash(err.localizedMessage ?: "Unable to verify payment status")
            }
            _controlState.update { it.copy(isPaymentBusy = false) }
        }
    }

    fun prepareUpiCheckout(
        vendor: Vendor,
        itemTitle: String,
        scopeType: String,
        assignedScopeModules: String,
        seats: Int,
        billingCycle: String,
        promoCode: String,
        subtotalAmount: Double,
        discountAmount: Double,
        finalAmount: Double,
        autoRenew: Boolean,
        onLaunchUpiIntent: (Intent) -> Unit
    ) {
        val cfg = _controlState.value.paymentGatewayConfig
        val orderRef = "UPI-${System.currentTimeMillis()}"
        val intent = paymentGatewayClient.buildUpiPayIntent(
            merchantVpa = cfg.upiVpa,
            merchantName = cfg.merchantName,
            orderRef = orderRef,
            note = "$itemTitle (${seats}x)",
            amount = finalAmount,
            currency = "INR"
        )
        if (intent == null) {
            flash("Configure a valid Merchant UPI VPA (e.g., merchant@bank) first.")
            return
        }
        viewModelScope.launch {
            _controlState.update { it.copy(isPaymentBusy = true) }
            val pendingDoc = PaymentTransactionDoc(
                id = orderRef,
                userId = currentUserId,
                orderRef = orderRef,
                vendorId = vendor.id,
                vendorName = vendor.name,
                itemTitle = itemTitle,
                scopeType = scopeType,
                seats = seats,
                billingCycle = billingCycle,
                promoCode = promoCode,
                subtotalAmount = subtotalAmount,
                discountAmount = discountAmount,
                finalAmount = finalAmount,
                currency = "INR",
                gatewayMode = "UPI Intent",
                status = "Pending",
                autoRenewEnabled = autoRenew
            )
            val saveRes = commerceRepository.createPendingPayment(
                orderRef = orderRef,
                vendor = vendor,
                itemTitle = itemTitle,
                scopeType = scopeType,
                seats = seats,
                billingCycle = billingCycle,
                promoCode = promoCode,
                subtotalAmount = subtotalAmount,
                discountAmount = discountAmount,
                finalAmount = finalAmount,
                currency = "INR",
                gatewayMode = "UPI Intent",
                gatewayRef = "",
                checkoutUrl = "",
                autoRenewEnabled = autoRenew
            )
            if (saveRes.isSuccess) {
                _controlState.update {
                    it.copy(
                        isPaymentBusy = false,
                        activeUpiOrder = pendingDoc,
                        activeUpiVendor = vendor,
                        activeUpiScopeModules = assignedScopeModules
                    )
                }
                onLaunchUpiIntent(intent)
            } else {
                _controlState.update { it.copy(isPaymentBusy = false) }
                flash("Could not create pending UPI order")
            }
        }
    }

    fun handleUpiActivityResult(data: Intent?) {
        val order = _controlState.value.activeUpiOrder ?: return
        val vendor = _controlState.value.activeUpiVendor
        val modulesCsv = _controlState.value.activeUpiScopeModules
        _controlState.update {
            it.copy(activeUpiOrder = null, activeUpiVendor = null, activeUpiScopeModules = "")
        }

        val parsed = paymentGatewayClient.parseUpiResultIntent(data)
        viewModelScope.launch {
            when {
                parsed.isSuccess && vendor != null -> {
                    val res = commerceRepository.finalizePaidOrderAndIssueLicense(
                        payment = order,
                        vendor = vendor,
                        gatewayRef = parsed.referenceId,
                        assignedScopeModules = modulesCsv,
                        vault = vault
                    )
                    flash(
                        res.fold(
                            onSuccess = { key -> "UPI Paid! License issued: $key" },
                            onFailure = { it.localizedMessage ?: "License issuance failed" }
                        )
                    )
                }
                parsed.isSubmittedPending -> {
                    flash("UPI transaction submitted; pending bank confirmation (${parsed.referenceId})")
                }
                else -> {
                    commerceRepository.markPaymentFailed(order.id, "UPI ${parsed.statusText}")
                    flash("UPI payment ${parsed.statusText.lowercase()}")
                }
            }
        }
    }

    // --- Push Notifications (Offers, Subscription Remaining & Auto-Renewal) ---

    fun sendVendorPushNotification(
        context: Context,
        targetVendors: List<Vendor>,
        category: String,
        title: String,
        message: String,
        alsoOpenEmail: Boolean = false
    ) {
        if (targetVendors.isEmpty() || title.isBlank() || message.isBlank()) {
            flash("Select vendor(s) and enter notification title and message")
            return
        }
        val cfg = _controlState.value.paymentGatewayConfig
        viewModelScope.launch {
            var webhookSentCount = 0
            targetVendors.forEach { vendor ->
                val remaining = vendor.remainingDays()
                val enrichedMessage = message
                    .replace("{vendor}", vendor.name)
                    .replace("{days}", if (remaining > 0) "$remaining days" else "Expired")
                    .replace("{autorenew}", if (vendor.autoRenew) "ON" else "OFF")

                val localPosted = VendorNotificationHelper.postSystemNotification(
                    context = context,
                    category = category,
                    title = "$title · ${vendor.name}",
                    body = enrichedMessage
                )
                var channelLabel = if (localPosted) "System Push + Firestore" else "Firestore Alert Log"

                if (cfg.pushWebhookUrl.isNotBlank()) {
                    val webhookRes = VendorNotificationHelper.dispatchPushWebhook(
                        webhookUrl = cfg.pushWebhookUrl,
                        bearerToken = cfg.pushWebhookToken,
                        vendorId = vendor.id,
                        vendorName = vendor.name,
                        recipientEmail = vendor.adminEmail,
                        recipientPhone = vendor.phone,
                        category = category,
                        title = title,
                        message = enrichedMessage
                    )
                    if (webhookRes.isSuccess) {
                        webhookSentCount++
                        channelLabel = "HTTPS Push Webhook + System"
                    }
                }

                commerceRepository.recordVendorNotification(
                    vendor = vendor,
                    category = category,
                    title = title,
                    message = enrichedMessage,
                    deliveryChannel = channelLabel
                )
            }

            if (alsoOpenEmail && targetVendors.size == 1) {
                val v = targetVendors.first()
                VendorNotificationHelper.launchVendorEmail(context, v.adminEmail, title, message)
            }

            flash(
                if (webhookSentCount > 0) "Pushed $category alert to ${targetVendors.size} vendor(s) (+$webhookSentCount webhook)"
                else "Dispatched $category notification to ${targetVendors.size} vendor(s)"
            )
        }
    }

    fun scanExpiringAndAutoRenewalsAndNotify(context: Context, vendors: List<Vendor>, activeOffers: List<PromoOfferDoc>) {
        val candidates = vendors.filter { it.remainingDays() <= 15 || !it.autoRenew }
        if (candidates.isEmpty()) {
            flash("All vendor subscriptions are healthy (>15 days & auto-renew active)")
            return
        }
        val offerHint = activeOffers.firstOrNull { it.isActive }?.let {
            " Use offer code ${it.code} for ${it.discountPercent}% off!"
        }.orEmpty()

        viewModelScope.launch {
            candidates.forEach { v ->
                val days = v.remainingDays()
                val category = if (days <= 15) "Subscription Remaining" else "Auto Renewal"
                val title = when {
                    days <= 0 -> "Subscription expired for ${v.name}"
                    days <= 15 -> "Subscription expires in $days day(s)"
                    else -> "Auto-Renewal disabled reminder"
                }
                val body = when {
                    days <= 0 -> "Your HealthSuite 360 subscription has expired. Renew online now.$offerHint"
                    v.autoRenew -> "$days day(s) remaining; auto-renewal is ON for uninterrupted access.$offerHint"
                    else -> "$days day(s) remaining and auto-renewal is OFF. Renew online or turn on auto-renew.$offerHint"
                }
                VendorNotificationHelper.postSystemNotification(context, category, title, body)
                commerceRepository.recordVendorNotification(
                    vendor = v,
                    category = category,
                    title = title,
                    message = body,
                    deliveryChannel = "Renewal Scanner + Push"
                )
            }
            flash("Dispatched ${candidates.size} subscription/auto-renewal notifications")
        }
    }

    // --- License Anti-Piracy & Sharing Enforcement ---

    fun recordLicenseDeviceCheckIn(license: VendorLicenseDoc, deviceId: String, extraIpJump: Boolean) {
        viewModelScope.launch {
            val res = commerceRepository.registerLicenseDeviceCheckIn(
                license = license,
                deviceFingerprint = deviceId,
                distinctIpIncrement = if (extraIpJump) 1 else 0
            )
            flash(
                res.fold(
                    onSuccess = { status -> "Device check-in recorded -> $status" },
                    onFailure = { it.localizedMessage ?: "Device check-in failed" }
                )
            )
        }
    }

    fun enforceSinglePrimaryDevice(license: VendorLicenseDoc) {
        viewModelScope.launch {
            val res = commerceRepository.enforceSinglePrimaryDevice(license)
            flash(if (res.isSuccess) "Revoked unauthorized extra devices for ${license.vendorName}" else "Failed to enforce device lock")
        }
    }

    fun revokeLicenseAndSuspendVendor(license: VendorLicenseDoc) {
        viewModelScope.launch {
            val res = commerceRepository.revokeLicenseAndSuspendVendor(license)
            flash(if (res.isSuccess) "Revoked ${license.licenseKey} & suspended ${license.vendorName}" else "Failed to suspend vendor")
        }
    }

    // --- Existing Ticket, Settings, CSV, Drive & WhatsApp ---

    fun resolveTicket(ticketId: String) {
        viewModelScope.launch {
            val result = repository.resolveTicket(ticketId)
            flash(if (result.isSuccess) "Ticket resolved and logged" else result.exceptionOrNull()?.localizedMessage ?: "Failed to resolve ticket")
        }
    }

    fun toggleSetting(item: PlatformSettingDoc) {
        viewModelScope.launch {
            val nextEnabled = !item.enabled
            val result = repository.toggleSetting(item.key, item.label, nextEnabled)
            if (result.isSuccess) {
                repository.recordAuditLog("${if (nextEnabled) "Enabled" else "Disabled"} setting: ${item.label}")
                flash("Updated ${item.label}")
            } else {
                flash(result.exceptionOrNull()?.localizedMessage ?: "Failed to save setting")
            }
        }
    }

    fun buildVendorsCsv(vendors: List<Vendor>): String {
        val header = "ID,Name,City,Type,Plan,Status,AdminEmail,BillingEmail,Phone,Address,Licenses,ActiveSeats,AutoRenew,DaysRemaining"
        val body = vendors.joinToString("\n") { v ->
            listOf(
                v.vendorCode.ifBlank { v.id }, v.name, v.city, v.type, v.plan, v.status,
                v.adminEmail, v.billingEmail, v.phone, v.address,
                v.purchasedLicenses.toString(), v.activeSeats.toString(), v.autoRenew.toString(), v.remainingDays().toString()
            ).joinToString(",") { cell ->
                val safe = sanitizeCsvCell(cell)
                "\"${safe.replace("\"", "\"\"")}\""
            }
        }
        return if (body.isEmpty()) header else "$header\n$body"
    }

    private fun sanitizeCsvCell(value: String): String {
        val cleaned = value.replace("\r", " ").replace("\n", " ")
        val firstNonSpace = cleaned.trimStart().firstOrNull()
        return if (firstNonSpace != null && firstNonSpace in charArrayOf('=', '+', '-', '@', '\t')) {
            "'$cleaned"
        } else {
            cleaned
        }
    }

    fun filterVendors(vendors: List<Vendor>): List<Vendor> {
        val state = _controlState.value
        val query = state.searchQuery.trim()
        return vendors.filter { v ->
            val matchesStatus = state.statusFilter == "All" || v.status.equals(state.statusFilter, ignoreCase = true)
            val matchesPlan = when (state.planFilter) {
                "All" -> true
                "Expiring <=15d" -> v.remainingDays() <= 15
                else -> v.plan.equals(state.planFilter, ignoreCase = true)
            }
            val matchesQuery = query.isEmpty() || listOf(
                v.vendorCode, v.id, v.name, v.city, v.type, v.plan, v.status,
                v.adminEmail, v.billingEmail, v.phone, v.address, v.assignedModulesCsv
            ).joinToString(" ").contains(query, ignoreCase = true)
            matchesStatus && matchesPlan && matchesQuery
        }
    }

    fun startDriveUpload(
        activity: Activity,
        vendors: List<Vendor>,
        onNeedConsent: (android.app.PendingIntent) -> Unit
    ) {
        val cachedToken = _controlState.value.pendingDriveAccessToken
        if (!cachedToken.isNullOrBlank()) {
            performDriveUpload(cachedToken, vendors)
            return
        }
        viewModelScope.launch {
            _controlState.update { it.copy(isDriveUploading = true, driveError = null) }
            when (val auth = driveUploader.requestDriveAuthorization(activity, currentUserEmail)) {
                is DriveAuthorizationOutcome.Authorized -> {
                    _controlState.update { it.copy(pendingDriveAccessToken = auth.accessToken) }
                    performDriveUpload(auth.accessToken, vendors)
                }
                is DriveAuthorizationOutcome.ResolutionRequired -> {
                    _controlState.update { it.copy(isDriveUploading = false) }
                    onNeedConsent(auth.pendingIntent)
                }
                is DriveAuthorizationOutcome.Failed -> {
                    _controlState.update { it.copy(isDriveUploading = false, driveError = auth.message) }
                    flash(auth.message)
                }
            }
        }
    }

    fun completeDriveUploadWithToken(accessToken: String, vendors: List<Vendor>) {
        _controlState.update { it.copy(pendingDriveAccessToken = accessToken, driveError = null) }
        performDriveUpload(accessToken, vendors)
    }

    fun onDriveConsentCancelled(message: String = "Google Drive permission was cancelled.") {
        _controlState.update { it.copy(isDriveUploading = false, driveError = message) }
        flash(message)
    }

    private fun performDriveUpload(accessToken: String, vendors: List<Vendor>) {
        viewModelScope.launch {
            _controlState.update { it.copy(isDriveUploading = true, driveError = null) }
            val fileName = "healthsuite-360-vendors-${System.currentTimeMillis() / 1000}.csv"
            val csv = buildVendorsCsv(vendors)
            val result = driveUploader.uploadCsvToDrive(
                accessToken = accessToken,
                expectedUserEmail = currentUserEmail,
                fileName = fileName,
                csvContent = csv
            )
            result.onSuccess { uploaded ->
                _controlState.update {
                    it.copy(isDriveUploading = false, lastDriveUpload = uploaded, driveError = null)
                }
                val ownerLabel = uploaded.ownerEmail.ifBlank { currentUserEmail }
                repository.recordAuditLog("Uploaded ${uploaded.fileName} to $ownerLabel Google Drive")
                flash("Uploaded ${uploaded.fileName} to your Google Drive ($ownerLabel)")
            }.onFailure { err ->
                val msg = err.localizedMessage ?: "Google Drive upload failed"
                _controlState.update {
                    it.copy(isDriveUploading = false, pendingDriveAccessToken = null, driveError = msg)
                }
                if (err is SecurityException) {
                    repository.recordAuditLog("Blocked unauthorized Google Drive backup attempt ($msg)", status = "Blocked")
                }
                flash(msg)
            }
        }
    }

    fun updateWhatsAppBridgeConfig(url: String, token: String) {
        val cleanUrl = url.trim()
        val cleanToken = token.trim()
        vault.putEncryptedString(bridgeUrlKey(currentUserId), cleanUrl)
        vault.putEncryptedString(bridgeTokenKey(currentUserId), cleanToken)
        _controlState.update { it.copy(whatsAppBridgeUrl = cleanUrl, whatsAppBridgeToken = cleanToken) }
        flash("WhatsApp bridge configuration saved")
    }

    fun refreshWhatsAppBridgeStatus() {
        val state = _controlState.value
        if (state.whatsAppBridgeUrl.isBlank()) {
            _controlState.update {
                it.copy(whatsAppStatus = WhatsAppBridgeStatus(statusMessage = "Enter your HTTPS WhatsApp bridge URL or scan a config QR"))
            }
            return
        }
        viewModelScope.launch {
            _controlState.update { it.copy(whatsAppStatus = it.whatsAppStatus.copy(isLoading = true)) }
            val res = whatsAppClient.fetchBridgeStatusAndQr(state.whatsAppBridgeUrl, state.whatsAppBridgeToken)
            res.onSuccess { status ->
                _controlState.update { it.copy(whatsAppStatus = status) }
                if (status.isConnected) {
                    repository.recordAuditLog("Verified WhatsApp link (${status.accountLabel ?: "Bridge"})")
                }
            }.onFailure { err ->
                _controlState.update {
                    it.copy(whatsAppStatus = WhatsAppBridgeStatus(false, statusMessage = err.localizedMessage ?: "Unable to reach WhatsApp bridge"))
                }
            }
        }
    }

    fun openQrScanner() {
        _controlState.update { it.copy(isQrScannerOpen = true) }
    }

    fun closeQrScanner() {
        _controlState.update { it.copy(isQrScannerOpen = false) }
    }

    fun handleScannedQr(rawPayload: String) {
        _controlState.update { it.copy(isQrScannerOpen = false) }
        val trimmed = rawPayload.trim()
        if (trimmed.isEmpty()) return
        if (trimmed.length > 2048) {
            flash("QR payload rejected: exceeds maximum length (2048 chars).")
            return
        }

        if (trimmed.startsWith("{")) {
            val parsed = runCatching {
                val obj = org.json.JSONObject(trimmed)
                val url = obj.optString("bridgeUrl").ifBlank { obj.optString("url") }.trim()
                val tok = obj.optString("token").trim()
                if (url.isNotBlank()) url to tok else null
            }.getOrNull()
            if (parsed != null) {
                if (NetworkUrlValidator.parseSafeHttpsUrl(parsed.first) == null) {
                    flash("Blocked unsafe or non-HTTPS WhatsApp bridge URL in QR payload.")
                    return
                }
                updateWhatsAppBridgeConfig(parsed.first, parsed.second)
                refreshWhatsAppBridgeStatus()
                return
            }
        }
        if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
            if (NetworkUrlValidator.parseSafeHttpsUrl(trimmed) == null) {
                flash("Blocked unsafe non-HTTPS or private bridge URL in QR payload.")
                return
            }
            updateWhatsAppBridgeConfig(trimmed, _controlState.value.whatsAppBridgeToken)
            refreshWhatsAppBridgeStatus()
            return
        }
        val currentUrl = _controlState.value.whatsAppBridgeUrl
        if (currentUrl.isBlank()) {
            flash("Configure HTTPS WhatsApp bridge URL before submitting a device-link QR")
            return
        }
        viewModelScope.launch {
            _controlState.update { it.copy(whatsAppStatus = it.whatsAppStatus.copy(isLoading = true)) }
            val result = whatsAppClient.submitScannedPairPayload(currentUrl, _controlState.value.whatsAppBridgeToken, trimmed)
            result.onSuccess { status ->
                _controlState.update { it.copy(whatsAppStatus = status) }
                repository.recordAuditLog("Submitted WhatsApp Link-Device QR to bridge")
                flash(status.statusMessage)
            }.onFailure { err ->
                val msg = err.localizedMessage ?: "Failed to submit QR payload"
                _controlState.update { it.copy(whatsAppStatus = it.whatsAppStatus.copy(isLoading = false, statusMessage = msg)) }
                flash(msg)
            }
        }
    }

    fun sendWhatsAppMessage(context: Context, phone: String, message: String, useDirectFallback: Boolean) {
        val bridgeUrl = _controlState.value.whatsAppBridgeUrl
        if (useDirectFallback || bridgeUrl.isBlank()) {
            val opened = whatsAppClient.openWhatsAppDirect(context, phone, message)
            if (opened) {
                viewModelScope.launch {
                    repository.recordAuditLog("Opened WhatsApp dispatch for ${phone.ifBlank { "contact" }}")
                }
            } else {
                flash("WhatsApp is not installed on this device")
            }
            return
        }
        viewModelScope.launch {
            val res = whatsAppClient.sendBridgeMessage(bridgeUrl, _controlState.value.whatsAppBridgeToken, phone, message)
            res.onSuccess { summary ->
                repository.recordAuditLog(summary)
                flash(summary)
            }.onFailure { err ->
                flash(err.localizedMessage ?: "Bridge message failed")
            }
        }
    }

    // --- Home Screen Shortcut & Web Portal PWA Installability ---

    fun pinHomeScreenShortcut(context: Context) {
        pwaInstallHelper.requestPinAppShortcut(context)
            .onSuccess { msg ->
                viewModelScope.launch {
                    repository.recordAuditLog("Requested Android Home Screen app shortcut")
                }
                flash(msg)
            }
            .onFailure { err ->
                flash(err.localizedMessage ?: "Unable to request home screen shortcut")
            }
    }

    fun inspectPwaPortal(portalUrl: String) {
        val clean = portalUrl.trim()
        _controlState.update { it.copy(pwaPortalUrl = clean, isCheckingPwa = true) }
        viewModelScope.launch {
            val res = pwaInstallHelper.inspectPwaInstallability(clean)
            res.onSuccess { report ->
                _controlState.update { it.copy(isCheckingPwa = false, pwaReport = report) }
                flash(report.summary)
            }.onFailure { err ->
                _controlState.update { it.copy(isCheckingPwa = false, pwaReport = null) }
                flash(err.localizedMessage ?: "PWA check failed")
            }
        }
    }

    fun openSafePwaPortal(context: Context, portalUrl: String) {
        pwaInstallHelper.openSafeWebPortal(context, portalUrl)
            .onFailure { err ->
                flash(err.localizedMessage ?: "Enter a valid HTTPS portal URL")
            }
    }

    fun flash(message: String) {
        toastJob?.cancel()
        _controlState.update { it.copy(toastMessage = message) }
        toastJob = viewModelScope.launch {
            delay(3200L)
            _controlState.update { it.copy(toastMessage = null) }
        }
    }

    companion object {
        private const val TAG = "SuperAdminVM"
        private fun bridgeUrlKey(uid: String) = "wa_bridge_url_$uid"
        private fun bridgeTokenKey(uid: String) = "wa_bridge_token_$uid"
        private fun gwUrlKey(uid: String) = "pg_api_url_$uid"
        private fun gwTokenKey(uid: String) = "pg_api_token_$uid"
        private fun gwUpiKey(uid: String) = "pg_upi_vpa_$uid"
        private fun gwMerchantKey(uid: String) = "pg_merchant_name_$uid"
        private fun pushUrlKey(uid: String) = "push_webhook_url_$uid"
        private fun pushTokenKey(uid: String) = "push_webhook_token_$uid"
    }
}
