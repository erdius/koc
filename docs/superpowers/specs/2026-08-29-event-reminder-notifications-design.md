# Event Reminder Notifications — Design

Date: 2026-08-29
Repos covered: `KofC6650` (Android, this repo) and `KofC6650-iOS` (github.com/erdius/koc-ios)

## Problem

Event cards on both apps already have "Add to Calendar" and "Share" action
icons. There is no way to get a local reminder before an event without
manually adding it to the phone's calendar app and hoping that app's own
default alert fires. This adds a third icon — a bell — that arms a simple,
fixed-lead-time local notification, independent of the device calendar and
independent of the existing "I'm Going" star.

## UX

- A bell icon is inserted between the existing calendar icon and share icon
  in the event-card action row, styled identically (same navy rounded box,
  gold tint, 10dp padding).
- Tap toggles it on/off, exactly like the star RSVP icon: no dialog, no
  picker.
  - Off state: outline bell (`Icons.Outlined.Notifications` / SF Symbol
    `bell`).
  - On (armed) state: filled bell (`Icons.Filled.Notifications` / SF Symbol
    `bell.fill`), tinted gold same as the star's filled state.
- Both `Icons.Filled.Notifications` and `Icons.Outlined.Notifications` are
  already present in `material-icons-core` (verified in the AAR already on
  the Android build classpath) — no new icon dependency needed.

## Reminder rule

Fixed lead time, no per-event configuration:

- **1 hour before** the event's `date` + `time`.
- If `time` is missing/blank (all-day / time-less events), fall back to
  **8:00 AM on `date`** — same fallback style as the existing
  `AddToCalendarTimeDialog` default (which uses 9:00 AM), just picked to
  land before a typical event rather than at one.

Completely independent of `RsvpStore`/`RsvpStore.swift` (the star) — no
shared state, no side effects between the two icons.

Past events (already excluded/filtered from "upcoming" elsewhere in both
apps) do not show the bell icon at all — confirm exact filter point during
implementation and reuse it rather than adding new past/future logic.

## Data model

### Android — new `data/ReminderStore.kt`

Mirrors the existing key-prefix `SharedPreferences` style used by
`RsvpStore.kt` and `NextEventWidgetData.kt` (no new serialization
dependency):

- One `SharedPreferences` file, `"reminder_store"`.
- `KEY_EVENT_IDS`: `Set<String>` of armed event ids (same shape as
  `RsvpStore`'s `KEY_EVENT_IDS`).
- Per armed id, four more keys prefixed by id — `"title_$id"`,
  `"date_$id"`, `"time_$id"`, `"location_$id"` — enough to recompute the
  trigger time and notification content **without hitting the network**,
  which is what lets the boot-reschedule receiver work offline.
- `isArmed(context, eventId): Boolean`
- `arm(context, event: EventDto)` — writes the id into the set plus its
  snapshot fields.
- `disarm(context, eventId: String)` — removes the id and its snapshot
  keys.
- `allArmed(context): List<ReminderRecord>` — used only by the boot
  receiver to re-schedule everything.

### iOS — new `ReminderStore.swift`

Direct structural copy of `RsvpStore.swift` (singleton `ObservableObject`,
`Set<String>` in `UserDefaults`, `isArmed`/`toggle`). No snapshot fields
needed on this side — `UNUserNotificationCenter` already durably owns the
scheduled request across reboots, so the store here exists purely to drive
the bell icon's filled/outline state.

## Android scheduling mechanics

- **New `notifications/ReminderScheduler.kt`**: `schedule(context, event)`
  computes the trigger `Instant` per the rule above, builds a
  `PendingIntent` (`requestCode = event.id.hashCode()`) targeting a new
  `ReminderReceiver`, and calls
  `alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ...)`.
  `cancel(context, eventId)` rebuilds the same `PendingIntent` shape and
  calls `alarmManager.cancel(...)`.
- **New `notifications/ReminderReceiver.kt`** (`BroadcastReceiver`):
  receives the alarm, builds and posts the actual `Notification` via
  `NotificationManager`, creating the notification channel on first use if
  it doesn't exist yet (idempotent — no `Application` subclass needed for
  this, matches the codebase's existing preference for avoiding unnecessary
  abstraction).
- **New `notifications/BootRescheduleReceiver.kt`**: registered for
  `android.intent.action.BOOT_COMPLETED`. On boot, reads
  `ReminderStore.allArmed()`, drops any whose computed trigger has already
  passed, and re-arms the rest via `ReminderScheduler.schedule`. Required
  because `AlarmManager` alarms are wiped on reboot but the store survives.
- **Manifest additions**: `POST_NOTIFICATIONS` (Android 13+ runtime
  permission), `SCHEDULE_EXACT_ALARM` (or `USE_EXACT_ALARM` depending on
  target SDK behavior at implementation time — confirm against
  `compileSdk 35`'s current exact-alarm policy), `RECEIVE_BOOT_COMPLETED`,
  plus `<receiver>` entries for `ReminderReceiver` and
  `BootRescheduleReceiver`.
- **Permission request**: reuse the exact pattern already used for storage
  permission — `rememberLauncherForActivityResult(
  ActivityResultContracts.RequestPermission())` — triggered the first time
  the user arms any reminder (not eagerly on app launch).

## iOS scheduling mechanics

- Arming calls `UNUserNotificationCenter.current().requestAuthorization(
  options: [.alert, .sound])` if not already authorized, then builds a
  `UNCalendarNotificationTrigger` from the computed date and adds a
  `UNNotificationRequest` with `identifier = event.id`.
- Disarming calls
  `removePendingNotificationRequests(withIdentifiers: [event.id])`.
- No reboot handling needed — the OS persists pending requests across
  restarts.
- On authorization denial, show the same inline-message style the existing
  calendar-permission flow uses (`EventCardView`'s
  `calendarStatusMessage`) — e.g. "Enable notifications in Settings to get
  reminders." — rather than inventing a new error-messaging pattern.

## Notification content & tap behavior

- Title: event title. Body: "in 1 hour" (or "today" for the all-day
  fallback case), plus location if present — short, matches the compact
  style of the existing widget text.
- Tapping the notification just opens the app via standard launch. No
  deep-link to the specific event/tab. Kept minimal deliberately — YAGNI —
  can be revisited later if it turns out to matter in practice.

## Edge cases

- Toggle-off before the trigger fires cancels cleanly on both platforms
  (described above).
- An event's date/time changing after a reminder is armed (rare — Google
  Calendar event ids are stable per recurring instance) is out of scope;
  not worth reconciling against.
- Past events don't show the bell icon (see UX section).

## Testing

Manual verification only, matching how prior UI-layer work in these repos
(the QR-code icon swap, Feed the Homeless multi-date) was verified — no
unit test scaffolding exists for this layer today and this doesn't
introduce enough logic to justify adding one. Verification plan:

1. Arm a reminder on an event with a trigger 1–2 minutes in the future (by
   temporarily adjusting the lead-time constant, or picking/faking an
   event close enough) on a real Android device — confirm the notification
   fires and looks right.
2. Confirm toggling off before the trigger cancels it (no notification
   appears).
3. Force-reboot the Android test device (or use `adb shell am broadcast
   -a android.intent.action.BOOT_COMPLETED`) with an armed reminder still
   pending, confirm it re-fires at the right time.
4. Repeat the arm/fire/cancel check on David's physical iPhone via direct
   install.
