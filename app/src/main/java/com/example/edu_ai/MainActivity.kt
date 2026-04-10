// #file app/src/main/java/com/example/edu_ai/MainActivity.kt
// #version 1.0.1
// #The absolute entry point of the app that boots up the navigation UI.

package com.example.edu_ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.edu_ai.ui.navigation.AppNavigation
import com.example.edu_ai.ui.theme.Edu_AITheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Edu_AITheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Turn on the Navigation system!
                    AppNavigation()
                }
            }
        }
    }
}
