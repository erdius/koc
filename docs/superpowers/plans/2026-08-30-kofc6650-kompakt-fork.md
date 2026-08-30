# KofC6650Kompakt Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fork the KofC6650 Android phone app into a new, independent
repo — `KofC6650Kompakt` — re-themed for the Mudita Kompakt's monochrome
e-ink display using Mudita's official MMD Compose library, with full
feature parity.

**Architecture:** Copy the source tree wholesale into a new repo, rename
the package, and get it building/installing byte-for-byte functionally
identical to the phone app first (still Material3-themed). Then swap in
MMD's theme, split the 3,386-line `MainActivity.kt` into per-screen
files, and re-theme each screen with MMD components one at a time,
verifying on the actual Mudita device after every step since e-ink
rendering can't be judged from a phone emulator.

**Tech Stack:** Kotlin, Jetpack Compose, `com.mudita:MMD:1.0.0` (Mudita's
official e-ink component library — same one already used by ErdStream,
ErdCal, ErdMusic), Gradle Kotlin DSL, JDK17.

**Spec:** `docs/superpowers/specs/2026-08-30-kofc6650-kompakt-fork-design.md`

## Global Constraints

- New repo: `~/Projects/KofC6650Kompakt`, pushed to
  `github.com/erdius/KofC6650Kompakt` (public, matching `erdius/koc`)
- `applicationId` / `namespace`: `com.erdman.kofc6650kompakt`
- `versionCode = 1`, `versionName = "1.0.0"` (fresh product)
- Sideload-only: no release signing config, no Play Publisher plugin —
  `assembleDebug` + `adb install` is the only distribution path
- No `strokeColor`/`strokeWidth`/`fillType="evenOdd"` in any vector
  drawable — Kompakt's launcher renders those as solid black blocks
  instead of transparent outlines (confirmed bug, already hit and fixed
  on ErdCal)
- Every screen must be verified on the physical Mudita Kompakt
  (`adb -s MK20250402537`) before its task is considered done — an
  emulator or the phone app's own screenshots don't tell you how e-ink
  actually renders it
- `widget/NextEventWidgetProvider.kt`, `data/NextEventWidgetData.kt`,
  `res/layout/widget_next_event.xml`, `res/xml/next_event_widget_info.xml`,
  and the widget `<receiver>` block in `AndroidManifest.xml` are **not**
  ported — Kompakt has no home-screen widget surface

---

### Task 1: Fork the repo and get it building unchanged

**Files:**
- Create: `~/Projects/KofC6650Kompakt/` (new git repo, full source tree)
- Create: `~/Projects/KofC6650Kompakt/settings.gradle.kts`
- Create: `~/Projects/KofC6650Kompakt/app/build.gradle.kts`
- Create: `~/Projects/KofC6650Kompakt/local.properties` (gitignored)

**Interfaces:**
- Produces: a buildable, installable app at package
  `com.erdman.kofc6650kompakt`, still using the original Material3 theme
  and monolithic `MainActivity.kt` — this task proves the fork mechanics
  work before any visual changes happen.

- [ ] **Step 1: Copy the source tree**

```bash
mkdir -p ~/Projects/KofC6650Kompakt
cd ~/Projects/KofC6650Kompakt
git init

# Copy everything except build artifacts and machine-local files
rsync -a --exclude='.git' --exclude='.gradle' --exclude='build' \
  --exclude='local.properties' --exclude='keystore.properties' \
  --exclude='play-store' --exclude='.superpowers' \
  --exclude='docs/superpowers' \
  ~/Projects/KofC6650/ ~/Projects/KofC6650Kompakt/
```

- [ ] **Step 2: Drop the widget files (not ported — see Global Constraints)**

```bash
cd ~/Projects/KofC6650Kompakt
rm app/src/main/java/com/erdman/kofc6650/widget/NextEventWidgetProvider.kt
rm app/src/main/java/com/erdman/kofc6650/data/NextEventWidgetData.kt
rm app/src/main/res/layout/widget_next_event.xml
rm app/src/main/res/xml/next_event_widget_info.xml
rmdir app/src/main/java/com/erdman/kofc6650/widget
```

- [ ] **Step 3: Remove the widget's `<receiver>` block and
  `updateNextEventWidget` calls**

Open `app/src/main/AndroidManifest.xml` and delete this block:

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
```

In `app/src/main/java/com/erdman/kofc6650/MainActivity.kt`, find the
`private fun updateNextEventWidget(...)` function (originally around
line 236) and every call site that invokes it, and delete both the
function and its call sites.

- [ ] **Step 4: Rename the package**

```bash
cd ~/Projects/KofC6650Kompakt
# Move the directory tree
mkdir -p app/src/main/java/com/erdman/kofc6650kompakt
git mv app/src/main/java/com/erdman/kofc6650/* app/src/main/java/com/erdman/kofc6650kompakt/ 2>/dev/null \
  || mv app/src/main/java/com/erdman/kofc6650/* app/src/main/java/com/erdman/kofc6650kompakt/
rmdir app/src/main/java/com/erdman/kofc6650

# Rewrite every package declaration and import statement
grep -rl 'com\.erdman\.kofc6650' app/src/main/java --include='*.kt' | \
  xargs sed -i '' 's/com\.erdman\.kofc6650\b/com.erdman.kofc6650kompakt/g'
```

The `\b` word boundary in the `sed` pattern matters: it stops
`com.erdman.kofc6650` from also matching inside the *already-renamed*
`com.erdman.kofc6650kompakt` if this is ever run twice, and it must not
touch the display string `"KofC 6650"` in `strings.xml` (that file has
no `.kt` extension so the `grep -rl ... --include='*.kt'` above already
skips it).

- [ ] **Step 5: Write the new `settings.gradle.kts`**

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://mudita.jfrog.io/artifactory/mmd-release") }
    }
}

rootProject.name = "KofC6650Kompakt"
include(":app")
```

- [ ] **Step 6: Write the new `app/build.gradle.kts`**

Sideload-only, so this drops the Play Publisher plugin and all signing
config from the original, and adds the MMD dependency:

```kotlin
plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    namespace = "com.erdman.kofc6650kompakt"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.erdman.kofc6650kompakt"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3"
    }

    packagingOptions {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.activity:activity-compose:1.8.0")

    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3:1.2.1")
    implementation("androidx.compose.material:material")

    // Mudita's e-ink component library (theme, lists, bottom sheets, etc.)
    implementation("com.mudita:MMD:1.0.0")

    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.11.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    implementation("io.coil-kt:coil-compose:2.6.0")

    implementation("com.google.zxing:core:3.5.3")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
```

- [ ] **Step 7: Copy the Gradle wrapper and write `local.properties`**

```bash
cd ~/Projects/KofC6650Kompakt
cp -r ~/Projects/KofC6650/gradlew ~/Projects/KofC6650/gradlew.bat ~/Projects/KofC6650/gradle .
chmod +x gradlew
echo "sdk.dir=/opt/homebrew/share/android-commandlinetools" > local.properties
```

- [ ] **Step 8: Write `.gitignore`**

```
*.iml
.gradle
/local.properties
.idea/
.DS_Store
/build
/captures
.externalNativeBuild
.cxx
local.properties
/app/build
/app/release
*.apk
*.ap_
*.dex
*.class
bin/
gen/
out/
.navigation/
captures/
.cxx/
*.log
```

- [ ] **Step 9: Build and install, verify parity with the phone app**

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
./gradlew :app:assembleDebug -q
adb -s MK20250402537 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s MK20250402537 shell monkey -p com.erdman.kofc6650kompakt -c android.intent.category.LAUNCHER 1
```

Expected: builds with no errors, installs alongside the existing
`com.erdman.kofc6650` app (different applicationId, so no conflict),
launches to the PIN gate exactly as the phone app does — still fully
Material3/navy/gold themed at this point, that's expected and correct
for this task.

- [ ] **Step 10: Commit**

```bash
git add -A
git commit -m "Fork KofC6650 into KofC6650Kompakt, rename package, strip widget"
gh repo create erdius/KofC6650Kompakt --public --source=. --remote=origin
git push -u origin main
```

---

### Task 2: Wire up MMD's e-ink theme

**Files:**
- Create: `app/src/main/java/com/erdman/kofc6650kompakt/ui/theme/KofC6650KompaktTheme.kt`
- Modify: `app/src/main/java/com/erdman/kofc6650kompakt/MainActivity.kt` (the `setContent` block, and the `KofcApp` composable's theme wrapper)
- Modify: `app/src/main/java/com/erdman/kofc6650kompakt/ui/theme/KofC6650Theme.kt` (trimmed to just its color constants — see Step 2; fully removed later, in Task 8)

**Interfaces:**
- Produces: `KofC6650KompaktTheme(content: @Composable () -> Unit)` — the
  root theme composable every later task's screens run inside.

This is a verified, working pattern copied from
`~/Projects/ErdStream/app/src/main/java/com/erdman/erdstream/ui/theme/ErdStreamTheme.kt`
(ErdStream is on the same MMD version and hits the same "no
`IconButtonMMD`, ripple ghosts on e-ink" problem this app will hit).

- [ ] **Step 1: Write the theme file**

```kotlin
package com.erdman.kofc6650kompakt.ui.theme

import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.DelegatableNode
import com.mudita.mmd.ThemeMMD
import com.mudita.mmd.eInkColorScheme
import com.mudita.mmd.eInkTypography

// Standard Material ripple is an animated pulse -- visible motion that
// ghosts on e-ink. MMD's own components (ButtonMMD, etc.) are already
// ripple-free, but it has no IconButtonMMD, and this app's own
// .combinedClickable call sites don't set indication = null explicitly, so
// this no-op Indication is still provided app-wide below to cover those.
private object NoRippleIndication : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode {
        return object : Modifier.Node() {}
    }

    override fun equals(other: Any?): Boolean = other === this
    override fun hashCode(): Int = System.identityHashCode(this)
}

@Composable
fun KofC6650KompaktTheme(content: @Composable () -> Unit) {
    ThemeMMD(
        colorScheme = eInkColorScheme,
        typography = eInkTypography,
    ) {
        CompositionLocalProvider(LocalIndication provides NoRippleIndication) {
            content()
        }
    }
}
```

- [ ] **Step 2: Trim the old theme file down to just its color constants — do NOT delete it yet**

`KofcNavy`, `KofcNavyLight`, `KofcGold`, `KofcGoldMuted`, `KofcBackground`,
and `KofcCard` (the `val`s at the top of
`ui/theme/KofC6650Theme.kt`) are referenced across the *entire*
still-unsplit `MainActivity.kt` — well beyond just the `KofcApp`
function — in the PIN gate, every dialog, the month calendar grid, event
cards, and every screen's buttons. Deleting the whole file now would
break the build until Tasks 4-8 finish migrating every one of those call
sites, which is out of scope for this task. Instead, edit
`ui/theme/KofC6650Theme.kt` down to just the six `val` declarations —
delete the `KofcLightColors`/`KofcDarkColors`/`KofC6650Theme(...)`
composable below them, keep the color constants as a transitional file:

```kotlin
package com.erdman.kofc6650kompakt.ui.theme

import androidx.compose.ui.graphics.Color

// Transitional: these six constants are still referenced throughout the
// pre-MMD screens. Task 8 removes the last reference and deletes this
// file once every screen has been re-themed (Tasks 4-8).
val KofcNavy = Color(0xFF1A2F5E)
val KofcNavyLight = Color(0xFF2A4374)
val KofcGold = Color(0xFFC9A84C)
val KofcGoldMuted = Color(0xFF9A7A2C)
val KofcBackground = Color(0xFFF0F2F5)
val KofcCard = Color(0xFFFFFFFF)
```

- [ ] **Step 3: Swap the theme wrapper in `MainActivity.kt`**

Find `import com.erdman.kofc6650kompakt.ui.theme.KofC6650Theme` and every
call site wrapping content in `KofC6650Theme { ... }` (there should be
exactly one, around where `setContent` is called inside `onCreate`).
Replace the import with
`import com.erdman.kofc6650kompakt.ui.theme.KofC6650KompaktTheme` and the
call site with `KofC6650KompaktTheme { ... }`.

Also delete the hardcoded brand-color references used directly in
`KofcApp` (`KofcNavy`, `KofcNavyLight`, `KofcGold`, `KofcGoldMuted`,
`KofcBackground`, `KofcCard` — e.g. `containerColor = KofcNavy` on the
top app bar and tab row) — replace each with the corresponding
`MaterialTheme.colorScheme` role (`primary`, `onPrimary`, `background`,
`surface`) so they pick up MMD's `eInkColorScheme` instead. Don't try to
get every one of these exactly right yet — full per-screen re-theming
happens in Tasks 4-8. The goal of this step is just: no compile errors
referencing the deleted `KofcNavy`/`KofcGold`/etc. color constants.

- [ ] **Step 4: Build and install, verify the theme applies**

```bash
./gradlew :app:assembleDebug -q
adb -s MK20250402537 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s MK20250402537 shell monkey -p com.erdman.kofc6650kompakt -c android.intent.category.LAUNCHER 1
adb -s MK20250402537 exec-out screencap -p > /tmp/kompakt-task2.png
```

Expected: builds, launches, top bar and tab row are no longer
navy/gold — the app now uses MMD's black/white/grayscale palette even
though individual screens still use unreplaced Material3 widgets
(`LazyColumn`, `AlertDialog`, `Button`). That's expected; screen-level
MMD component swaps happen in Tasks 4-8.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "Replace Material3 navy/gold theme with MMD e-ink theme"
git push
```

---

### Task 3: Split MainActivity.kt into per-screen files

**Files:**
- Create: `app/src/main/java/com/erdman/kofc6650kompakt/ui/screens/PinGateScreen.kt`
- Create: `app/src/main/java/com/erdman/kofc6650kompakt/ui/screens/CalendarScreen.kt`
- Create: `app/src/main/java/com/erdman/kofc6650kompakt/ui/screens/MinutesScreen.kt`
- Create: `app/src/main/java/com/erdman/kofc6650kompakt/ui/screens/PhotosScreen.kt`
- Create: `app/src/main/java/com/erdman/kofc6650kompakt/ui/screens/PaymentsScreen.kt`
- Create: `app/src/main/java/com/erdman/kofc6650kompakt/ui/dialogs/HeaderDialogs.kt`
- Create: `app/src/main/java/com/erdman/kofc6650kompakt/ui/components/CommonComponents.kt`
- Modify: `app/src/main/java/com/erdman/kofc6650kompakt/MainActivity.kt` (shrinks to `ComponentActivity` + `KofcApp` scaffold/tab routing)

**Interfaces:**
- Produces: the same composables that exist today, just relocated —
  `PinGateScreen`, `CalendarAgendaTab`, `MinutesTab`, `PhotosTab`,
  `PaymentsTab`, `JoinKofcDialog`, `AboutDialog`, `WhatsNewDialog`,
  `DirectorsOfficersDialog`, `RefreshableList`, `StarredOnlyToggleRow`,
  `ViewModeIconToggle`, `EventFilterToggle`, `EventSearchField`,
  `formatDate` — all called from `KofcApp` in `MainActivity.kt` exactly
  as before.
- Consumes: nothing new — this is a pure refactor, no behavior change.

This is a **mechanical move**, not a rewrite — every function below
still has its Material3 body from Task 1's copy; only its *location*
changes. The line numbers are from the original
`~/Projects/KofC6650/app/src/main/java/com/erdman/kofc6650/MainActivity.kt`
(3,386 lines) and should still be approximately right in this repo's
copy since no lines have been added or removed above them yet — but
search for the function signature to confirm the exact range before
cutting, don't trust the line number blindly.

**Function → file mapping** (verified against the source):

| Functions | ~Original lines | New file |
|---|---|---|
| `PinGateScreen` | 859-981 | `ui/screens/PinGateScreen.kt` |
| `RefreshableList`, `StarredOnlyToggleRow`, `ViewModeIconToggle`, `EventFilterToggle`, `EventSearchField`, `formatDate` | 224-235, 983-1317 | `ui/components/CommonComponents.kt` |
| `CalendarAgendaTab`, `monthCalendarContent`, `eventMatchesQuery`, `dateBucket`, `eventSections`, `EventCard`, `reportProblem`, `openLocationInMaps`, `shareEvent`, `addEventToCalendar`, `AddToCalendarTimeDialog`, `FeedTheHomelessSignupDialog` | 1318-1514, 1818-2122, 2945-3386 | `ui/screens/CalendarScreen.kt` |
| `MinutesTab`, `minutesFileYear`, `parseMinutesFileName`, `MinutesFileCard` | 2123-2307 | `ui/screens/MinutesScreen.kt` |
| `PhotosModeToggle`, `PhotosTab`, `SubmitPhotosTab`, `RecentPhotosTab`, `sharePhoto` | 2307-2945 | `ui/screens/PhotosScreen.kt` |
| `PaymentsTab`, `DuesPaymentDialog`, `BadgePaymentDialog` | 1514-1818 | `ui/screens/PaymentsScreen.kt` |
| `JoinKofcDialog`, `WhatsNewDialog`, `AboutDialog`, `DirectorsOfficersDialog`, `leadershipSection`, `LeadershipRow` | 608-858 | `ui/dialogs/HeaderDialogs.kt` |
| `class MainActivity`, `KofcApp` | 182-608 | stays in `MainActivity.kt` |

- [ ] **Step 1: For each new file, create it with a package declaration and this task's imports**

Each new file needs `package com.erdman.kofc6650kompakt.ui.screens` (or
`.ui.dialogs` / `.ui.components`), plus every import its moved functions
actually use — copy the relevant `import` lines from the top of
`MainActivity.kt` (don't guess; check what each function references,
e.g. `EventDto` needs `import com.erdman.kofc6650kompakt.data.KofcModels`
or wherever `EventDto` lives).

- [ ] **Step 2: Move each function group using its real signature**

For example, for `CommonComponents.kt`:

```bash
cd ~/Projects/KofC6650Kompakt/app/src/main/java/com/erdman/kofc6650kompakt
# Find the exact current line range by searching for the signature,
# don't assume the table above is still byte-exact after Task 2's edits:
grep -n "private fun formatDate\|private fun RefreshableList\|private fun StarredOnlyToggleRow\|private fun ViewModeIconToggle\|private fun EventFilterToggle\|private fun EventSearchField" MainActivity.kt
```

Cut each function's full body (from its `@Composable`/`private fun` line
through its matching closing `}`) out of `MainActivity.kt` and paste it
into the target file, in the order shown in the table.

- [ ] **Step 3: Remove the `private` modifier from every moved top-level function**

Kotlin's top-level `private` means *file-private* — a function that's
`private` in `CommonComponents.kt` can't be called from
`CalendarScreen.kt` or `MainActivity.kt` anymore. Change every moved
`private fun FooBar(...)` to `internal fun FooBar(...)` (or drop the
modifier entirely) so cross-file calls still resolve. Composables that
are genuinely only called from within their own new file (e.g. a dialog
that's only opened from one screen) can stay `private`.

- [ ] **Step 4: Add imports for the moved functions at their call sites**

`MainActivity.kt`'s `KofcApp` calls `PinGateScreen(...)`,
`CalendarAgendaTab(...)`, `MinutesTab(...)`, `PhotosTab(...)`,
`PaymentsTab(...)`, `JoinKofcDialog(...)`, `AboutDialog(...)`,
`WhatsNewDialog(...)`, `DirectorsOfficersDialog(...)` — add the matching
`import com.erdman.kofc6650kompakt.ui.screens.*` /
`import com.erdman.kofc6650kompakt.ui.dialogs.*` lines to
`MainActivity.kt`.

- [ ] **Step 5: Build, fix any remaining unresolved references**

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
./gradlew :app:assembleDebug -q
```

Expected: the first build will likely surface a handful of "unresolved
reference" errors for helper functions/constants that were used across
the old single-file boundary and need one more `internal` fix or import
added. Iterate until it compiles clean — this is expected, not a sign
something is wrong with the plan.

- [ ] **Step 6: Install and verify identical behavior to Task 2**

```bash
adb -s MK20250402537 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s MK20250402537 shell monkey -p com.erdman.kofc6650kompakt -c android.intent.category.LAUNCHER 1
adb -s MK20250402537 exec-out screencap -p > /tmp/kompakt-task3.png
```

Expected: pixel-equivalent to Task 2's screenshot (`/tmp/kompakt-task2.png`)
— this was a pure refactor, nothing should look different yet.

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "Split MainActivity.kt into per-screen files"
git push
```

---

### Task 4: Re-theme the Calendar screen with MMD components

**Files:**
- Modify: `app/src/main/java/com/erdman/kofc6650kompakt/ui/screens/CalendarScreen.kt`

**Interfaces:**
- Consumes: `KofC6650KompaktTheme` (Task 2), the file produced by Task 3.
- Produces: same public signatures as before
  (`CalendarAgendaTab(events, errorMessage, isRefreshing, onRefresh,
  onSignUpClick, feedTheHomelessOpenDates)`) — only the widgets inside
  change, not the function's contract with `MainActivity.kt`.

Apply these verified MMD substitutions (real code, from
`~/Projects/ErdStream`'s working usage) everywhere the corresponding
Material3 widget appears in this file:

- [ ] **Step 1: Replace the event list's `LazyColumn` with `LazyColumnMMD`**

```kotlin
import com.mudita.mmd.components.lazy.LazyColumnMMD
```

```kotlin
// Before:
LazyColumn(modifier = Modifier.fillMaxSize(), state = listState) { ... }

// After:
LazyColumnMMD(modifier = Modifier.fillMaxSize(), state = listState) { ... }
```

Its `state`/`contentPadding`/trailing-lambda `content` parameters are
drop-in compatible with `LazyColumn` — no other changes needed at call
sites that only use those three.

- [ ] **Step 2: Replace `AlertDialog` usages (`AddToCalendarTimeDialog`, `FeedTheHomelessSignupDialog`) with `ModalBottomSheetMMD`**

```kotlin
import com.mudita.mmd.components.bottom_sheet.ModalBottomSheetMMD
import com.mudita.mmd.components.bottom_sheet.rememberModalBottomSheetMMDState
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.text.TextMMD
```

```kotlin
// Before:
AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Add to Calendar") },
    text = { /* content */ },
    confirmButton = { TextButton(onClick = onConfirm) { Text("Add") } },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
)

// After:
val sheetState = rememberModalBottomSheetMMDState(skipPartiallyExpanded = true)
ModalBottomSheetMMD(onDismissRequest = onDismiss, sheetState = sheetState) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        TextMMD(text = "Add to Calendar", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        /* content */
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButtonMMD(onClick = onDismiss) { Text("Cancel") }
            ButtonMMD(onClick = onConfirm) { Text("Add") }
        }
    }
}
```

Apply this same before/after shape to every `AlertDialog` in this file.

- [ ] **Step 3: Swap `Button`/`TextButton`/`OutlinedButton` for `ButtonMMD`/`OutlinedButtonMMD`**

Every clickable action button in `EventCard`, `AddToCalendarTimeDialog`,
and `FeedTheHomelessSignupDialog` (e.g. "Add to Calendar", "Get
Directions", "Share", the star-tap reminder-prompt buttons) — swap the
Material3 composable for the MMD equivalent using the same parameters
(`onClick`, trailing content lambda). `IconButton` has no MMD
equivalent (per the theme file's comment in Task 2) — leave `IconButton`
call sites as-is; the `NoRippleIndication` from Task 2 already strips
their ripple.

- [ ] **Step 4: Build, install, and visually verify on the Mudita**

```bash
./gradlew :app:assembleDebug -q
adb -s MK20250402537 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s MK20250402537 shell monkey -p com.erdman.kofc6650kompakt -c android.intent.category.LAUNCHER 1
# Navigate to the Calendar tab, open an event, tap the star to trigger
# the reminder prompt, tap "Add to Calendar" to trigger that sheet
adb -s MK20250402537 exec-out screencap -p > /tmp/kompakt-calendar.png
```

Expected: event list scrolls with MMD's jump-scroll behavior, dialogs
render as bottom sheets instead of centered alert boxes, buttons have no
ripple animation, everything is black/white/grayscale.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "Re-theme Calendar screen with MMD components"
git push
```

---

### Task 4.5: Re-theme shared components with MMD (plan gap found in Task 4)

**Inserted during execution** — Task 4's implementer found that
`ui/components/CommonComponents.kt` (produced by Task 3's file split)
was never assigned its own re-theme task, even though it holds the
app's *actual* `LazyColumn` call site (inside `RefreshableList`) and is
used by Calendar, Minutes, and Photos — Task 4 could not do its own
`LazyColumn` → `LazyColumnMMD` swap because the widget doesn't live in
`CalendarScreen.kt` at all. This task closes that gap before Tasks 5
and 6 (Minutes, Photos) also hit it.

**Files:**
- Modify: `app/src/main/java/com/erdman/kofc6650kompakt/ui/components/CommonComponents.kt`

**Interfaces:**
- Consumes: `KofC6650KompaktTheme` (Task 2).
- Produces: same signatures as before (`RefreshableList(...)`,
  `StarredOnlyToggleRow(starredOnly, onChange)`,
  `ViewModeIconToggle(pref)`, `EventFilterToggle(signupOnly, onChange)`,
  `EventSearchField(query, onQueryChange)`, `formatDate(dateStr)`) —
  called from `CalendarScreen.kt`, `MinutesScreen.kt`,
  `PhotosScreen.kt` exactly as before.

- [ ] **Step 1: Replace `RefreshableList`'s `LazyColumn` with `LazyColumnMMD`**

Same substitution as Task 4 Step 1 —
`import com.mudita.mmd.components.lazy.LazyColumnMMD`, swap the
composable name, keep `state`/`contentPadding`/content lambda as-is.

- [ ] **Step 2: Migrate this file's own `KofcNavy`/`KofcGold`/`KofcGoldMuted` usages**

Same approach as Task 4's color migration: drop the explicit color so
it inherits `MaterialTheme.colorScheme` from `eInkColorScheme`, or use
`Color.Black`/`Color.White` directly where a genuinely hardcoded
monochrome accent makes sense (e.g. `StarredOnlyToggleRow`'s
starred/not-starred visual distinction).

- [ ] **Step 3: Build, install, and visually verify on the Mudita**

```bash
./gradlew :app:assembleDebug -q
adb -s MK20250402537 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s MK20250402537 shell monkey -p com.erdman.kofc6650kompakt -c android.intent.category.LAUNCHER 1
# Calendar, Minutes, and Photos tabs all render through RefreshableList —
# check all three still scroll and pull-to-refresh correctly
adb -s MK20250402537 exec-out screencap -p > /tmp/kompakt-commoncomponents.png
```

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "Re-theme shared components (RefreshableList, toggles) with MMD"
git push
```

---

### Task 5: Re-theme the Minutes screen with MMD components

**Files:**
- Modify: `app/src/main/java/com/erdman/kofc6650kompakt/ui/screens/MinutesScreen.kt`

**Interfaces:**
- Consumes: `KofC6650KompaktTheme` (Task 2).
- Produces: same signature as before (`MinutesTab(repository)`).

- [ ] **Step 1: Replace `MinutesTab`'s file list `LazyColumn` with `LazyColumnMMD`**

Same substitution as Task 4 Step 1 — `import
com.mudita.mmd.components.lazy.LazyColumnMMD`, swap the composable name,
keep `state`/`contentPadding`/content lambda as-is.

- [ ] **Step 2: Restyle `MinutesFileCard`'s `Text`/`Card` to MMD equivalents**

```kotlin
import com.mudita.mmd.components.text.TextMMD
```

Replace `Text(...)` calls with `TextMMD(...)` using the same
`text`/`fontSize`/`fontWeight` parameters. If `MinutesFileCard` wraps its
content in a Material3 `Card`, replace the `Card`'s `colors =
CardDefaults.cardColors(...)` override (if any sets a brand color) with
no override, so it inherits `MaterialTheme.colorScheme.surface` from
`eInkColorScheme` instead.

- [ ] **Step 3: Build, install, and visually verify on the Mudita**

```bash
./gradlew :app:assembleDebug -q
adb -s MK20250402537 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s MK20250402537 shell monkey -p com.erdman.kofc6650kompakt -c android.intent.category.LAUNCHER 1
# Navigate to the Minutes tab
adb -s MK20250402537 exec-out screencap -p > /tmp/kompakt-minutes.png
```

Expected: file list renders with MMD's jump-scroll, all text/cards in
black/white/grayscale, no navy/gold anywhere.

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "Re-theme Minutes screen with MMD components"
git push
```

---

### Task 6: Re-theme the Photos screen with MMD components

**Files:**
- Modify: `app/src/main/java/com/erdman/kofc6650kompakt/ui/screens/PhotosScreen.kt`

**Interfaces:**
- Consumes: `KofC6650KompaktTheme` (Task 2).
- Produces: same signatures as before (`PhotosTab(repository,
  pinManager, photos, photosError, isLoadingPhotos, onRefreshPhotos,
  browseMode, onBrowseModeChange)`, `PhotosModeToggle(browseMode,
  onChange)`).

Per the spec, `RecentPhotosTab`'s photo grid and pinch-zoom/pan viewer
are heterogeneous-height content — per the spec's Design System section,
this does **not** get `LazyColumnMMD` (documented tradeoff, same one
ErdCal/ErdStream hit for their own mixed-height screens); it keeps its
current `LazyVerticalGrid`/custom scroll, just restyled.

- [ ] **Step 1: Restyle `SubmitPhotosTab`'s form (`TextField`, `Button`) with MMD equivalents**

```kotlin
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.text.TextMMD
```

Swap `Button`/`OutlinedButton` for `ButtonMMD`/`OutlinedButtonMMD` (same
pattern as Task 4 Step 3). Leave `TextField`/`OutlinedTextField` as
Material3 — MMD has no documented text-field component in any sibling
app's usage, so these inherit their look from `eInkColorScheme` via
`MaterialTheme.colorScheme` automatically and don't need a widget swap.

- [ ] **Step 2: Restyle `PhotosModeToggle` and grid item captions with `TextMMD`**

Swap `Text(...)` for `TextMMD(...)` using the same parameters, same as
Task 5 Step 2.

- [ ] **Step 3: Confirm any confirmation dialogs use `ModalBottomSheetMMD`**

If `SubmitPhotosTab` or `RecentPhotosTab` show a confirmation dialog
(e.g. before submitting, or for a destructive action), apply the same
`AlertDialog` → `ModalBottomSheetMMD` substitution as Task 4 Step 2.

- [ ] **Step 4: Build, install, and visually verify on the Mudita**

```bash
./gradlew :app:assembleDebug -q
adb -s MK20250402537 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s MK20250402537 shell monkey -p com.erdman.kofc6650kompakt -c android.intent.category.LAUNCHER 1
# Navigate to the Photos tab, toggle Submit/Recent, open the pinch-zoom viewer
adb -s MK20250402537 exec-out screencap -p > /tmp/kompakt-photos.png
```

Expected: form and toggle in black/white/grayscale, photo grid and
zoom/pan viewer functionally unchanged (photos themselves are still
color images — that's expected, only the app chrome is monochrome).

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "Re-theme Photos screen with MMD components"
git push
```

---

### Task 7: Re-theme the Payments screen with MMD components

**Files:**
- Modify: `app/src/main/java/com/erdman/kofc6650kompakt/ui/screens/PaymentsScreen.kt`

**Interfaces:**
- Consumes: `KofC6650KompaktTheme` (Task 2).
- Produces: same signature as before (`PaymentsTab()`).

- [ ] **Step 1: Restyle `PaymentsTab`'s action buttons ("Pay Dues", "Pay for Badge") with `ButtonMMD`**

Same substitution as Task 4 Step 3.

- [ ] **Step 2: Replace `DuesPaymentDialog` and `BadgePaymentDialog`'s `AlertDialog` with `ModalBottomSheetMMD`**

Same substitution as Task 4 Step 2 — both dialogs collect an amount/
confirmation before handing off to `PayPalSubmitter`, so their shape is
title + form content + Cancel/Confirm button row.

- [ ] **Step 3: Build, install, and visually verify on the Mudita**

```bash
./gradlew :app:assembleDebug -q
adb -s MK20250402537 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s MK20250402537 shell monkey -p com.erdman.kofc6650kompakt -c android.intent.category.LAUNCHER 1
# Navigate to the Payments tab, open both dialogs
adb -s MK20250402537 exec-out screencap -p > /tmp/kompakt-payments.png
```

Expected: black/white/grayscale throughout, dialogs render as bottom
sheets.

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "Re-theme Payments screen with MMD components"
git push
```

---

### Task 8: Re-theme the PIN gate and header dialogs with MMD components

**Files:**
- Modify: `app/src/main/java/com/erdman/kofc6650kompakt/ui/screens/PinGateScreen.kt`
- Modify: `app/src/main/java/com/erdman/kofc6650kompakt/ui/dialogs/HeaderDialogs.kt`
- Modify: `app/src/main/java/com/erdman/kofc6650kompakt/MainActivity.kt` (Step 5's whole-codebase grep found `KofcNavy` still used at 3 lines here — an import plus two `CircularProgressIndicator(color = KofcNavy)` calls for the Calendar/Photos tabs' initial-loading spinners, inside `KofcApp`'s tab-content routing. Task 2 only touched `KofcApp`'s own top-bar/tab-row colors, not these two spinners buried further down in the same function — a plan gap, found during Task 8's own execution. Migrate both to `Color.Black`, matching the identical `CircularProgressIndicator` substitution already used in Tasks 4-7.)
- Delete: `app/src/main/java/com/erdman/kofc6650kompakt/ui/theme/KofC6650Theme.kt` (transitional color-constants file from Task 2 Step 2 — this task removes its last references)

**Interfaces:**
- Consumes: `KofC6650KompaktTheme` (Task 2); the transitional
  `KofcNavy`/`KofcGold`/etc. constants from `KofC6650Theme.kt` (Task 2
  Step 2), which this task deletes.
- Produces: same signatures as before (`PinGateScreen(pinManager,
  onOpenUrl)`, `JoinKofcDialog(onDismiss)`, `AboutDialog(...)`,
  `WhatsNewDialog(onDismiss)`, `DirectorsOfficersDialog(onDismiss)`).

- [ ] **Step 1: Restyle `PinGateScreen`'s PIN entry keypad/buttons with `ButtonMMD`**

Same substitution as Task 4 Step 3 for every digit/action button.

- [ ] **Step 2: Restyle the QR code display in `JoinKofcDialog`**

`QrCodeGenerator.kt` (ported unchanged in Task 1) produces a `Bitmap` —
confirm it still renders as pure black-on-white (it should already,
since QR codes are inherently monochrome); no change needed to the
generator itself, only to the dialog chrome around it.

- [ ] **Step 3: Replace `JoinKofcDialog`, `AboutDialog`, `WhatsNewDialog`, `DirectorsOfficersDialog`'s `AlertDialog`/`Dialog` wrappers with `ModalBottomSheetMMD`**

Same substitution as Task 4 Step 2 for each of the four dialogs.

- [ ] **Step 4: Restyle `AboutDialog`'s text-size picker and `DirectorsOfficersDialog`'s `LeadershipRow` list with `TextMMD`/`LazyColumnMMD`**

Same substitutions as Task 5 Steps 1-2.

- [ ] **Step 5: Remove every remaining reference to the transitional color constants, then delete the file**

By this point Tasks 4-8 have re-themed every screen that used
`KofcNavy`/`KofcNavyLight`/`KofcGold`/`KofcGoldMuted`/`KofcBackground`/
`KofcCard` (defined in `ui/theme/KofC6650Theme.kt`, trimmed to just
those constants back in Task 2 Step 2). Confirm nothing still uses them:

```bash
grep -rn "KofcNavy\|KofcGold\|KofcBackground\|KofcCard\b" app/src/main/java/com/erdman/kofc6650kompakt --include='*.kt'
```

Expected: the only matches left should be inside `PinGateScreen.kt` and
`HeaderDialogs.kt` themselves — the two files this task's Steps 1-4
just finished re-theming. Replace each remaining hit the same way Tasks
4-7 did: `KofcNavy`/`KofcGold` used as a `Text`/`Icon` color or a
`Button`/`Card` `containerColor` → drop the explicit color and let it
inherit `MaterialTheme.colorScheme` from `eInkColorScheme`, or use
`Color.Black`/`Color.White` directly for a hardcoded monochrome accent
where the surrounding code genuinely needs one (e.g. a selected-state
highlight). Once the `grep` above returns nothing outside
`KofC6650Theme.kt`'s own declaration lines:

```bash
rm app/src/main/java/com/erdman/kofc6650kompakt/ui/theme/KofC6650Theme.kt
```

Rebuild and confirm no unresolved-reference errors before moving on —
if there are any, some call site was missed above.

- [ ] **Step 6: Build, install, and visually verify on the Mudita**

```bash
./gradlew :app:assembleDebug -q
adb -s MK20250402537 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s MK20250402537 shell monkey -p com.erdman.kofc6650kompakt -c android.intent.category.LAUNCHER 1
# On first launch this hits the PIN gate; enter 1882, then open the
# header icon (QR popup), info icon (About), and Directors/Officers link
adb -s MK20250402537 exec-out screencap -p > /tmp/kompakt-pingate.png
```

Expected: PIN keypad and all four header dialogs in black/white/
grayscale, no remaining `AlertDialog` centered popups anywhere in the
app.

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "Re-theme PIN gate and header dialogs with MMD components; remove transitional color file"
git push
```

---

### Task 9: Redesign the app icon in solid black/white

**Files:**
- Modify: `app/src/main/res/drawable/ic_launcher_background.xml`
- Modify: `app/src/main/res/drawable/ic_launcher_foreground.xml`

**Interfaces:**
- Produces: a launcher icon that renders correctly on Kompakt's
  launcher (solid `fillColor` shapes only).

The current icon (a navy "K" monogram on a gold background) already
uses solid `fillColor` paths with no strokes — good, no risk of the
Kompakt stroke-rendering bug — so this task is a straight color swap,
not a redraw.

- [ ] **Step 1: Change the background to solid white**

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:fillColor="#FFFFFF"
        android:pathData="M0,0h108v108h-108z" />
</vector>
```

- [ ] **Step 2: Change the "K" monogram to solid black**

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <!-- Bold black "K" monogram -->
    <path
        android:fillColor="#000000"
        android:pathData="M36,28 L48,28 L48,80 L36,80 Z" />
    <path
        android:fillColor="#000000"
        android:pathData="M43.76,49.76 L71.76,21.76 L80.24,30.24 L52.24,58.24 Z" />
    <path
        android:fillColor="#000000"
        android:pathData="M43.76,58.24 L71.76,86.24 L80.24,77.76 L52.24,49.76 Z" />
</vector>
```

- [ ] **Step 3: Build, install, and verify the launcher icon on the Mudita home screen**

```bash
./gradlew :app:assembleDebug -q
adb -s MK20250402537 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s MK20250402537 shell am start -a android.intent.action.MAIN -c android.intent.category.HOME
adb -s MK20250402537 exec-out screencap -p > /tmp/kompakt-launcher-icon.png
```

Expected: a solid black "K" on a solid white circle/square, no gray
blocks where an outline was intended — confirms the icon isn't hitting
the stroke-rendering bug (moot here since no strokes are used, but this
is the actual verification the bug requires).

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "Redesign launcher icon in solid black/white"
git push
```

---

### Task 10: Final full-device verification pass

**Files:** none (verification only)

**Interfaces:** none — this task closes out the spec's Testing section.

- [ ] **Step 1: Fresh install from a clean build**

```bash
cd ~/Projects/KofC6650Kompakt
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
./gradlew clean :app:assembleDebug -q
adb -s MK20250402537 uninstall com.erdman.kofc6650kompakt
adb -s MK20250402537 install app/build/outputs/apk/debug/app-debug.apk
```

- [ ] **Step 2: Walk every screen and screenshot it**

```bash
adb -s MK20250402537 shell monkey -p com.erdman.kofc6650kompakt -c android.intent.category.LAUNCHER 1
```

Manually (via the device or `adb shell input tap`) go through: PIN
gate → Calendar tab (agenda view, month view, search, star an event,
trigger the reminder prompt, "Add to Calendar", and — if a "Feed the
Homeless" event with an open date is present at the time — open
`FeedTheHomelessSignupDialog` too; Task 4's review flagged this
dialog's button/color conversions as unverified on-device, since
triggering it depends on live signup data) → Minutes tab → Photos
tab (both Submit and Recent modes, pinch-zoom viewer) → Payments tab
(both dialogs) → header QR popup → About dialog → text-size picker →
Directors/Officers dialog. Screenshot each with `adb -s MK20250402537
exec-out screencap -p > /tmp/kompakt-final-<screen>.png`.

- [ ] **Step 3: Confirm against the spec's constraints**

Check each screenshot against the Global Constraints section of this
plan and the spec's Design System section:
- No navy (`#1A2F5E`) or gold (`#C9A84C`) anywhere
- No visible ripple animation on any tap (hard to screenshot — tap and
  watch for a pulse instead)
- Every list scrolls with MMD's jump-scroll (not the standard Compose
  fling)
- Every confirmation/info dialog is a bottom sheet, not a centered
  `AlertDialog` box
- Launcher icon is solid black/white with no gray block artifacts

- [ ] **Step 4: Fix any regression found, otherwise done**

If a screen doesn't match, go back to that screen's task and fix it in
a new commit (don't silently patch it in this task — keep the git
history attributable to the screen it belongs to). Once every screen
passes, this plan is complete.
