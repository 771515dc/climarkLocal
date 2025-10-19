package com.cs407.climark

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.cs407.climark.ui.theme.ClimarkTheme

// ⬅️ Adjust this import to your actual package for MapScreen:
import com.cs407.climark.ui.theme.screens.MapScreen
// e.g., if you used a different package, change to that path.

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ClimarkTheme {
                // Show your map screen instead of the sample Greeting
                MapScreen() // or MapScreen(Modifier.fillMaxSize()) if you prefer
            }
        }
    }
}

