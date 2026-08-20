package com.erdman.kofc6650.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit

/**
 * Persisted agenda-vs-month display preference for the Sign Ups and
 * Calendar tabs, shared across both so switching it on one tab's toggle
 * is reflected on the other next time it's viewed.
 */
class CalendarViewModePreference(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var mode by mutableStateOf(
        Mode.entries.firstOrNull { it.name == prefs.getString(KEY_MODE, null) } ?: Mode.AGENDA
    )
        private set

    fun choose(newMode: Mode) {
        mode = newMode
        prefs.edit { putString(KEY_MODE, newMode.name) }
    }

    enum class Mode(val label: String) {
        AGENDA("Agenda"),
        MONTH("Month"),
    }

    companion object {
        private const val PREFS_NAME = "kofc_calendar_view_mode"
        private const val KEY_MODE = "mode"
    }
}
