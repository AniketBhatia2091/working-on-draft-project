package com.daykhata.app.domain.security

import com.daykhata.app.domain.model.AutoLockTimeout

class AutoLockManager(
    private val timeProvider: () -> Long = { System.currentTimeMillis() }
) {
    private var lastInteractionEpoch: Long = timeProvider()

    fun recordInteraction() {
        lastInteractionEpoch = timeProvider()
    }

    fun isTimedOut(timeout: AutoLockTimeout): Boolean {
        if (timeout == AutoLockTimeout.IMMEDIATELY) {
            // For immediately, any idle period counts as timed out
            return true
        }
        val elapsedMillis = timeProvider() - lastInteractionEpoch
        val timeoutMillis = timeout.seconds * 1000L
        return elapsedMillis >= timeoutMillis
    }

    fun getElapsedSeconds(): Long {
        return (timeProvider() - lastInteractionEpoch) / 1000L
    }

    fun reset() {
        lastInteractionEpoch = timeProvider()
    }
}
