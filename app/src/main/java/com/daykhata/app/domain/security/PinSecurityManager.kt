package com.daykhata.app.domain.security

import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PinSecurityManager {

    private const val ITERATIONS = 10000
    private const val KEY_LENGTH = 256
    private const val SALT_BYTES = 32

    fun validatePinFormat(pin: String): Boolean {
        return pin.length in 4..6 && pin.all { it.isDigit() }
    }

    fun generateSalt(): ByteArray {
        val random = SecureRandom()
        val salt = ByteArray(SALT_BYTES)
        random.nextBytes(salt)
        return salt
    }

    fun hashPin(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        val factory = try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        } catch (_: NoSuchAlgorithmException) {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
        }
        return factory.generateSecret(spec).encoded
    }

    fun verifyPin(pin: String, saltHex: String, expectedHashHex: String): Boolean {
        if (!validatePinFormat(pin)) return false
        val salt = fromHexString(saltHex) ?: return false
        val expectedHash = fromHexString(expectedHashHex) ?: return false
        val computedHash = hashPin(pin, salt)
        return MessageDigest.isEqual(computedHash, expectedHash)
    }

    fun getLockoutDurationSeconds(failedAttempts: Int): Long {
        return when {
            failedAttempts < 5 -> 0L
            failedAttempts == 5 -> 30L
            failedAttempts == 6 -> 60L
            failedAttempts == 7 -> 300L
            else -> 900L
        }
    }

    fun toHexString(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun fromHexString(hex: String): ByteArray? {
        if (hex.length % 2 != 0) return null
        return try {
            ByteArray(hex.length / 2) { i ->
                hex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
            }
        } catch (_: Exception) {
            null
        }
    }
}
