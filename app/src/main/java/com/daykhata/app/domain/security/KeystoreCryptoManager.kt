package com.daykhata.app.domain.security

import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

interface KeystoreCrypto {
    fun encrypt(plainText: String): String
    fun decrypt(cipherTextWithIv: String): String?
}

object KeystoreCryptoManager : KeystoreCrypto {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "daykhata_keystore_pin_key"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH = 128
    private const val GCM_IV_LENGTH = 12

    // Fallback key used only when AndroidKeyStore provider is not available (e.g. host JVM unit tests)
    private var jvmTestKey: SecretKey? = null

    private fun isAndroidKeystoreAvailable(): Boolean {
        return try {
            KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            true
        } catch (_: Throwable) {
            false
        }
    }

    private fun getOrCreateKey(): SecretKey {
        return if (isAndroidKeystoreAvailable()) {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                val keyGen = KeyGenerator.getInstance("AES", ANDROID_KEYSTORE)
                val spec = android.security.keystore.KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or android.security.keystore.KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
                keyGen.init(spec)
                keyGen.generateKey()
            }
            (keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
        } else {
            // JVM host environment fallback for unit tests
            if (jvmTestKey == null) {
                val keyBytes = ByteArray(32) { (it * 7).toByte() }
                jvmTestKey = SecretKeySpec(keyBytes, "AES")
            }
            jvmTestKey!!
        }
    }

    override fun encrypt(plainText: String): String {
        if (plainText.isEmpty()) return ""
        val secretKey = getOrCreateKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val iv = ByteArray(GCM_IV_LENGTH)
        SecureRandom().nextBytes(iv)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)
        val cipherBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return "${PinSecurityManager.toHexString(iv)}:${PinSecurityManager.toHexString(cipherBytes)}"
    }

    override fun decrypt(cipherTextWithIv: String): String? {
        if (cipherTextWithIv.isBlank()) return ""
        val parts = cipherTextWithIv.split(":")
        if (parts.size != 2) {
            // If legacy unencrypted data is encountered, return it directly for smooth migration
            return cipherTextWithIv
        }
        val iv = PinSecurityManager.fromHexString(parts[0]) ?: return null
        val cipherBytes = PinSecurityManager.fromHexString(parts[1]) ?: return null
        return try {
            val secretKey = getOrCreateKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            val plainBytes = cipher.doFinal(cipherBytes)
            String(plainBytes, Charsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }
}
