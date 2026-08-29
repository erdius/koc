package com.erdman.kofc6650.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.erdman.kofc6650.data.ReminderStore
import java.time.ZoneId

/**
 * AlarmManager alarms are wiped on reboot; this reads every armed
 * reminder back out of ReminderStore and re-schedules it (or, if its
 * trigger has already passed while the phone was off, disarms it).
 */
class BootRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val now = System.currentTimeMillis()
        for (record in ReminderStore.allArmed(context)) {
            val trigger = ReminderScheduler.triggerTime(record.date, record.time)
            val triggerMillis = trigger?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
            if (triggerMillis == null || triggerMillis <= now) {
                ReminderStore.disarm(context, record.id)
            } else {
                ReminderScheduler.schedule(context, record)
            }
        }
    }
}
