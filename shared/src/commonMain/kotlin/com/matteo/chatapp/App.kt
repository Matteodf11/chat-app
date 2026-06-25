package com.matteo.chatapp

import androidx.compose.runtime.*
import com.matteo.chatapp.repository.SessionManager
import com.matteo.chatapp.ui.screens.LoginScreen
import com.matteo.chatapp.ui.screens.MainScreen
import com.matteo.chatapp.ui.theme.ChatAppTheme

@Composable
fun App() {
    ChatAppTheme {
        var isLoggedIn by remember { mutableStateOf(SessionManager.isLoggedIn()) }

        if (isLoggedIn) {
            MainScreen(onLogout = {
                SessionManager.clearSession()
                isLoggedIn = false
            })
        } else {
            LoginScreen(
                onLoginSuccess = { isLoggedIn = true }
            )
        }
    }
}