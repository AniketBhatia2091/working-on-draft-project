package com.daykhata.app.domain

import com.daykhata.app.domain.model.ViewMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Single source of truth for the app-wide view mode (Home vs Personal).
 * Default mode on every app launch is strictly HOME.
 */
class AppModeState {
    private val _mode = MutableStateFlow(ViewMode.HOME)
    val mode: StateFlow<ViewMode> = _mode.asStateFlow()

    val isPersonalMode: Boolean
        get() = _mode.value == ViewMode.PERSONAL

    fun setMode(newMode: ViewMode) {
        _mode.value = newMode
    }

    fun toggleMode() {
        _mode.value = if (_mode.value == ViewMode.HOME) ViewMode.PERSONAL else ViewMode.HOME
    }
}
