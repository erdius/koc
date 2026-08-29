# Event Reminder Notifications Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a bell icon to event cards (Android + iOS) that arms a fixed-lead-time local notification reminder for that event, independent of the device calendar and the existing "I'm Going" star.

**Architecture:** A per-platform local-only notification pipeline: a small persistence store tracks which events are armed, a scheduler computes the trigger time (1 hour before the event, or 8:00 AM same-day if the event has no listed time) and hands it to the platform's native local-notification API (`AlarmManager` + `NotificationManager` on Android, `UNUserNotificationCenter` on iOS), and the event card's action row gets a third icon that toggles the whole thing. No server, no push infrastructure, no new third-party dependency on either platform.

**Tech Stack:** Kotlin + Jetpack Compose + `AlarmManager`/`NotificationManager` (Android); Swift + SwiftUI + `UserNotifications` (iOS).

**Spec:** `docs/superpowers/specs/2026-08-29-event-reminder-notifications-design.md`

## Global Constraints

- Fixed lead time, no per-event configuration: **1 hour before** `event.date`+`event.time`; if `time` is blank, trigger at **8:00 AM on `date`** instead (not 8:00 AM minus an hour — 8:00 AM *is* the trigger).
- Bell icon sits **between** the existing "Add to Calendar" icon and the "Share" icon in the event-card action row, styled identically to those two (same navy rounded box, gold tint, 10dp/10pt padding).
- Outline bell = not armed, filled bell = armed. Toggle on tap, no dialog, no picker.
- Completely independent of `RsvpStore` (the star) — separate backing store, no shared state.
- Bell icon is hidden entirely for past events.
- Android: **inexact** alarms only (`AlarmManager.setAndAllowWhileIdle`, never `setExactAndAllowWhileIdle`) — no `SCHEDULE_EXACT_ALARM` permission is requested or declared. See spec for why.
- Android: notification channel importance is `IMPORTANCE_HIGH`. Status-bar icon is the framework's `android.R.drawable.ic_popup_reminder` — no new drawable asset.
- Android: reminders must survive a device reboot (re-armed via a `BOOT_COMPLETED` receiver reading the persisted store) since `AlarmManager` alarms are wiped on reboot.
- iOS: authorization is requested with `[.alert, .sound, .badge]`. Armed notifications carry `content.badge = 1`.
- iOS: the badge clears whenever the app becomes active for any reason (not just via the notification tap) — no per-notification read-tracking.
- Tapping a fired notification just opens the app via standard launch on both platforms — no deep link to the specific event or tab.
- Neither repo has any unit/instrumented test target for this layer (confirmed: no `app/src/test` or `app/src/androidTest` Kotlin files in `KofC6650`; iOS only has a screenshot UI test target, no unit test target). Verification throughout this plan is manual build+install+observe, matching how prior UI work in these repos (the QR-icon swap, Feed the Homeless multi-date) was verified — do not add a test scaffold as part of this work.
- Android build: `export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home` before any `./gradlew` command. `minSdk 26`, `targetSdk 36`, `compileSdk 36`.
- iOS build: after creating any new `.swift` file, run `xcodegen generate` from `~/Projects/KofC6650-iOS` before building, or Xcode won't see the file. Deployment target iOS 16. Build/install to the physical iPhone with:
  ```bash
  xcodebuild -project KofC6650.xcodeproj -scheme KofC6650 -configuration Debug \
    -destination 'id=C9AD2513-FAAD-5608-B462-A8A18E783F8C' -derivedDataPath build_derived build
  xcrun devicectl device install app --device C9AD2513-FAAD-5608-B462-A8A18E783F8C \
    build_derived/Build/Products/Debug-iphoneos/KofC6650.app
  xcrun devicectl device process launch --device C9AD2513-FAAD-5608-B462-A8A18E783F8C com.erdman.kofc6650
  ```
  (`devicectl` cannot screenshot a physical device — visual verification requires David to check the screen himself.)

---

## Task 1: Android — `ReminderStore` persistence

**Files:**
- Create: `app/src/main/java/com/erdman/kofc6650/data/ReminderStore.kt`

**Interfaces:**
- Consumes: `com.erdman.kofc6650.data.EventDto` (existing — fields `id: String`, `title: String`, `date: String`, `time: String?`, `location: String?`).
- Produces: `ReminderRecord(id, title, date, time, location)`; `ReminderStore.isArmed(context, eventId): Boolean`; `ReminderStore.arm(context, event: EventDto)`; `ReminderStore.disarm(context, eventId: String)`; `ReminderStore.allArmed(context): List<ReminderRecord>`. Tasks 2–4 depend on these exact names.

- [ ] **Step 1: Write `ReminderStore.kt`**

```kotlin
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
```

- [ ] **Step 2: Verify it compiles**

Run: `cd ~/Projects/KofC6650 && export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home && ./gradlew :app:compileDebugKotlin -q`
Expected: no output, exit code 0.

- [ ] **Step 3: Commit**

```bash
cd ~/Projects/KofC6650
git add app/src/main/java/com/erdman/kofc6650/data/ReminderStore.kt
git commit -m "Add ReminderStore for persisting armed event reminders"
```

---

## Task 2: Android — scheduler + receiver (the notification engine)

**Files:**
- Create: `app/src/main/java/com/erdman/kofc6650/notifications/ReminderScheduler.kt`
- Create: `app/src/main/java/com/erdman/kofc6650/notifications/ReminderReceiver.kt`

**Interfaces:**
- Consumes: `com.erdman.kofc6650.data.EventDto`, `com.erdman.kofc6650.data.ReminderRecord` (Task 1).
- Produces: `ReminderScheduler.triggerTime(date: String, time: String?): LocalDateTime?`; `ReminderScheduler.schedule(context, event: EventDto)`; `ReminderScheduler.schedule(context, record: ReminderRecord)`; `ReminderScheduler.cancel(context, eventId: String)`. `ReminderReceiver` (a `BroadcastReceiver`, referenced by class in Task 3's manifest work). Task 3 and Task 4 depend on these exact names.

- [ ] **Step 1: Write `ReminderScheduler.kt`**

```kotlin
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
```

- [ ] **Step 2: Write `ReminderReceiver.kt`**

```kotlin
package com.erdman.kofc6650.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.erdman.kofc6650.MainActivity

/**
 * Fires when an armed reminder's AlarmManager alarm goes off; builds and
 * posts the actual system notification.
 */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val eventId = intent.getStringExtra(EXTRA_EVENT_ID) ?: return
        val title = intent.getStringExtra(EXTRA_TITLE) ?: return
        val location = intent.getStringExtra(EXTRA_LOCATION)

        ensureChannel(context)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            eventId.hashCode(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val body = if (location.isNullOrBlank()) "in 1 hour" else "in 1 hour · $location"
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(eventId.hashCode(), notification)
        } catch (e: SecurityException) {
            // Notification permission was revoked after this reminder was armed; nothing to do.
        }
    }

    private fun ensureChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Event Reminders",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Reminders for KofC 6650 events you've set a bell for"
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "event_reminders"
        const val EXTRA_EVENT_ID = "event_id"
        const val EXTRA_TITLE = "title"
        const val EXTRA_LOCATION = "location"
    }
}
```

- [ ] **Step 3: Verify it compiles**

Run: `cd ~/Projects/KofC6650 && export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home && ./gradlew :app:compileDebugKotlin -q`
Expected: no output, exit code 0.

- [ ] **Step 4: Commit**

```bash
cd ~/Projects/KofC6650
git add app/src/main/java/com/erdman/kofc6650/notifications/ReminderScheduler.kt \
        app/src/main/java/com/erdman/kofc6650/notifications/ReminderReceiver.kt
git commit -m "Add inexact-alarm reminder scheduler and notification receiver"
```

---

## Task 3: Android — boot rescheduling + manifest wiring

**Files:**
- Create: `app/src/main/java/com/erdman/kofc6650/notifications/BootRescheduleReceiver.kt`
- Modify: `app/src/main/AndroidManifest.xml`

**Interfaces:**
- Consumes: `ReminderStore.allArmed(context)` (Task 1), `ReminderScheduler.triggerTime/schedule` (Task 2), `ReminderReceiver` (Task 2, referenced by manifest `<receiver>` name).
- Produces: `BootRescheduleReceiver` registered for `BOOT_COMPLETED`. Nothing later depends on new names from this task.

- [ ] **Step 1: Write `BootRescheduleReceiver.kt`**

```kotlin
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
```

- [ ] **Step 2: Add permissions and receivers to `AndroidManifest.xml`**

In `app/src/main/AndroidManifest.xml`, find:

```xml
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" android:maxSdkVersion="28" />
```

Replace with:

```xml
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" android:maxSdkVersion="28" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
```

Then find:

```xml
        <receiver
            android:name=".widget.NextEventWidgetProvider"
            android:exported="false">
            <intent-filter>
                <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
            </intent-filter>
            <meta-data
                android:name="android.appwidget.provider"
                android:resource="@xml/next_event_widget_info" />
        </receiver>
    </application>
</manifest>
```

Replace with:

```xml
        <receiver
            android:name=".widget.NextEventWidgetProvider"
            android:exported="false">
            <intent-filter>
                <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
            </intent-filter>
            <meta-data
                android:name="android.appwidget.provider"
                android:resource="@xml/next_event_widget_info" />
        </receiver>

        <receiver
            android:name=".notifications.ReminderReceiver"
            android:exported="false" />

        <receiver
            android:name=".notifications.BootRescheduleReceiver"
            android:exported="false">
            <intent-filter>
                <action android:name="android.intent.action.BOOT_COMPLETED" />
            </intent-filter>
        </receiver>
    </application>
</manifest>
```

- [ ] **Step 3: Verify it compiles**

Run: `cd ~/Projects/KofC6650 && export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home && ./gradlew :app:compileDebugKotlin -q`
Expected: no output, exit code 0.

- [ ] **Step 4: Commit**

```bash
cd ~/Projects/KofC6650
git add app/src/main/java/com/erdman/kofc6650/notifications/BootRescheduleReceiver.kt \
        app/src/main/AndroidManifest.xml
git commit -m "Re-arm reminders on boot; wire up manifest permissions and receivers"
```

---

## Task 4: Android — bell icon on the event card + full manual verification

**Files:**
- Modify: `app/src/main/java/com/erdman/kofc6650/MainActivity.kt` (imports near the top; `EventCard` composable, currently around lines 2893–3136 — exact line numbers will have shifted from earlier edits in this session, search for `private fun EventCard(`)

**Interfaces:**
- Consumes: `ReminderStore.isArmed/arm/disarm` (Task 1), `ReminderScheduler.schedule/cancel` (Task 2).
- Produces: nothing new consumed by later tasks — this is the last Android task.

- [ ] **Step 1: Add imports**

In `MainActivity.kt`'s import block, add these four (matching the existing alphabetized-ish grouping near the other `androidx.compose.material.icons.*` and `com.erdman.kofc6650.*` imports):

```kotlin
import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Notifications
import androidx.core.content.ContextCompat
import com.erdman.kofc6650.data.ReminderStore
import com.erdman.kofc6650.notifications.ReminderScheduler
```

(`android.os.Build` is already imported in this file — reused below, not re-added.)

- [ ] **Step 2: Add armed-state and a past-event check inside `EventCard`**

Find this existing line inside `private fun EventCard(...)`:

```kotlin
    var isGoing by remember(event.id) { mutableStateOf(RsvpStore.isGoing(context, event.id)) }
```

Add directly below it:

```kotlin
    var isReminderArmed by remember(event.id) { mutableStateOf(ReminderStore.isArmed(context, event.id)) }
    val isPastEvent = remember(event.id) {
        val day = try { LocalDate.parse(event.date) } catch (e: Exception) { null }
        day != null && day.isBefore(LocalDate.now())
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            isReminderArmed = true
            ReminderStore.arm(context, event)
            ReminderScheduler.schedule(context, event)
        } else {
            Toast.makeText(context, "Notification permission is needed for event reminders.", Toast.LENGTH_SHORT).show()
        }
    }
```

- [ ] **Step 3: Insert the bell icon Box between the calendar and share Boxes**

Find this existing block inside the same `EventCard`'s `FlowRow`:

```kotlin
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(KofcNavy)
                        .clickable { showAddToCalendarSheet = true }
                        .padding(10.dp),
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = "Add to My Calendar", tint = KofcGold)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(KofcNavy)
                        .clickable { shareEvent(context, event) }
                        .padding(10.dp),
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share event", tint = KofcGold)
                }
```

Replace it with (adds the bell `Box` in between, guarded by `isPastEvent`):

```kotlin
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(KofcNavy)
                        .clickable { showAddToCalendarSheet = true }
                        .padding(10.dp),
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = "Add to My Calendar", tint = KofcGold)
                }
                if (!isPastEvent) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(KofcNavy)
                            .clickable {
                                if (isReminderArmed) {
                                    isReminderArmed = false
                                    ReminderStore.disarm(context, event.id)
                                    ReminderScheduler.cancel(context, event.id)
                                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                                ) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    isReminderArmed = true
                                    ReminderStore.arm(context, event)
                                    ReminderScheduler.schedule(context, event)
                                }
                            }
                            .padding(10.dp),
                    ) {
                        Icon(
                            if (isReminderArmed) Icons.Filled.Notifications else Icons.Outlined.Notifications,
                            contentDescription = if (isReminderArmed) "Reminder set" else "Set a reminder",
                            tint = KofcGold,
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(KofcNavy)
                        .clickable { shareEvent(context, event) }
                        .padding(10.dp),
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share event", tint = KofcGold)
                }
```

- [ ] **Step 4: Build and install**

```bash
cd ~/Projects/KofC6650
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
./gradlew :app:assembleDebug -q
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.erdman.kofc6650/.MainActivity
```

Expected: build succeeds, install succeeds, app launches.

- [ ] **Step 5: Manually verify the bell icon and permission prompt**

Open any upcoming event card (Calendar or Volunteer Sign Ups tab). Confirm:
- A bell icon (outline) appears between the calendar icon and the share icon.
- Tapping it triggers the Android notification-permission system prompt (first time only). Grant it.
- The bell switches to filled/gold-tinted.
- Open a past event's card (browse back a month in the calendar view) — confirm no bell icon appears there at all.

- [ ] **Step 6: Manually verify a reminder actually fires**

Events come live from the real council Google Calendar, so don't edit a
real event's time to force a near-future trigger. Instead, temporarily
override the computed trigger in code, test, then revert:

In `app/src/main/java/com/erdman/kofc6650/notifications/ReminderScheduler.kt`,
temporarily change the first line of `triggerTime` from:

```kotlin
    fun triggerTime(date: String, time: String?): LocalDateTime? {
        val day = try {
```

to:

```kotlin
    fun triggerTime(date: String, time: String?): LocalDateTime? {
        return LocalDateTime.now().plusMinutes(2) // TEMPORARY test override, revert before committing
        val day = try {
```

Rebuild/reinstall per Step 4, arm a reminder on any upcoming event, then:
- Wait for the notification to appear as a heads-up banner with the event title and "in 1 hour" (or the location-suffixed variant).
- Tap it — confirm the app opens.
- Arm another test reminder, then tap the bell again before it fires to disarm it — confirm no notification appears at its trigger time.

Then revert the temporary override before continuing:

```bash
cd ~/Projects/KofC6650
git checkout -- app/src/main/java/com/erdman/kofc6650/notifications/ReminderScheduler.kt
```

- [ ] **Step 7: Manually verify reboot survival**

With a reminder armed and its trigger still in the future, run:

```bash
adb shell am broadcast -a android.intent.action.BOOT_COMPLETED -p com.erdman.kofc6650
```

(This simulates the boot broadcast without a full device reboot.) Confirm the reminder still fires at its original trigger time — if useful, check via `adb shell dumpsys alarm | grep -A2 com.erdman.kofc6650` that the alarm is present after the broadcast.

- [ ] **Step 8: Commit**

```bash
cd ~/Projects/KofC6650
git add app/src/main/java/com/erdman/kofc6650/MainActivity.kt
git commit -m "Add reminder bell icon to event cards"
```

---

## Task 5: iOS — `ReminderStore` persistence

**Files:**
- Create: `KofC6650/ReminderStore.swift`

**Interfaces:**
- Consumes: nothing new (plain `Foundation`/`UserDefaults`).
- Produces: `ReminderStore.shared: ReminderStore` (singleton `ObservableObject`), `.isArmed(_ eventId: String) -> Bool`, `.arm(_ eventId: String)`, `.disarm(_ eventId: String)`. Task 7 depends on these exact names.

- [ ] **Step 1: Write `ReminderStore.swift`**

```swift
import Foundation

/// Tracks which events have an armed reminder notification, purely
/// locally -- UNUserNotificationCenter durably owns the actual scheduled
/// request, so this store exists only to drive the bell icon's
/// filled/outline state. A singleton (matches RsvpStore's pattern) so
/// arming a reminder on one tab's card is instantly reflected on the same
/// event's card on another tab, since SwiftUI's TabView keeps every tab's
/// view alive rather than recreating it on switch.
final class ReminderStore: ObservableObject {
    static let shared = ReminderStore()

    @Published private var ids: Set<String> {
        didSet { UserDefaults.standard.set(Array(ids), forKey: Self.key) }
    }

    private static let key = "remindedEventIds"

    private init() {
        ids = Set(UserDefaults.standard.stringArray(forKey: Self.key) ?? [])
    }

    func isArmed(_ eventId: String) -> Bool {
        ids.contains(eventId)
    }

    func arm(_ eventId: String) {
        ids.insert(eventId)
    }

    func disarm(_ eventId: String) {
        ids.remove(eventId)
    }
}
```

- [ ] **Step 2: Regenerate the Xcode project and verify it compiles**

```bash
cd ~/Projects/KofC6650-iOS
xcodegen generate
xcodebuild -project KofC6650.xcodeproj -scheme KofC6650 -configuration Debug \
  -destination 'id=C9AD2513-FAAD-5608-B462-A8A18E783F8C' -derivedDataPath build_derived build
```

Expected: `** BUILD SUCCEEDED **`.

- [ ] **Step 3: Commit**

```bash
cd ~/Projects/KofC6650-iOS
git add KofC6650/ReminderStore.swift KofC6650.xcodeproj
git commit -m "Add ReminderStore for tracking armed event reminders"
```

---

## Task 6: iOS — `ReminderScheduler`

**Files:**
- Create: `KofC6650/ReminderScheduler.swift`

**Interfaces:**
- Consumes: `EventDto` (existing, `Models/CalendarModels.swift` — fields `id`, `title`, `date`, `time`, `location`).
- Produces: `ReminderScheduler.AuthResult` (`.scheduled` / `.denied`), `ReminderScheduler.triggerDate(for: EventDto) -> Date?`, `ReminderScheduler.schedule(_ event: EventDto) async -> AuthResult`, `ReminderScheduler.cancel(_ eventId: String)`. Task 7 depends on these exact names.

- [ ] **Step 1: Write `ReminderScheduler.swift`**

```swift
import Foundation
import UserNotifications

/// Schedules and cancels the local notification backing an armed
/// reminder. Date/time combination logic mirrors CalendarExporter's
/// (UTC-anchored "yyyy-MM-dd" day, combined with a locally-parsed "h:mm a"
/// time) for consistency with the existing Add-to-Calendar path. See
/// docs/superpowers/specs/2026-08-29-event-reminder-notifications-design.md.
enum ReminderScheduler {
    enum AuthResult {
        case scheduled
        case denied
    }

    private static let isoDateFormatter: DateFormatter = {
        let f = DateFormatter()
        f.dateFormat = "yyyy-MM-dd"
        f.locale = Locale(identifier: "en_US_POSIX")
        f.timeZone = TimeZone(identifier: "UTC")
        return f
    }()

    private static let timeFormatter: DateFormatter = {
        let f = DateFormatter()
        f.dateFormat = "h:mm a"
        f.locale = Locale(identifier: "en_US")
        return f
    }()

    /// Nil if event.date can't be parsed. 1 hour before event start, or
    /// 8:00 AM on the event's date if it has no listed time.
    static func triggerDate(for event: EventDto) -> Date? {
        guard let day = isoDateFormatter.date(from: event.date) else { return nil }
        let calendar = Calendar(identifier: .gregorian)

        let parsedTime = event.time.flatMap { $0.isEmpty ? nil : timeFormatter.date(from: $0) }
        var components = calendar.dateComponents([.year, .month, .day], from: day)
        if let parsedTime {
            let timeComponents = calendar.dateComponents([.hour, .minute], from: parsedTime)
            components.hour = timeComponents.hour
            components.minute = timeComponents.minute
            guard let eventStart = calendar.date(from: components) else { return nil }
            return calendar.date(byAdding: .hour, value: -1, to: eventStart)
        } else {
            components.hour = 8
            components.minute = 0
            return calendar.date(from: components)
        }
    }

    @discardableResult
    static func schedule(_ event: EventDto) async -> AuthResult {
        guard await requestAuthorization() else { return .denied }
        guard let trigger = triggerDate(for: event), trigger > Date() else { return .scheduled }

        let content = UNMutableNotificationContent()
        content.title = event.title
        if let location = event.location, !location.trimmingCharacters(in: .whitespaces).isEmpty {
            content.body = "in 1 hour · \(location)"
        } else {
            content.body = "in 1 hour"
        }
        content.sound = .default
        content.badge = 1

        let calendar = Calendar(identifier: .gregorian)
        let dateComponents = calendar.dateComponents([.year, .month, .day, .hour, .minute], from: trigger)
        let unTrigger = UNCalendarNotificationTrigger(dateMatching: dateComponents, repeats: false)
        let request = UNNotificationRequest(identifier: event.id, content: content, trigger: unTrigger)
        try? await UNUserNotificationCenter.current().add(request)
        return .scheduled
    }

    static func cancel(_ eventId: String) {
        UNUserNotificationCenter.current().removePendingNotificationRequests(withIdentifiers: [eventId])
    }

    private static func requestAuthorization() async -> Bool {
        let center = UNUserNotificationCenter.current()
        let settings = await center.notificationSettings()
        switch settings.authorizationStatus {
        case .authorized, .provisional:
            return true
        case .notDetermined:
            return (try? await center.requestAuthorization(options: [.alert, .sound, .badge])) ?? false
        default:
            return false
        }
    }
}
```

- [ ] **Step 2: Regenerate the Xcode project and verify it compiles**

```bash
cd ~/Projects/KofC6650-iOS
xcodegen generate
xcodebuild -project KofC6650.xcodeproj -scheme KofC6650 -configuration Debug \
  -destination 'id=C9AD2513-FAAD-5608-B462-A8A18E783F8C' -derivedDataPath build_derived build
```

Expected: `** BUILD SUCCEEDED **`.

- [ ] **Step 3: Commit**

```bash
cd ~/Projects/KofC6650-iOS
git add KofC6650/ReminderScheduler.swift KofC6650.xcodeproj
git commit -m "Add local-notification scheduler for event reminders"
```

---

## Task 7: iOS — bell button on the event card

**Files:**
- Modify: `KofC6650/Views/EventCardView.swift`

**Interfaces:**
- Consumes: `ReminderStore.shared` (Task 5), `ReminderScheduler.schedule/cancel` (Task 6), `EventDateFormatter.isTodayOrLater(_:)` (existing, `KofC6650/EventDateFormatter.swift`).
- Produces: nothing new consumed by later tasks.

- [ ] **Step 1: Add the reminder store, armed-state, and status-message state**

Find:

```swift
    @ObservedObject private var rsvpStore = RsvpStore.shared
    @ObservedObject private var feedTheHomelessStatusStore = FeedTheHomelessStatusStore.shared
    @State private var showAddToCalendarSheet = false
    @State private var showFeedTheHomelessSheet = false
    @State private var calendarStatusMessage: String?

    private var isGoing: Bool { rsvpStore.isGoing(event.id) }
```

Replace with:

```swift
    @ObservedObject private var rsvpStore = RsvpStore.shared
    @ObservedObject private var feedTheHomelessStatusStore = FeedTheHomelessStatusStore.shared
    @ObservedObject private var reminderStore = ReminderStore.shared
    @State private var showAddToCalendarSheet = false
    @State private var showFeedTheHomelessSheet = false
    @State private var calendarStatusMessage: String?
    @State private var reminderStatusMessage: String?

    private var isGoing: Bool { rsvpStore.isGoing(event.id) }
    private var isReminderArmed: Bool { reminderStore.isArmed(event.id) }
```

- [ ] **Step 2: Add the reminder button, insert it into the action row, and add its alert**

Find:

```swift
                HStack(spacing: 8) {
                    if event.title == "Feed the Homeless" {
                        feedTheHomelessButton
                    } else if let signupUrl = event.signupUrl, let url = URL(string: signupUrl) {
                        actionButton(title: "Sign Up to Volunteer →", url: url)
                    } else if let linkUrl = event.linkUrl, let url = URL(string: linkUrl) {
                        actionButton(title: "Open Link →", url: url)
                    }

                    addToCalendarButton
                    shareButton
                }
```

Replace with:

```swift
                HStack(spacing: 8) {
                    if event.title == "Feed the Homeless" {
                        feedTheHomelessButton
                    } else if let signupUrl = event.signupUrl, let url = URL(string: signupUrl) {
                        actionButton(title: "Sign Up to Volunteer →", url: url)
                    } else if let linkUrl = event.linkUrl, let url = URL(string: linkUrl) {
                        actionButton(title: "Open Link →", url: url)
                    }

                    addToCalendarButton
                    if EventDateFormatter.isTodayOrLater(event.date) {
                        reminderButton
                    }
                    shareButton
                }
```

Find the `.alert(` block for `calendarStatusMessage`:

```swift
        .alert(
            "Add to My Calendar",
            isPresented: Binding(
                get: { calendarStatusMessage != nil },
                set: { if !$0 { calendarStatusMessage = nil } }
            )
        ) {
            Button("OK") { calendarStatusMessage = nil }
        } message: {
            Text(calendarStatusMessage ?? "")
        }
    }
```

Add a second alert directly after it, still inside the same view body:

```swift
        .alert(
            "Add to My Calendar",
            isPresented: Binding(
                get: { calendarStatusMessage != nil },
                set: { if !$0 { calendarStatusMessage = nil } }
            )
        ) {
            Button("OK") { calendarStatusMessage = nil }
        } message: {
            Text(calendarStatusMessage ?? "")
        }
        .alert(
            "Reminder",
            isPresented: Binding(
                get: { reminderStatusMessage != nil },
                set: { if !$0 { reminderStatusMessage = nil } }
            )
        ) {
            Button("OK") { reminderStatusMessage = nil }
        } message: {
            Text(reminderStatusMessage ?? "")
        }
    }
```

- [ ] **Step 3: Add the `reminderButton` computed view**

Find:

```swift
    private var shareButton: some View {
        ShareLink(item: shareText) {
            Image(systemName: "square.and.arrow.up")
                .foregroundColor(KofcColors.gold)
                .padding(10)
                .background(KofcColors.navy)
                .cornerRadius(8)
        }
        .padding(.top, 4)
    }
```

Add directly above it:

```swift
    private var reminderButton: some View {
        Button {
            UIImpactFeedbackGenerator(style: .medium).impactOccurred()
            if isReminderArmed {
                reminderStore.disarm(event.id)
                ReminderScheduler.cancel(event.id)
            } else {
                Task {
                    switch await ReminderScheduler.schedule(event) {
                    case .scheduled:
                        reminderStore.arm(event.id)
                    case .denied:
                        reminderStatusMessage = "Enable notifications in Settings to get reminders."
                    }
                }
            }
        } label: {
            Image(systemName: isReminderArmed ? "bell.fill" : "bell")
                .foregroundColor(KofcColors.gold)
                .padding(10)
                .background(KofcColors.navy)
                .cornerRadius(8)
        }
        .padding(.top, 4)
    }

    private var shareButton: some View {
        ShareLink(item: shareText) {
            Image(systemName: "square.and.arrow.up")
                .foregroundColor(KofcColors.gold)
                .padding(10)
                .background(KofcColors.navy)
                .cornerRadius(8)
        }
        .padding(.top, 4)
    }
```

- [ ] **Step 4: Build and install to David's iPhone**

```bash
cd ~/Projects/KofC6650-iOS
xcodegen generate
xcodebuild -project KofC6650.xcodeproj -scheme KofC6650 -configuration Debug \
  -destination 'id=C9AD2513-FAAD-5608-B462-A8A18E783F8C' -derivedDataPath build_derived build
xcrun devicectl device install app --device C9AD2513-FAAD-5608-B462-A8A18E783F8C \
  build_derived/Build/Products/Debug-iphoneos/KofC6650.app
xcrun devicectl device process launch --device C9AD2513-FAAD-5608-B462-A8A18E783F8C com.erdman.kofc6650
```

Expected: build succeeds, install succeeds, app launches.

- [ ] **Step 5: Ask David to manually verify the bell icon and permission prompt**

`devicectl` cannot screenshot a physical device, so this step needs David to check the screen. Confirm:
- A bell icon (outline) appears between the calendar icon and the share icon on any upcoming event card.
- Tapping it triggers iOS's notification-permission system prompt (first time only). Grant it.
- The bell switches to filled.
- Scrolling to a past event's card shows no bell icon there at all.

- [ ] **Step 6: Commit**

```bash
cd ~/Projects/KofC6650-iOS
git add KofC6650/Views/EventCardView.swift
git commit -m "Add reminder bell button to event cards"
```

---

## Task 8: iOS — badge clearing + full manual verification

**Files:**
- Modify: `KofC6650/ContentView.swift`

**Interfaces:**
- Consumes: `UNUserNotificationCenter` (system framework only).
- Produces: nothing — last task in the plan.

- [ ] **Step 1: Import `UserNotifications`**

At the top of `ContentView.swift`, find:

```swift
import SwiftUI
import UIKit
import WidgetKit
```

Replace with:

```swift
import SwiftUI
import UIKit
import UserNotifications
import WidgetKit
```

- [ ] **Step 2: Clear the badge whenever the app becomes active**

Find:

```swift
        .onChange(of: scenePhase) { newPhase in
            defer { previousScenePhase = newPhase }
            if previousScenePhase == .background && newPhase == .active {
                Task { await viewModel.refresh() }
            }
        }
```

Replace with:

```swift
        .onChange(of: scenePhase) { newPhase in
            defer { previousScenePhase = newPhase }
            if newPhase == .active {
                UNUserNotificationCenter.current().setBadgeCount(0)
            }
            if previousScenePhase == .background && newPhase == .active {
                Task { await viewModel.refresh() }
            }
        }
```

(The badge clear is intentionally its own `if`, not nested inside the background→active one — it also needs to fire on the very first launch, when `previousScenePhase` is seeded to `.active` and that inner condition never fires.)

- [ ] **Step 3: Build and install to David's iPhone**

```bash
cd ~/Projects/KofC6650-iOS
xcodegen generate
xcodebuild -project KofC6650.xcodeproj -scheme KofC6650 -configuration Debug \
  -destination 'id=C9AD2513-FAAD-5608-B462-A8A18E783F8C' -derivedDataPath build_derived build
xcrun devicectl device install app --device C9AD2513-FAAD-5608-B462-A8A18E783F8C \
  build_derived/Build/Products/Debug-iphoneos/KofC6650.app
xcrun devicectl device process launch --device C9AD2513-FAAD-5608-B462-A8A18E783F8C com.erdman.kofc6650
```

Expected: build succeeds, install succeeds, app launches.

- [ ] **Step 4: Manually verify a reminder fires, badges, and clears**

Events come live from the real council Google Calendar, so don't edit a
real event's time to force a near-future trigger. Instead, temporarily
override the computed trigger in code, test, then revert:

In `KofC6650/ReminderScheduler.swift`, temporarily change the first line
of `triggerDate` from:

```swift
    static func triggerDate(for event: EventDto) -> Date? {
        guard let day = isoDateFormatter.date(from: event.date) else { return nil }
```

to:

```swift
    static func triggerDate(for event: EventDto) -> Date? {
        return Date().addingTimeInterval(120) // TEMPORARY test override, revert before committing
        guard let day = isoDateFormatter.date(from: event.date) else { return nil }
```

Rebuild/reinstall per Step 3, then have David:
- Arm a reminder on any upcoming event.
- Confirm the notification banner appears within ~2 minutes with the event title and "in 1 hour" (or the location-suffixed body).
- Confirm the app icon shows a badge (the number 1) after it fires, without opening the app yet.
- Open the app (from the home screen, not by tapping the notification) — confirm the badge clears immediately.
- Arm another test reminder, then tap the bell again before it fires to disarm it — confirm no notification appears at its trigger time.

Then revert the temporary override before continuing:

```bash
cd ~/Projects/KofC6650-iOS
git checkout -- KofC6650/ReminderScheduler.swift
```

- [ ] **Step 5: Commit**

```bash
cd ~/Projects/KofC6650-iOS
git add KofC6650/ContentView.swift
git commit -m "Clear notification badge whenever the app becomes active"
```
