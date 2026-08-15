package com.erdman.kofc6650.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit

/**
 * Shared, persisted PIN state. Entering the correct PIN anywhere (Submit
 * Photos used to have its own field; now it's only asked for once, at the
 * app-level PinGateScreen) remembers it so it's never asked for again on
 * this device.
 */
class PinManager(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var savedPin by mutableStateOf(prefs.getString(KEY_SAVED_PIN, "") ?: "")
        private set

    val isUnlocked: Boolean get() = savedPin == CORRECT_PIN

    fun verify(pin: String): Boolean {
        if (pin != CORRECT_PIN) return false
        savedPin = pin
        prefs.edit { putString(KEY_SAVED_PIN, pin) }
        return true
    }

    fun clear() {
        savedPin = ""
        prefs.edit { remove(KEY_SAVED_PIN) }
    }

    companion object {
        const val CORRECT_PIN = "1882"
        private const val PREFS_NAME = "kofc_pin"
        private const val KEY_SAVED_PIN = "saved_pin"
    }
}
