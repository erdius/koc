package com.erdman.kofc6650.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

/**
 * Tracks which events the user has marked "I'm Going" to, purely locally
 * -- there's no server-side RSVP concept, this is just a personal planner.
 */
object RsvpStore {
    private const val PREFS_NAME = "rsvp_store"
    private const val KEY_EVENT_IDS = "rsvp_event_ids"

    // Bumped on every toggle so a composable filtering by star state (the
    // "starred only" event list filter) can read it to subscribe to
    // changes -- isGoing() itself is a plain SharedPreferences read, not
    // Compose state, so it wouldn't otherwise trigger recomposition when a
    // different card's star is toggled.
    private var version by mutableIntStateOf(0)
    fun currentVersion(): Int = version

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
        version++
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
