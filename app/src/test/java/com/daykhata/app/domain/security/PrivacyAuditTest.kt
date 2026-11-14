package com.daykhata.app.domain.security

import com.daykhata.app.domain.HomeMessageBuilder
import com.daykhata.app.domain.HomeWeeklyMessageBuilder
import com.daykhata.app.domain.model.HomeDayTotal
import com.daykhata.app.domain.model.HomeExpenseItem
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.DayOfWeek

class PrivacyAuditTest {

    @Test
    fun `daily home message contains strictly home data and no leaks`() {
        val items = listOf(
            HomeExpenseItem(homeLabel = "Milk & Bread", homeAmount = 120),
            HomeExpenseItem(homeLabel = "Vegetables", homeAmount = 250)
        )
        val privateSensitiveWord = "SecretPersonalTreat"
        val privateSensitiveAmount = "999"

        val message = HomeMessageBuilder.buildHomeMessage(
            dateMillis = 1700000000000L,
            items = items,
            homeBalance = 14630
        )

        // Verifications
        assertTrue(message.contains("Milk & Bread"))
        assertTrue(message.contains("₹120"))
        assertTrue(message.contains("Vegetables"))
        assertTrue(message.contains("₹250"))
        assertTrue(message.contains("₹370"))
        assertTrue(message.contains("₹14,630"))

        assertFalse(message.contains(privateSensitiveWord))
        assertFalse(message.contains(privateSensitiveAmount))
    }

    @Test
    fun `weekly home message contains strictly home figures and no private leaks`() {
        val days = listOf(
            HomeDayTotal(DayOfWeek.MONDAY, 1700000000000L, "Mon, 10 Nov", 200),
            HomeDayTotal(DayOfWeek.TUESDAY, 1700086400000L, "Tue, 11 Nov", 300)
        )
        val privateSavingsWord = "PrivateSavingsSecret"

        val message = HomeWeeklyMessageBuilder.buildHomeWeeklyMessage(
            weekRange = "10 Nov – 16 Nov",
            dayTotals = days,
            weekTotal = 500,
            homeBalance = 14500
        )

        assertTrue(message.contains("Weekly Expenses – 10 Nov – 16 Nov"))
        assertTrue(message.contains("Mon, 10 Nov – ₹200"))
        assertTrue(message.contains("Tue, 11 Nov – ₹300"))
        assertTrue(message.contains("Week total: ₹500"))
        assertTrue(message.contains("Balance left: ₹14,500"))

        assertFalse(message.contains(privateSavingsWord))
        assertFalse(message.contains("Savings"))
        assertFalse(message.contains("Real"))
    }

    @Test
    fun `source code contains zero android Log or println statements`() {
        val projectRoot = File(".").canonicalFile
        val srcDir = File(projectRoot, "src/main/java")
        val filesToCheck = if (srcDir.exists()) srcDir.walkTopDown().filter { it.extension == "kt" }.toList()
        else File(projectRoot, "app/src/main/java").walkTopDown().filter { it.extension == "kt" }.toList()

        assertTrue("Source files found", filesToCheck.isNotEmpty())

        val violations = mutableListOf<String>()
        for (file in filesToCheck) {
            val lines = file.readLines()
            lines.forEachIndexed { index, line ->
                val trimmed = line.trim()
                if (trimmed.startsWith("//") || trimmed.startsWith("*")) return@forEachIndexed

                if (line.contains("android.util.Log") ||
                    line.contains("Log.d(") ||
                    line.contains("Log.i(") ||
                    line.contains("Log.w(") ||
                    line.contains("Log.e(") ||
                    line.contains("Log.v(") ||
                    line.contains("println(")
                ) {
                    violations.add("${file.name}:${index + 1} - $trimmed")
                }
            }
        }

        assertTrue("Found prohibited logging statements in production code: $violations", violations.isEmpty())
    }

    @Test
    fun `manifest disables allowBackup for privacy protection`() {
        val projectRoot = File(".").canonicalFile
        val manifestFile = File(projectRoot, "src/main/AndroidManifest.xml").takeIf { it.exists() }
            ?: File(projectRoot, "app/src/main/AndroidManifest.xml")

        assertTrue("AndroidManifest.xml exists", manifestFile.exists())
        val content = manifestFile.readText()
        assertTrue("android:allowBackup must be false", content.contains("android:allowBackup=\"false\""))
    }
}
