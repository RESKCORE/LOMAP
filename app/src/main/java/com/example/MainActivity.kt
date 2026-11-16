package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.presentation.RidePilotViewModel
import com.example.presentation.navigation.RidePilotAppShell
import com.example.ui.theme.RidePilotTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val ridePilotViewModel: RidePilotViewModel = viewModel(
                factory = RidePilotViewModel.Factory(applicationContext)
            )
            val settings by ridePilotViewModel.settingsState.collectAsStateWithLifecycle()

            RidePilotTheme(themeMode = settings.themeMode) {
                Scaffold(contentWindowInsets = WindowInsets.safeDrawing) { innerPadding ->
                    RidePilotAppShell(viewModel = ridePilotViewModel)
                }
            }
        }
    }
}
