# KofC6650Kompakt — design

## Summary

A new, separate Android app — `KofC6650Kompakt` — forked from the current
KofC6650 phone app, re-themed for the Mudita Kompakt's e-ink display using
Mudita's official MMD (Mudita Mindful Design) Compose library. Same pattern
already used for ErdStream, ErdCal, and ErdMusic: an independent repo, not a
shared module, with business logic ported largely unchanged and only the UI
layer rebuilt for e-ink (monochrome, no ripples, jump-scroll lists).

Personal, sideload-only build for David's own Kompakt device — no store
listing, no release-signing pipeline, matching how the other three Kompakt
apps are distributed today.

## Scope

**In scope**: full feature parity with the phone app. The real tab set
(verified against `MainActivity.kt`, correcting an earlier draft of this
spec that had the wrong tabs) is:
- PIN gate (`PinGateScreen`, `data/PinManager.kt`)
- **Calendar** tab — agenda/month view, search, sign-up filter toggle,
  star/reminder feature, `EventCard`, `AddToCalendarTimeDialog`,
  `FeedTheHomelessSignupDialog`
- **Minutes** tab — Google Drive file list (`MinutesTab`,
  `MinutesFileCard`)
- **Photos** tab — submit + recent photos, toggled via
  `PhotosModeToggle` (`SubmitPhotosTab`, `RecentPhotosTab`, grid +
  pinch-zoom/pan)
- **Payments** tab — Dues/Badge payment via PayPal (`PaymentsTab`,
  `DuesPaymentDialog`, `BadgePaymentDialog`)
- Header dialogs: `JoinKofcDialog` (QR popup), `AboutDialog`,
  `WhatsNewDialog`, `DirectorsOfficersDialog`
- App icon, re-rendered in solid black/white

**Out of scope**:
- Backend changes (same Google Calendar API, same `koc-photos.erdcloud.org`
  endpoints, same PayPal/Drive integrations)
- iOS app changes
- A shared Gradle module between KofC6650 and KofC6650Kompakt — this is an
  independent fork, matching ErdStream/ErdCal/ErdMusic
- `widget/NextEventWidgetProvider.kt` (home-screen widget) — Kompakt has no
  Android home-screen widget surface, so this file isn't ported
- Store publishing / release signing — sideload via `adb install` only

## Repo & package

- New repo: `~/Projects/KofC6650Kompakt`, pushed to
  `github.com/erdius/KofC6650Kompakt`
- `applicationId`: `com.erdman.kofc6650kompakt`
- Toolchain: same as the other Kompakt apps (JDK17 via
  `/opt/homebrew/opt/openjdk@17`, Android SDK already configured on this
  machine, Gradle wrapper copied over rather than re-downloaded)

## What ports over unchanged

Everything in `data/` and `notifications/` is pure logic/networking with no
UI dependency, and copies over as-is:

`GoogleCalendarApi.kt`, `QrCodeGenerator.kt`, `GoogleDriveApi.kt`,
`PayPalSubmitter.kt`, `PhotoUploadModels.kt`, `ReminderStore.kt`,
`CalendarViewModePreference.kt`, `SignupModels.kt`, `RsvpStore.kt`,
`DriveModels.kt`, `OfflineCache.kt`, `KofcModels.kt`, `WhatsNew.kt`,
`KofcRepository.kt`, `AppearanceModePreference.kt`,
`GoogleCalendarModels.kt`, `LeadershipModels.kt`, `PinManager.kt`,
`RecentPhotosApi.kt`, `SignupApi.kt`, `ScreenshotMode.kt`,
`FontScalePreference.kt`, `RecentPhotosModels.kt`,
`BootRescheduleReceiver.kt`, `ReminderScheduler.kt`, `ReminderReceiver.kt`.

`AppearanceModePreference.kt` (light/dark mode) and `WhatsNew.kt`'s copy
carry over as data plumbing but their *effect* on the UI changes — see
below.

## Design system

Replace `ui/theme/KofC6650Theme.kt` (Material3 `lightColorScheme`/
`darkColorScheme` built from the navy `#1A2F5E`/gold `#C9A84C` brand
palette) with Mudita's `com.mudita:MMD:1.0.0` library, resolved via
`maven { url = uri("https://mudita.jfrog.io/artifactory/mmd-release") }`
in `settings.gradle.kts` — identical setup to ErdStream/ErdCal/ErdMusic.

- `ThemeMMD`/`eInkColorScheme` replaces `KofC6650Theme` — pure
  black/white/grayscale, no brand colors, no light/dark mode split (e-ink
  has no concept of "dark mode"; `AppearanceModePreference` becomes a
  no-op carried over for data-compat but unused in UI)
- `LazyColumnMMD` for uniform-height lists (event lists in Sign Ups and
  Calendar tabs) — provides its own jump-scroll + scrollbar
- A hand-rolled `IndicationNodeFactory` (copied from ErdStream's
  `ErdStreamTheme.kt` pattern) suppresses ripple on raw
  `.combinedClickable` sites MMD doesn't cover — same tradeoff as
  ErdStream (MMD has no `IconButtonMMD`)
- `ModalBottomSheetMMD` replaces `AlertDialog` for confirmations (PIN
  reset, delete/destructive prompts, the reminder-notification prompt) —
  MMD has no dialog component, same pattern as ErdMusic/ErdStream
- The Recent Photos grid and pinch-zoom/pan viewer are heterogeneous-height
  content, so they keep a custom scroll implementation rather than
  `LazyColumnMMD` (same tradeoff ErdCal/ErdStream document for their own
  mixed-height screens) — but restyled to MMD's monochrome look

## File restructuring

The current `MainActivity.kt` is 3,386 lines, and a full e-ink re-theme
touches nearly all of it. Rather than porting one giant file, split it by
screen during the port:

- `MainActivity.kt` — `ComponentActivity`, top-level `KofcApp`
  scaffold/tab routing only
- `ui/screens/PinGateScreen.kt`
- `ui/screens/CalendarScreen.kt` (agenda/month view, search, sign-up
  filter, star/reminder UI, `EventCard`, `AddToCalendarTimeDialog`,
  `FeedTheHomelessSignupDialog`)
- `ui/screens/MinutesScreen.kt`
- `ui/screens/PhotosScreen.kt` (`PhotosModeToggle`, submit + recent
  photos)
- `ui/screens/PaymentsScreen.kt` (`DuesPaymentDialog`,
  `BadgePaymentDialog`)
- `ui/dialogs/HeaderDialogs.kt` — `JoinKofcDialog`, `AboutDialog`,
  `WhatsNewDialog`, `DirectorsOfficersDialog`
- `ui/components/` — shared pieces used across screens

## App icon

Current launcher icon (`ic_launcher_foreground.xml`) is a gold-circle/
navy-K vector — needs a solid black/white redesign for Kompakt. Per the
known Kompakt-launcher rendering bug (already hit and fixed on ErdCal):
stroke-only paths (`strokeColor`/`strokeWidth`) render as solid black
blocks instead of transparent outlines on that launcher, so the new icon
must use solid `fillColor` shapes only — no `strokeColor`/`strokeWidth`,
no `fillType="evenOdd"`.

## Testing

- Build and sideload via `adb install` onto the Mudita (`MK20250402537`),
  same as this session's KofC6650 phone-app install
- Visually verify each of the 4 tabs + PIN gate + About screen on the
  actual e-ink display before considering the port done (e-ink rendering,
  jump-scroll behavior, and contrast can't be judged from a phone
  emulator or the existing app's screenshots)
- No new automated tests planned beyond whatever unit tests, if any, exist
  in the current KofC6650 repo for the ported `data/` files
