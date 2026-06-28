package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.ui.navigation.FitPalAppContent
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    
    // Load persisted font preferences
    val sharedPrefs = getSharedPreferences("fitpal_settings", android.content.Context.MODE_PRIVATE)
    com.example.ui.theme.globalFontFamilyName = sharedPrefs.getString("font_family_name", "SansSerif") ?: "SansSerif"
    com.example.ui.theme.globalFontSizeScale = sharedPrefs.getFloat("font_size_scale", 1.0f)

    setContent {
      MyApplicationTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
          FitPalAppContent()
        }
      }
    }
  }
}

