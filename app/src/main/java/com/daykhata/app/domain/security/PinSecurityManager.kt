package com.daykhata.app.domain.security

import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PinSecurityManager {

    const val LEGACY_ITERATIONS = 10000
    const val CURRENT_ITERATIONS = 120000
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

    fun hashPin(pin: String, salt: ByteArray, iterations: Int = CURRENT_ITERATIONS): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, iterations, KEY_LENGTH)
        val factory = try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        } catch (_: NoSuchAlgorithmException) {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
        }
        return factory.generateSecret(spec).encoded
    }

    data class PinVerificationResult(
        val isValid: Boolean,
        val needsRehash: Boolean
    )

    fun verifyPinWithIterations(
        pin: String,
        saltHex: String,
        expectedHashHex: String,
        iterations: Int
    ): PinVerificationResult {
        if (!validatePinFormat(pin)) return PinVerificationResult(isValid = false, needsRehash = false)
        val salt = fromHexString(saltHex) ?: return PinVerificationResult(isValid = false, needsRehash = false)
        val expectedHash = fromHexString(expectedHashHex) ?: return PinVerificationResult(isValid = false, needsRehash = false)

        val actualIterations = if (iterations <= 0) LEGACY_ITERATIONS else iterations
        val computedHash = hashPin(pin, salt, actualIterations)
        val matches = MessageDigest.isEqual(computedHash, expectedHash)

        return if (matches) {
            PinVerificationResult(
                isValid = true,
                needsRehash = (actualIterations < CURRENT_ITERATIONS)
            )
        } else {
            PinVerificationResult(isValid = false, needsRehash = false)
        }
    }

    fun verifyPin(pin: String, saltHex: String, expectedHashHex: String): Boolean {
        return verifyPinWithIterations(pin, saltHex, expectedHashHex, CURRENT_ITERATIONS).isValid ||
               verifyPinWithIterations(pin, saltHex, expectedHashHex, LEGACY_ITERATIONS).isValid
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
