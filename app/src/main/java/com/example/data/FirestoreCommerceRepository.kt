package com.example.data

import com.example.model.ModuleGroupDoc
import com.example.model.ModuleStage
import com.example.model.PaymentTransactionDoc
import com.example.model.PlatformModuleDoc
import com.example.model.PromoOfferDoc
import com.example.model.Vendor
import com.example.model.VendorLicenseDoc
import com.example.model.VendorNotificationDoc
import com.example.model.VendorStatus
import com.example.security.KeystoreVault
import com.example.security.LicenseSecurityEngine
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import java.util.Locale

class FirestoreCommerceRepository(
    private val db: FirebaseFirestore,
    private val adminRepo: FirestoreAdminRepository,
    private val auth: FirebaseAuth = Firebase.auth
) {
    private fun requireUserId(): String {
        return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("User must be signed in with Google.")
    }

    private fun userDoc(userId: String) = db.collection("users").document(userId)
    private fun modulesCol(userId: String) = userDoc(userId).collection("modules")
    private fun groupsCol(userId: String) = userDoc(userId).collection("moduleGroups")
    private fun offersCol(userId: String) = userDoc(userId).collection("offers")
    private fun licensesCol(userId: String) = userDoc(userId).collection("licenses")
    private fun paymentsCol(userId: String) = userDoc(userId).collection("payments")
    private fun notificationsCol(userId: String) = userDoc(userId).collection("notifications")
    private fun vendorsCol(userId: String) = userDoc(userId).collection("vendors")
    private fun auditLogsCol(userId: String) = userDoc(userId).collection("auditLogs")

    fun observeModules(userId: String): Flow<List<PlatformModuleDoc>> = adminRepo.observeCollection(
        userId = userId,
        path = "users/$userId/modules",
        query = modulesCol(userId).orderBy("createdAt", Query.Direction.ASCENDING),
        clazz = PlatformModuleDoc::class.java
    )

    fun observeModuleGroups(userId: String): Flow<List<ModuleGroupDoc>> = adminRepo.observeCollection(
        userId = userId,
        path = "users/$userId/moduleGroups",
        query = groupsCol(userId).orderBy("createdAt", Query.Direction.ASCENDING),
        clazz = ModuleGroupDoc::class.java
    )

    fun observeOffers(userId: String): Flow<List<PromoOfferDoc>> = adminRepo.observeCollection(
        userId = userId,
        path = "users/$userId/offers",
        query = offersCol(userId).orderBy("createdAt", Query.Direction.DESCENDING),
        clazz = PromoOfferDoc::class.java
    )

    fun observeLicenses(userId: String): Flow<List<VendorLicenseDoc>> = adminRepo.observeCollection(
        userId = userId,
        path = "users/$userId/licenses",
        query = licensesCol(userId).orderBy("createdAt", Query.Direction.DESCENDING),
        clazz = VendorLicenseDoc::class.java
    )

    fun observePayments(userId: String): Flow<List<PaymentTransactionDoc>> = adminRepo.observeCollection(
        userId = userId,
        path = "users/$userId/payments",
        query = paymentsCol(userId).orderBy("createdAt", Query.Direction.DESCENDING),
        clazz = PaymentTransactionDoc::class.java
    )

    fun observeNotifications(userId: String): Flow<List<VendorNotificationDoc>> = adminRepo.observeCollection(
        userId = userId,
        path = "users/$userId/notifications",
        query = notificationsCol(userId).orderBy("createdAt", Query.Direction.DESCENDING),
        clazz = VendorNotificationDoc::class.java
    )

    suspend fun updateModulePricing(
        moduleId: String,
        monthlyPrice: Double,
        annualPrice: Double,
        discountPercent: Int,
        licenseAllEligible: Boolean,
        stage: String
    ): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/modules/$moduleId"
        return try {
            val uid = requireUserId()
            require(monthlyPrice >= 0.0 && annualPrice >= 0.0) { "Prices cannot be negative" }
            val safeStage = if (stage in ModuleStage.entries.map { it.label }) stage else ModuleStage.Live.label
            val docRef = modulesCol(uid).document(moduleId)
            path = docRef.path

            val batch = db.batch()
            batch.update(
                docRef,
                mapOf(
                    "monthlyPrice" to monthlyPrice,
                    "annualPrice" to annualPrice,
                    "discountPercent" to discountPercent.coerceIn(0, 95),
                    "licenseAllEligible" to licenseAllEligible,
                    "stage" to safeStage,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                auditLogsCol(uid).document(),
                mapOf(
                    "userId" to uid,
                    "message" to "Updated module $moduleId pricing (₹$monthlyPrice/mo, $discountPercent% off)".take(240),
                    "status" to "Success",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun createModule(
        name: String,
        description: String,
        monthlyPrice: Double,
        annualPrice: Double,
        discountPercent: Int,
        licenseAllEligible: Boolean
    ): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/modules"
        return try {
            val uid = requireUserId()
            val cleanName = name.trim()
            require(cleanName.isNotEmpty() && cleanName.length <= 100) { "Enter module name" }
            val code = "MOD-${cleanName.uppercase(Locale.ROOT).filter { it.isLetterOrDigit() }.take(8)}-${System.currentTimeMillis() % 1000}"
            val docRef = modulesCol(uid).document(code)
            path = docRef.path

            val batch = db.batch()
            batch.set(
                docRef,
                mapOf(
                    "userId" to uid,
                    "moduleCode" to code,
                    "name" to cleanName,
                    "description" to description.trim().ifBlank { "HealthSuite 360 licensed module" }.take(240),
                    "stage" to ModuleStage.Live.label,
                    "monthlyPrice" to monthlyPrice.coerceAtLeast(0.0),
                    "annualPrice" to annualPrice.coerceAtLeast(0.0),
                    "currency" to "INR",
                    "discountPercent" to discountPercent.coerceIn(0, 95),
                    "licenseAllEligible" to licenseAllEligible,
                    "isActive" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                auditLogsCol(uid).document(),
                mapOf(
                    "userId" to uid,
                    "message" to "Created licensable module $cleanName ($code)".take(240),
                    "status" to "Success",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun createModuleGroup(
        name: String,
        description: String,
        selectedModules: List<String>,
        includesAllModules: Boolean,
        bundlePrice: Double,
        discountPercent: Int,
        maxSeatsDefault: Int
    ): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/moduleGroups"
        return try {
            val uid = requireUserId()
            val cleanName = name.trim()
            require(cleanName.isNotEmpty()) { "Enter module group name" }
            val csv = if (includesAllModules) "ALL_MODULES" else selectedModules.joinToString(",")
            require(csv.isNotBlank()) { "Select at least one module or enable All Modules" }

            val code = "GRP-${System.currentTimeMillis() % 100000}"
            val docRef = groupsCol(uid).document(code)
            path = docRef.path

            val batch = db.batch()
            batch.set(
                docRef,
                mapOf(
                    "userId" to uid,
                    "groupCode" to code,
                    "name" to cleanName.take(120),
                    "description" to description.trim().ifBlank { "Custom module licensing group" }.take(240),
                    "moduleCodesCsv" to csv.take(600),
                    "bundlePrice" to bundlePrice.coerceAtLeast(0.0),
                    "discountPercent" to discountPercent.coerceIn(0, 95),
                    "includesAllModules" to includesAllModules,
                    "maxSeatsDefault" to maxSeatsDefault.coerceIn(1, 5000),
                    "isActive" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                auditLogsCol(uid).document(),
                mapOf(
                    "userId" to uid,
                    "message" to "Created licensing group $cleanName ($csv)".take(240),
                    "status" to "Success",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun createOffer(
        code: String,
        title: String,
        description: String,
        discountPercent: Int,
        targetScope: String,
        validDays: Int
    ): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/offers"
        return try {
            val uid = requireUserId()
            val cleanCode = code.trim().uppercase(Locale.ROOT).filter { it.isLetterOrDigit() || it == '_' || it == '-' }
            require(cleanCode.length >= 2 && cleanCode.length <= 32) { "Offer code must be 2-32 alphanumeric chars" }
            require(title.trim().isNotEmpty()) { "Enter offer title" }
            val docRef = offersCol(uid).document(cleanCode)
            path = docRef.path
            val validUntil = System.currentTimeMillis() + validDays.coerceIn(1, 365) * 86_400_000L

            val batch = db.batch()
            batch.set(
                docRef,
                mapOf(
                    "userId" to uid,
                    "code" to cleanCode,
                    "title" to title.trim().take(120),
                    "description" to description.trim().ifBlank { "Promotional licensing discount" }.take(240),
                    "discountPercent" to discountPercent.coerceIn(1, 95),
                    "targetScope" to targetScope.take(60),
                    "validUntilMillis" to validUntil,
                    "isActive" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                auditLogsCol(uid).document(),
                mapOf(
                    "userId" to uid,
                    "message" to "Created offer $cleanCode ($discountPercent% off)".take(240),
                    "status" to "Success",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleOfferActive(offer: PromoOfferDoc): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/offers/${offer.id}"
        return try {
            val uid = requireUserId()
            val docRef = offersCol(uid).document(offer.id)
            path = docRef.path
            docRef.update(
                mapOf(
                    "isActive" to !offer.isActive,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun createPendingPayment(
        orderRef: String,
        vendor: Vendor,
        itemTitle: String,
        scopeType: String,
        seats: Int,
        billingCycle: String,
        promoCode: String,
        subtotalAmount: Double,
        discountAmount: Double,
        finalAmount: Double,
        currency: String,
        gatewayMode: String,
        gatewayRef: String,
        checkoutUrl: String,
        autoRenewEnabled: Boolean
    ): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/payments/$orderRef"
        return try {
            val uid = requireUserId()
            val docRef = paymentsCol(uid).document(orderRef)
            path = docRef.path
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "orderRef" to orderRef,
                    "vendorId" to vendor.id,
                    "vendorName" to vendor.name,
                    "itemTitle" to itemTitle.take(160),
                    "scopeType" to scopeType.take(40),
                    "seats" to seats.coerceIn(1, 5000),
                    "billingCycle" to if (billingCycle == "Annual") "Annual" else "Monthly",
                    "promoCode" to promoCode.take(40),
                    "subtotalAmount" to subtotalAmount,
                    "discountAmount" to discountAmount,
                    "finalAmount" to finalAmount,
                    "currency" to currency.take(8),
                    "gatewayMode" to gatewayMode.take(40),
                    "status" to "Pending",
                    "gatewayRef" to gatewayRef.take(140),
                    "checkoutUrl" to checkoutUrl.take(600),
                    "autoRenewEnabled" to autoRenewEnabled,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun finalizePaidOrderAndIssueLicense(
        payment: PaymentTransactionDoc,
        vendor: Vendor,
        gatewayRef: String,
        assignedScopeModules: String,
        vault: KeystoreVault
    ): Result<String> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/payments/${payment.id}"
        return try {
            val uid = requireUserId()
            val payRef = paymentsCol(uid).document(payment.id)
            path = payRef.path
            val daysToAdd = if (payment.billingCycle == "Annual") 365L else 30L
            val baseMillis = maxOf(System.currentTimeMillis(), vendor.subscriptionExpiresAtMillis)
            val newExpiry = baseMillis + daysToAdd * 86_400_000L

            val licenseKey = LicenseSecurityEngine.generateSignedLicenseKey(
                vault = vault,
                vendorCode = vendor.vendorCode.ifBlank { vendor.id },
                scopeCode = payment.scopeType,
                seats = payment.seats
            )
            val licenseDocId = "LIC-${payment.orderRef.filter { it.isLetterOrDigit() }.take(24)}"

            val existingModules = vendor.assignedModulesCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val addedModules = assignedScopeModules.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val mergedModules = (existingModules + addedModules).distinct().joinToString(",").take(600)

            val batch = db.batch()
            batch.update(
                payRef,
                mapOf(
                    "status" to "Paid",
                    "gatewayRef" to gatewayRef.ifBlank { payment.orderRef }.take(140),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                vendorsCol(uid).document(vendor.id),
                mapOf(
                    "status" to VendorStatus.Active.label,
                    "purchasedLicenses" to (vendor.purchasedLicenses + payment.seats).coerceAtMost(5000),
                    "autoRenew" to payment.autoRenewEnabled,
                    "subscriptionExpiresAtMillis" to newExpiry,
                    "assignedModulesCsv" to mergedModules.ifBlank { payment.itemTitle.take(120) },
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                licensesCol(uid).document(licenseDocId),
                mapOf(
                    "userId" to uid,
                    "licenseKey" to licenseKey,
                    "vendorId" to vendor.id,
                    "vendorName" to vendor.name,
                    "vendorEmail" to vendor.adminEmail,
                    "scopeName" to payment.itemTitle.take(160),
                    "purchasedSeats" to payment.seats.coerceIn(1, 5000),
                    "activeDevicesCsv" to "primary_terminal_${vendor.vendorCode.lowercase(Locale.ROOT)}",
                    "activeDeviceCount" to 1,
                    "distinctIpCount" to 1,
                    "violationStatus" to "Compliant",
                    "lastHeartbeatMillis" to System.currentTimeMillis(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                auditLogsCol(uid).document(),
                mapOf(
                    "userId" to uid,
                    "message" to "Verified payment ${payment.orderRef} (₹${payment.finalAmount}) & issued $licenseKey".take(240),
                    "status" to "Success",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(licenseKey)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun markPaymentFailed(paymentId: String, reason: String): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/payments/$paymentId"
        return try {
            val uid = requireUserId()
            val payRef = paymentsCol(uid).document(paymentId)
            path = payRef.path
            val batch = db.batch()
            batch.update(
                payRef,
                mapOf(
                    "status" to "Failed",
                    "gatewayRef" to reason.take(140),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                auditLogsCol(uid).document(),
                mapOf(
                    "userId" to uid,
                    "message" to "Payment $paymentId failed: $reason".take(240),
                    "status" to "Warning",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun registerLicenseDeviceCheckIn(
        license: VendorLicenseDoc,
        deviceFingerprint: String,
        distinctIpIncrement: Int
    ): Result<String> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/licenses/${license.id}"
        return try {
            val uid = requireUserId()
            val cleanFp = deviceFingerprint.trim().filter { it.isLetterOrDigit() || it == '_' || it == '-' }.take(40)
            require(cleanFp.isNotEmpty()) { "Enter a valid device fingerprint ID" }

            val devices = (license.activeDeviceList() + cleanFp).distinct()
            val newDeviceCount = devices.size
            val newIpCount = (license.distinctIpCount + distinctIpIncrement.coerceAtLeast(0)).coerceAtLeast(1)
            val eval = LicenseSecurityEngine.evaluateTelemetry(
                purchasedSeats = license.purchasedSeats,
                activeDeviceCount = newDeviceCount,
                distinctIpCount = newIpCount
            )

            val batch = db.batch()
            val licRef = licensesCol(uid).document(license.id)
            path = licRef.path
            batch.update(
                licRef,
                mapOf(
                    "activeDevicesCsv" to devices.joinToString(",").take(1000),
                    "activeDeviceCount" to newDeviceCount,
                    "distinctIpCount" to newIpCount,
                    "violationStatus" to eval.status,
                    "lastHeartbeatMillis" to System.currentTimeMillis(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                vendorsCol(uid).document(license.vendorId),
                mapOf(
                    "activeSeats" to newDeviceCount,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                auditLogsCol(uid).document(),
                mapOf(
                    "userId" to uid,
                    "message" to "License check-in ${license.licenseKey}: ${eval.status} ($newDeviceCount/${license.purchasedSeats} seats)".take(240),
                    "status" to if (eval.status == "Unauthorized Sharing") "Blocked" else "Success",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(eval.status)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun enforceSinglePrimaryDevice(license: VendorLicenseDoc): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/licenses/${license.id}"
        return try {
            val uid = requireUserId()
            val primaryDevice = license.activeDeviceList().firstOrNull() ?: "primary_device"
            val batch = db.batch()
            val licRef = licensesCol(uid).document(license.id)
            path = licRef.path
            batch.update(
                licRef,
                mapOf(
                    "activeDevicesCsv" to primaryDevice,
                    "activeDeviceCount" to 1,
                    "distinctIpCount" to 1,
                    "violationStatus" to "Compliant",
                    "lastHeartbeatMillis" to System.currentTimeMillis(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                vendorsCol(uid).document(license.vendorId),
                mapOf(
                    "activeSeats" to 1,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                auditLogsCol(uid).document(),
                mapOf(
                    "userId" to uid,
                    "message" to "Revoked extra devices on ${license.licenseKey}; locked to $primaryDevice".take(240),
                    "status" to "Success",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun revokeLicenseAndSuspendVendor(license: VendorLicenseDoc): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/licenses/${license.id}"
        return try {
            val uid = requireUserId()
            val batch = db.batch()
            val licRef = licensesCol(uid).document(license.id)
            path = licRef.path
            batch.update(
                licRef,
                mapOf(
                    "violationStatus" to "Revoked",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                vendorsCol(uid).document(license.vendorId),
                mapOf(
                    "status" to VendorStatus.Suspended.label,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                auditLogsCol(uid).document(),
                mapOf(
                    "userId" to uid,
                    "message" to "Revoked ${license.licenseKey} & suspended ${license.vendorName} for unauthorized sharing".take(240),
                    "status" to "Blocked",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun recordVendorNotification(
        vendor: Vendor,
        category: String,
        title: String,
        message: String,
        deliveryChannel: String,
        status: String = "Sent"
    ): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/notifications"
        return try {
            val uid = requireUserId()
            val notifRef = notificationsCol(uid).document()
            path = notifRef.path
            val batch = db.batch()
            batch.set(
                notifRef,
                mapOf(
                    "userId" to uid,
                    "vendorId" to vendor.id,
                    "vendorName" to vendor.name,
                    "recipientEmail" to vendor.adminEmail.take(140),
                    "recipientPhone" to vendor.phone.take(32),
                    "category" to category.take(60),
                    "title" to title.take(160),
                    "message" to message.take(600),
                    "deliveryChannel" to deliveryChannel.take(80),
                    "status" to status.take(40),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                auditLogsCol(uid).document(),
                mapOf(
                    "userId" to uid,
                    "message" to "Sent $category alert to ${vendor.name}: $title".take(240),
                    "status" to "Success",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }
}
