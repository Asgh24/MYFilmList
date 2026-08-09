package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.MainScreen
import com.example.ui.theme.MediaPulseTheme
import com.example.ui.util.AppThemeMode
import com.example.ui.viewmodel.MediaViewModel

class MainActivity : ComponentActivity() {

  private val mediaViewModel: MediaViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val themeMode by mediaViewModel.themeMode.collectAsStateWithLifecycle()
      val isDark = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
      }

      MediaPulseTheme(darkTheme = isDark) {
        Surface(modifier = Modifier.fillMaxSize()) {
          MainScreen(viewModel = mediaViewModel)
        }
      }
    }
  }
}

