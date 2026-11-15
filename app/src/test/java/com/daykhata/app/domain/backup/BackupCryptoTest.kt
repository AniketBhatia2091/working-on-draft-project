package com.daykhata.app.domain.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

class BackupCryptoTest {

    private fun samplePayload(): BackupPayload {
        return BackupPayload(
            version = 1,
            exportTimestamp = 1700000000000L,
            expenses = listOf(
                BackupExpenseDto(
                    date = 1700000000000L,
                    realAmount = 250,
                    homeAmount = 300,
                    homeLabel = "Dinner",
                    personalLabel = "Dinner with dessert"
                ),
                BackupExpenseDto(
                    date = 1700086400000L,
                    realAmount = 50,
                    homeAmount = 50,
                    homeLabel = "Tea",
                    personalLabel = "Chai"
                )
            ),
            settings = BackupSettingsDto(
                budget = 20000,
                cutOffLimit = 3000,
                periodType = "MONTHLY",
                periodStartDate = 1700000000000L
            ),
            familyContacts = listOf(
                BackupContactDto("Papa", "9876543210"),
                BackupContactDto("Maa", "9876543211")
            ),
            savingsGoal = BackupGoalDto(
                savingsGoal = 5000,
                goalPeriod = "MONTHLY"
            )
        )
    }

    @Test
    fun `encrypt then decrypt round trip returns identical payload`() {
        val payload = samplePayload()
        val json = BackupCrypto.serializePayload(payload)
        val passphrase = "SecureBackupPassphrase123".toCharArray()

        val encryptedBytes = BackupCrypto.encryptBackup(json, passphrase)
        assertTrue(encryptedBytes.size > 40)

        // Decrypt with fresh passphrase char array
        val decryptedJson = BackupCrypto.decryptBackup(encryptedBytes, "SecureBackupPassphrase123".toCharArray())
        val restoredPayload = BackupCrypto.deserializePayload(decryptedJson)

        assertEquals(payload.version, restoredPayload.version)
        assertEquals(payload.expenses.size, restoredPayload.expenses.size)
        assertEquals(payload.expenses[0].realAmount, restoredPayload.expenses[0].realAmount)
        assertEquals(payload.expenses[0].homeLabel, restoredPayload.expenses[0].homeLabel)
        assertEquals(payload.expenses[0].personalLabel, restoredPayload.expenses[0].personalLabel)
        assertEquals(payload.settings.budget, restoredPayload.settings.budget)
        assertEquals(payload.familyContacts.size, restoredPayload.familyContacts.size)
        assertEquals(payload.savingsGoal.savingsGoal, restoredPayload.savingsGoal.savingsGoal)
    }

    @Test
    fun `wrong passphrase fails decryption`() {
        val payload = samplePayload()
        val json = BackupCrypto.serializePayload(payload)
        val encrypted = BackupCrypto.encryptBackup(json, "CorrectPassphrase1".toCharArray())

        try {
            BackupCrypto.decryptBackup(encrypted, "WrongPassphrase999".toCharArray())
            fail("Expected exception for wrong passphrase")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("Could not open this backup") == true)
        }
    }

    @Test
    fun `tampered ciphertext fails GCM authentication check`() {
        val payload = samplePayload()
        val json = BackupCrypto.serializePayload(payload)
        val encrypted = BackupCrypto.encryptBackup(json, "MySecretPassphrase12".toCharArray())

        // Tamper with the last byte of ciphertext/tag
        val tampered = encrypted.copyOf()
        tampered[tampered.size - 1] = (tampered[tampered.size - 1].toInt() xor 0xFF).toByte()

        try {
            BackupCrypto.decryptBackup(tampered, "MySecretPassphrase12".toCharArray())
            fail("Expected exception for tampered ciphertext")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("Could not open this backup") == true)
        }
    }

    @Test
    fun `truncated file fails with clear error`() {
        val payload = samplePayload()
        val json = BackupCrypto.serializePayload(payload)
        val encrypted = BackupCrypto.encryptBackup(json, "MySecretPassphrase12".toCharArray())

        // Truncate to less than 40 header bytes
        val truncated = encrypted.copyOfRange(0, 25)

        try {
            BackupCrypto.decryptBackup(truncated, "MySecretPassphrase12".toCharArray())
            fail("Expected exception for truncated backup")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("truncated or invalid") == true)
        }
    }

    @Test
    fun `wrong magic header is rejected`() {
        val payload = samplePayload()
        val json = BackupCrypto.serializePayload(payload)
        val encrypted = BackupCrypto.encryptBackup(json, "MySecretPassphrase12".toCharArray())

        // Corrupt magic header 'DKBK' -> 'XXXX'
        val corruptedMagic = encrypted.copyOf()
        corruptedMagic[0] = 'X'.code.toByte()
        corruptedMagic[1] = 'X'.code.toByte()

        try {
            BackupCrypto.decryptBackup(corruptedMagic, "MySecretPassphrase12".toCharArray())
            fail("Expected exception for invalid magic header")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("magic header mismatch") == true)
        }
    }

    @Test
    fun `two exports of the same data produce different ciphertexts due to random salt and nonce`() {
        val payload = samplePayload()
        val json = BackupCrypto.serializePayload(payload)
        val pass = "IdenticalPassphrase1".toCharArray()

        val export1 = BackupCrypto.encryptBackup(json, pass.copyOf())
        val export2 = BackupCrypto.encryptBackup(json, pass.copyOf())

        assertFalse("Different exports must have unique salts and nonces", export1.contentEquals(export2))
    }

    @Test
    fun `unknown newer format version is rejected`() {
        val payload = samplePayload()
        val json = BackupCrypto.serializePayload(payload)
        val encrypted = BackupCrypto.encryptBackup(json, "PassphraseTest123".toCharArray())

        // Modify version bytes (bytes 4..7) to version 99
        val byteIn = ByteArrayInputStream(encrypted)
        val dataIn = DataInputStream(byteIn)
        val magic = ByteArray(4).apply { dataIn.readFully(this) }
        dataIn.readInt() // original version

        val remaining = ByteArray(encrypted.size - 8).apply { dataIn.readFully(this) }

        val byteOut = ByteArrayOutputStream()
        val dataOut = DataOutputStream(byteOut)
        dataOut.write(magic)
        dataOut.writeInt(99) // Newer unknown version
        dataOut.write(remaining)

        try {
            BackupCrypto.decryptBackup(byteOut.toByteArray(), "PassphraseTest123".toCharArray())
            fail("Expected exception for future version 99")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("Unsupported backup version: 99") == true)
        }
    }

    @Test
    fun `serialized backup JSON does not contain any PIN hash salt or security state`() {
        val payload = samplePayload()
        val json = BackupCrypto.serializePayload(payload)

        assertFalse(json.contains("pin_hash"))
        assertFalse(json.contains("pin_salt"))
        assertFalse(json.contains("pin_iterations"))
        assertFalse(json.contains("failed_attempts"))
        assertFalse(json.contains("lockout_until"))
        assertFalse(json.contains("biometric"))
    }
}
