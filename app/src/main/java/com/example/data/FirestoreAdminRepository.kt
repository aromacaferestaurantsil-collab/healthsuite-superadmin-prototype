package com.example.data

import android.content.Context
import android.util.Log
import com.example.R
import com.example.model.AuditLogEntry
import com.example.model.PlatformSettingDoc
import com.example.model.SupportTicket
import com.example.model.Vendor
import com.example.model.VendorStatus
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.security.SecureRandom

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write")
}

fun handleFirestoreError(
    exception: Throwable,
    operationType: OperationType,
    path: String?
): String {
    val causes = generateSequence(exception) { it.cause }
    val rootError = causes.firstOrNull { it is FirebaseFirestoreException }
        ?: causes.firstOrNull { it !is CancellationException }
        ?: exception

    val currentUser = Firebase.auth.currentUser
    val providerInfoList = currentUser?.providerData?.map { provider ->
        JSONObject().apply {
            put("providerId", provider.providerId)
            put("email", provider.email)
        }
    } ?: emptyList()

    val authInfoJson = JSONObject().apply {
        put("userId", currentUser?.uid)
        put("email", currentUser?.email)
        put("emailVerified", currentUser?.isEmailVerified)
        put("tenantId", currentUser?.tenantId)
        put("providerInfo", JSONArray(providerInfoList))
    }

    val errorInfoJson = JSONObject().apply {
        put("error", rootError.message ?: rootError.toString())
        put("operationType", operationType.value)
        put("path", path)
        put("authInfo", authInfoJson)
    }

    val jsonString = errorInfoJson.toString()
    Log.e("FirestoreError", "Firestore Error: $jsonString")
    return jsonString
}

class FirestoreAdminRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun userDoc(userId: String) = db.collection("users").document(userId)
    private fun vendorsCol(userId: String) = userDoc(userId).collection("vendors")
    private fun ticketsCol(userId: String) = userDoc(userId).collection("tickets")
    private fun auditLogsCol(userId: String) = userDoc(userId).collection("auditLogs")
    private fun settingsCol(userId: String) = userDoc(userId).collection("settings")
    private fun modulesCol(userId: String) = userDoc(userId).collection("modules")
    private fun moduleGroupsCol(userId: String) = userDoc(userId).collection("moduleGroups")
    private fun offersCol(userId: String) = userDoc(userId).collection("offers")
    private fun licensesCol(userId: String) = userDoc(userId).collection("licenses")

    fun observeVendors(userId: String): Flow<List<Vendor>> = observeCollection(
        userId = userId,
        path = "users/$userId/vendors",
        query = vendorsCol(userId).orderBy("createdAt", Query.Direction.DESCENDING),
        clazz = Vendor::class.java
    )

    fun observeTickets(userId: String): Flow<List<SupportTicket>> = observeCollection(
        userId = userId,
        path = "users/$userId/tickets",
        query = ticketsCol(userId).orderBy("createdAt", Query.Direction.DESCENDING),
        clazz = SupportTicket::class.java
    )

    fun observeAuditLogs(userId: String): Flow<List<AuditLogEntry>> = observeCollection(
        userId = userId,
        path = "users/$userId/auditLogs",
        query = auditLogsCol(userId).orderBy("createdAt", Query.Direction.DESCENDING),
        clazz = AuditLogEntry::class.java
    )

    fun observeSettings(userId: String): Flow<List<PlatformSettingDoc>> = observeCollection(
        userId = userId,
        path = "users/$userId/settings",
        query = settingsCol(userId),
        clazz = PlatformSettingDoc::class.java
    )

    internal fun <T : Any> observeCollection(
        userId: String,
        path: String,
        query: Query,
        clazz: Class<T>
    ): Flow<List<T>> = flow {
        require(userId.isNotBlank()) { "Authenticated userId required" }
        emitAll(
            query.snapshots()
                .map { snap ->
                    snap.toObjects(clazz, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun seedWorkspaceIfEmpty(): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid"
        return try {
            val now = System.currentTimeMillis()
            val dayMs = 86_400_000L
            val existingVendors = vendorsCol(uid).limit(10).get().await()
            val existingModules = modulesCol(uid).limit(1).get().await()

            val batch = db.batch()
            var hasWrites = false

            if (existingVendors.isEmpty) {
                hasWrites = true
                data class SeedV(
                    val code: String, val name: String, val city: String, val type: String,
                    val plan: String, val status: String, val adminEmail: String,
                    val billingEmail: String, val phone: String, val address: String,
                    val seats: Int, val activeSeats: Int, val autoRenew: Boolean,
                    val daysLeft: Int, val modules: String
                )
                val seedVendors = listOf(
                    SeedV(
                        "VEN-1001", "Aarogya Medical Centre", "Guwahati", "Hospital",
                        "Enterprise", "Active", "admin@aarogyamed.org", "billing@aarogyamed.org",
                        "+919864011223", "GS Road, Bhangagarh, Guwahati 781005",
                        5, 4, true, 42, "PharmaSuite,LabSuite,DoctorSuite"
                    ),
                    SeedV(
                        "VEN-1002", "North East Diagnostics", "Silchar", "Laboratory",
                        "Growth", "Pending", "ops@nediagnostics.in", "accounts@nediagnostics.in",
                        "+919864022334", "Club Road, Silchar 788001",
                        2, 1, false, 6, "LabSuite"
                    ),
                    SeedV(
                        "VEN-1003", "CarePlus Pharmacy", "Dibrugarh", "Pharmacy",
                        "Starter", "Active", "owner@carepluspharma.in", "finance@carepluspharma.in",
                        "+919864033445", "Thana Chariali, Dibrugarh 786001",
                        1, 3, true, 9, "PharmaSuite"
                    ),
                    SeedV(
                        "VEN-1004", "LifeSpring Clinic", "Jorhat", "Clinic",
                        "Growth", "Suspended", "director@lifespringclinic.org", "billing@lifespringclinic.org",
                        "+919864044556", "AT Road, Jorhat 785001",
                        1, 1, false, -2, "DoctorSuite"
                    )
                )
                seedVendors.forEach { v ->
                    batch.set(
                        vendorsCol(uid).document(v.code),
                        mapOf(
                            "userId" to uid,
                            "vendorCode" to v.code,
                            "name" to v.name,
                            "city" to v.city,
                            "type" to v.type,
                            "plan" to v.plan,
                            "status" to v.status,
                            "adminEmail" to v.adminEmail,
                            "billingEmail" to v.billingEmail,
                            "phone" to v.phone,
                            "address" to v.address,
                            "purchasedLicenses" to v.seats,
                            "activeSeats" to v.activeSeats,
                            "autoRenew" to v.autoRenew,
                            "subscriptionExpiresAtMillis" to (now + v.daysLeft * dayMs),
                            "assignedModulesCsv" to v.modules,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }

                val seedTickets = listOf(
                    listOf("TKT-2048", "Unable to renew subscription", "CarePlus Pharmacy", "Open", "High"),
                    listOf("TKT-2047", "Lab report export formatting", "North East Diagnostics", "In progress", "Normal")
                )
                seedTickets.forEach { t ->
                    batch.set(
                        ticketsCol(uid).document(t[0]),
                        mapOf(
                            "userId" to uid,
                            "ticketCode" to t[0],
                            "title" to t[1],
                            "vendor" to t[2],
                            "status" to t[3],
                            "priority" to t[4],
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }

                val seedSettings = listOf(
                    Triple("mfa", "Enforce 2-Step Verification on sign-in", true),
                    Triple("self_reg", "Allow vendor self-registration", false),
                    Triple("flag_secure", "Block screenshots & screen capture", true),
                    Triple("alerts", "Critical platform alerts", true)
                )
                seedSettings.forEach { (key, label, enabled) ->
                    batch.set(
                        settingsCol(uid).document(key),
                        mapOf(
                            "userId" to uid,
                            "label" to label,
                            "enabled" to enabled,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            } else {
                // Backfill earlier vendor docs lacking contact/subscription fields so rules updates stay compliant
                existingVendors.documents.forEach { doc ->
                    if (!doc.contains("adminEmail") || !doc.contains("purchasedLicenses")) {
                        hasWrites = true
                        val code = doc.getString("vendorCode") ?: doc.id
                        val slug = code.lowercase().replace("-", "")
                        batch.update(
                            doc.reference,
                            mapOf(
                                "adminEmail" to (doc.getString("adminEmail") ?: "admin@$slug.healthsuite.org"),
                                "billingEmail" to (doc.getString("billingEmail") ?: "billing@$slug.healthsuite.org"),
                                "phone" to (doc.getString("phone") ?: "+919864000000"),
                                "address" to (doc.getString("address") ?: "${doc.getString("city") ?: "Main Road"} Medical District"),
                                "purchasedLicenses" to (doc.getLong("purchasedLicenses") ?: 1L),
                                "activeSeats" to (doc.getLong("activeSeats") ?: 1L),
                                "autoRenew" to (doc.getBoolean("autoRenew") ?: true),
                                "subscriptionExpiresAtMillis" to (doc.getLong("subscriptionExpiresAtMillis") ?: (now + 12 * dayMs)),
                                "assignedModulesCsv" to (doc.getString("assignedModulesCsv") ?: "PharmaSuite"),
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                }
            }

            if (existingModules.isEmpty) {
                hasWrites = true
                data class SeedMod(
                    val code: String, val name: String, val desc: String,
                    val stage: String, val monthly: Double, val annual: Double, val disc: Int
                )
                val seedModules = listOf(
                    SeedMod("MOD-PHARMA", "PharmaSuite", "Pharmacy billing, inventory & batch expiry", "Live", 1499.0, 14990.0, 10),
                    SeedMod("MOD-LAB", "LabSuite", "Laboratory reporting, barcoding & HL7 sync", "Live", 1999.0, 19990.0, 0),
                    SeedMod("MOD-PATIENT", "PatientSuite", "OPD appointments, queue & patient portal", "Beta", 1299.0, 12990.0, 15),
                    SeedMod("MOD-DOCTOR", "DoctorSuite", "Clinical EMR, e-prescription & telehealth", "Beta", 1799.0, 17990.0, 5),
                    SeedMod("MOD-ANALYTICS", "HealthAnalytics", "Executive KPI, revenue & cohort insights", "Planned", 2499.0, 24990.0, 0),
                    SeedMod("MOD-CARE", "Care Connect", "Multi-facility referrals & follow-up workflows", "Planned", 1599.0, 15990.0, 0)
                )
                seedModules.forEach { m ->
                    batch.set(
                        modulesCol(uid).document(m.code),
                        mapOf(
                            "userId" to uid,
                            "moduleCode" to m.code,
                            "name" to m.name,
                            "description" to m.desc,
                            "stage" to m.stage,
                            "monthlyPrice" to m.monthly,
                            "annualPrice" to m.annual,
                            "currency" to "INR",
                            "discountPercent" to m.disc,
                            "licenseAllEligible" to true,
                            "isActive" to true,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }

                // Seed Module Groups including an All-Modules Licensing group
                batch.set(
                    moduleGroupsCol(uid).document("GRP-CORE"),
                    mapOf(
                        "userId" to uid,
                        "groupCode" to "GRP-CORE",
                        "name" to "Clinical Core Bundle",
                        "description" to "PharmaSuite + LabSuite + DoctorSuite bundle",
                        "moduleCodesCsv" to "PharmaSuite,LabSuite,DoctorSuite",
                        "bundlePrice" to 4299.0,
                        "discountPercent" to 15,
                        "includesAllModules" to false,
                        "maxSeatsDefault" to 3,
                        "isActive" to true,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.set(
                    moduleGroupsCol(uid).document("GRP-ALL"),
                    mapOf(
                        "userId" to uid,
                        "groupCode" to "GRP-ALL",
                        "name" to "All Modules Enterprise Suite",
                        "description" to "Full access to all HealthSuite 360 licensable modules",
                        "moduleCodesCsv" to "ALL_MODULES",
                        "bundlePrice" to 7999.0,
                        "discountPercent" to 25,
                        "includesAllModules" to true,
                        "maxSeatsDefault" to 5,
                        "isActive" to true,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                // Seed Promotional Offers
                batch.set(
                    offersCol(uid).document("HEALTH20"),
                    mapOf(
                        "userId" to uid,
                        "code" to "HEALTH20",
                        "title" to "20% Off All-Modules Suite",
                        "description" to "Upgrade or renew full multi-module licensing with 20% off",
                        "discountPercent" to 20,
                        "targetScope" to "All Modules",
                        "validUntilMillis" to (now + 21 * dayMs),
                        "isActive" to true,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.set(
                    offersCol(uid).document("RENEW15"),
                    mapOf(
                        "userId" to uid,
                        "code" to "RENEW15",
                        "title" to "15% Early Auto-Renewal Bonus",
                        "description" to "Enable auto-renewal before expiry to unlock 15% off",
                        "discountPercent" to 15,
                        "targetScope" to "Renewal",
                        "validUntilMillis" to (now + 30 * dayMs),
                        "isActive" to true,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                // Seed License Telemetry (including single-license multi-device sharing case for CarePlus Pharmacy)
                batch.set(
                    licensesCol(uid).document("LIC-1001"),
                    mapOf(
                        "userId" to uid,
                        "licenseKey" to "HS360-VEN1001-4A19-8F2C",
                        "vendorId" to "VEN-1001",
                        "vendorName" to "Aarogya Medical Centre",
                        "vendorEmail" to "admin@aarogyamed.org",
                        "scopeName" to "All Modules Enterprise Suite",
                        "purchasedSeats" to 5,
                        "activeDevicesCsv" to "dev_hosp_01,dev_hosp_02,dev_hosp_03,dev_hosp_04",
                        "activeDeviceCount" to 4,
                        "distinctIpCount" to 2,
                        "violationStatus" to "Compliant",
                        "lastHeartbeatMillis" to (now - 3600_000L),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.set(
                    licensesCol(uid).document("LIC-1003"),
                    mapOf(
                        "userId" to uid,
                        "licenseKey" to "HS360-VEN1003-9E02-C41B",
                        "vendorId" to "VEN-1003",
                        "vendorName" to "CarePlus Pharmacy",
                        "vendorEmail" to "owner@carepluspharma.in",
                        "scopeName" to "PharmaSuite (Single License)",
                        "purchasedSeats" to 1,
                        "activeDevicesCsv" to "dev_dib_counter1,dev_guw_ext9,dev_jor_laptop3",
                        "activeDeviceCount" to 3,
                        "distinctIpCount" to 3,
                        "violationStatus" to "Unauthorized Sharing",
                        "lastHeartbeatMillis" to (now - 900_000L),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            if (hasWrites) {
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun addVendor(
        name: String,
        city: String,
        type: String,
        plan: String = "Starter",
        autoRenew: Boolean = true,
        adminEmail: String,
        billingEmail: String,
        phone: String,
        address: String,
        currentVendorCount: Int
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/vendors"
        return try {
            val uid = requireUserId()
            val cleanName = name.trim()
            val cleanCity = city.trim().ifBlank { "Not specified" }
            val safeType = if (type in listOf("Hospital", "Laboratory", "Pharmacy", "Clinic")) type else "Clinic"
            val safePlan = if (plan in listOf("Enterprise", "Growth", "Starter")) plan else "Starter"
            val cleanAdminEmail = adminEmail.trim()
            val cleanBillingEmail = billingEmail.trim().ifBlank { cleanAdminEmail }
            val cleanPhone = phone.trim()
            val cleanAddress = address.trim()

            require(cleanName.isNotEmpty() && cleanName.length <= 120) { "Enter a valid organisation name" }
            require(cleanAdminEmail.contains("@") && cleanAdminEmail.length <= 140) { "Enter a valid admin email ID" }
            require(cleanPhone.filter { it.isDigit() }.length >= 8) { "Enter a valid phone number" }
            require(cleanAddress.isNotEmpty() && cleanAddress.length <= 300) { "Enter the vendor address" }

            val randomSuffix = SecureRandom().nextInt(90) + 10
            val code = "VEN-${(System.currentTimeMillis() % 100_000_000L).toString().padStart(8, '0')}-$randomSuffix"
            val docRef = vendorsCol(uid).document(code)
            docPath = docRef.path
            val expiresAt = System.currentTimeMillis() + 30 * 86_400_000L

            val batch = db.batch()
            batch.set(
                docRef,
                mapOf(
                    "userId" to uid,
                    "vendorCode" to code,
                    "name" to cleanName,
                    "city" to cleanCity.take(80),
                    "type" to safeType,
                    "plan" to safePlan,
                    "status" to VendorStatus.Pending.label,
                    "adminEmail" to cleanAdminEmail,
                    "billingEmail" to cleanBillingEmail.take(140),
                    "phone" to cleanPhone.take(32),
                    "address" to cleanAddress,
                    "purchasedLicenses" to 1,
                    "activeSeats" to 1,
                    "autoRenew" to autoRenew,
                    "subscriptionExpiresAtMillis" to expiresAt,
                    "assignedModulesCsv" to when (safePlan) {
                        "Enterprise" -> "PharmaSuite,LabSuite,DoctorSuite"
                        "Growth" -> "LabSuite,DoctorSuite"
                        else -> "PharmaSuite"
                    },
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            val logRef = auditLogsCol(uid).document()
            batch.set(
                logRef,
                mapOf(
                    "userId" to uid,
                    "message" to "Registered vendor $cleanName ($code) · $cleanAdminEmail".take(240),
                    "status" to "Success",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateVendorContactDetails(
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
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/vendors/$vendorId"
        return try {
            val uid = requireUserId()
            require(vendorId.isNotBlank()) { "Vendor ID required" }
            val cleanName = name.trim()
            require(cleanName.isNotEmpty() && cleanName.length <= 120) { "Enter a valid organisation name" }
            val safeType = if (type in listOf("Hospital", "Laboratory", "Pharmacy", "Clinic")) type else "Clinic"
            val safePlan = if (plan in listOf("Enterprise", "Growth", "Starter")) plan else "Starter"
            require(adminEmail.trim().contains("@")) { "Enter a valid admin email ID" }
            require(phone.trim().filter { it.isDigit() }.length >= 8) { "Enter a valid phone number" }
            require(address.trim().isNotEmpty()) { "Enter vendor address" }

            val docRef = vendorsCol(uid).document(vendorId)
            docPath = docRef.path
            val batch = db.batch()
            batch.update(
                docRef,
                mapOf(
                    "name" to cleanName,
                    "type" to safeType,
                    "plan" to safePlan,
                    "city" to city.trim().ifBlank { "Not specified" }.take(80),
                    "adminEmail" to adminEmail.trim().take(140),
                    "billingEmail" to billingEmail.trim().ifBlank { adminEmail.trim() }.take(140),
                    "phone" to phone.trim().take(32),
                    "address" to address.trim().take(300),
                    "autoRenew" to autoRenew,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            val logRef = auditLogsCol(uid).document()
            batch.set(
                logRef,
                mapOf(
                    "userId" to uid,
                    "message" to "Updated profile, plan ($safePlan) & contact for $cleanName ($vendorId)".take(240),
                    "status" to "Success",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun toggleVendorAutoRenew(vendorId: String, autoRenew: Boolean): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/vendors/$vendorId"
        return try {
            val uid = requireUserId()
            val docRef = vendorsCol(uid).document(vendorId)
            docPath = docRef.path
            val batch = db.batch()
            batch.update(
                docRef,
                mapOf(
                    "autoRenew" to autoRenew,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                auditLogsCol(uid).document(),
                mapOf(
                    "userId" to uid,
                    "message" to "${if (autoRenew) "Enabled" else "Paused"} auto-renewal for $vendorId".take(240),
                    "status" to "Success",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateVendorStatus(vendorId: String, newStatus: VendorStatus): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/vendors/$vendorId"
        return try {
            val uid = requireUserId()
            require(vendorId.isNotBlank()) { "Vendor ID required" }
            val docRef = vendorsCol(uid).document(vendorId)
            docPath = docRef.path

            val batch = db.batch()
            batch.update(
                docRef,
                mapOf(
                    "status" to newStatus.label,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            val logRef = auditLogsCol(uid).document()
            batch.set(
                logRef,
                mapOf(
                    "userId" to uid,
                    "message" to "${newStatus.label} vendor $vendorId".take(240),
                    "status" to if (newStatus == VendorStatus.Suspended) "Warning" else "Success",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun resolveTicket(ticketId: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/tickets/$ticketId"
        return try {
            val uid = requireUserId()
            require(ticketId.isNotBlank()) { "Ticket ID required" }
            val docRef = ticketsCol(uid).document(ticketId)
            docPath = docRef.path

            val batch = db.batch()
            batch.update(
                docRef,
                mapOf(
                    "status" to "Resolved",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            val logRef = auditLogsCol(uid).document()
            batch.set(
                logRef,
                mapOf(
                    "userId" to uid,
                    "message" to "Resolved ticket $ticketId".take(240),
                    "status" to "Success",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun toggleSetting(key: String, label: String, enabled: Boolean): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/settings/$key"
        return try {
            val uid = requireUserId()
            require(key.isNotBlank()) { "Setting key required" }
            val docRef = settingsCol(uid).document(key)
            docPath = docRef.path

            val snapshot = docRef.get().await()
            if (snapshot.exists()) {
                docRef.update(
                    mapOf(
                        "enabled" to enabled,
                        "label" to label.take(120),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "label" to label.take(120),
                        "enabled" to enabled,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun recordAuditLog(message: String, status: String = "Success"): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/auditLogs"
        return try {
            val uid = requireUserId()
            val cleanMsg = message.trim().take(240)
            require(cleanMsg.isNotEmpty()) { "Audit log message required" }
            val safeStatus = if (status in listOf("Success", "Warning", "Blocked")) status else "Success"
            val docRef = auditLogsCol(uid).document()
            docPath = docRef.path

            docRef.set(
                mapOf(
                    "userId" to uid,
                    "message" to cleanMsg,
                    "status" to safeStatus,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }
}
