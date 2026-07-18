package com.matteo.chatapp.repository

import android.content.Context
import android.content.SharedPreferences
import com.matteo.chatapp.ui.theme.ThemeAccent
import com.matteo.chatapp.ui.theme.ThemeMode

actual object ThemePreferences {
    private var preferences: SharedPreferences? = null

    internal fun initialize(context: Context) {
        preferences = context.applicationContext.getSharedPreferences("chat_app_theme", Context.MODE_PRIVATE)
    }

    actual fun getThemeMode(): ThemeMode = enumValueOrDefault(preferences?.getString("theme_mode", null), ThemeMode.DARK)

    actual fun setThemeMode(mode: ThemeMode) {
        preferences?.edit()?.putString("theme_mode", mode.name)?.apply()
    }

    actual fun getThemeAccent(): ThemeAccent = enumValueOrDefault(preferences?.getString("theme_accent", null), ThemeAccent.CORAL)

    actual fun setThemeAccent(accent: ThemeAccent) {
        preferences?.edit()?.putString("theme_accent", accent.name)?.apply()
    }

    private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String?, default: T): T {
        return enumValues<T>().firstOrNull { it.name == value } ?: default
    }
}

fun initializeThemePreferences(context: Context) {
    ThemePreferences.initialize(context)
}
