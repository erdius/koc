# Council 6650

An Android companion app for Council 6650 (Cary & Apex, NC).

## Features

- **Volunteer Sign Ups** — upcoming council events with a SignUpGenius link, read directly from the council's public Google Calendar (via the Calendar API, with recurring events properly expanded), matching what's shown on the [council website](https://www.kofc6650.org/get-involved)
- **Submit Photos** — opens a Google Form for submitting event photos
- **Recent Photos** — shows the 7 most recent photos from the council's photo slideshow as native, tappable images (tap to enlarge)

## Requirements

- Android 8.0 (API 26) or higher

## Building

```sh
./gradlew assembleDebug
```

The debug APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

## Configuration

This app is specific to Council 6650 and has its data sources hardcoded rather than configurable:

- The Google Calendar ID and API key are in `app/src/main/java/com/erdman/kofc6650/data/KofcRepository.kt`. The API key is the same one already public in the council's own `volunteer-signup.html` embed — it's restricted to read-only Calendar API access.
- The Recent Photos tab reads from a Google Apps Script web app (bound to the council's Google Slides deck) that exports the most recent slides as JSON. Its URL is also in `KofcRepository.kt`.
- The Submit Photos form URL is in `MainActivity.kt`.

To reuse this app for a different council, swap out these values for your own calendar, Slides deck, and form.
