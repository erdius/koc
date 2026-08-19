package com.erdman.kofc6650.data

import android.content.Context

/**
 * Bump VERSION whenever there's something worth telling users about, and
 * update CHANGELOG to match -- shown once via a dialog the first time the
 * app launches after updating to that version.
 */
object WhatsNew {
    const val VERSION = "1.0.13"
    const val CHANGELOG = "• Long-press the app icon for quick access to Recent Photos and Submit Photos\n" +
        "• Photo captions and submitter names now show in Recent Photos\n" +
        "• Report a Problem from About sends feedback straight to us\n" +
        "• Choose System, Light, or Dark mode in About\n" +
        "• Tap the header for a QR code to join Council 6650\n" +
        "• You'll now see a short \"What's New\" summary like this one after updates"

    private const val PREFS_NAME = "whats_new"
    private const val KEY_LAST_SEEN = "last_seen_version"

    fun shouldShow(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LAST_SEEN, null) != VERSION
    }

    fun markSeen(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LAST_SEEN, VERSION)
            .apply()
    }
}
