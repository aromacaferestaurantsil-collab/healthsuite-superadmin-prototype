package com.example.integrations

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.model.WhatsAppBridgeStatus
import com.example.security.NetworkUrlValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Connects to a user-configured WhatsApp Link-Device bridge server (e.g., Baileys / whatsapp-web.js)
 * over HTTPS (or localhost/10.0.2.2 for local testing), and provides native WhatsApp deep-link dispatch.
 */
class WhatsAppBridgeClient(
    private val httpClient: OkHttpClient = NetworkUrlValidator.createSafeHttpClient(12, 20, 20)
) {
    fun validateBridgeUrl(rawUrl: String): HttpUrl? =
        NetworkUrlValidator.parseSafeHttpsUrl(rawUrl)

    suspend fun fetchBridgeStatusAndQr(
        bridgeBaseUrl: String,
        bearerToken: String?
    ): Result<WhatsAppBridgeStatus> = withContext(Dispatchers.IO) {
        runCatching {
            val base = validateBridgeUrl(bridgeBaseUrl)
                ?: throw IllegalArgumentException("Use a valid public HTTPS WhatsApp bridge URL.")

            val statusUrl = base.newBuilder().addPathSegment("status").build()
            val statusReqBuilder = Request.Builder().url(statusUrl).get()
            if (!bearerToken.isNullOrBlank()) {
                statusReqBuilder.header("Authorization", "Bearer ${bearerToken.trim()}")
            }

            val statusJson = httpClient.newCall(statusReqBuilder.build()).execute().use { res ->
                val body = res.body?.string().orEmpty()
                if (!res.isSuccessful) {
                    throw IllegalStateException("Bridge status returned HTTP ${res.code}")
                }
                JSONObject(body)
            }

            val connected = statusJson.optBoolean("connected", false)
            val accountLabel = statusJson.optString("account").ifBlank {
                statusJson.optString("phone").ifBlank { null }
            }

            var qrPayload: String? = statusJson.optString("qr").takeIf { it.isNotBlank() }
            if (!connected && qrPayload == null) {
                val qrUrl = base.newBuilder().addPathSegment("qr").build()
                val qrReqBuilder = Request.Builder().url(qrUrl).get()
                if (!bearerToken.isNullOrBlank()) {
                    qrReqBuilder.header("Authorization", "Bearer ${bearerToken.trim()}")
                }
                runCatching {
                    httpClient.newCall(qrReqBuilder.build()).execute().use { qrRes ->
                        if (qrRes.isSuccessful) {
                            val qrBody = qrRes.body?.string().orEmpty()
                            val qrJson = JSONObject(qrBody)
                            qrPayload = qrJson.optString("qr")
                                .ifBlank { qrJson.optString("qrCode") }
                                .takeIf { it.isNotBlank() }
                        }
                    }
                }
            }

            WhatsAppBridgeStatus(
                isConnected = connected,
                accountLabel = accountLabel,
                qrPayload = qrPayload,
                statusMessage = when {
                    connected -> "Linked to ${accountLabel ?: "WhatsApp device"}"
                    qrPayload != null -> "Scan QR in WhatsApp -> Linked Devices"
                    else -> statusJson.optString("message", "Bridge reachable · awaiting pairing QR")
                }
            )
        }
    }

    suspend fun submitScannedPairPayload(
        bridgeBaseUrl: String,
        bearerToken: String?,
        scannedQrPayload: String
    ): Result<WhatsAppBridgeStatus> = withContext(Dispatchers.IO) {
        runCatching {
            val base = validateBridgeUrl(bridgeBaseUrl)
                ?: throw IllegalArgumentException("Configure a valid HTTPS WhatsApp bridge URL first.")

            val pairUrl = base.newBuilder().addPathSegment("pair").build()
            val payload = JSONObject().apply {
                put("qr", scannedQrPayload)
            }.toString().toRequestBody("application/json; charset=UTF-8".toMediaType())

            val reqBuilder = Request.Builder().url(pairUrl).post(payload)
            if (!bearerToken.isNullOrBlank()) {
                reqBuilder.header("Authorization", "Bearer ${bearerToken.trim()}")
            }

            httpClient.newCall(reqBuilder.build()).execute().use { res ->
                val body = res.body?.string().orEmpty()
                if (!res.isSuccessful) {
                    throw IllegalStateException("Bridge pairing failed (HTTP ${res.code})")
                }
                val json = if (body.isNotBlank()) JSONObject(body) else JSONObject()
                WhatsAppBridgeStatus(
                    isConnected = json.optBoolean("connected", false),
                    accountLabel = json.optString("account").takeIf { it.isNotBlank() },
                    qrPayload = json.optString("qr").takeIf { it.isNotBlank() },
                    statusMessage = json.optString("message", "Pairing payload submitted to bridge")
                )
            }
        }
    }

    suspend fun sendBridgeMessage(
        bridgeBaseUrl: String,
        bearerToken: String?,
        recipientPhone: String,
        messageText: String
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val base = validateBridgeUrl(bridgeBaseUrl)
                ?: throw IllegalArgumentException("Configure a valid HTTPS WhatsApp bridge URL first.")
            val cleanPhone = recipientPhone.filter { it.isDigit() }
            require(cleanPhone.length >= 8) { "Enter a valid phone number with country code" }
            require(messageText.isNotBlank()) { "Enter a message to send" }

            val sendUrl = base.newBuilder().addPathSegment("send").build()
            val payload = JSONObject().apply {
                put("to", cleanPhone)
                put("message", messageText.trim())
            }.toString().toRequestBody("application/json; charset=UTF-8".toMediaType())

            val reqBuilder = Request.Builder().url(sendUrl).post(payload)
            if (!bearerToken.isNullOrBlank()) {
                reqBuilder.header("Authorization", "Bearer ${bearerToken.trim()}")
            }

            httpClient.newCall(reqBuilder.build()).execute().use { res ->
                if (!res.isSuccessful) {
                    throw IllegalStateException("WhatsApp bridge send failed (HTTP ${res.code})")
                }
                "Dispatched via WhatsApp Bridge to +$cleanPhone"
            }
        }
    }

    fun openWhatsAppDirect(context: Context, recipientPhone: String, messageText: String): Boolean {
        val digits = recipientPhone.filter { it.isDigit() }
        val encodedText = URLEncoder.encode(messageText.trim(), "UTF-8")
        val uriString = if (digits.isNotEmpty()) {
            "https://wa.me/$digits?text=$encodedText"
        } else {
            "https://wa.me/?text=$encodedText"
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }
}
