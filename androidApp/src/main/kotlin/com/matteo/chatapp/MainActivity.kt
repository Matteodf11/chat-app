package com.matteo.chatapp

import android.os.Bundle
import android.content.Intent
import com.matteo.chatapp.platform.NotificationNavigation
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.matteo.chatapp.repository.initializeThemePreferences
import com.matteo.chatapp.platform.initializeAndroidPlatform
import com.matteo.chatapp.platform.setAndroidAppInForeground

class MainActivity : ComponentActivity() {
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent) {
        intent.getStringExtra("notification_dm_id")?.let(NotificationNavigation::openDm)
        intent.removeExtra("notification_dm_id")
    }
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
        handleNotificationIntent(intent)
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
