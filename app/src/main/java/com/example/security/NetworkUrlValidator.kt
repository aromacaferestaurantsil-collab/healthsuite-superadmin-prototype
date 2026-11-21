package com.example.security

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.net.InetAddress
import java.util.Locale

/**
 * Validates outbound HTTP client URLs and browser launch links against strict HTTPS
 * and blocks loopback, link-local metadata, multicast, and private network addresses.
 */
object NetworkUrlValidator {
    private val blockedHostNames = setOf(
        "localhost",
        "localhost.localdomain",
        "ip6-localhost",
        "ip6-loopback",
        "metadata.google.internal",
        "metadata"
    )

    fun parseSafeHttpsUrl(rawUrl: String): HttpUrl? {
        val trimmed = rawUrl.trim().trimEnd('/')
        if (trimmed.isEmpty() || trimmed.length > 2048) return null
        val parsed = trimmed.toHttpUrlOrNull() ?: return null
        if (!parsed.isHttps) return null
        if (parsed.username.isNotEmpty() || parsed.password.isNotEmpty()) return null
        val host = parsed.host.lowercase(Locale.ROOT)
        if (host.isBlank() || host in blockedHostNames || host.endsWith(".local") || host.endsWith(".internal")) {
            return null
        }
        if (isDisallowedIpLiteral(host)) return null
        return parsed
    }

    /**
     * Checks numeric IPv4/IPv6 literals synchronously so untrusted URLs targeting
     * loopback, private RFC1918, or cloud metadata subnets are rejected before network I/O.
     */
    fun isDisallowedIpLiteral(rawHost: String): Boolean {
        val host = rawHost.trim().removePrefix("[").removeSuffix("]")
        val looksLikeIp = host.all { it.isDigit() || it == '.' } || host.contains(':')
        if (!looksLikeIp) return false

        val addr = runCatching { InetAddress.getByName(host) }.getOrNull() ?: return true
        if (addr.isAnyLocalAddress ||
            addr.isLoopbackAddress ||
            addr.isLinkLocalAddress ||
            addr.isSiteLocalAddress ||
            addr.isMulticastAddress
        ) {
            return true
        }
        val bytes = addr.address
        // Block carrier-grade NAT (100.64.0.0/10) and IPv6 unique local addresses (fc00::/7)
        if (bytes.size == 4) {
            val b0 = bytes[0].toInt() and 0xFF
            val b1 = bytes[1].toInt() and 0xFF
            if (b0 == 100 && b1 in 64..127) return true
            if (b0 == 0 || b0 == 127) return true
            if (b0 == 169 && b1 == 254) return true
        } else if (bytes.size == 16) {
            val first = bytes[0].toInt() and 0xFF
            if ((first and 0xFE) == 0xFC) return true
        }
        return false
    }

    fun createSafeHttpClient(
        connectTimeoutSeconds: Long = 12L,
        readTimeoutSeconds: Long = 20L,
        writeTimeoutSeconds: Long = 20L
    ): okhttp3.OkHttpClient = okhttp3.OkHttpClient.Builder()
        .connectTimeout(connectTimeoutSeconds, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(readTimeoutSeconds, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(writeTimeoutSeconds, java.util.concurrent.TimeUnit.SECONDS)
        .followRedirects(false)
        .followSslRedirects(false)
        .build()
}
