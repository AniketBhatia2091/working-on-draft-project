package com.daykhata.app.util

import com.daykhata.app.domain.model.FamilyContact
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CurrencyUtilsTest {

    @Test
    fun `formats standard numbers with Indian rupee grouping`() {
        assertEquals("₹0", CurrencyUtils.formatIndianRupees(0))
        assertEquals("₹80", CurrencyUtils.formatIndianRupees(80))
        assertEquals("₹150", CurrencyUtils.formatIndianRupees(150))
        assertEquals("₹999", CurrencyUtils.formatIndianRupees(999))
        assertEquals("₹1,000", CurrencyUtils.formatIndianRupees(1000))
        assertEquals("₹14,770", CurrencyUtils.formatIndianRupees(14770))
        assertEquals("₹1,05,000", CurrencyUtils.formatIndianRupees(105000))
        assertEquals("₹10,50,000", CurrencyUtils.formatIndianRupees(1050000))
        assertEquals("₹1,00,00,000", CurrencyUtils.formatIndianRupees(10000000))
    }

    @Test
    fun `formats negative numbers correctly`() {
        assertEquals("₹-500", CurrencyUtils.formatIndianRupees(-500))
        assertEquals("₹-14,770", CurrencyUtils.formatIndianRupees(-14770))
    }

    @Test
    fun `phone sanitization prepends country code 91 for 10 digit numbers`() {
        val sanitized10 = FamilyContact.sanitizePhone("9876543210")
        assertEquals("919876543210", sanitized10)

        val sanitizedWithSpecialChars = FamilyContact.sanitizePhone("+91 98765-43210")
        assertEquals("919876543210", sanitizedWithSpecialChars)

        val sanitizedInternational = FamilyContact.sanitizePhone("14155552671")
        assertEquals("14155552671", sanitizedInternational)

        // Invalid: too short or too long
        assertNull(FamilyContact.sanitizePhone("12345"))
        assertNull(FamilyContact.sanitizePhone("12345678901234567"))
    }
}
