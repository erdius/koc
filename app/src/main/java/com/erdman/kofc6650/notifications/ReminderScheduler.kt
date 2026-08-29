package com.erdman.kofc6650.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.erdman.kofc6650.data.EventDto
import com.erdman.kofc6650.data.ReminderRecord
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

    fun schedule(context: Context, event: EventDto) {
        schedule(context, event.id, event.date, event.time, event.title, event.location)
    }

    fun schedule(context: Context, record: ReminderRecord) {
        schedule(context, record.id, record.date, record.time, record.title, record.location)
    }

    private fun schedule(
        context: Context,
        eventId: String,
        date: String,
        time: String?,
        title: String,
        location: String?,
    ) {
        val trigger = triggerTime(date, time) ?: return
        val triggerMillis = trigger.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        if (triggerMillis <= System.currentTimeMillis()) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_EVENT_ID, eventId)
            putExtra(ReminderReceiver.EXTRA_TITLE, title)
            putExtra(ReminderReceiver.EXTRA_LOCATION, location)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            eventId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
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
