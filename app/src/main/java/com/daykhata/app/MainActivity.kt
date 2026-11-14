package com.daykhata.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.daykhata.app.domain.model.SecuritySettings
import com.daykhata.app.domain.model.ViewMode
import com.daykhata.app.domain.security.AutoLockManager
import com.daykhata.app.ui.navigation.AppNavigation
import com.daykhata.app.ui.theme.DayKhataTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {

    private val autoLockManager = AutoLockManager()
    private var currentSecuritySettings = SecuritySettings()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appContainer = (application as DayKhataApplication).container

        // Pre-unlock hook: Apply FLAG_SECURE synchronously BEFORE personal figures render
        appContainer.appModeState.onPreUnlockPersonal = {
            updateSecureFlag(true)
        }

        // ProcessLifecycleOwner observer: Auto-relock to HOME when app is sent to background
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                appContainer.appModeState.setMode(ViewMode.HOME)
                autoLockManager.reset()
            }
        })

        // Observe Security Settings for "Always block screenshots" and timeout config
        lifecycleScope.launch {
            appContainer.settingsRepository.securitySettings.collect { settings ->
                currentSecuritySettings = settings
                val isPersonal = appContainer.appModeState.isPersonalMode
                updateSecureFlag(isPersonal || settings.alwaysBlockScreenshots)
            }
        }

        // Observe ViewMode to update FLAG_SECURE when returning to HOME mode
        lifecycleScope.launch {
            appContainer.appModeState.mode.collect { mode ->
                if (mode == ViewMode.HOME) {
                    updateSecureFlag(currentSecuritySettings.alwaysBlockScreenshots)
                    autoLockManager.reset()
                } else {
                    updateSecureFlag(true)
                    autoLockManager.recordInteraction()
                }
            }
        }

        // Inactivity timeout monitor while in PERSONAL mode
        lifecycleScope.launch {
            while (isActive) {
                delay(1000L)
                if (appContainer.appModeState.isPersonalMode) {
                    if (autoLockManager.isTimedOut(currentSecuritySettings.autoLockTimeout)) {
                        appContainer.appModeState.setMode(ViewMode.HOME)
                        autoLockManager.reset()
                    }
                }
            }
        }

        setContent {
            DayKhataTheme {
                AppNavigation(
                    container = appContainer,
                    activity = this
                )
            }
        }
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        autoLockManager.recordInteraction()
    }

    private fun updateSecureFlag(isSecure: Boolean) {
        if (isSecure) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}
