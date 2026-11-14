package com.daykhata.app.domain.model

import java.util.UUID

/**
 * Model representing a family contact saved for direct WhatsApp updates.
 */
data class FamilyContact(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val phone: String
) {
    companion object {
        /**
         * Validates and sanitizes a phone input:
         * - Strips non-digit characters (+, -, spaces, parentheses).
         * - Defaults 10-digit input to country code 91.
         * - Requires 10 to 15 digits total.
         * Returns sanitized digits or null if invalid.
         */
        fun sanitizePhone(input: String, defaultCountryCode: String = "91"): String? {
            val digits = input.filter { it.isDigit() }
            return when {
                digits.length == 10 -> "$defaultCountryCode$digits"
                digits.length in 11..15 -> digits
                else -> null
            }
        }
    }
}
