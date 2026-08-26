package com.erdman.kofc6650.data

import android.content.Context

/**
 * Bump VERSION whenever there's something worth telling users about, and
 * update CHANGELOG to match -- shown once via a dialog the first time the
 * app launches after updating to that version.
 */
object WhatsNew {
    const val VERSION = "1.0.15"
    const val CHANGELOG = "• Feed the Homeless signups now happen right in the app instead of an external link\n" +
        "• New Month view for the Sign Ups and Calendar tabs — toggle between Agenda and Month to browse events on a calendar grid\n" +
        "• Updated contact info on the Directors & Officers page"

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
