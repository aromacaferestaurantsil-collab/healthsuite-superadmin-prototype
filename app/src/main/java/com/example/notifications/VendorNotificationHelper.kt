package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.R
import com.example.security.NetworkUrlValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object VendorNotificationHelper {
    const val CHANNEL_OFFERS = "hs360_offers_discounts"
    const val CHANNEL_RENEWALS = "hs360_subscription_renewals"
    const val CHANNEL_SECURITY = "hs360_license_security"

    private val httpClient = NetworkUrlValidator.createSafeHttpClient(12, 15, 15)

    fun ensureChannels(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val channels = listOf(
            NotificationChannel(
                CHANNEL_OFFERS,
                "Offers & Discounts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Promotional module discounts and bundle offers for vendors"
            },
            NotificationChannel(
                CHANNEL_RENEWALS,
                "Subscriptions & Auto-Renewal",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Subscription remaining days and auto-renewal reminders"
            },
            NotificationChannel(
                CHANNEL_SECURITY,
                "License & Seat Security",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Unauthorized license sharing and multi-device violation alerts"
            }
        )
        channels.forEach { manager.createNotificationChannel(it) }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun postSystemNotification(
        context: Context,
        category: String,
        title: String,
        body: String
    ): Boolean {
        if (!hasNotificationPermission(context)) return false
        ensureChannels(context)
        val channelId = when {
            category.contains("License", ignoreCase = true) || category.contains("Security", ignoreCase = true) ->
                CHANNEL_SECURITY
            category.contains("Subscription", ignoreCase = true) || category.contains("Renewal", ignoreCase = true) ->
                CHANNEL_RENEWALS
            else -> CHANNEL_OFFERS
        }
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(
                if (channelId == CHANNEL_OFFERS) NotificationCompat.PRIORITY_DEFAULT
                else NotificationCompat.PRIORITY_HIGH
            )
            .setAutoCancel(true)
            .build()

        val notificationId = (title + body + System.nanoTime()).hashCode() and 0x7FFFFFFF
        return runCatching {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
            true
        }.getOrDefault(false)
    }

    suspend fun dispatchPushWebhook(
        webhookUrl: String,
        bearerToken: String?,
        vendorId: String,
        vendorName: String,
        recipientEmail: String,
        recipientPhone: String,
        category: String,
        title: String,
        message: String
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val parsed = NetworkUrlValidator.parseSafeHttpsUrl(webhookUrl)
                ?: throw IllegalArgumentException("Push webhook URL must be a valid public HTTPS endpoint.")

            val payload = JSONObject().apply {
                put("vendorId", vendorId)
                put("vendorName", vendorName)
                put("email", recipientEmail)
                put("phone", recipientPhone)
                put("category", category)
                put("title", title)
                put("message", message)
                put("timestampMillis", System.currentTimeMillis())
            }.toString().toRequestBody("application/json; charset=UTF-8".toMediaType())

            val reqBuilder = Request.Builder().url(parsed).post(payload)
            if (!bearerToken.isNullOrBlank()) {
                reqBuilder.header("Authorization", "Bearer ${bearerToken.trim()}")
            }
            httpClient.newCall(reqBuilder.build()).execute().use { res ->
                if (!res.isSuccessful) {
                    throw IllegalStateException("Push webhook returned HTTP ${res.code}")
                }
                "Delivered via HTTPS Push Webhook"
            }
        }
    }

    fun launchVendorEmail(
        context: Context,
        email: String,
        subject: String,
        body: String
    ): Boolean {
        val cleanEmail = email.trim()
        if (!cleanEmail.contains("@")) return false
        val uri = Uri.parse(
            "mailto:${Uri.encode(cleanEmail)}?subject=${Uri.encode(subject)}&body=${Uri.encode(body)}"
        )
        val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }
}
