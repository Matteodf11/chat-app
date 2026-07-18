package com.matteo.chatapp.repository

import com.matteo.chatapp.ui.theme.ThemeAccent
import com.matteo.chatapp.ui.theme.ThemeMode
import java.util.prefs.Preferences

actual object ThemePreferences {
    private val preferences = Preferences.userRoot().node("com/matteo/chatapp")

    actual fun getThemeMode(): ThemeMode = enumValueOrDefault(preferences.get("theme_mode", null), ThemeMode.DARK)

    actual fun setThemeMode(mode: ThemeMode) {
        preferences.put("theme_mode", mode.name)
    }

    actual fun getThemeAccent(): ThemeAccent = enumValueOrDefault(preferences.get("theme_accent", null), ThemeAccent.CORAL)

    actual fun setThemeAccent(accent: ThemeAccent) {
        preferences.put("theme_accent", accent.name)
    }

    private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String?, default: T): T {
        return enumValues<T>().firstOrNull { it.name == value } ?: default
    }
}
