package com.matteo.chatapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.matteo.chatapp.repository.initializeThemePreferences
import com.matteo.chatapp.platform.initializeAndroidPlatform
import com.matteo.chatapp.platform.setAndroidAppInForeground

class MainActivity : ComponentActivity() {
    override fun onStart() {
        super.onStart()
        setAndroidAppInForeground(true)
    }

    override fun onStop() {
        setAndroidAppInForeground(false)
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeThemePreferences(this)
        initializeAndroidPlatform(this)
        enableEdgeToEdge()

        setContent {
            App()
        }
    }

}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
