package com.daykhata.app.domain.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PinSecurityManagerTest {

    @Test
    fun `PIN format validation accepts 4 to 6 digits only`() {
        assertTrue(PinSecurityManager.validatePinFormat("1234"))
        assertTrue(PinSecurityManager.validatePinFormat("12345"))
        assertTrue(PinSecurityManager.validatePinFormat("123456"))

        assertFalse(PinSecurityManager.validatePinFormat("123")) // Too short
        assertFalse(PinSecurityManager.validatePinFormat("1234567")) // Too long
        assertFalse(PinSecurityManager.validatePinFormat("123a")) // Letters
        assertFalse(PinSecurityManager.validatePinFormat("")) // Empty
        assertFalse(PinSecurityManager.validatePinFormat("    ")) // Whitespace
    }

    @Test
    fun `generated salts are 32 bytes and cryptographically unique`() {
        val salt1 = PinSecurityManager.generateSalt()
        val salt2 = PinSecurityManager.generateSalt()

        assertEquals(32, salt1.size)
        assertEquals(32, salt2.size)
        assertFalse(salt1.contentEquals(salt2))
    }

    @Test
    fun `PBKDF2 hashing produces deterministic hash for same PIN and salt`() {
        val pin = "1234"
        val salt = PinSecurityManager.generateSalt()

        val hash1 = PinSecurityManager.hashPin(pin, salt)
        val hash2 = PinSecurityManager.hashPin(pin, salt)

        assertTrue(hash1.contentEquals(hash2))
        assertEquals(32, hash1.size)
    }

    @Test
    fun `different PINs produce different hashes`() {
        val salt = PinSecurityManager.generateSalt()
        val hash1 = PinSecurityManager.hashPin("1234", salt)
        val hash2 = PinSecurityManager.hashPin("1235", salt)

        assertFalse(hash1.contentEquals(hash2))
    }

    @Test
    fun `different salts produce different hashes for same PIN`() {
        val salt1 = PinSecurityManager.generateSalt()
        val salt2 = PinSecurityManager.generateSalt()
        val hash1 = PinSecurityManager.hashPin("1234", salt1)
        val hash2 = PinSecurityManager.hashPin("1234", salt2)

        assertFalse(hash1.contentEquals(hash2))
    }

    @Test
    fun `verifyPin validates correct PIN and rejects incorrect PIN`() {
        val pin = "5678"
        val salt = PinSecurityManager.generateSalt()
        val saltHex = PinSecurityManager.toHexString(salt)
        val hashHex = PinSecurityManager.toHexString(PinSecurityManager.hashPin(pin, salt))

        assertTrue(PinSecurityManager.verifyPin("5678", saltHex, hashHex))
        assertFalse(PinSecurityManager.verifyPin("0000", saltHex, hashHex))
        assertFalse(PinSecurityManager.verifyPin("567", saltHex, hashHex)) // Invalid format
        assertFalse(PinSecurityManager.verifyPin("56789", saltHex, hashHex))
    }

    @Test
    fun `hex encoding and decoding round-trips accurately`() {
        val bytes = byteArrayOf(0x00, 0x0f, 0x10, 0x7f, (-128).toByte(), (-1).toByte())
        val hex = PinSecurityManager.toHexString(bytes)
        val decoded = PinSecurityManager.fromHexString(hex)

        assertNotNull(decoded)
        assertTrue(bytes.contentEquals(decoded))
    }

    @Test
    fun `lockout duration increases with failed attempts`() {
        assertEquals(0L, PinSecurityManager.getLockoutDurationSeconds(0))
        assertEquals(0L, PinSecurityManager.getLockoutDurationSeconds(1))
        assertEquals(0L, PinSecurityManager.getLockoutDurationSeconds(4))
        // 5th attempt locks out for 30s
        assertEquals(30L, PinSecurityManager.getLockoutDurationSeconds(5))
        // 6th attempt locks out for 60s (1m)
        assertEquals(60L, PinSecurityManager.getLockoutDurationSeconds(6))
        // 7th attempt locks out for 300s (5m)
        assertEquals(300L, PinSecurityManager.getLockoutDurationSeconds(7))
        // 8+ attempts locks out for 900s (15m)
        assertEquals(900L, PinSecurityManager.getLockoutDurationSeconds(8))
        assertEquals(900L, PinSecurityManager.getLockoutDurationSeconds(15))
    }
}
