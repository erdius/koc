package com.erdman.kofc6650.data

import android.content.Context

/**
 * Tracks which events the user has marked "I'm Going" to, purely locally
 * -- there's no server-side RSVP concept, this is just a personal planner.
 */
object RsvpStore {
    private const val PREFS_NAME = "rsvp_store"
    private const val KEY_EVENT_IDS = "rsvp_event_ids"

    fun isGoing(context: Context, eventId: String): Boolean {
        return prefs(context).getStringSet(KEY_EVENT_IDS, emptySet())?.contains(eventId) == true
    }

    fun toggle(context: Context, eventId: String) {
        val p = prefs(context)
        val current = HashSet(p.getStringSet(KEY_EVENT_IDS, emptySet()) ?: emptySet())
        if (!current.add(eventId)) {
            current.remove(eventId)
        }
        p.edit().putStringSet(KEY_EVENT_IDS, current).apply()
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
