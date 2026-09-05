package com.erdman.kofc6650.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.erdman.kofc6650.data.EventDto
import com.erdman.kofc6650.data.ReminderRecord
import com.erdman.kofc6650.data.ReminderStore
import com.erdman.kofc6650.data.RsvpStore
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Schedules and cancels the inexact AlarmManager alarm backing an armed
 * reminder. Deliberately inexact (setAndAllowWhileIdle, not
 * setExactAndAllowWhileIdle) -- see
 * docs/superpowers/specs/2026-08-29-event-reminder-notifications-design.md
 * for why: exact alarms on API 31+ need either a Play-Store-restricted
 * manifest permission or a manual settings grant, for a feature that's
 * fundamentally a "sometime in the next several minutes" nudge.
 */
object ReminderScheduler {
    private const val LEAD_TIME_HOURS = 1L
    private val TIME_FORMAT = DateTimeFormatter.ofPattern("h:mm a", Locale.US)

    // Event times are the council's own wall-clock time (KofcRepository's
    // formatTime extracts "6:00 PM" straight from the source's ISO offset,
    // not converted to the device's timezone), so combining them into an
    // absolute instant must anchor to the council's actual timezone rather
    // than whatever timezone the device happens to be in -- otherwise a
    // 6pm Eastern event becomes 6pm Pacific on a Pacific-configured phone,
    // three hours off from the real event time.
    val COUNCIL_ZONE: ZoneId = ZoneId.of("America/New_York")

    /** Null if date can't be parsed. Public so BootRescheduleReceiver can
     *  recompute a stored record's trigger to decide whether it already
     *  passed. */
    fun triggerTime(date: String, time: String?): LocalDateTime? {
        val day = try {
            LocalDate.parse(date)
        } catch (e: Exception) {
            return null
        }
        val parsedTime = time?.takeIf { it.isNotBlank() }?.let {
            try {
                LocalTime.parse(it, TIME_FORMAT)
            } catch (e: Exception) {
                null
            }
        }
        return if (parsedTime != null) {
            LocalDateTime.of(day, parsedTime).minusHours(LEAD_TIME_HOURS)
        } else {
            LocalDateTime.of(day, LocalTime.of(8, 0))
        }
    }

    /** Returns whether an alarm was actually scheduled -- false (a silent
     *  no-op) when the date can't be parsed or the trigger has already
     *  passed. Callers must check this before recording the reminder as
     *  armed, otherwise a UI/store race with schedule()'s own timing check
     *  can mark something armed that never actually got an alarm. */
    fun schedule(context: Context, event: EventDto): Boolean =
        schedule(context, event.id, event.date, event.time, event.title, event.location)

    fun schedule(context: Context, record: ReminderRecord): Boolean =
        schedule(context, record.id, record.date, record.time, record.title, record.location)

    private fun schedule(
        context: Context,
        eventId: String,
        date: String,
        time: String?,
        title: String,
        location: String?,
    ): Boolean {
        val trigger = triggerTime(date, time) ?: return false
        val triggerMillis = trigger.atZone(COUNCIL_ZONE).toInstant().toEpochMilli()
        if (triggerMillis <= System.currentTimeMillis()) return false

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_EVENT_ID, eventId)
            putExtra(ReminderReceiver.EXTRA_TITLE, title)
            putExtra(ReminderReceiver.EXTRA_LOCATION, location)
            putExtra(ReminderReceiver.EXTRA_HAS_TIME, !time.isNullOrBlank())
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            eventId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
        return true
    }

    /**
     * Updates a pending reminder's notification content (title/location)
     * without touching its already-scheduled trigger time.
     * PendingIntent.FLAG_UPDATE_CURRENT replaces the extras of an existing
     * pending intent matched by request code in place, so AlarmManager's
     * existing alarm registration keeps firing at the same time but with
     * these fresh extras -- used by reconcile() so a cosmetic title/
     * location change on an unchanged-time event doesn't require touching
     * (and risking) the alarm's timing at all.
     */
    private fun refreshExtras(context: Context, event: EventDto) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_EVENT_ID, event.id)
            putExtra(ReminderReceiver.EXTRA_TITLE, event.title)
            putExtra(ReminderReceiver.EXTRA_LOCATION, event.location)
            putExtra(ReminderReceiver.EXTRA_HAS_TIME, !event.time.isNullOrBlank())
        }
        PendingIntent.getBroadcast(
            context,
            event.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /**
     * Re-syncs armed reminders against a fresh events fetch -- call after
     * every successful refresh. Nothing else ever cancels/reschedules an
     * alarm when the *council's* calendar changes (only the manual
     * un-star action cancels one), so without this, an event that gets
     * cancelled or rescheduled upstream leaves a stale alarm armed with no
     * card left in the feed to un-star it from.
     */
    fun reconcile(context: Context, freshEvents: List<EventDto>) {
        val freshById = freshEvents.associateBy { it.id }
        for (record in ReminderStore.allArmed(context)) {
            val event = freshById[record.id]
            if (event == null) {
                // Gone from the feed (cancelled, or fell outside the fetch
                // window) -- nothing to remind about, and no card left for
                // the user to un-star from, so un-star it here instead.
                cancel(context, record.id)
                ReminderStore.disarm(context, record.id)
                RsvpStore.unstar(context, record.id)
                continue
            }
            if (event.date == record.date && event.time == record.time) {
                // Nothing that affects the alarm's trigger changed --
                // deliberately does NOT cancel/reschedule here. These
                // alarms are inexact (setAndAllowWhileIdle), so one can be
                // sitting delayed-but-pending just past its nominal trigger
                // time; cancelling and re-checking it here would recompute
                // that same trigger as already passed and disarm a reminder
                // that was actually about to fire. Still refresh the
                // pending intent's extras and the stored snapshot in case
                // title/location changed cosmetically, so the eventual
                // notification and the store both reflect current data.
                refreshExtras(context, event)
                ReminderStore.arm(context, event)
                continue
            }
            // Date/time changed -- the previously scheduled alarm (for the
            // old time) is stale regardless of whether the new one can be
            // scheduled, so it needs cancelling either way.
            cancel(context, record.id)
            if (schedule(context, event)) {
                ReminderStore.arm(context, event)
            } else {
                // The event's new trigger time has already passed --
                // disarm rather than leave a phantom "armed" record with
                // no alarm behind it.
                ReminderStore.disarm(context, record.id)
            }
        }
    }

    fun cancel(context: Context, eventId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            eventId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.cancel(pendingIntent)
    }
}
