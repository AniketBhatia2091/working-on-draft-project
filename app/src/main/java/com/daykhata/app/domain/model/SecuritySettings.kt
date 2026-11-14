package com.daykhata.app.domain.model

enum class AutoLockTimeout(val label: String, val seconds: Long) {
    IMMEDIATELY("Immediately", 0L),
    SECONDS_30("30 seconds", 30L),
    MINUTE_1("1 minute", 60L),
    MINUTES_5("5 minutes", 300L);

    companion object {
        fun fromSeconds(seconds: Long): AutoLockTimeout {
            return entries.find { it.seconds == seconds } ?: SECONDS_30
        }
    }
}

data class SecuritySettings(
    val isPinSet: Boolean = false,
    val isBiometricEnabled: Boolean = false,
    val autoLockTimeout: AutoLockTimeout = AutoLockTimeout.SECONDS_30,
    val alwaysBlockScreenshots: Boolean = false,
    val failedAttempts: Int = 0,
    val lockoutUntilEpoch: Long = 0L
) {
    val isLockedOut: Boolean
        get() = System.currentTimeMillis() < lockoutUntilEpoch

    val remainingLockoutSeconds: Long
        get() {
            val diff = (lockoutUntilEpoch - System.currentTimeMillis()) / 1000L
            return if (diff > 0) diff else 0L
        }
}
