package com.example.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Encrypts sensitive per-user security credentials (TOTP secrets, WhatsApp bridge tokens)
 * using AES-256-GCM backed by the hardware/TEE Android Keystore before persisting in SharedPreferences.
 */
class KeystoreVault(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun getOrCreateAesKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        if (existing != null) return existing.secretKey

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    fun putEncryptedString(key: String, value: String) {
        if (value.isEmpty()) {
            prefs.edit().remove(key).apply()
            return
        }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateAesKey())
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        val combined = ByteArray(1 + iv.size + ciphertext.size).apply {
            this[0] = iv.size.toByte()
            System.arraycopy(iv, 0, this, 1, iv.size)
            System.arraycopy(ciphertext, 0, this, 1 + iv.size, ciphertext.size)
        }
        prefs.edit().putString(key, Base64.encodeToString(combined, Base64.NO_WRAP)).apply()
    }

    fun getEncryptedString(key: String): String? {
        val encoded = prefs.getString(key, null) ?: return null
        return runCatching {
            val raw = Base64.decode(encoded, Base64.NO_WRAP)
            require(raw.size > 2)
            val ivLen = raw[0].toInt() and 0xFF
            require(ivLen in 12..32 && 1 + ivLen < raw.size)
            val iv = raw.copyOfRange(1, 1 + ivLen)
            val ciphertext = raw.copyOfRange(1 + ivLen, raw.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateAesKey(), GCMParameterSpec(128, iv))
            String(cipher.doFinal(ciphertext), Charsets.UTF_8)
        }.onFailure {
            remove(key)
        }.getOrNull()
    }

    fun putBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }

    fun getBoolean(key: String, default: Boolean = false): Boolean =
        prefs.getBoolean(key, default)

    fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "healthsuite360_vault_aes_v1"
        const val PREFS_NAME = "healthsuite_encrypted_vault"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
