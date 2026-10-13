package com.matteo.chatapp

import androidx.compose.runtime.*
import com.matteo.chatapp.repository.SessionManager
import com.matteo.chatapp.repository.ThemePreferences
import com.matteo.chatapp.ui.screens.LoginScreen
import com.matteo.chatapp.ui.screens.MainScreen
import com.matteo.chatapp.ui.theme.ChatAppTheme
import com.matteo.chatapp.ui.theme.ThemeAccent
import com.matteo.chatapp.ui.theme.ThemeMode
import com.matteo.chatapp.di.AppContainer
import com.matteo.chatapp.network.SessionRefresher
import com.matteo.chatapp.network.isSessionRejected
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.CircularProgressIndicator

@Composable
fun App() {
    var themeMode by remember { mutableStateOf(ThemePreferences.getThemeMode()) }
    var themeAccent by remember { mutableStateOf(ThemePreferences.getThemeAccent()) }
    val appContainer = remember { AppContainer() }

    DisposableEffect(appContainer) {
        onDispose { appContainer.close() }
    }

    ChatAppTheme(themeMode = themeMode, themeAccent = themeAccent) {
        com.matteo.chatapp.platform.PrepareCameraCapture()
        var sessionKey by remember { mutableStateOf(0) }
        var isLoggedIn by remember { mutableStateOf(SessionManager.isLoggedIn()) }
        var isCheckingSession by remember { mutableStateOf(SessionManager.hasRememberedSession()) }
        var sessionRestoreError by remember { mutableStateOf(false) }
        var sessionRestoreAttempt by remember { mutableStateOf(0) }
        val scope = rememberCoroutineScope()

        LaunchedEffect(sessionRestoreAttempt) {
            sessionRestoreError = false
            val refreshToken = SessionManager.getRefreshToken()
            if (SessionManager.hasRememberedSession() && refreshToken != null) {
                isCheckingSession = true
                try {
                    SessionRefresher.refreshSession()
                    isLoggedIn = true
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    if (isSessionRejected(e)) SessionManager.clearSession()
                    else sessionRestoreError = true
                }
            }
            isCheckingSession = false
        }

        LaunchedEffect(isLoggedIn) {
            var nextRefreshDelay = 10 * 60 * 1000L
            while (isLoggedIn) {
                delay(nextRefreshDelay)
                val refreshToken = SessionManager.getRefreshToken() ?: break
                try {
                    SessionRefresher.refreshSession()
                    nextRefreshDelay = 10 * 60 * 1000L
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    if (isSessionRejected(e)) {
                        SessionManager.clearSession()
                        isLoggedIn = false
                        sessionKey++
                    } else {
                        nextRefreshDelay = 30_000
                    }
                }
            }
        }

        if (isCheckingSession) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (sessionRestoreError) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("No hemos podido conectar. Tu sesión sigue guardada.")
                    Button(onClick = { isCheckingSession = true; sessionRestoreAttempt++ }) {
                        Text("Reintentar")
                    }
                }
            }
        } else if (isLoggedIn) {
            com.matteo.chatapp.platform.PrepareMessageNotifications()
            key(sessionKey) {
                MainScreen(
                    createServerViewModel = appContainer::createServerViewModel,
                    createChatViewModel = appContainer::createChatViewModel,
                    createFriendViewModel = appContainer::createFriendViewModel,
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
                            scope.launch { runCatching { appContainer.authRepository.logout(refreshToken) } }
                        }
                        SessionManager.clearSession()
                        isLoggedIn = false
                        sessionKey++
                    }
                )
            }
        } else {
            LoginScreen(
                createAuthViewModel = appContainer::createAuthViewModel,
                sessionKey = sessionKey,
                onLoginSuccess = {
                    sessionKey++
                    isLoggedIn = true
                }
            )
        }
    }
}
