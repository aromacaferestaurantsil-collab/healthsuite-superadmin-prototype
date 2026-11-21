package com.example.integrations

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.MainActivity
import com.example.R
import com.example.security.NetworkUrlValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class PwaReadinessReport(
    val portalUrl: String,
    val isInstallable: Boolean,
    val httpsVerified: Boolean,
    val appName: String,
    val displayMode: String,
    val iconCount: Int,
    val hasServiceWorker: Boolean,
    val manifestUrl: String,
    val summary: String
)

class PwaInstallHelper(
    private val httpClient: OkHttpClient = NetworkUrlValidator.createSafeHttpClient(12, 15, 15)
) {
    fun isHomeScreenPinSupported(context: Context): Boolean =
        ShortcutManagerCompat.isRequestPinShortcutSupported(context)

    fun requestPinAppShortcut(context: Context): Result<String> = runCatching {
        check(ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
            "Your Android launcher does not support pinned home-screen shortcuts."
        }
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse("healthsuite360://console")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val shortcut = ShortcutInfoCompat.Builder(context, "hs360_super_admin_pin")
            .setShortLabel("HealthSuite 360")
            .setLongLabel("HealthSuite 360 · Super Admin")
            .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
            .setIntent(launchIntent)
            .build()
        val requested = ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
        check(requested) { "Home screen shortcut request was declined by launcher." }
        "Home screen app shortcut requested."
    }

    fun openSafeWebPortal(context: Context, rawUrl: String): Result<Unit> = runCatching {
        val safeUrl = NetworkUrlValidator.parseSafeHttpsUrl(rawUrl)
            ?: throw IllegalArgumentException("Enter a valid public HTTPS portal URL.")
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(safeUrl.toString())).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    suspend fun inspectPwaInstallability(rawPortalUrl: String): Result<PwaReadinessReport> =
        withContext(Dispatchers.IO) {
            runCatching {
                val safeUrl = NetworkUrlValidator.parseSafeHttpsUrl(rawPortalUrl)
                    ?: throw IllegalArgumentException("Enter a valid public HTTPS URL (e.g., https://portal.example.com).")

                val pageReq = Request.Builder()
                    .url(safeUrl)
                    .header("Accept", "text/html,application/xhtml+xml")
                    .get()
                    .build()

                val html = httpClient.newCall(pageReq).execute().use { res ->
                    if (!res.isSuccessful) {
                        throw IllegalStateException("Portal returned HTTP ${res.code}")
                    }
                    res.body?.string()?.take(250_000).orEmpty()
                }

                val hasServiceWorker = html.contains("serviceWorker", ignoreCase = true) ||
                    html.contains("registerSW", ignoreCase = true)

                val manifestHref = extractManifestHref(html)
                if (manifestHref.isNullOrBlank()) {
                    return@runCatching PwaReadinessReport(
                        portalUrl = safeUrl.toString(),
                        isInstallable = false,
                        httpsVerified = true,
                        appName = "",
                        displayMode = "missing",
                        iconCount = 0,
                        hasServiceWorker = hasServiceWorker,
                        manifestUrl = "",
                        summary = "HTTPS verified, but no <link rel=\"manifest\"> found on page."
                    )
                }

                val resolvedManifest = safeUrl.resolve(manifestHref)
                val validatedManifest = resolvedManifest?.let {
                    NetworkUrlValidator.parseSafeHttpsUrl(it.toString())
                } ?: throw SecurityException("Manifest link points outside a valid public HTTPS host.")

                val manifestJsonText = httpClient.newCall(
                    Request.Builder().url(validatedManifest).get().build()
                ).execute().use { res ->
                    if (!res.isSuccessful) {
                        throw IllegalStateException("Manifest fetch failed (HTTP ${res.code})")
                    }
                    res.body?.string()?.take(100_000).orEmpty()
                }

                val manifestJson = JSONObject(manifestJsonText)
                val appName = manifestJson.optString("name")
                    .ifBlank { manifestJson.optString("short_name") }
                val displayMode = manifestJson.optString("display", "browser")
                val startUrl = manifestJson.optString("start_url")
                val iconsArray = manifestJson.optJSONArray("icons")
                val iconCount = iconsArray?.length() ?: 0
                val standaloneReady = displayMode in setOf("standalone", "minimal-ui", "fullscreen")
                val isInstallable = appName.isNotBlank() &&
                    startUrl.isNotBlank() &&
                    standaloneReady &&
                    iconCount > 0

                PwaReadinessReport(
                    portalUrl = safeUrl.toString(),
                    isInstallable = isInstallable,
                    httpsVerified = true,
                    appName = appName.ifBlank { "Unnamed Web App" },
                    displayMode = displayMode,
                    iconCount = iconCount,
                    hasServiceWorker = hasServiceWorker,
                    manifestUrl = validatedManifest.toString(),
                    summary = if (isInstallable) {
                        "PWA installable ($appName · display=$displayMode · $iconCount icon(s))."
                    } else {
                        "Manifest loaded ($displayMode, $iconCount icons); ensure display=standalone, start_url, name, and icons are set."
                    }
                )
            }
        }

    private fun extractManifestHref(html: String): String? {
        val linkPattern = Pattern.compile(
            "<link\\b[^>]*\\brel\\s*=\\s*[\"']?[^\"'>]*\\bmanifest\\b[^\"'>]*[\"']?[^>]*>",
            Pattern.CASE_INSENSITIVE
        )
        val matcher = linkPattern.matcher(html)
        if (!matcher.find()) return null
        val tag = matcher.group()
        val hrefPattern = Pattern.compile(
            "\\bhref\\s*=\\s*[\"']([^\"'>]+)[\"']",
            Pattern.CASE_INSENSITIVE
        )
        val hrefMatcher = hrefPattern.matcher(tag)
        return if (hrefMatcher.find()) hrefMatcher.group(1)?.trim() else null
    }
}
