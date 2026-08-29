package com.erdman.kofc6650

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.CalendarContract
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.TextStyle as ComposeTextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.erdman.kofc6650.data.ArchiveMonthDto
import com.erdman.kofc6650.data.DriveFileDto
import com.erdman.kofc6650.data.EventDto
import com.erdman.kofc6650.data.FontScalePreference
import com.erdman.kofc6650.data.KofcRepository
import com.erdman.kofc6650.data.LeadershipContact
import com.erdman.kofc6650.data.LeadershipDirectory
import com.erdman.kofc6650.data.OfflineCache
import com.erdman.kofc6650.data.PhotoUploadFile
import com.erdman.kofc6650.data.PinManager
import com.erdman.kofc6650.data.RecentPhotoDto
import com.erdman.kofc6650.data.ReminderStore
import com.erdman.kofc6650.data.RsvpStore
import com.erdman.kofc6650.data.SignupStatusDto
import com.erdman.kofc6650.data.WhatsNew
import com.erdman.kofc6650.notifications.ReminderScheduler
import com.erdman.kofc6650.ui.theme.KofC6650Theme
import com.erdman.kofc6650.ui.theme.KofcGold
import com.erdman.kofc6650.ui.theme.KofcGoldMuted
import com.erdman.kofc6650.ui.theme.KofcNavy
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    // Home screen shortcuts route here via an Intent extra rather than a
    // deep link, since the tabs aren't separate screens/destinations --
    // just an index into the same KofcApp's TabRow. Held at the Activity
    // level (not inside KofcApp's state) so onNewIntent can update it too.
    private val pendingTargetTab = mutableStateOf<Int?>(null)
    // Distinguishes the Submit Photos vs. Recent Photos shortcuts now that
    // they're one merged tab with a Browse/Submit toggle instead of two
    // separate tab indices.
    private val pendingPhotosMode = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingTargetTab.value = intent?.getStringExtra("target_tab")?.toIntOrNull()
        pendingPhotosMode.value = intent?.getStringExtra("photos_mode")
        setContent {
            val appearanceModePref = remember { com.erdman.kofc6650.data.AppearanceModePreference(this) }
            val darkTheme = when (appearanceModePref.mode) {
                com.erdman.kofc6650.data.AppearanceModePreference.Mode.SYSTEM -> isSystemInDarkTheme()
                com.erdman.kofc6650.data.AppearanceModePreference.Mode.LIGHT -> false
                com.erdman.kofc6650.data.AppearanceModePreference.Mode.DARK -> true
            }
            KofC6650Theme(darkTheme = darkTheme) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    KofcApp(
                        pendingTargetTab = pendingTargetTab,
                        pendingPhotosMode = pendingPhotosMode,
                        appearanceModePref = appearanceModePref,
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingTargetTab.value = intent.getStringExtra("target_tab")?.toIntOrNull()
        pendingPhotosMode.value = intent.getStringExtra("photos_mode")
    }
}

private fun formatDate(dateStr: String): String = try {
    val date = LocalDate.parse(dateStr)
    val dayOfWeek = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.US)
    val month = date.month.getDisplayName(TextStyle.FULL, Locale.US)
    "$dayOfWeek, $month ${date.dayOfMonth}, ${date.year}"
} catch (e: Exception) {
    dateStr
}

// Hands the next upcoming event to the home screen widget -- the widget
// provider can't reach the network or KofcRepository itself, so this is
// the only way it learns anything.
private fun updateNextEventWidget(context: android.content.Context, allEvents: List<EventDto>) {
    val today = LocalDate.now()
    val next = allEvents
        .filter { event ->
            val date = try { LocalDate.parse(event.date) } catch (e: Exception) { null }
            date != null && !date.isBefore(today)
        }
        .sortedBy { it.date }
        .firstOrNull()
    val info = next?.let {
        com.erdman.kofc6650.data.NextEventInfo(
            title = it.title,
            dateDisplay = formatDate(it.date),
            time = it.time?.takeIf { t -> t.isNotBlank() },
            location = it.location,
        )
    }
    com.erdman.kofc6650.data.NextEventWidgetData.save(context, info)
    com.erdman.kofc6650.widget.NextEventWidgetProvider.updateAll(context)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KofcApp(
    pendingTargetTab: MutableState<Int?> = remember { mutableStateOf(null) },
    pendingPhotosMode: MutableState<String?> = remember { mutableStateOf(null) },
    appearanceModePref: com.erdman.kofc6650.data.AppearanceModePreference? = null,
) {
    val repository = remember { KofcRepository() }
    val context = LocalContext.current
    val pinManager = remember { PinManager(context) }
    val fontScalePref = remember { FontScalePreference(context) }
    val resolvedAppearanceModePref = appearanceModePref
        ?: remember { com.erdman.kofc6650.data.AppearanceModePreference(context) }
    // A neutral fontScale (not the true system density) -- the header opts
    // out of both the in-app Text Size preference AND the device's own
    // accessibility font size, so it always renders at a fixed size and
    // never overflows the TopAppBar's fixed-width title slot.
    val baseDensity = Density(density = LocalDensity.current.density, fontScale = 1f)
    val scaledDensity = Density(
        density = LocalDensity.current.density,
        fontScale = fontScalePref.preset.multiplier,
    )

    if (!pinManager.isUnlocked) {
        CompositionLocalProvider(LocalDensity provides scaledDensity) {
            PinGateScreen(
                pinManager = pinManager,
                onOpenUrl = { url -> context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) },
            )
        }
        return
    }

    CompositionLocalProvider(LocalDensity provides scaledDensity) {

    var showAbout by remember { mutableStateOf(false) }
    var showDirectorsOfficers by remember { mutableStateOf(false) }
    var showJoinKofc by remember { mutableStateOf(false) }
    var showWhatsNew by remember { mutableStateOf(WhatsNew.shouldShow(context)) }
    var tabIndex by remember { mutableIntStateOf(pendingTargetTab.value ?: 0) }
    LaunchedEffect(pendingTargetTab.value) {
        pendingTargetTab.value?.let {
            tabIndex = it
            pendingTargetTab.value = null
        }
    }
    var allEvents by remember { mutableStateOf<List<EventDto>>(emptyList()) }
    var isLoadingAllEvents by remember { mutableStateOf(true) }
    var allEventsError by remember { mutableStateOf<String?>(null) }
    var photos by remember { mutableStateOf<List<RecentPhotoDto>>(emptyList()) }
    var isLoadingPhotos by remember { mutableStateOf(true) }
    var photosError by remember { mutableStateOf<String?>(null) }
    var hasNewPhotos by remember { mutableStateOf(false) }
    var feedTheHomelessOpenDates by remember { mutableStateOf<List<SignupStatusDto>?>(null) }
    var refreshTrigger by remember { mutableIntStateOf(0) }
    val offlineCache = remember { OfflineCache(context) }
    // true = Browse, false = Submit -- defaults to Browse since viewing is
    // the more common reason someone opens this tab.
    var photosBrowseMode by remember { mutableStateOf(true) }
    LaunchedEffect(pendingPhotosMode.value) {
        pendingPhotosMode.value?.let {
            photosBrowseMode = it != "submit"
            pendingPhotosMode.value = null
        }
    }

    // Mirrors the old "Recent Photos" tab's onClick behavior (mark seen the
    // moment that content becomes visible), just keyed on the merged tab's
    // mode instead of a separate tab selection.
    LaunchedEffect(tabIndex, photosBrowseMode) {
        if (tabIndex == 2 && photosBrowseMode) {
            offlineCache.markPhotosSeen(photos)
            hasNewPhotos = false
        }
    }

    // Refetch whenever the app comes back to the foreground, so events/photos
    // added elsewhere show up without the user having to remember to tap the
    // refresh button. Skips the very first resume, which fires immediately
    // on launch and would otherwise double the initial fetch.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        var isFirstResume = true
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (isFirstResume) {
                    isFirstResume = false
                } else {
                    refreshTrigger++
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(refreshTrigger) {
        if (com.erdman.kofc6650.data.ScreenshotMode.isActive(context)) {
            allEvents = com.erdman.kofc6650.data.ScreenshotMode.sampleAllEvents
            isLoadingAllEvents = false
            isLoadingPhotos = false
            return@LaunchedEffect
        }
        isLoadingAllEvents = true
        allEventsError = null
        isLoadingPhotos = true
        photosError = null
        coroutineScope {
            launch {
                try {
                    val bundle = repository.getCouncilEvents()
                    allEvents = bundle.allEvents
                    offlineCache.saveEvents(bundle.signupEvents, bundle.allEvents)
                } catch (e: Exception) {
                    val cached = offlineCache.loadEvents()
                    if (cached != null) {
                        allEvents = cached.allEvents
                        allEventsError = "Showing saved events from ${offlineCache.relativeEventsSavedAt()} — couldn't reach the server."
                    } else {
                        allEventsError = "Could not load calendar events."
                    }
                } finally {
                    isLoadingAllEvents = false
                    updateNextEventWidget(context, allEvents)
                }
            }
            launch {
                try {
                    val fetched = repository.getRecentPhotos()
                    photos = fetched
                    offlineCache.savePhotos(fetched)
                } catch (e: Exception) {
                    val cached = offlineCache.loadPhotos()
                    if (cached != null) {
                        photos = cached
                        photosError = "Showing saved photos from ${offlineCache.relativePhotosSavedAt()} — couldn't reach the server."
                    } else {
                        photosError = "Could not load recent photos."
                    }
                } finally {
                    isLoadingPhotos = false
                    hasNewPhotos = offlineCache.hasNewPhotos(photos)
                }
            }
            launch {
                // Feeds the "Open · N of 6 filled" badge shown on the Feed
                // the Homeless event card. No offline cache -- this is a
                // live, fast-changing count, not worth showing stale; a
                // failed fetch just leaves the card's status badge absent
                // rather than blocking anything (the signup dialog itself
                // still fetches its own fresh copy when opened).
                feedTheHomelessOpenDates = try {
                    repository.getFeedTheHomelessStatus()
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = KofcNavy,
                        titleContentColor = KofcGold,
                    ),
                    title = {
                        // Overrides the Text Size preference for this one
                        // subtree -- the header stays a fixed size no
                        // matter what the user picks in About.
                        CompositionLocalProvider(LocalDensity provides baseDensity) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(KofcGold, CircleShape)
                                        .clickable { showJoinKofc = true },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        painterResource(R.drawable.ic_qr_code),
                                        contentDescription = "Join Council 6650 QR code",
                                        tint = KofcNavy,
                                        modifier = Modifier.size(22.dp),
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Knights of Columbus",
                                        color = KofcGold,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(
                                        text = "Council 6650 — Cary & Apex, NC",
                                        color = Color(0xFFAABBCC),
                                        fontSize = 12.sp,
                                    )
                                }
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { showDirectorsOfficers = true }, modifier = Modifier.size(36.dp)) {
                            Icon(
                                Icons.Default.Email,
                                contentDescription = "Directors & Officers",
                                tint = KofcGold,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        IconButton(onClick = { showAbout = true }, modifier = Modifier.size(36.dp)) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = "About",
                                tint = KofcGold,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        IconButton(onClick = { refreshTrigger++ }, modifier = Modifier.size(36.dp)) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = KofcGold,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    },
                )
                ScrollableTabRow(
                    selectedTabIndex = tabIndex,
                    containerColor = KofcNavy,
                    contentColor = KofcGold,
                    edgePadding = 12.dp,
                ) {
                    Tab(
                        selected = tabIndex == 0,
                        onClick = { tabIndex = 0 },
                        text = { Text("Calendar") },
                    )
                    Tab(
                        selected = tabIndex == 1,
                        onClick = { tabIndex = 1 },
                        text = { Text("Minutes") },
                    )
                    Tab(
                        selected = tabIndex == 2,
                        onClick = { tabIndex = 2 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Photos")
                                if (hasNewPhotos) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(KofcGold, CircleShape),
                                    )
                                }
                            }
                        },
                    )
                    Tab(
                        selected = tabIndex == 3,
                        onClick = { tabIndex = 3 },
                        text = { Text("Payments") },
                    )
                }
            }
        },
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // Only block the whole screen on the very first load of each
            // tab's data -- once there's something to show, a refresh
            // (pull-to-refresh or tab resume) should keep the list visible
            // with its own pull indicator instead of blanking the screen.
            if (isLoadingAllEvents && allEvents.isEmpty() && tabIndex == 0) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = KofcNavy)
                }
            } else if (tabIndex == 0) {
                CalendarAgendaTab(
                    events = allEvents,
                    errorMessage = allEventsError,
                    isRefreshing = isLoadingAllEvents,
                    onRefresh = { refreshTrigger++ },
                    onSignUpClick = { url ->
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    },
                    feedTheHomelessOpenDates = feedTheHomelessOpenDates,
                )
            } else if (tabIndex == 1) {
                MinutesTab(repository = repository)
            } else if (tabIndex == 2 && photosBrowseMode && isLoadingPhotos && photos.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = KofcNavy)
                }
            } else if (tabIndex == 2) {
                PhotosTab(
                    repository = repository,
                    pinManager = pinManager,
                    photos = photos,
                    photosError = photosError,
                    isLoadingPhotos = isLoadingPhotos,
                    onRefreshPhotos = { refreshTrigger++ },
                    browseMode = photosBrowseMode,
                    onBrowseModeChange = { photosBrowseMode = it },
                )
            } else {
                PaymentsTab()
            }
        }
    }

    if (showAbout) {
        AboutDialog(
            pinManager = pinManager,
            fontScalePref = fontScalePref,
            appearanceModePref = resolvedAppearanceModePref,
            onDismiss = { showAbout = false },
        )
    }

    if (showDirectorsOfficers) {
        DirectorsOfficersDialog(onDismiss = { showDirectorsOfficers = false })
    }

    if (showJoinKofc) {
        JoinKofcDialog(onDismiss = { showJoinKofc = false })
    }

    if (showWhatsNew) {
        WhatsNewDialog(
            onDismiss = {
                WhatsNew.markSeen(context)
                showWhatsNew = false
            },
        )
    }

    } // CompositionLocalProvider(LocalDensity)
}

private const val JOIN_KOFC_URL = "https://www.kofc.org/get-involved/join-kofc"
private const val JOIN_KOFC_PROMO_CODE = "BLESSEDMCGIVNEY"
private const val FEED_THE_HOMELESS_DETAILS_URL = "https://www.kofc6650.org/programs/feed-the-homeless"

@Composable
private fun JoinKofcDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val qrBitmap = remember { com.erdman.kofc6650.data.QrCodeGenerator.generate(JOIN_KOFC_URL) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Not a member yet?") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Image(
                    bitmap = qrBitmap.asImageBitmap(),
                    contentDescription = "QR code to join Council 6650",
                    modifier = Modifier
                        .size(220.dp)
                        .background(Color.White)
                        .padding(12.dp),
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = JOIN_KOFC_URL,
                    fontSize = 13.sp,
                    color = KofcNavy,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.clickable {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(JOIN_KOFC_URL)))
                    },
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Use code for a free one-year membership:",
                    fontSize = 13.sp,
                    color = Color(0xFF999999),
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = JOIN_KOFC_PROMO_CODE,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = KofcGoldMuted,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        },
    )
}

@Composable
private fun WhatsNewDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("What's New") },
        text = { Text(WhatsNew.CHANGELOG) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Got It") }
        },
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AboutDialog(
    pinManager: PinManager,
    fontScalePref: FontScalePreference,
    appearanceModePref: com.erdman.kofc6650.data.AppearanceModePreference,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("About") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier = Modifier.size(56.dp).background(KofcGold, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "K", color = KofcNavy, fontWeight = FontWeight.Bold, fontSize = 26.sp)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text("Knights of Columbus", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Text("Council 6650 — Cary & Apex, NC", fontSize = 13.sp, color = KofcGoldMuted)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Version ${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})",
                    fontSize = 13.sp,
                    color = Color(0xFF999999),
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Text Size", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                SingleChoiceSegmentedButtonRow {
                    FontScalePreference.Preset.entries.forEachIndexed { index, preset ->
                        SegmentedButton(
                            selected = fontScalePref.preset == preset,
                            onClick = { fontScalePref.choose(preset) },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = FontScalePreference.Preset.entries.size,
                            ),
                            icon = {},
                        ) {
                            Text(preset.label, fontSize = 11.sp, maxLines = 1, softWrap = false)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("Appearance", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                SingleChoiceSegmentedButtonRow {
                    com.erdman.kofc6650.data.AppearanceModePreference.Mode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = appearanceModePref.mode == mode,
                            onClick = { appearanceModePref.choose(mode) },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = com.erdman.kofc6650.data.AppearanceModePreference.Mode.entries.size,
                            ),
                            icon = {},
                        ) {
                            Text(mode.label, fontSize = 11.sp, maxLines = 1, softWrap = false)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(onClick = { reportProblem(context) }) {
                    Text("Report a Problem")
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "You'll be asked for the council PIN again next time you open the app.",
                    fontSize = 12.sp,
                    color = Color(0xFF999999),
                    textAlign = TextAlign.Center,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                pinManager.clear()
                onDismiss()
            }) {
                Text("Reset saved PIN", color = Color(0xFFA12626))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DirectorsOfficersDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = KofcNavy,
                        titleContentColor = KofcGold,
                    ),
                    title = { Text("Directors & Officers") },
                    actions = {
                        TextButton(onClick = onDismiss) {
                            Text("Done", color = KofcGold)
                        }
                    },
                )
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 32.dp),
                ) {
                    leadershipSection(title = "Officers", contacts = LeadershipDirectory.officers) { email ->
                        context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email")))
                    }
                    leadershipSection(title = "Directors", contacts = LeadershipDirectory.directors) { email ->
                        context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email")))
                    }
                }
            }
        }
    }
}

private fun LazyListScope.leadershipSection(
    title: String,
    contacts: List<LeadershipContact>,
    onEmailClick: (String) -> Unit,
) {
    item {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = KofcGoldMuted,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
    items(contacts) { contact ->
        LeadershipRow(contact = contact, onEmailClick = onEmailClick)
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
    }
}

@Composable
private fun LeadershipRow(contact: LeadershipContact, onEmailClick: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (contact.email != null) {
                    Modifier.clickable { onEmailClick(contact.email) }
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = contact.title,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = KofcGoldMuted,
            )
            Text(
                text = contact.name,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        if (contact.email != null) {
            Icon(Icons.Default.Email, contentDescription = "Email", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun PinGateScreen(pinManager: PinManager, onOpenUrl: (String) -> Unit) {
    var pinInput by remember { mutableStateOf("") }
    var showIncorrect by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier.size(56.dp).background(KofcGold, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "K", color = KofcNavy, fontWeight = FontWeight.Bold, fontSize = 26.sp)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Knights of Columbus",
            fontSize = 21.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(text = "Council 6650 — Cary & Apex, NC", fontSize = 13.sp, color = KofcGoldMuted)

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Enter the council PIN to continue",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(12.dp))

        androidx.compose.material3.OutlinedTextField(
            value = pinInput,
            onValueChange = {
                pinInput = it
                showIncorrect = false
                pinManager.verify(it)
            },
            label = { Text("Council PIN") },
            singleLine = true,
            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword,
            ),
            modifier = Modifier.width(200.dp),
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                if (!pinManager.verify(pinInput)) {
                    showIncorrect = true
                }
            },
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = KofcNavy,
                contentColor = KofcGold,
            ),
        ) {
            Text("Unlock")
        }

        if (showIncorrect) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Incorrect PIN", fontSize = 13.sp, color = Color(0xFFA12626))
        }

        Spacer(modifier = Modifier.height(20.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Not a member yet?",
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(6.dp))

        val signUpText = buildAnnotatedString {
            pushStringAnnotation(tag = "URL", annotation = "https://www.kofc.org/get-involved/join-kofc/")
            withStyle(style = SpanStyle(color = KofcGold, textDecoration = TextDecoration.Underline)) {
                append("Sign up here")
            }
            pop()
            append(" and use code ")
            withStyle(style = SpanStyle(fontWeight = FontWeight.SemiBold)) {
                append("BLESSEDMCGIVNEY")
            }
            append(" for a free one-year membership.\n\n")
            append(
                "We're Council 6650, serving St. Michael the Archangel, St. Andrew the Apostle " +
                    "and Mother Teresa Catholic Churches in Cary and Apex, NC."
            )
        }
        ClickableText(
            text = signUpText,
            style = ComposeTextStyle(
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            ),
            modifier = Modifier.fillMaxWidth(),
            onClick = { offset ->
                signUpText.getStringAnnotations(tag = "URL", start = offset, end = offset)
                    .firstOrNull()
                    ?.let { onOpenUrl(it.item) }
            },
        )
    }
}

// Material3's PullToRefreshBox needs a newer material3 than this project
// pins (1.2.1), but the older @ExperimentalMaterialApi pullRefresh API is
// already on the classpath transitively -- no new dependency needed.
@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun RefreshableList(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    listState: LazyListState = rememberLazyListState(),
    content: LazyListScope.() -> Unit,
) {
    val pullRefreshState = rememberPullRefreshState(refreshing = isRefreshing, onRefresh = onRefresh)
    Box(modifier = Modifier.fillMaxSize().pullRefresh(pullRefreshState)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
            state = listState,
            content = content,
        )
        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            backgroundColor = Color.White,
            contentColor = KofcNavy,
        )
    }
}

@Composable
private fun RsvpLegend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.Star,
            contentDescription = null,
            tint = KofcGoldMuted,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "Tap the star to track your events",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = KofcGoldMuted,
        )
    }
}

// A single icon button (rather than a full-width Agenda/Month pill) that
// sits inline with the title -- the icon shows the mode a tap switches TO,
// with a filled background once Month is active so the button still reads
// as a toggle rather than a plain action button.
@Composable
private fun ViewModeIconToggle(pref: com.erdman.kofc6650.data.CalendarViewModePreference) {
    val isMonth = pref.mode == com.erdman.kofc6650.data.CalendarViewModePreference.Mode.MONTH
    IconButton(
        onClick = {
            pref.choose(
                if (isMonth) {
                    com.erdman.kofc6650.data.CalendarViewModePreference.Mode.AGENDA
                } else {
                    com.erdman.kofc6650.data.CalendarViewModePreference.Mode.MONTH
                },
            )
        },
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isMonth) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent),
    ) {
        Icon(
            Icons.Filled.DateRange,
            contentDescription = if (isMonth) "Switch to Agenda view" else "Switch to Month view",
            tint = if (isMonth) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp),
        )
    }
}

// Same slim pill look the Agenda/Month toggle used to have, but for the
// separate all-events vs. sign-up-opportunities-only filter (session-only
// state, not persisted -- unlike the view mode, there's no strong reason to
// remember this choice across app launches).
@Composable
private fun EventFilterToggle(signupOnly: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(2.dp),
    ) {
        listOf(false to "All Events", true to "Volunteer").forEach { (value, label) ->
            val selected = signupOnly == value
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selected) MaterialTheme.colorScheme.surface else Color.Transparent)
                    .clickable { onChange(value) }
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// A month grid with a dot under any date that has an event, plus the
// selected date's events listed below (reusing EventCard so an individual
// event looks identical to the agenda view). `events` covers today-or-later
// plus the last 14 days on both tabs' month views -- browsing further back
// than that just shows an empty grid, hence the disabled "back" button when
// already on the current month. Rendered as plain (non-lazy) content since
// it's always called from inside a LazyColumn item {} -- nesting a
// LazyVerticalGrid there would fight the outer scroll.
private fun LazyListScope.monthCalendarContent(
    events: List<EventDto>,
    onSignUpClick: (String) -> Unit,
    headerHeight: Dp,
    feedTheHomelessOpenDates: List<SignupStatusDto>?,
) {
    item {
        var displayedMonth by remember { mutableStateOf(YearMonth.now()) }
        var selectedDate by remember { mutableStateOf(LocalDate.now()) }
        val isCurrentMonth = displayedMonth == YearMonth.now()
        val eventDates = remember(events) { events.mapNotNull { runCatching { LocalDate.parse(it.date) }.getOrNull() }.toSet() }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    IconButton(
                        onClick = { displayedMonth = displayedMonth.minusMonths(1) },
                        enabled = !isCurrentMonth,
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Previous month",
                            tint = if (isCurrentMonth) Color(0xFFCCCCCC) else KofcNavy,
                        )
                    }
                    Text(
                        text = displayedMonth.month.getDisplayName(TextStyle.FULL, Locale.US) + " " + displayedMonth.year,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                    )
                    IconButton(onClick = { displayedMonth = displayedMonth.plusMonths(1) }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next month", tint = KofcNavy)
                    }
                    TextButton(onClick = {
                        displayedMonth = YearMonth.now()
                        selectedDate = LocalDate.now()
                    }) {
                        Text("Today", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    }
                }

                Row(modifier = Modifier.fillMaxWidth()) {
                    for (d in listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa")) {
                        Text(
                            text = d,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = KofcGoldMuted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                val firstOfMonth = displayedMonth.atDay(1)
                // DayOfWeek.SUNDAY.value == 7; this maps Sunday to a leading
                // offset of 0 instead of 6 so the grid starts on Sunday.
                val leadingBlanks = firstOfMonth.dayOfWeek.value % 7
                val daysInMonth = displayedMonth.lengthOfMonth()
                val totalCells = ((leadingBlanks + daysInMonth + 6) / 7) * 7

                for (week in 0 until totalCells / 7) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        for (col in 0 until 7) {
                            val cellIndex = week * 7 + col
                            val dayNumber = cellIndex - leadingBlanks + 1
                            Box(
                                modifier = Modifier.weight(1f).height(44.dp).padding(1.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (dayNumber in 1..daysInMonth) {
                                    val date = displayedMonth.atDay(dayNumber)
                                    val isSelected = date == selectedDate
                                    val isToday = date == LocalDate.now()
                                    val hasEvent = eventDates.contains(date)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) KofcGold else Color.Transparent)
                                            .then(
                                                if (isToday && !isSelected) {
                                                    Modifier.border(1.5.dp, KofcGold, RoundedCornerShape(8.dp))
                                                } else Modifier
                                            )
                                            .clickable { selectedDate = date },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = dayNumber.toString(),
                                                fontSize = 14.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) KofcNavy else MaterialTheme.colorScheme.onBackground,
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .background(
                                                        if (hasEvent) (if (isSelected) KofcNavy else KofcGold) else Color.Transparent,
                                                        CircleShape,
                                                    ),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = formatDate(selectedDate.toString()),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(8.dp))

        val selectedDayEvents = events
            .filter { runCatching { LocalDate.parse(it.date) }.getOrNull() == selectedDate }
            .sortedBy { it.time ?: "" }
        if (selectedDayEvents.isEmpty()) {
            Text(
                text = "No events this day.",
                color = Color(0xFF999999),
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                textAlign = TextAlign.Center,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                selectedDayEvents.forEach { event ->
                    EventCard(event = event, onSignUpClick = onSignUpClick, feedTheHomelessOpenDates = feedTheHomelessOpenDates)
                }
            }
        }
    }

    // Guarantees just enough scrollable room for the auto-scroll-to-grid
    // effect to fully push the header out of view even when the selected
    // day's event list is short (e.g. "No events this day.") -- without
    // this, LazyColumn clamps the scroll to the actual content height and
    // the header sticks around partially visible. Sized to the header's own
    // measured height (the exact amount of extra scroll room needed) rather
    // than a full screen, so there's no dead space to keep scrolling past.
    item {
        Spacer(modifier = Modifier.height(headerHeight))
    }
}

private fun eventMatchesQuery(event: EventDto, query: String): Boolean {
    val q = query.trim()
    if (q.isEmpty()) return true
    return event.title.contains(q, ignoreCase = true) ||
        event.location?.contains(q, ignoreCase = true) == true ||
        event.description?.contains(q, ignoreCase = true) == true
}

@Composable
private fun EventSearchField(query: String, onQueryChange: (String) -> Unit) {
    androidx.compose.material3.OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Search events…", fontSize = 14.sp) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Filled.Close, contentDescription = "Clear search", modifier = Modifier.size(18.dp))
                }
            }
        },
        singleLine = true,
        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth().height(52.dp),
    )
}

@Composable
private fun CalendarAgendaTab(
    events: List<EventDto>,
    errorMessage: String?,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onSignUpClick: (String) -> Unit,
    feedTheHomelessOpenDates: List<SignupStatusDto>?,
) {
    val context = LocalContext.current
    val viewModePref = remember { com.erdman.kofc6650.data.CalendarViewModePreference(context) }
    val today = LocalDate.now()
    var signupOnly by remember { mutableStateOf(false) }
    // What used to be the separate Volunteer Sign Ups tab is now just this
    // filter -- same predicate the repository used to build that tab's
    // event list (getCouncilEvents' signupOnly), applied client-side here
    // instead so it composes with the view mode and search below.
    val baseEvents = if (signupOnly) events.filter { !it.signupUrl.isNullOrBlank() } else events
    val upcoming = baseEvents
        .filter { event ->
            val date = try {
                LocalDate.parse(event.date)
            } catch (e: Exception) {
                null
            }
            date != null && !date.isBefore(today)
        }
        .sortedBy { it.date }
    // The month grid also keeps the last 2 weeks of past events for
    // context (unlike the list view above, which stays today-or-later
    // only) -- selecting a recent past date still shows what happened.
    val monthViewEvents = baseEvents
        .filter { event ->
            val date = try {
                LocalDate.parse(event.date)
            } catch (e: Exception) {
                null
            }
            date != null && !date.isBefore(today.minusDays(14))
        }
        .sortedBy { it.date }
    var searchQuery by remember { mutableStateOf("") }
    val searchActive = searchQuery.isNotBlank()

    val isMonthMode = viewModePref.mode == com.erdman.kofc6650.data.CalendarViewModePreference.Mode.MONTH
    val listState = rememberLazyListState()
    val gridItemIndex = if (errorMessage != null) 2 else 1
    val density = LocalDensity.current
    var headerHeightPx by remember { mutableIntStateOf(0) }
    val headerHeight = with(density) { headerHeightPx.toDp() }
    LaunchedEffect(isMonthMode, errorMessage != null, headerHeightPx) {
        if (isMonthMode && !searchActive) {
            listState.animateScrollToItem(gridItemIndex)
        }
    }

    RefreshableList(isRefreshing = isRefreshing, onRefresh = onRefresh, listState = listState) {
        item {
            Column(modifier = Modifier.onGloballyPositioned { headerHeightPx = it.size.height }) {
                CompositionLocalProvider(LocalDensity provides Density(density = LocalDensity.current.density, fontScale = 1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Spacer(modifier = Modifier.width(40.dp))
                        Text(
                            text = "Upcoming Events",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f),
                        )
                        ViewModeIconToggle(viewModePref)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                EventFilterToggle(signupOnly) { signupOnly = it }
                Spacer(modifier = Modifier.height(8.dp))
                EventSearchField(searchQuery) { searchQuery = it }
                Spacer(modifier = Modifier.height(8.dp))
                if (!searchActive) {
                    RsvpLegend()
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        if (errorMessage != null) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF5F5))) {
                    Text(
                        text = errorMessage,
                        color = Color(0xFFC0392B),
                        modifier = Modifier.padding(14.dp),
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        if (searchActive) {
            val results = baseEvents.filter { eventMatchesQuery(it, searchQuery) }.sortedBy { it.date }
            if (results.isEmpty()) {
                item {
                    Text(
                        text = "No events match \"$searchQuery\".",
                        color = Color(0xFF999999),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                    )
                }
            } else {
                items(results) { event ->
                    EventCard(event = event, onSignUpClick = onSignUpClick, feedTheHomelessOpenDates = feedTheHomelessOpenDates)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        } else if (isMonthMode) {
            monthCalendarContent(monthViewEvents, onSignUpClick, headerHeight, feedTheHomelessOpenDates)
        } else {
            if (errorMessage == null && upcoming.isEmpty()) {
                item {
                    Text(
                        text = "No upcoming events on the calendar.",
                        color = Color(0xFF999999),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                    )
                }
            }

            eventSections(upcoming, onSignUpClick, feedTheHomelessOpenDates)
        }
    }
}

// Groups events into Today / This Week / Next Week / Later / Past 2 weeks
// sections so a longer list is easier to scan at a glance instead of one
// flat chronological list. Only the Sign Ups tab's agenda feeds this a past
// event, but the Calendar tab's list stays today-or-later, so "Past 2
// weeks" simply never has anything in it there.
private fun dateBucket(dateStr: String): String {
    val date = try { LocalDate.parse(dateStr) } catch (e: Exception) { return "Later" }
    val days = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), date)
    return when {
        days < 0L -> "Past 2 weeks"
        days == 0L -> "Today"
        days in 1..6 -> "This Week"
        days in 7..13 -> "Next Week"
        else -> "Later"
    }
}

private fun LazyListScope.eventSections(
    events: List<EventDto>,
    onSignUpClick: (String) -> Unit,
    feedTheHomelessOpenDates: List<SignupStatusDto>?,
) {
    for (bucketName in listOf("Today", "This Week", "Next Week", "Later", "Past 2 weeks")) {
        val bucketEvents = events.filter { dateBucket(it.date) == bucketName }.sortedBy { it.date }
        if (bucketEvents.isNotEmpty()) {
            item {
                Text(
                    text = bucketName.uppercase(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    color = KofcGold,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                )
            }
            items(bucketEvents) { event ->
                EventCard(event = event, onSignUpClick = onSignUpClick, feedTheHomelessOpenDates = feedTheHomelessOpenDates)
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun PaymentsTab() {
    val context = LocalContext.current
    var showDuesPayment by remember { mutableStateOf(false) }
    var showBadgePayment by remember { mutableStateOf(false) }

    // No option/amount to pick beforehand, so a plain PayPal link works
    // (unlike dues/badge, which need the in-app page's dropdown first).
    val donateUrl = "https://www.paypal.com/donate/?cmd=_s-xclick&hosted_button_id=VQ2AXC3TRTC62"

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
    ) {
        item {
            Text(
                text = "Dues & Badge Payments",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "💳 Pick an option below, then check out securely with PayPal",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = KofcGoldMuted,
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = { showDuesPayment = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = KofcNavy, contentColor = KofcGold),
                    ) {
                        Text("Pay Membership Dues")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { showBadgePayment = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = KofcNavy, contentColor = KofcGold),
                    ) {
                        Text("Pay for a Council Badge")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "You'll pick your dues category or badge type on the council's payment page, then finish checkout in PayPal.",
                        fontSize = 13.sp,
                        color = Color(0xFF555555),
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "❤️ Support the LAMB Foundation",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = KofcGoldMuted,
                    )
                    Text(
                        text = "Msgr. Michael A. Carey Council 6650 — assisting those with intellectual disabilities.",
                        fontSize = 13.sp,
                        color = Color(0xFF555555),
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                    )
                    Button(
                        onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(donateUrl))) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = KofcNavy, contentColor = KofcGold),
                    ) {
                        Text("Donate")
                    }
                }
            }
        }
    }

    if (showDuesPayment) {
        DuesPaymentDialog(onDismiss = { showDuesPayment = false })
    }

    if (showBadgePayment) {
        BadgePaymentDialog(onDismiss = { showBadgePayment = false })
    }
}

// Submits the council's PayPal hosted-button form ourselves (native
// picker/fields instead of embedding the council's webpage), then hands
// the resulting checkout URL off to the system browser -- we never touch
// payment entry ourselves.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DuesPaymentDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedOption by remember { mutableStateOf("Annual Membership Dues") }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val options = listOf(
        "Annual Membership Dues" to "Annual Membership Dues — \$39.88",
        "Dues + Penny-a-Day Fund" to "Dues + Penny-a-Day Fund — \$43.66",
    )

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { Text("Membership Dues") },
                    actions = { TextButton(onClick = onDismiss) { Text("Cancel") } },
                )
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                    Text("Choose an option", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    options.forEach { (value, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedOption = value }
                                .padding(vertical = 8.dp),
                        ) {
                            RadioButton(selected = selectedOption == value, onClick = { selectedOption = value })
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(label)
                        }
                    }

                    errorMessage?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(it, color = Color(0xFFA12626))
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            isSubmitting = true
                            errorMessage = null
                            scope.launch {
                                try {
                                    val url = withContext(Dispatchers.IO) {
                                        com.erdman.kofc6650.data.PayPalSubmitter.submit(
                                            hostedButtonId = "WZEZAAQZP7HAU",
                                            fields = mapOf(
                                                "on0" to "Select One",
                                                "os0" to selectedOption,
                                                "currency_code" to "USD",
                                            ),
                                        )
                                    }
                                    isSubmitting = false
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                    onDismiss()
                                } catch (e: Exception) {
                                    isSubmitting = false
                                    errorMessage = "Couldn't reach PayPal. Please try again."
                                }
                            }
                        },
                        enabled = !isSubmitting,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = KofcNavy, contentColor = KofcGold),
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = KofcGold, strokeWidth = 2.dp)
                        } else {
                            Text("Continue to PayPal")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BadgePaymentDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedOption by remember { mutableStateOf("Name Badge Only") }
    var nameOnBadge by remember { mutableStateOf("") }
    var officerRole by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val options = listOf(
        "Name Badge Only" to "Name Badge Only — \$15.00",
        "Name Badge & Magnet" to "Name Badge & Magnet — \$18.00",
    )
    val canSubmit = nameOnBadge.isNotBlank() && !isSubmitting

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { Text("Council Badge") },
                    actions = { TextButton(onClick = onDismiss) { Text("Cancel") } },
                )
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                ) {
                    Text("Choose an option", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    options.forEach { (value, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedOption = value }
                                .padding(vertical = 8.dp),
                        ) {
                            RadioButton(selected = selectedOption == value, onClick = { selectedOption = value })
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(label)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    androidx.compose.material3.OutlinedTextField(
                        value = nameOnBadge,
                        onValueChange = { nameOnBadge = it },
                        label = { Text("How name should appear") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    androidx.compose.material3.OutlinedTextField(
                        value = officerRole,
                        onValueChange = { officerRole = it },
                        label = { Text("Officer role (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    errorMessage?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(it, color = Color(0xFFA12626))
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            isSubmitting = true
                            errorMessage = null
                            scope.launch {
                                try {
                                    val url = withContext(Dispatchers.IO) {
                                        com.erdman.kofc6650.data.PayPalSubmitter.submit(
                                            hostedButtonId = "EJHU5WCSMXMXQ",
                                            fields = mapOf(
                                                "on0" to "Select One",
                                                "os0" to selectedOption,
                                                "on1" to "How name should appear",
                                                "os1" to nameOnBadge,
                                                "on2" to "Officer role (optional)",
                                                "os2" to officerRole,
                                                "currency_code" to "USD",
                                            ),
                                        )
                                    }
                                    isSubmitting = false
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                    onDismiss()
                                } catch (e: Exception) {
                                    isSubmitting = false
                                    errorMessage = "Couldn't reach PayPal. Please try again."
                                }
                            }
                        },
                        enabled = canSubmit,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = KofcNavy, contentColor = KofcGold),
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = KofcGold, strokeWidth = 2.dp)
                        } else {
                            Text("Continue to PayPal")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedTheHomelessSignupDialog(initialDate: String, onDismiss: () -> Unit) {
    val repository = remember { KofcRepository() }
    val scope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(true) }
    var loadFailed by remember { mutableStateOf(false) }
    // Up to 4 dates can be open at once now -- selectedIndex tracks which
    // one this dialog is showing, defaulting to whichever open date
    // matches the event card that was tapped (falling back to the
    // soonest one if that date isn't open), with left/right arrows to
    // move between the others when there's more than one.
    var openDates by remember { mutableStateOf<List<SignupStatusDto>>(emptyList()) }
    var selectedIndex by remember { mutableIntStateOf(0) }
    var signedUp by remember { mutableStateOf(false) }

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var whatsapp by remember { mutableStateOf("") }
    var asAlternate by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    suspend fun loadStatus() {
        isLoading = true
        loadFailed = false
        val fetched = try {
            repository.getFeedTheHomelessStatus()
        } catch (e: Exception) {
            loadFailed = true
            null
        }
        if (fetched != null) {
            openDates = fetched
            val matchIndex = fetched.indexOfFirst { it.date == initialDate }
            selectedIndex = if (matchIndex >= 0) matchIndex else 0
        }
        isLoading = false
    }

    LaunchedEffect(Unit) { loadStatus() }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { Text("Feed the Homeless") },
                    actions = { TextButton(onClick = onDismiss) { Text("Close") } },
                )
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                ) {
                    val currentStatus = openDates.getOrNull(selectedIndex)
                    when {
                        isLoading -> {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(color = KofcNavy)
                            }
                        }
                        loadFailed -> {
                            Text("Couldn't load signup status. Try again in a moment.", color = Color(0xFFA12626))
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { scope.launch { loadStatus() } },
                                colors = ButtonDefaults.buttonColors(containerColor = KofcNavy, contentColor = KofcGold),
                            ) {
                                Text("Retry")
                            }
                        }
                        signedUp -> {
                            Text(
                                "You're signed up! See you there.",
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1E6B34),
                            )
                        }
                        currentStatus == null -> {
                            Text(
                                "Signups for the next Feed the Homeless event haven't opened yet. Check back soon, or watch for the announcement email.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        else -> {
                            val full = currentStatus.filledCount >= currentStatus.totalCount
                            if (openDates.size > 1) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    IconButton(
                                        onClick = { selectedIndex-- },
                                        enabled = selectedIndex > 0,
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                            contentDescription = "Earlier date",
                                            tint = if (selectedIndex > 0) KofcNavy else Color(0xFFCCCCCC),
                                        )
                                    }
                                    Text(
                                        "Date ${selectedIndex + 1} of ${openDates.size} open",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = KofcGoldMuted,
                                        modifier = Modifier.weight(1f),
                                        textAlign = TextAlign.Center,
                                    )
                                    IconButton(
                                        onClick = { selectedIndex++ },
                                        enabled = selectedIndex < openDates.size - 1,
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            contentDescription = "Later date",
                                            tint = if (selectedIndex < openDates.size - 1) KofcNavy else Color(0xFFCCCCCC),
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                            Text(formatDate(currentStatus.date), fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            Text(
                                "${currentStatus.filledCount} of ${currentStatus.totalCount} spots filled",
                                fontSize = 13.sp,
                                color = KofcGoldMuted,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                            Text(
                                "⚠️ Please be sure to select the correct date when signing up, as registration is open several months in advance.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .padding(top = 10.dp)
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                    .padding(10.dp),
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            val roster = currentStatus.slots.filter { !it.name.isNullOrBlank() }
                            if (roster.isNotEmpty()) {
                                Text(
                                    "Signed up",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = KofcGoldMuted,
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    roster.forEach { slot ->
                                        Text(
                                            slot.name!! + (if (slot.label == "Alternate") "  ·  Alternate" else ""),
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            if (full) {
                                Text(
                                    "All spots are filled for this date. Thank you to everyone who signed up!",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            } else {
                                val alternateTaken = currentStatus.slots.firstOrNull { it.label == "Alternate" }?.name != null

                                androidx.compose.material3.OutlinedTextField(
                                    value = name,
                                    onValueChange = { name = it },
                                    label = { Text("Your Name") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                androidx.compose.material3.OutlinedTextField(
                                    value = email,
                                    onValueChange = { email = it },
                                    label = { Text("Email") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                androidx.compose.material3.OutlinedTextField(
                                    value = whatsapp,
                                    onValueChange = { whatsapp = it },
                                    label = { Text("WhatsApp (optional)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )

                                if (!alternateTaken) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { asAlternate = !asAlternate }
                                            .padding(top = 10.dp),
                                    ) {
                                        Checkbox(checked = asAlternate, onCheckedChange = { asAlternate = it })
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Sign me up as the Alternate instead", fontSize = 13.sp)
                                    }
                                }

                                errorMessage?.let {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(it, color = Color(0xFFA12626))
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        val trimmedName = name.trim()
                                        val trimmedEmail = email.trim()
                                        if (trimmedName.isBlank()) {
                                            errorMessage = "Enter your name."
                                            return@Button
                                        }
                                        if (!trimmedEmail.contains("@")) {
                                            errorMessage = "Enter a valid email."
                                            return@Button
                                        }
                                        errorMessage = null
                                        isSubmitting = true
                                        val claimDate = currentStatus.date
                                        scope.launch {
                                            repository.claimFeedTheHomelessSlot(
                                                claimDate,
                                                trimmedName,
                                                trimmedEmail,
                                                whatsapp.trim(),
                                                asAlternate,
                                            )
                                            // The write can take a moment to show up on a
                                            // re-fetch, so poll a few times (checking for
                                            // this exact name, not just a count change --
                                            // someone else could be signing up at the same
                                            // moment) before concluding it actually failed.
                                            var matched = false
                                            var latestForDate: SignupStatusDto? = null
                                            var latestAll: List<SignupStatusDto>? = null
                                            for (delayMs in listOf(600L, 1200L, 1800L)) {
                                                delay(delayMs)
                                                latestAll = try {
                                                    repository.getFeedTheHomelessStatus()
                                                } catch (e: Exception) {
                                                    null
                                                }
                                                latestForDate = latestAll?.firstOrNull { it.date == claimDate }
                                                if (latestForDate != null && latestForDate.slots.any { it.name == trimmedName }) {
                                                    matched = true
                                                    break
                                                }
                                                if (latestForDate != null && latestForDate.filledCount >= latestForDate.totalCount) {
                                                    break
                                                }
                                            }
                                            isSubmitting = false
                                            if (matched && latestAll != null) {
                                                openDates = latestAll
                                                signedUp = true
                                            } else if (latestForDate != null && latestForDate.filledCount >= latestForDate.totalCount) {
                                                errorMessage = if (asAlternate) {
                                                    "The alternate spot may already be taken. Refresh to check."
                                                } else {
                                                    "Someone just took the last spot for this date."
                                                }
                                            } else {
                                                errorMessage = "Couldn't confirm your signup. Refresh first — you may already be signed up — before trying again."
                                            }
                                        }
                                    },
                                    enabled = !isSubmitting,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = KofcNavy, contentColor = KofcGold),
                                ) {
                                    if (isSubmitting) {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = KofcGold, strokeWidth = 2.dp)
                                    } else {
                                        Text("Sign Up")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Self-contained (fetches its own data, matching PhotosTab's pattern)
// rather than threaded through KofcApp's shared refreshTrigger bundle --
// council minutes change on their own schedule, unrelated to
// events/photos, so there's no reason to couple its fetch to theirs.
@Composable
private fun MinutesTab(repository: KofcRepository) {
    val context = LocalContext.current
    var files by remember { mutableStateOf<List<DriveFileDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var refreshTrigger by remember { mutableIntStateOf(0) }
    var showPastYears by remember { mutableStateOf(false) }

    LaunchedEffect(refreshTrigger) {
        isLoading = true
        errorMessage = null
        try {
            files = repository.getMinutesFiles()
        } catch (e: Exception) {
            errorMessage = "Could not load minutes. Pull down to try again."
        } finally {
            isLoading = false
        }
    }

    // Files with no parseable date prefix stay in the current list rather
    // than silently vanishing into an archive nobody thinks to check.
    val currentYear = remember { LocalDate.now().year }
    val currentFiles = remember(files) { files.filter { (minutesFileYear(it.name) ?: currentYear) >= currentYear } }
    val pastYearFiles = remember(files) { files.filter { (minutesFileYear(it.name) ?: currentYear) < currentYear } }

    if (isLoading && files.isEmpty() && errorMessage == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = KofcNavy)
        }
    } else {
        RefreshableList(isRefreshing = isLoading, onRefresh = { refreshTrigger++ }) {
            item {
                CompositionLocalProvider(LocalDensity provides Density(density = LocalDensity.current.density, fontScale = 1f)) {
                    Text(
                        text = "Council Minutes",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (errorMessage != null) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF5F5))) {
                        Text(
                            text = errorMessage!!,
                            color = Color(0xFFC0392B),
                            modifier = Modifier.padding(14.dp),
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            if (errorMessage == null && files.isEmpty()) {
                item {
                    Text(
                        text = "No minutes found.",
                        color = Color(0xFF999999),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                    )
                }
            }

            items(currentFiles) { file ->
                MinutesFileCard(
                    file = file,
                    onOpen = {
                        file.webViewLink?.let { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it))) }
                    },
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (pastYearFiles.isNotEmpty()) {
                item {
                    Text(
                        text = (if (showPastYears) "▲ Hide" else "▼ Show") + " Past Years (${pastYearFiles.size})",
                        color = KofcGoldMuted,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showPastYears = !showPastYears }
                            .padding(vertical = 12.dp),
                    )
                }
                if (showPastYears) {
                    items(pastYearFiles) { file ->
                        MinutesFileCard(
                            file = file,
                            onOpen = {
                                file.webViewLink?.let { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it))) }
                            },
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

// Returns null (rather than guessing) for anything that doesn't start with
// a parseable date -- callers treat null as "keep it visible" so an
// unexpected file name never disappears into the Past Years section.
private fun minutesFileYear(name: String): Int? {
    val withoutExtension = name.removeSuffix(".pdf").removeSuffix(".PDF")
    return try {
        LocalDate.parse(withoutExtension.take(10)).year
    } catch (e: Exception) {
        null
    }
}

private data class ParsedMinutesFile(val title: String, val dateLabel: String?)

// Filenames are consistently "yyyy-MM-dd <description>.pdf" (the meeting
// date, not the upload date) -- parsed out for display since it's more
// useful than the raw file name, with the raw name as a graceful fallback
// for anything that doesn't follow the convention.
private fun parseMinutesFileName(name: String): ParsedMinutesFile {
    val withoutExtension = name.removeSuffix(".pdf").removeSuffix(".PDF")
    val datePrefix = withoutExtension.take(10)
    val hasDatePrefix = try {
        LocalDate.parse(datePrefix)
        true
    } catch (e: Exception) {
        false
    }
    return if (hasDatePrefix) {
        val rest = withoutExtension.drop(10).trim().removePrefix("-").trim()
        ParsedMinutesFile(title = rest.ifBlank { withoutExtension }, dateLabel = formatDate(datePrefix))
    } else {
        ParsedMinutesFile(title = withoutExtension, dateLabel = null)
    }
}

@Composable
private fun MinutesFileCard(file: DriveFileDto, onOpen: () -> Unit) {
    val parsed = remember(file.name) { parseMinutesFileName(file.name) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = parsed.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (parsed.dateLabel != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "📅 " + parsed.dateLabel,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = KofcGoldMuted,
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onOpen,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = KofcNavy,
                    contentColor = KofcGold,
                ),
            ) {
                Text("View Minutes →")
            }
        }
    }
}

// Same slim pill look as the other filter/mode toggles in this file, for
// switching the merged Photos tab between browsing and submitting --
// session-only state, not persisted, matching EventFilterToggle's reasoning.
@Composable
private fun PhotosModeToggle(browseMode: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(2.dp),
    ) {
        listOf(true to "Browse", false to "Submit").forEach { (value, label) ->
            val selected = browseMode == value
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selected) MaterialTheme.colorScheme.surface else Color.Transparent)
                    .clickable { onChange(value) }
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// Merges what used to be the separate Submit Photos and Recent Photos tabs
// behind a toggle -- both underlying composables are untouched, this just
// picks which one to show.
@Composable
private fun PhotosTab(
    repository: KofcRepository,
    pinManager: PinManager,
    photos: List<RecentPhotoDto>,
    photosError: String?,
    isLoadingPhotos: Boolean,
    onRefreshPhotos: () -> Unit,
    browseMode: Boolean,
    onBrowseModeChange: (Boolean) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            PhotosModeToggle(browseMode, onBrowseModeChange)
        }
        Box(modifier = Modifier.weight(1f)) {
            if (browseMode) {
                RecentPhotosTab(
                    repository = repository,
                    photos = photos,
                    errorMessage = photosError,
                    isRefreshing = isLoadingPhotos,
                    onRefresh = onRefreshPhotos,
                )
            } else {
                SubmitPhotosTab(repository = repository, pinManager = pinManager)
            }
        }
    }
}

@Composable
private fun SubmitPhotosTab(repository: KofcRepository, pinManager: PinManager) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var submitterName by remember { mutableStateOf("") }
    var caption by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var statusIsError by remember { mutableStateOf(false) }

    val pickPhotosLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 20),
    ) { uris -> selectedUris = uris }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
    ) {
        item {
            CompositionLocalProvider(LocalDensity provides Density(density = LocalDensity.current.density, fontScale = 1f)) {
                Text(
                    text = "Share Your Event Photos",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Council Photo Submissions",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "📷 Submit photos from council events, activities, and volunteer work",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = KofcGoldMuted,
                    )
                    Text(
                        text = "Choose photos from your gallery and submit — no Google account needed.",
                        fontSize = 14.sp,
                        color = Color(0xFF555555),
                        modifier = Modifier.padding(top = 8.dp),
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    androidx.compose.material3.OutlinedTextField(
                        value = submitterName,
                        onValueChange = { submitterName = it },
                        label = { Text("Your name (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    androidx.compose.material3.OutlinedTextField(
                        value = caption,
                        onValueChange = { caption = it },
                        label = { Text("Caption (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            pickPhotosLauncher.launch(
                                androidx.activity.result.PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly,
                                ),
                            )
                        },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = KofcNavy,
                            contentColor = KofcGold,
                        ),
                    ) {
                        Text(
                            if (selectedUris.isEmpty()) {
                                "Choose Photos from Gallery"
                            } else {
                                "${selectedUris.size} photo${if (selectedUris.size == 1) "" else "s"} selected — change"
                            },
                        )
                    }

                    if (selectedUris.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            selectedUris.forEach { uri ->
                                AsyncImage(
                                    model = uri,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(6.dp)),
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            when {
                                selectedUris.isEmpty() -> {
                                    statusIsError = true
                                    statusMessage = "Choose at least one photo"
                                }
                                else -> {
                                    isSubmitting = true
                                    statusMessage = null
                                    scope.launch {
                                        try {
                                            val files = selectedUris.mapIndexedNotNull { index, uri ->
                                                val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                                                val bytes = context.contentResolver.openInputStream(uri)
                                                    ?.use { it.readBytes() }
                                                bytes?.let {
                                                    PhotoUploadFile(
                                                        bytes = it,
                                                        filename = "photo_$index.${mimeType.substringAfter('/').ifBlank { "jpg" }}",
                                                        mimeType = mimeType,
                                                    )
                                                }
                                            }
                                            val result = repository.uploadPhotos(
                                                pinManager.savedPin,
                                                submitterName,
                                                caption,
                                                files,
                                            )
                                            statusIsError = false
                                            statusMessage =
                                                "Uploaded ${result.saved} photo${if (result.saved == 1) "" else "s"}. Thank you!"
                                            selectedUris = emptyList()
                                            submitterName = ""
                                            caption = ""
                                        } catch (e: Exception) {
                                            statusIsError = true
                                            statusMessage = e.message ?: "Something went wrong"
                                        } finally {
                                            isSubmitting = false
                                        }
                                    }
                                }
                            }
                        },
                        enabled = !isSubmitting,
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = KofcNavy,
                            contentColor = KofcGold,
                        ),
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = KofcGold,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text("Submit Photos")
                        }
                    }

                    statusMessage?.let { msg ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = msg,
                            fontSize = 13.sp,
                            color = if (statusIsError) Color(0xFFA12626) else Color(0xFF1E6B34),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentPhotosTab(
    repository: KofcRepository,
    photos: List<RecentPhotoDto>,
    errorMessage: String?,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
) {
    var enlargedPhotoUrl by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Saving via MediaStore.Images requires this permission on API < 29
    // (Q introduced scoped storage, where it's not needed); the pending
    // save runs once the user grants it.
    var pendingSaveUrl by remember { mutableStateOf<String?>(null) }
    val storagePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val url = pendingSaveUrl
        pendingSaveUrl = null
        if (granted && url != null) {
            scope.launch { savePhotoToGallery(context, url) }
        } else if (!granted) {
            Toast.makeText(context, "Storage permission is needed to save photos.", Toast.LENGTH_SHORT).show()
        }
    }

    // Archive browsing: null selectedMonth means "current month" (the
    // photos/isError passed in from KofcApp's own refresh cycle). Picking a
    // past month fetches and displays that month's photos instead, without
    // touching the current-month state owned by the caller.
    var showMonthPicker by remember { mutableStateOf(false) }
    var archiveMonths by remember { mutableStateOf<List<ArchiveMonthDto>>(emptyList()) }
    var isLoadingArchiveMonths by remember { mutableStateOf(false) }
    var selectedMonth by remember { mutableStateOf<ArchiveMonthDto?>(null) }
    var archivePhotos by remember { mutableStateOf<List<RecentPhotoDto>>(emptyList()) }
    var isLoadingArchivePhotos by remember { mutableStateOf(false) }
    var archiveError by remember { mutableStateOf(false) }

    LaunchedEffect(showMonthPicker) {
        if (showMonthPicker && archiveMonths.isEmpty()) {
            isLoadingArchiveMonths = true
            try {
                archiveMonths = repository.getPhotoArchiveMonths()
            } catch (e: Exception) {
                // Leave archiveMonths empty; the picker shows "No archived photos yet."
            } finally {
                isLoadingArchiveMonths = false
            }
        }
    }

    LaunchedEffect(selectedMonth) {
        val month = selectedMonth ?: return@LaunchedEffect
        isLoadingArchivePhotos = true
        archiveError = false
        try {
            archivePhotos = repository.getArchivedPhotos(month.month)
        } catch (e: Exception) {
            archiveError = true
        } finally {
            isLoadingArchivePhotos = false
        }
    }

    val viewingArchive = selectedMonth != null
    val displayedPhotos = if (viewingArchive) archivePhotos else photos
    val displayedErrorMessage = if (viewingArchive) {
        if (archiveError) "Could not load photos for this month." else null
    } else {
        errorMessage
    }
    val displayedIsLoading = viewingArchive && isLoadingArchivePhotos
    val currentMonthTitle = remember {
        val today = LocalDate.now()
        "${today.month.getDisplayName(TextStyle.FULL, Locale.US)} ${today.year}"
    }

    RefreshableList(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 32.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column {
                    Text(
                        text = "FAITH IN ACTION",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = KofcGold,
                    )
                    Text(
                        text = selectedMonth?.label ?: currentMonthTitle,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
                TextButton(onClick = {
                    if (viewingArchive) {
                        selectedMonth = null
                    } else {
                        showMonthPicker = true
                    }
                }) {
                    Text(if (viewingArchive) "Back to Recent" else "Browse Past Months")
                }
            }
            Text(
                text = "Tap a photo to save or share it",
                fontSize = 13.sp,
                color = Color(0xFF999999),
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (displayedIsLoading) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = KofcNavy)
                }
            }
        }

        if (displayedErrorMessage != null) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF5F5))) {
                    Text(
                        text = displayedErrorMessage,
                        color = Color(0xFFC0392B),
                        modifier = Modifier.padding(14.dp),
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        if (com.erdman.kofc6650.data.ScreenshotMode.isActive(context) && !viewingArchive) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().height(340.dp),
                    colors = CardDefaults.cardColors(containerColor = KofcNavy),
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "📷", fontSize = 40.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Sample Event Photo",
                                color = KofcGold,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                            )
                        }
                    }
                }
            }
        } else {
            if (!displayedIsLoading && displayedErrorMessage == null && displayedPhotos.isEmpty()) {
                item {
                    Text(
                        text = if (viewingArchive) "No photos for this month." else "No photos yet.",
                        color = Color(0xFF999999),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                    )
                }
            }

            items(displayedPhotos) { photo ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column {
                        AsyncImage(
                            model = photo.thumbnailUrl,
                            contentDescription = null,
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { enlargedPhotoUrl = photo.mediumUrl },
                        )
                        if (!photo.caption.isNullOrEmpty()) {
                            Text(
                                text = photo.caption,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 8.dp),
                            )
                        }
                        if (!photo.submittedBy.isNullOrEmpty()) {
                            Text(
                                text = "Submitted by ${photo.submittedBy}",
                                fontSize = 11.sp,
                                color = Color(0xFF999999),
                                modifier = Modifier.padding(
                                    start = 8.dp,
                                    end = 8.dp,
                                    top = if (photo.caption.isNullOrEmpty()) 8.dp else 2.dp,
                                    bottom = 8.dp,
                                ),
                            )
                        } else if (!photo.caption.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    if (showMonthPicker) {
        AlertDialog(
            onDismissRequest = { showMonthPicker = false },
            title = { Text("Browse Past Months") },
            text = {
                when {
                    isLoadingArchiveMonths -> Box(modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = KofcNavy)
                    }
                    archiveMonths.isEmpty() -> Text("No archived photos yet.", color = Color(0xFF999999))
                    else -> Column {
                        archiveMonths.forEach { month ->
                            TextButton(
                                onClick = {
                                    selectedMonth = month
                                    showMonthPicker = false
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("${month.label} (${month.count})", modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMonthPicker = false }) { Text("Close") }
            },
        )
    }

    val enlargedUrl = enlargedPhotoUrl
    if (enlargedUrl != null) {
        Dialog(
            onDismissRequest = { enlargedPhotoUrl = null },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            var scale by remember(enlargedUrl) { mutableStateOf(1f) }
            var offset by remember(enlargedUrl) { mutableStateOf(Offset.Zero) }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.9f)),
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = enlargedUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .pointerInput(enlargedUrl) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 5f)
                                offset = if (scale <= 1f) Offset.Zero else offset + pan
                            }
                        }
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y,
                        ),
                )

                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                ) {
                    IconButton(onClick = { sharePhoto(context, scope, enlargedUrl) }) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                    }
                    IconButton(
                        onClick = {
                            val needsPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
                            if (needsPermission) {
                                pendingSaveUrl = enlargedUrl
                                storagePermissionLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            } else {
                                scope.launch { savePhotoToGallery(context, enlargedUrl) }
                            }
                        },
                    ) {
                        Icon(painterResource(R.drawable.ic_save), contentDescription = "Save to gallery", tint = Color.White)
                    }
                    IconButton(onClick = { enlargedPhotoUrl = null }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }
            }
        }
    }
}

private suspend fun loadBitmap(context: android.content.Context, url: String): Bitmap? {
    val loader = ImageLoader(context)
    val request = ImageRequest.Builder(context).data(url).allowHardware(false).build()
    val result = (loader.execute(request).drawable as? BitmapDrawable)?.bitmap
    return result
}

private fun sharePhoto(context: android.content.Context, scope: kotlinx.coroutines.CoroutineScope, url: String) {
    scope.launch {
        val bitmap = loadBitmap(context, url) ?: run {
            Toast.makeText(context, "Couldn't load this photo.", Toast.LENGTH_SHORT).show()
            return@launch
        }
        val uri = withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, "shared_images").apply { mkdirs() }
            val file = File(dir, "photo_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out) }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Photo"))
    }
}

private suspend fun savePhotoToGallery(context: android.content.Context, url: String) {
    val bitmap = loadBitmap(context, url) ?: run {
        Toast.makeText(context, "Couldn't load this photo.", Toast.LENGTH_SHORT).show()
        return
    }
    val saved = withContext(Dispatchers.IO) {
        try {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "koc6650_${System.currentTimeMillis()}.jpg")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/KofC6650")
                }
            }
            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: return@withContext false
            context.contentResolver.openOutputStream(uri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }
            true
        } catch (e: Exception) {
            false
        }
    }
    Toast.makeText(
        context,
        if (saved) "Photo saved to gallery." else "Couldn't save this photo.",
        Toast.LENGTH_SHORT,
    ).show()
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun EventCard(
    event: EventDto,
    onSignUpClick: (String) -> Unit,
    feedTheHomelessOpenDates: List<SignupStatusDto>? = null,
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    var showAddToCalendarSheet by remember { mutableStateOf(false) }
    var showFeedTheHomelessSheet by remember { mutableStateOf(false) }
    var isGoing by remember(event.id) { mutableStateOf(RsvpStore.isGoing(context, event.id)) }
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

    // Up to 4 dates can be open at once now (Kris opens several months
    // ahead so slower volunteers still get a shot at a future date), so
    // each card looks up its own date in the list rather than assuming
    // there's only one open date to compare against; a still-loading/
    // failed fetch (null) is left ambiguous rather than guessed at.
    val matchingOpenDate = feedTheHomelessOpenDates?.firstOrNull { it.date == event.date }
    val isFeedTheHomelessOpenForThisDate = matchingOpenDate != null
    val isFeedTheHomelessFull = matchingOpenDate != null &&
        matchingOpenDate.filledCount >= matchingOpenDate.totalCount

    if (showAddToCalendarSheet) {
        AddToCalendarTimeDialog(
            event = event,
            onAdd = { hour, minute ->
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                showAddToCalendarSheet = false
                addEventToCalendar(context, event, hour, minute)
            },
            onCancel = { showAddToCalendarSheet = false },
        )
    }

    if (showFeedTheHomelessSheet) {
        FeedTheHomelessSignupDialog(initialDate = event.date, onDismiss = { showFeedTheHomelessSheet = false })
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = event.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(end = 28.dp),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "📅 " + formatDate(event.date) + (event.time?.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = KofcGoldMuted,
            )
            if (!event.location.isNullOrBlank()) {
                Text(
                    text = "📍 " + event.location,
                    fontSize = 13.sp,
                    color = Color(0xFF666666),
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .clickable { openLocationInMaps(context, event.location) },
                )
            }
            if (!event.description.isNullOrBlank()) {
                Text(
                    text = event.description,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            if (event.title == "Feed the Homeless") {
                Text(
                    text = "Event Details →",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = KofcGoldMuted,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .clickable {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(FEED_THE_HOMELESS_DETAILS_URL)))
                        },
                )
                if (isFeedTheHomelessOpenForThisDate) {
                    val filled = matchingOpenDate!!.filledCount
                    val total = matchingOpenDate.totalCount
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 8.dp),
                    ) {
                        Box(modifier = Modifier.size(8.dp).background(Color(0xFF3A7D5C), CircleShape))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Open · $filled of $total spots filled",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF3A7D5C),
                        )
                    }
                    Spacer(modifier = Modifier.height(5.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(if (total > 0) filled.toFloat() / total else 0f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(0xFF3A7D5C)),
                        )
                    }
                } else if (feedTheHomelessOpenDates != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 8.dp),
                    ) {
                        Box(modifier = Modifier.size(8.dp).background(Color(0xFF999999), CircleShape))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Not open yet",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF999999),
                        )
                    }
                }
            }
            FlowRow(
                modifier = Modifier.padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (event.title == "Feed the Homeless") {
                    // Not-open-yet is a dead end (the dialog would just repeat
                    // what the card already says), so that one's disabled. Full
                    // stays enabled -- with the roster now shown in the dialog,
                    // tapping through is still useful even once signups are
                    // closed off, just to see who's going. Before the status
                    // fetch resolves at all (null), the button used to default
                    // to the enabled "Sign Up" look and then flip to disabled a
                    // second later once the fetch landed -- a visible flicker.
                    // Treating "still unknown" the same as "not open" avoids
                    // ever showing a button that looks tappable-and-open before
                    // that's actually confirmed.
                    val notReady = feedTheHomelessOpenDates == null
                    val notOpenYet = !notReady && !isFeedTheHomelessOpenForThisDate
                    Button(
                        onClick = { showFeedTheHomelessSheet = true },
                        enabled = !notReady && !notOpenYet,
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = KofcNavy,
                            contentColor = KofcGold,
                        ),
                    ) {
                        Text(
                            when {
                                notReady -> "Checking Status…"
                                notOpenYet -> "Not Open Yet"
                                isFeedTheHomelessFull -> "View Signups"
                                else -> "Sign Up to Volunteer →"
                            },
                        )
                    }
                } else if (!event.signupUrl.isNullOrBlank()) {
                    Button(
                        onClick = { onSignUpClick(event.signupUrl) },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = KofcNavy,
                            contentColor = KofcGold,
                        ),
                    ) {
                        Text("Sign Up to Volunteer →")
                    }
                } else if (!event.linkUrl.isNullOrBlank()) {
                    Button(
                        onClick = { onSignUpClick(event.linkUrl) },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = KofcNavy,
                            contentColor = KofcGold,
                        ),
                    ) {
                        Text("Open Link →")
                    }
                }
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
            }
        }

        // A corner marker, not another action button -- keeps it from
        // reading as a second "sign up" CTA next to the real one.
        IconButton(
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                isGoing = !isGoing
                RsvpStore.toggle(context, event.id)
            },
            modifier = Modifier.align(Alignment.TopEnd),
        ) {
            if (isGoing) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = "Marked as signed up",
                    tint = KofcGold,
                )
            } else {
                Icon(
                    painterResource(R.drawable.ic_star_outline),
                    contentDescription = "Mark as signed up",
                    tint = Color(0xFF999999),
                )
            }
        }
        }
    }
}

private fun reportProblem(context: android.content.Context) {
    val body = """


        ---
        Version ${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})
        Android ${Build.VERSION.RELEASE}
        ${Build.MANUFACTURER} ${Build.MODEL}
    """.trimIndent()
    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:bird.dog@erdius.net")).apply {
        putExtra(Intent.EXTRA_SUBJECT, "KofC 6650 App - Problem Report")
        putExtra(Intent.EXTRA_TEXT, body)
    }
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        // No email app available to handle the intent; nothing to do.
    }
}

private fun openLocationInMaps(context: android.content.Context, location: String) {
    val uri = Uri.parse("geo:0,0?q=" + Uri.encode(location))
    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
}

private fun shareEvent(context: android.content.Context, event: EventDto) {
    val lines = mutableListOf(
        event.title,
        "📅 " + formatDate(event.date) + (event.time?.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""),
    )
    if (!event.location.isNullOrBlank()) {
        lines.add("📍 " + event.location)
    }
    (event.signupUrl ?: event.linkUrl)?.let { lines.add(it) }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, lines.joinToString("\n"))
    }
    context.startActivity(Intent.createChooser(intent, "Share Event"))
}

// Events default to a 1-hour block -- long enough to be useful on the
// calendar without implying a false precision the source data doesn't
// actually have.
private fun addEventToCalendar(context: android.content.Context, event: EventDto, hour: Int, minute: Int) {
    val date = try {
        LocalDate.parse(event.date)
    } catch (e: Exception) {
        return
    }

    val start = LocalDateTime.of(date, LocalTime.of(hour, minute))
    val startMillis = start.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    val intent = Intent(Intent.ACTION_INSERT).setData(CalendarContract.Events.CONTENT_URI)
        .putExtra(CalendarContract.Events.TITLE, event.title)
        .putExtra(CalendarContract.Events.EVENT_LOCATION, event.location ?: "")
        .putExtra(CalendarContract.Events.DESCRIPTION, event.description ?: "")
        .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
        .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, startMillis + 60 * 60 * 1000)

    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        // No calendar app available to handle the intent; nothing to do.
    }
}

// SignUpGenius events often offer several time slots (setup, serving,
// cleanup) that the source calendar data has no way to represent -- it
// only ever carries one time. Rather than guess, this asks which slot the
// user actually signed up for before launching the calendar insert intent.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddToCalendarTimeDialog(
    event: EventDto,
    onAdd: (hour: Int, minute: Int) -> Unit,
    onCancel: () -> Unit,
) {
    val defaultTime = remember(event.id) {
        event.time?.takeIf { it.isNotBlank() }?.let {
            try {
                LocalTime.parse(it, DateTimeFormatter.ofPattern("h:mm a", Locale.US))
            } catch (e: Exception) {
                null
            }
        } ?: LocalTime.of(9, 0)
    }
    val timePickerState = rememberTimePickerState(
        initialHour = defaultTime.hour,
        initialMinute = defaultTime.minute,
        is24Hour = false,
    )

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Add to My Calendar") },
        text = {
            Column {
                Text(
                    "If you signed up for a specific time slot, set it here so it's added to your calendar correctly.",
                    fontSize = 13.sp,
                    color = Color(0xFF666666),
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                TimePicker(state = timePickerState)
            }
        },
        confirmButton = {
            TextButton(onClick = { onAdd(timePickerState.hour, timePickerState.minute) }) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
        },
    )
}

