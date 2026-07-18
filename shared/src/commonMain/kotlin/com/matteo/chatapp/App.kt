package com.matteo.chatapp

import androidx.compose.runtime.*
import com.matteo.chatapp.repository.SessionManager
import com.matteo.chatapp.repository.ThemePreferences
import com.matteo.chatapp.ui.screens.LoginScreen
import com.matteo.chatapp.ui.screens.MainScreen
import com.matteo.chatapp.ui.theme.ChatAppTheme
import com.matteo.chatapp.ui.theme.ThemeAccent
import com.matteo.chatapp.ui.theme.ThemeMode
import com.matteo.chatapp.network.AuthApi
import com.matteo.chatapp.network.createHttpClient
import com.matteo.chatapp.network.SessionRefresher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.CircularProgressIndicator

@Composable
fun App() {
    var themeMode by remember { mutableStateOf(ThemePreferences.getThemeMode()) }
    var themeAccent by remember { mutableStateOf(ThemePreferences.getThemeAccent()) }
    val authApi = remember { AuthApi(createHttpClient()) }

    ChatAppTheme(themeMode = themeMode, themeAccent = themeAccent) {
        var sessionKey by remember { mutableStateOf(0) }
        var isLoggedIn by remember { mutableStateOf(SessionManager.isLoggedIn()) }
        var isCheckingSession by remember { mutableStateOf(SessionManager.hasRememberedSession()) }
        val scope = rememberCoroutineScope()

        LaunchedEffect(Unit) {
            val refreshToken = SessionManager.getRefreshToken()
            if (SessionManager.hasRememberedSession() && refreshToken != null) {
                try {
                    SessionRefresher.refreshSession()
                    isLoggedIn = true
                } catch (_: Exception) {
                    SessionManager.clearSession()
                }
            }
            isCheckingSession = false
        }

        LaunchedEffect(isLoggedIn) {
            while (isLoggedIn) {
                delay(10 * 60 * 1000L)
                val refreshToken = SessionManager.getRefreshToken() ?: break
                try {
                    SessionRefresher.refreshSession()
                } catch (_: Exception) {
                    SessionManager.clearSession()
                    isLoggedIn = false
                    sessionKey++
                }
            }
        }

        if (isCheckingSession) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (isLoggedIn) {
            key(sessionKey) {
                MainScreen(
                    sessionKey = sessionKey,
                    themeMode = themeMode,
                    themeAccent = themeAccent,
                    onThemeModeChange = {
                        themeMode = it
                        ThemePreferences.setThemeMode(it)
                    },
                    onThemeAccentChange = {
                        themeAccent = it
                        ThemePreferences.setThemeAccent(it)
                    },
                    onLogout = {
                        val refreshToken = SessionManager.getRefreshToken()
                        if (refreshToken != null) {
                            scope.launch { runCatching { authApi.logout(refreshToken) } }
                        }
                        SessionManager.clearSession()
                        isLoggedIn = false
                        sessionKey++
                    }
                )
            }
        } else {
            LoginScreen(
                sessionKey = sessionKey,
                onLoginSuccess = {
                    sessionKey++
                    isLoggedIn = true
                }
            )
        }
    }
}
