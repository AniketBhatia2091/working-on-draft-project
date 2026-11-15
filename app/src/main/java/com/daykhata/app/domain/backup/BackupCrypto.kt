package com.daykhata.app.domain.backup

import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.security.GeneralSecurityException
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object BackupCrypto {

    val MAGIC_HEADER = byteArrayOf('D'.code.toByte(), 'K'.code.toByte(), 'B'.code.toByte(), 'K'.code.toByte())
    const val CURRENT_VERSION = 1
    const val KDF_ITERATIONS = 210000
    private const val KEY_LENGTH = 256
    private const val SALT_LENGTH = 16
    private const val NONCE_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128
    const val MIN_PASSPHRASE_LENGTH = 8

    /**
     * Encrypts the payload JSON using PBKDF2-HMAC-SHA256 and AES-256-GCM.
     * Appends a versioned binary header:
     * [Magic 4B: 'DKBK'][Version 4B: 1][Iterations 4B: 210000][Salt 16B][Nonce 12B][Ciphertext + GCM Tag]
     */
    fun encryptBackup(payloadJson: String, passphrase: CharArray): ByteArray {
        require(passphrase.size >= MIN_PASSPHRASE_LENGTH) {
            "Passphrase must be at least $MIN_PASSPHRASE_LENGTH characters."
        }

        val random = SecureRandom()
        val salt = ByteArray(SALT_LENGTH).apply { random.nextBytes(this) }
        val nonce = ByteArray(NONCE_LENGTH).apply { random.nextBytes(this) }

        val spec = PBEKeySpec(passphrase, salt, KDF_ITERATIONS, KEY_LENGTH)
        val factory = try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        } catch (_: NoSuchAlgorithmException) {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
        }

        val keyBytes = factory.generateSecret(spec).encoded
        spec.clearPassword()
        val secretKey = SecretKeySpec(keyBytes, "AES")

        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, nonce)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)
            val cipherBytes = cipher.doFinal(payloadJson.toByteArray(Charsets.UTF_8))

            val byteOut = ByteArrayOutputStream()
            val dataOut = DataOutputStream(byteOut)
            dataOut.write(MAGIC_HEADER)
            dataOut.writeInt(CURRENT_VERSION)
            dataOut.writeInt(KDF_ITERATIONS)
            dataOut.write(salt)
            dataOut.write(nonce)
            dataOut.write(cipherBytes)
            dataOut.flush()

            return byteOut.toByteArray()
        } finally {
            keyBytes.fill(0)
            passphrase.fill('\u0000')
        }
    }

    /**
     * Decrypts the versioned backup bytes.
     * Validates magic header, format version, and GCM authentication tag.
     */
    fun decryptBackup(encryptedBytes: ByteArray, passphrase: CharArray): String {
        require(passphrase.size >= MIN_PASSPHRASE_LENGTH) {
            "Passphrase must be at least $MIN_PASSPHRASE_LENGTH characters."
        }

        if (encryptedBytes.size < 40) {
            throw IllegalArgumentException("Backup file is truncated or invalid.")
        }

        val dataIn = DataInputStream(ByteArrayInputStream(encryptedBytes))

        val magic = ByteArray(4)
        dataIn.readFully(magic)
        if (!magic.contentEquals(MAGIC_HEADER)) {
            throw IllegalArgumentException("Invalid backup file: magic header mismatch.")
        }

        val version = dataIn.readInt()
        if (version > CURRENT_VERSION) {
            throw IllegalArgumentException("Unsupported backup version: $version. Please update DayKhata.")
        }
        if (version < 1) {
            throw IllegalArgumentException("Invalid backup version: $version.")
        }

        val iterations = dataIn.readInt()
        if (iterations <= 0) {
            throw IllegalArgumentException("Invalid KDF iteration count: $iterations.")
        }

        val salt = ByteArray(SALT_LENGTH)
        dataIn.readFully(salt)

        val nonce = ByteArray(NONCE_LENGTH)
        dataIn.readFully(nonce)

        val cipherBytes = ByteArray(encryptedBytes.size - 40)
        dataIn.readFully(cipherBytes)

        val spec = PBEKeySpec(passphrase, salt, iterations, KEY_LENGTH)
        val factory = try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        } catch (_: NoSuchAlgorithmException) {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
        }

        val keyBytes = factory.generateSecret(spec).encoded
        spec.clearPassword()
        val secretKey = SecretKeySpec(keyBytes, "AES")

        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, nonce)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)
            val decryptedBytes = cipher.doFinal(cipherBytes)
            return String(decryptedBytes, Charsets.UTF_8)
        } catch (e: GeneralSecurityException) {
            throw IllegalArgumentException("Could not open this backup. Incorrect passphrase or corrupted file.", e)
        } finally {
            keyBytes.fill(0)
            passphrase.fill('\u0000')
        }
    }

    /**
     * Converts a BackupPayload to its JSON string representation.
     */
    fun serializePayload(payload: BackupPayload): String {
        val root = JSONObject()
        root.put("version", payload.version)
        root.put("exportTimestamp", payload.exportTimestamp)

        // Expenses
        val expensesArray = JSONArray()
        payload.expenses.forEach { exp ->
            val obj = JSONObject().apply {
                put("date", exp.date)
                put("realAmount", exp.realAmount)
                put("homeAmount", exp.homeAmount)
                put("homeLabel", exp.homeLabel)
                put("personalLabel", exp.personalLabel)
            }
            expensesArray.put(obj)
        }
        root.put("expenses", expensesArray)

        // Settings
        val settingsObj = JSONObject().apply {
            put("budget", payload.settings.budget)
            put("cutOffLimit", payload.settings.cutOffLimit)
            put("periodType", payload.settings.periodType)
            put("periodStartDate", payload.settings.periodStartDate)
        }
        root.put("settings", settingsObj)

        // Family Contacts
        val contactsArray = JSONArray()
        payload.familyContacts.forEach { contact ->
            val obj = JSONObject().apply {
                put("name", contact.name)
                put("phone", contact.phone)
            }
            contactsArray.put(obj)
        }
        root.put("familyContacts", contactsArray)

        // Savings Goal
        val goalObj = JSONObject().apply {
            put("savingsGoal", payload.savingsGoal.savingsGoal)
            put("goalPeriod", payload.savingsGoal.goalPeriod)
        }
        root.put("savingsGoal", goalObj)

        return root.toString()
    }

    /**
     * Parses and strictly validates a decrypted JSON string into a BackupPayload.
     */
    fun deserializePayload(jsonStr: String): BackupPayload {
        val root = JSONObject(jsonStr)
        val version = root.optInt("version", 1)
        val exportTimestamp = root.optLong("exportTimestamp", System.currentTimeMillis())

        // Ensure sensitive security keys were never serialized
        require(!root.has("pin_hash") && !root.has("pin_salt") && !root.has("pin") && !root.has("biometric")) {
            "Security violation: Backup payload contains forbidden security credentials."
        }

        // Expenses
        val expensesList = mutableListOf<BackupExpenseDto>()
        val expensesArray = root.optJSONArray("expenses") ?: JSONArray()
        for (i in 0 until expensesArray.length()) {
            val obj = expensesArray.getJSONObject(i)
            val realAmount = obj.getInt("realAmount")
            val homeAmount = obj.getInt("homeAmount")
            require(realAmount >= 0 && homeAmount >= 0) { "Expense amounts cannot be negative." }
            expensesList.add(
                BackupExpenseDto(
                    date = obj.getLong("date"),
                    realAmount = realAmount,
                    homeAmount = homeAmount,
                    homeLabel = obj.getString("homeLabel"),
                    personalLabel = obj.optString("personalLabel", "")
                )
            )
        }

        // Settings
        val settingsObj = root.getJSONObject("settings")
        val budget = settingsObj.getInt("budget")
        val cutOff = settingsObj.getInt("cutOffLimit")
        require(budget > 0) { "Budget must be greater than zero." }
        require(cutOff >= 0) { "Cut-off limit cannot be negative." }
        val settingsDto = BackupSettingsDto(
            budget = budget,
            cutOffLimit = cutOff,
            periodType = settingsObj.optString("periodType", "MONTHLY"),
            periodStartDate = settingsObj.optLong("periodStartDate", System.currentTimeMillis())
        )

        // Family Contacts
        val contactsList = mutableListOf<BackupContactDto>()
        val contactsArray = root.optJSONArray("familyContacts") ?: JSONArray()
        for (i in 0 until contactsArray.length()) {
            val obj = contactsArray.getJSONObject(i)
            contactsList.add(
                BackupContactDto(
                    name = obj.getString("name"),
                    phone = obj.getString("phone")
                )
            )
        }

        // Savings Goal
        val goalObj = root.optJSONObject("savingsGoal")
        val savingsGoal = goalObj?.optInt("savingsGoal", 0) ?: 0
        require(savingsGoal >= 0) { "Savings goal cannot be negative." }
        val goalDto = BackupGoalDto(
            savingsGoal = savingsGoal,
            goalPeriod = goalObj?.optString("goalPeriod", "NONE") ?: "NONE"
        )

        return BackupPayload(
            version = version,
            exportTimestamp = exportTimestamp,
            expenses = expensesList,
            settings = settingsDto,
            familyContacts = contactsList,
            savingsGoal = goalDto
        )
    }
}
