package com.daykhata.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.daykhata.app.ui.navigation.AppNavigation
import com.daykhata.app.ui.theme.DayKhataTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appContainer = (application as DayKhataApplication).container

        setContent {
            DayKhataTheme {
                AppNavigation(container = appContainer)
            }
        }
    }
}
