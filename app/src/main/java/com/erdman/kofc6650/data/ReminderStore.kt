package com.erdman.kofc6650.data

import android.content.Context

/**
 * Snapshot of an armed event's display fields, enough to recompute its
 * trigger time and notification content without hitting the network --
 * this is what lets BootRescheduleReceiver re-arm everything after a
 * reboot while offline.
 */
data class ReminderRecord(
    val id: String,
    val title: String,
    val date: String,
    val time: String?,
    val location: String?,
)

/**
 * Tracks which events have an armed reminder notification, purely
 * locally -- mirrors RsvpStore's key-prefixed SharedPreferences style.
 * Unlike RsvpStore this also stores a snapshot of each armed event's
 * fields (not just its id), because AlarmManager alarms are wiped on
 * reboot and BootRescheduleReceiver needs enough data to re-arm them
 * without a network fetch.
 */
object ReminderStore {
    private const val PREFS_NAME = "reminder_store"
    private const val KEY_EVENT_IDS = "reminder_event_ids"

    fun isArmed(context: Context, eventId: String): Boolean {
        return prefs(context).getStringSet(KEY_EVENT_IDS, emptySet())?.contains(eventId) == true
    }

    fun arm(context: Context, event: EventDto) {
        val p = prefs(context)
        val current = HashSet(p.getStringSet(KEY_EVENT_IDS, emptySet()) ?: emptySet())
        current.add(event.id)
        p.edit()
            .putStringSet(KEY_EVENT_IDS, current)
            .putString("title_${event.id}", event.title)
            .putString("date_${event.id}", event.date)
            .putString("time_${event.id}", event.time)
            .putString("location_${event.id}", event.location)
            .apply()
    }

    fun disarm(context: Context, eventId: String) {
        val p = prefs(context)
        val current = HashSet(p.getStringSet(KEY_EVENT_IDS, emptySet()) ?: emptySet())
        current.remove(eventId)
        p.edit()
            .putStringSet(KEY_EVENT_IDS, current)
            .remove("title_$eventId")
            .remove("date_$eventId")
            .remove("time_$eventId")
            .remove("location_$eventId")
            .apply()
    }

    fun allArmed(context: Context): List<ReminderRecord> {
        val p = prefs(context)
        val ids = p.getStringSet(KEY_EVENT_IDS, emptySet()) ?: emptySet()
        return ids.mapNotNull { id ->
            val title = p.getString("title_$id", null) ?: return@mapNotNull null
            val date = p.getString("date_$id", null) ?: return@mapNotNull null
            ReminderRecord(
                id = id,
                title = title,
                date = date,
                time = p.getString("time_$id", null),
                location = p.getString("location_$id", null),
            )
        }
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
