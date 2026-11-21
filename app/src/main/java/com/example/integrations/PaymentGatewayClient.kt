package com.example.integrations

import android.content.Intent
import android.net.Uri
import com.example.security.NetworkUrlValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit

data class GatewayOrderCreated(
    val orderRef: String,
    val gatewayRef: String,
    val checkoutUrl: String
)

data class GatewayVerificationResult(
    val isPaid: Boolean,
    val status: String,
    val gatewayRef: String,
    val message: String
)

data class UpiPaymentResult(
    val isSuccess: Boolean,
    val isSubmittedPending: Boolean,
    val statusText: String,
    val referenceId: String
)

class PaymentGatewayClient(
    private val httpClient: OkHttpClient = NetworkUrlValidator.createSafeHttpClient(15, 25, 25)
) {
    fun validateGatewayUrl(rawUrl: String): HttpUrl? =
        NetworkUrlValidator.parseSafeHttpsUrl(rawUrl)

    suspend fun createCheckoutOrder(
        gatewayBaseUrl: String,
        bearerToken: String?,
        orderRef: String,
        vendorId: String,
        vendorName: String,
        vendorEmail: String,
        itemTitle: String,
        amount: Double,
        currency: String,
        autoRenew: Boolean
    ): Result<GatewayOrderCreated> = withContext(Dispatchers.IO) {
        runCatching {
            val base = validateGatewayUrl(gatewayBaseUrl)
                ?: throw IllegalArgumentException("Configure a valid HTTPS Payment Gateway API URL first.")
            require(amount > 0.0) { "Payable amount must be greater than zero." }

            val endpoint = base.newBuilder().addPathSegment("orders").build()
            val payload = JSONObject().apply {
                put("orderRef", orderRef)
                put("vendorId", vendorId)
                put("vendorName", vendorName)
                put("customerEmail", vendorEmail)
                put("description", itemTitle)
                put("amount", amount)
                put("currency", currency)
                put("autoRenew", autoRenew)
            }.toString().toRequestBody("application/json; charset=UTF-8".toMediaType())

            val reqBuilder = Request.Builder().url(endpoint).post(payload)
            if (!bearerToken.isNullOrBlank()) {
                reqBuilder.header("Authorization", "Bearer ${bearerToken.trim()}")
            }

            httpClient.newCall(reqBuilder.build()).execute().use { res ->
                val rawBody = res.body?.string().orEmpty()
                if (!res.isSuccessful) {
                    val errMsg = runCatching { JSONObject(rawBody).optString("message") }.getOrNull()
                    throw IllegalStateException(
                        errMsg?.takeIf { it.isNotBlank() }
                            ?: "Payment gateway order creation failed (HTTP ${res.code})"
                    )
                }
                val json = JSONObject(rawBody)
                val checkoutUrl = json.optString("checkoutUrl")
                    .ifBlank { json.optString("paymentLink") }
                    .ifBlank { json.optString("short_url") }
                val parsedCheckout = NetworkUrlValidator.parseSafeHttpsUrl(checkoutUrl)
                require(parsedCheckout != null) {
                    "Payment gateway did not return a valid public HTTPS checkoutUrl."
                }
                GatewayOrderCreated(
                    orderRef = json.optString("orderRef", orderRef),
                    gatewayRef = json.optString("gatewayRef").ifBlank { json.optString("id", orderRef) },
                    checkoutUrl = checkoutUrl
                )
            }
        }
    }

    suspend fun verifyOrderStatus(
        gatewayBaseUrl: String,
        bearerToken: String?,
        orderRef: String
    ): Result<GatewayVerificationResult> = withContext(Dispatchers.IO) {
        runCatching {
            val base = validateGatewayUrl(gatewayBaseUrl)
                ?: throw IllegalArgumentException("Configure a valid HTTPS Payment Gateway API URL first.")
            val statusUrl = base.newBuilder()
                .addPathSegment("orders")
                .addPathSegment(orderRef)
                .build()
            val reqBuilder = Request.Builder().url(statusUrl).get()
            if (!bearerToken.isNullOrBlank()) {
                reqBuilder.header("Authorization", "Bearer ${bearerToken.trim()}")
            }

            httpClient.newCall(reqBuilder.build()).execute().use { res ->
                val rawBody = res.body?.string().orEmpty()
                if (!res.isSuccessful) {
                    throw IllegalStateException("Payment verification failed (HTTP ${res.code})")
                }
                val json = JSONObject(rawBody)
                val statusRaw = json.optString("status", "Pending")
                val normalized = statusRaw.trim().lowercase(Locale.ROOT)
                val paid = normalized in setOf("paid", "captured", "succeeded", "completed", "success")
                val failed = normalized in setOf("failed", "cancelled", "expired")
                GatewayVerificationResult(
                    isPaid = paid,
                    status = when {
                        paid -> "Paid"
                        failed -> "Failed"
                        else -> "Pending"
                    },
                    gatewayRef = json.optString("gatewayRef").ifBlank { json.optString("paymentId", orderRef) },
                    message = json.optString("message", "Gateway status: $statusRaw")
                )
            }
        }
    }

    fun buildUpiPayIntent(
        merchantVpa: String,
        merchantName: String,
        orderRef: String,
        note: String,
        amount: Double,
        currency: String = "INR"
    ): Intent? {
        val cleanVpa = merchantVpa.trim()
        if (!cleanVpa.contains("@") || amount <= 0.0) return null
        val formattedAmount = String.format(Locale.US, "%.2f", amount)
        val uri = Uri.Builder()
            .scheme("upi")
            .authority("pay")
            .appendQueryParameter("pa", cleanVpa)
            .appendQueryParameter("pn", merchantName.ifBlank { "HealthSuite 360" })
            .appendQueryParameter("tr", orderRef)
            .appendQueryParameter("tn", note.take(80))
            .appendQueryParameter("am", formattedAmount)
            .appendQueryParameter("cu", if (currency.equals("USD", ignoreCase = true)) "INR" else currency.uppercase(Locale.ROOT))
            .build()
        return Intent(Intent.ACTION_VIEW, uri)
    }

    fun parseUpiResultIntent(data: Intent?): UpiPaymentResult {
        val responseRaw = data?.getStringExtra("response").orEmpty()
        if (responseRaw.isBlank()) {
            return UpiPaymentResult(
                isSuccess = false,
                isSubmittedPending = false,
                statusText = "Cancelled",
                referenceId = ""
            )
        }
        val params = responseRaw.split("&").mapNotNull { pair ->
            val idx = pair.indexOf('=')
            if (idx > 0) {
                pair.substring(0, idx).lowercase(Locale.ROOT) to Uri.decode(pair.substring(idx + 1))
            } else null
        }.toMap()

        val status = params["status"].orEmpty().trim()
        val ref = params["approvalrefno"].orEmpty()
            .ifBlank { params["txnref"].orEmpty() }
            .ifBlank { params["txnid"].orEmpty() }

        return when (status.lowercase(Locale.ROOT)) {
            "success" -> UpiPaymentResult(
                isSuccess = true,
                isSubmittedPending = false,
                statusText = "Paid",
                referenceId = ref
            )
            "submitted" -> UpiPaymentResult(
                isSuccess = false,
                isSubmittedPending = true,
                statusText = "Pending",
                referenceId = ref
            )
            else -> UpiPaymentResult(
                isSuccess = false,
                isSubmittedPending = false,
                statusText = "Failed",
                referenceId = ref
            )
        }
    }
}
