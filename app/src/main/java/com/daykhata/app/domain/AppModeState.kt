package com.daykhata.app.domain

import com.daykhata.app.domain.model.ViewMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Single source of truth for the app-wide view mode (Home vs Personal).
 * Default mode on every app launch is strictly HOME.
 * Personal mode is gated behind authentication.
 */
class AppModeState {
    private val _mode = MutableStateFlow(ViewMode.HOME)
    val mode: StateFlow<ViewMode> = _mode.asStateFlow()

    private val _authRequested = MutableStateFlow(false)
    val authRequested: StateFlow<Boolean> = _authRequested.asStateFlow()

    /**
     * Hook invoked immediately before the mode changes to PERSONAL.
     * Used to apply WindowManager.LayoutParams.FLAG_SECURE before personal numbers render.
     */
    var onPreUnlockPersonal: (() -> Unit)? = null

    val isPersonalMode: Boolean
        get() = _mode.value == ViewMode.PERSONAL

    fun requestPersonalMode() {
        if (_mode.value == ViewMode.HOME) {
            _authRequested.value = true
        }
    }

    fun onAuthenticated() {
        _authRequested.value = false
        onPreUnlockPersonal?.invoke()
        _mode.value = ViewMode.PERSONAL
    }

    fun cancelAuthRequest() {
        _authRequested.value = false
    }

    fun lockToHome() {
        _authRequested.value = false
        _mode.value = ViewMode.HOME
    }

    fun setMode(newMode: ViewMode) {
        if (newMode == ViewMode.HOME) {
            lockToHome()
        } else {
            requestPersonalMode()
        }
    }

    fun toggleMode() {
        if (_mode.value == ViewMode.PERSONAL) {
            lockToHome()
        } else {
            requestPersonalMode()
        }
    }
}
