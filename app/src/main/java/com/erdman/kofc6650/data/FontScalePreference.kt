package com.erdman.kofc6650.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit

/**
 * Persisted text-size preference, applied app-wide (including the PIN
 * gate) via CompositionLocalProvider(LocalDensity ...) at the root, so
 * every .sp-based Text size in the app scales without per-call-site changes.
 */
class FontScalePreference(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var preset by mutableStateOf(
        Preset.entries.firstOrNull { it.name == prefs.getString(KEY_PRESET, null) } ?: Preset.MEDIUM
    )
        private set

    fun choose(newPreset: Preset) {
        preset = newPreset
        prefs.edit { putString(KEY_PRESET, newPreset.name) }
    }

    enum class Preset(val label: String, val multiplier: Float) {
        SMALL("Small", 0.85f),
        MEDIUM("Medium", 1.0f),
        LARGE("Large", 1.25f),
        EXTRA_LARGE("X-Large", 1.5f),
    }

    companion object {
        private const val PREFS_NAME = "kofc_font_scale"
        private const val KEY_PRESET = "preset"
    }
}
