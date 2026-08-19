package com.erdman.kofc6650.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit

/**
 * Persisted light/dark mode preference, read at the theme root (see
 * MainActivity.onCreate) so it overrides isSystemInDarkTheme() when the
 * user picks something other than System.
 */
class AppearanceModePreference(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var mode by mutableStateOf(
        Mode.entries.firstOrNull { it.name == prefs.getString(KEY_MODE, null) } ?: Mode.SYSTEM
    )
        private set

    fun choose(newMode: Mode) {
        mode = newMode
        prefs.edit { putString(KEY_MODE, newMode.name) }
    }

    enum class Mode(val label: String) {
        SYSTEM("System"),
        LIGHT("Light"),
        DARK("Dark"),
    }

    companion object {
        private const val PREFS_NAME = "kofc_appearance_mode"
        private const val KEY_MODE = "mode"
    }
}
