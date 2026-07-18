package com.matteo.chatapp.repository

import com.matteo.chatapp.ui.theme.ThemeAccent
import com.matteo.chatapp.ui.theme.ThemeMode

expect object ThemePreferences {
    fun getThemeMode(): ThemeMode
    fun setThemeMode(mode: ThemeMode)
    fun getThemeAccent(): ThemeAccent
    fun setThemeAccent(accent: ThemeAccent)
}
