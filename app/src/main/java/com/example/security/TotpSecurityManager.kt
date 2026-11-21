package com.example.security

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import java.net.URLEncoder
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.experimental.and

/**
 * RFC 6238 TOTP (HMAC-SHA1, 6 digits, 30s period) & ZXing QR generator
 * for genuine Two-Step Verification with Google Authenticator / Authy / 1Password.
 */
object TotpSecurityManager {
    private const val BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
    private const val PERIOD_SECONDS = 30L
    private const val DIGITS = 6

    fun generateSecret(): String {
        val bytes = ByteArray(20)
        SecureRandom().nextBytes(bytes)
        return encodeBase32(bytes)
    }

    fun buildOtpAuthUri(accountEmail: String, secret: String): String {
        val label = URLEncoder.encode("HealthSuite 360:$accountEmail", "UTF-8").replace("+", "%20")
        val issuer = URLEncoder.encode("HealthSuite 360", "UTF-8").replace("+", "%20")
        return "otpauth://totp/$label?secret=$secret&issuer=$issuer&algorithm=SHA1&digits=$DIGITS&period=$PERIOD_SECONDS"
    }

    fun remainingSecondsInWindow(nowMillis: Long = System.currentTimeMillis()): Int {
        val elapsed = (nowMillis / 1000L) % PERIOD_SECONDS
        return (PERIOD_SECONDS - elapsed).toInt()
    }

    fun verifyCode(
        base32Secret: String,
        userCode: String,
        nowMillis: Long = System.currentTimeMillis(),
        allowedSkewWindows: Int = 1
    ): Boolean {
        val sanitized = userCode.trim().filter { it.isDigit() }
        if (sanitized.length != DIGITS) return false
        val keyBytes = decodeBase32(base32Secret)
        if (keyBytes.isEmpty()) return false

        val currentCounter = (nowMillis / 1000L) / PERIOD_SECONDS
        for (offset in -allowedSkewWindows..allowedSkewWindows) {
            val expected = generateTotpAtCounter(keyBytes, currentCounter + offset)
            if (MessageDigest.isEqual(
                    expected.toByteArray(Charsets.US_ASCII),
                    sanitized.toByteArray(Charsets.US_ASCII)
                )
            ) {
                return true
            }
        }
        return false
    }

    private fun generateTotpAtCounter(keyBytes: ByteArray, counter: Long): String {
        val data = ByteBuffer.allocate(8).putLong(counter).array()
        val mac = Mac.getInstance("HmacSHA1")
        mac.init(SecretKeySpec(keyBytes, "HmacSHA1"))
        val hash = mac.doFinal(data)
        val offset = (hash.last() and 0x0F).toInt()
        val binary = ((hash[offset].toInt() and 0x7F) shl 24) or
            ((hash[offset + 1].toInt() and 0xFF) shl 16) or
            ((hash[offset + 2].toInt() and 0xFF) shl 8) or
            (hash[offset + 3].toInt() and 0xFF)
        val otp = binary % 1_000_000
        return otp.toString().padStart(DIGITS, '0')
    }

    fun generateQrBitmap(content: String, sizePx: Int = 512): Bitmap? = runCatching {
        val hints = mapOf(
            EncodeHintType.MARGIN to 1,
            EncodeHintType.CHARACTER_SET to "UTF-8"
        )
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        val pixels = IntArray(sizePx * sizePx)
        for (y in 0 until sizePx) {
            val offset = y * sizePx
            for (x in 0 until sizePx) {
                pixels[offset + x] = if (matrix[x, y]) Color.parseColor("#111C2D") else Color.WHITE
            }
        }
        Bitmap.createBitmap(pixels, sizePx, sizePx, Bitmap.Config.ARGB_8888)
    }.getOrNull()

    private fun encodeBase32(bytes: ByteArray): String {
        val out = StringBuilder()
        var buffer = 0
        var bitsLeft = 0
        for (b in bytes) {
            buffer = (buffer shl 8) or (b.toInt() and 0xFF)
            bitsLeft += 8
            while (bitsLeft >= 5) {
                val index = (buffer shr (bitsLeft - 5)) and 0x1F
                bitsLeft -= 5
                out.append(BASE32_ALPHABET[index])
            }
        }
        if (bitsLeft > 0) {
            val index = (buffer shl (5 - bitsLeft)) and 0x1F
            out.append(BASE32_ALPHABET[index])
        }
        return out.toString()
    }

    private fun decodeBase32(input: String): ByteArray {
        val cleaned = input.uppercase().filter { it in BASE32_ALPHABET }
        if (cleaned.isEmpty()) return ByteArray(0)
        val out = ArrayList<Byte>((cleaned.length * 5) / 8)
        var buffer = 0
        var bitsLeft = 0
        for (ch in cleaned) {
            val value = BASE32_ALPHABET.indexOf(ch)
            if (value < 0) continue
            buffer = (buffer shl 5) or value
            bitsLeft += 5
            if (bitsLeft >= 8) {
                out.add(((buffer shr (bitsLeft - 8)) and 0xFF).toByte())
                bitsLeft -= 8
            }
        }
        return out.toByteArray()
    }
}
