package com.erdman.kofc6650

import android.content.ContentValues
import android.content.Intent
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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.Density
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
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
import androidx.core.content.FileProvider
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.erdman.kofc6650.data.ArchiveMonthDto
import com.erdman.kofc6650.data.EventDto
import com.erdman.kofc6650.data.FontScalePreference
import com.erdman.kofc6650.data.KofcRepository
import com.erdman.kofc6650.data.LeadershipContact
import com.erdman.kofc6650.data.LeadershipDirectory
import com.erdman.kofc6650.data.OfflineCache
import com.erdman.kofc6650.data.PhotoUploadFile
import com.erdman.kofc6650.data.PinManager
import com.erdman.kofc6650.data.RecentPhotoDto
import com.erdman.kofc6650.data.RsvpStore
import com.erdman.kofc6650.data.WhatsNew
import com.erdman.kofc6650.ui.theme.KofC6650Theme
import com.erdman.kofc6650.ui.theme.KofcGold
import com.erdman.kofc6650.ui.theme.KofcGoldMuted
import com.erdman.kofc6650.ui.theme.KofcNavy
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    // Home screen shortcuts route here via an Intent extra rather than a
    // deep link, since the tabs aren't separate screens/destinations --
    // just an index into the same KofcApp's TabRow. Held at the Activity
    // level (not inside KofcApp's state) so onNewIntent can update it too.
    private val pendingTargetTab = mutableStateOf<Int?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingTargetTab.value = intent?.getStringExtra("target_tab")?.toIntOrNull()
        setContent {
            KofC6650Theme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    KofcApp(pendingTargetTab = pendingTargetTab)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingTargetTab.value = intent.getStringExtra("target_tab")?.toIntOrNull()
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
fun KofcApp(pendingTargetTab: MutableState<Int?> = remember { mutableStateOf(null) }) {
    val repository = remember { KofcRepository() }
    val context = LocalContext.current
    val pinManager = remember { PinManager(context) }
    val fontScalePref = remember { FontScalePreference(context) }
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
    var showWhatsNew by remember { mutableStateOf(WhatsNew.shouldShow(context)) }
    var tabIndex by remember { mutableIntStateOf(pendingTargetTab.value ?: 0) }
    LaunchedEffect(pendingTargetTab.value) {
        pendingTargetTab.value?.let {
            tabIndex = it
            pendingTargetTab.value = null
        }
    }
    var events by remember { mutableStateOf<List<EventDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var eventsError by remember { mutableStateOf<String?>(null) }
    var allEvents by remember { mutableStateOf<List<EventDto>>(emptyList()) }
    var isLoadingAllEvents by remember { mutableStateOf(true) }
    var allEventsError by remember { mutableStateOf<String?>(null) }
    var photos by remember { mutableStateOf<List<RecentPhotoDto>>(emptyList()) }
    var isLoadingPhotos by remember { mutableStateOf(true) }
    var photosError by remember { mutableStateOf<String?>(null) }
    var hasNewPhotos by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableIntStateOf(0) }
    val offlineCache = remember { OfflineCache(context) }

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
        isLoading = true
        eventsError = null
        isLoadingAllEvents = true
        allEventsError = null
        isLoadingPhotos = true
        photosError = null
        coroutineScope {
            launch {
                try {
                    val bundle = repository.getCouncilEvents()
                    events = bundle.signupEvents
                    allEvents = bundle.allEvents
                    offlineCache.saveEvents(bundle.signupEvents, bundle.allEvents)
                } catch (e: Exception) {
                    val cached = offlineCache.loadEvents()
                    if (cached != null) {
                        events = cached.signupEvents
                        allEvents = cached.allEvents
                        val message = "Showing saved events from ${offlineCache.relativeEventsSavedAt()} — couldn't reach the server."
                        eventsError = message
                        allEventsError = message
                    } else {
                        eventsError = "Could not load calendar events."
                        allEventsError = "Could not load calendar events."
                    }
                } finally {
                    isLoading = false
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(KofcGold, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(text = "K", color = KofcNavy, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Knights of Columbus",
                                    color = KofcGold,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = "Council 6650 — Cary & Apex, NC",
                                    color = Color(0xFFAABBCC),
                                    fontSize = 11.sp,
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { showDirectorsOfficers = true }) {
                            Icon(Icons.Default.Email, contentDescription = "Directors & Officers", tint = KofcGold)
                        }
                        IconButton(onClick = { showAbout = true }) {
                            Icon(Icons.Default.Info, contentDescription = "About", tint = KofcGold)
                        }
                        IconButton(onClick = { refreshTrigger++ }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = KofcGold)
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
                        text = { Text("Volunteer Sign Ups") },
                    )
                    Tab(
                        selected = tabIndex == 1,
                        onClick = { tabIndex = 1 },
                        text = { Text("Calendar") },
                    )
                    Tab(
                        selected = tabIndex == 2,
                        onClick = { tabIndex = 2 },
                        text = { Text("Submit Photos") },
                    )
                    Tab(
                        selected = tabIndex == 3,
                        onClick = {
                            tabIndex = 3
                            offlineCache.markPhotosSeen(photos)
                            hasNewPhotos = false
                        },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Recent Photos")
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
                }
            }
        },
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // Only block the whole screen on the very first load of each
            // tab's data -- once there's something to show, a refresh
            // (pull-to-refresh or tab resume) should keep the list visible
            // with its own pull indicator instead of blanking the screen.
            if (isLoading && events.isEmpty() && tabIndex == 0) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = KofcNavy)
                }
            } else if (tabIndex == 0) {
                CalendarTab(
                    events = events,
                    errorMessage = eventsError,
                    isRefreshing = isLoading,
                    onRefresh = { refreshTrigger++ },
                    onSignUpClick = { url ->
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    },
                )
            } else if (tabIndex == 1 && isLoadingAllEvents && allEvents.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = KofcNavy)
                }
            } else if (tabIndex == 1) {
                CalendarAgendaTab(
                    events = allEvents,
                    errorMessage = allEventsError,
                    isRefreshing = isLoadingAllEvents,
                    onRefresh = { refreshTrigger++ },
                    onSignUpClick = { url ->
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    },
                )
            } else if (tabIndex == 2) {
                PhotosTab(repository = repository, pinManager = pinManager)
            } else if (isLoadingPhotos && photos.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = KofcNavy)
                }
            } else {
                RecentPhotosTab(
                    repository = repository,
                    photos = photos,
                    errorMessage = photosError,
                    isRefreshing = isLoadingPhotos,
                    onRefresh = { refreshTrigger++ },
                )
            }
        }
    }

    if (showAbout) {
        AboutDialog(pinManager = pinManager, fontScalePref = fontScalePref, onDismiss = { showAbout = false })
    }

    if (showDirectorsOfficers) {
        DirectorsOfficersDialog(onDismiss = { showDirectorsOfficers = false })
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
private fun AboutDialog(pinManager: PinManager, fontScalePref: FontScalePreference, onDismiss: () -> Unit) {
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
                    leadershipSection(title = "App", contacts = LeadershipDirectory.developer) { email ->
                        context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email")))
                    }
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
            append(" for a free one-year membership. We're Council 6650!")
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
    content: LazyListScope.() -> Unit,
) {
    val pullRefreshState = rememberPullRefreshState(refreshing = isRefreshing, onRefresh = onRefresh)
    Box(modifier = Modifier.fillMaxSize().pullRefresh(pullRefreshState)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
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
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Filled.Star,
            contentDescription = null,
            tint = KofcGoldMuted,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "Tap the star to track events you've signed up for",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = KofcGoldMuted,
        )
    }
}

@Composable
private fun CalendarTab(
    events: List<EventDto>,
    errorMessage: String?,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onSignUpClick: (String) -> Unit,
) {
    val today = LocalDate.now()
    val upcoming = events.filter { event ->
        val date = try {
            LocalDate.parse(event.date)
        } catch (e: Exception) {
            null
        }
        date != null && !date.isBefore(today)
    }

    RefreshableList(isRefreshing = isRefreshing, onRefresh = onRefresh) {
        item {
            Text(
                text = "Sign ups for Upcoming Volunteer Opportunities",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(8.dp))
            RsvpLegend()
            Spacer(modifier = Modifier.height(16.dp))
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

        if (errorMessage == null && upcoming.isEmpty()) {
            item {
                Text(
                    text = "No upcoming events on the calendar.",
                    color = Color(0xFF999999),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                )
            }
        }

        eventSections(upcoming, onSignUpClick)
    }
}

@Composable
private fun CalendarAgendaTab(
    events: List<EventDto>,
    errorMessage: String?,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onSignUpClick: (String) -> Unit,
) {
    val today = LocalDate.now()
    val upcoming = events
        .filter { event ->
            val date = try {
                LocalDate.parse(event.date)
            } catch (e: Exception) {
                null
            }
            date != null && !date.isBefore(today)
        }
        .sortedBy { it.date }

    RefreshableList(isRefreshing = isRefreshing, onRefresh = onRefresh) {
        item {
            Text(
                text = "Upcoming Events",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(8.dp))
            RsvpLegend()
            Spacer(modifier = Modifier.height(16.dp))
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

        if (errorMessage == null && upcoming.isEmpty()) {
            item {
                Text(
                    text = "No upcoming events on the calendar.",
                    color = Color(0xFF999999),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                )
            }
        }

        eventSections(upcoming, onSignUpClick)
    }
}

// Groups events into Today / This Week / Later sections so a longer list
// is easier to scan at a glance instead of one flat chronological list.
private fun dateBucket(dateStr: String): String {
    val date = try { LocalDate.parse(dateStr) } catch (e: Exception) { return "Later" }
    val days = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), date)
    return when {
        days == 0L -> "Today"
        days in 1..6 -> "This Week"
        else -> "Later"
    }
}

private fun LazyListScope.eventSections(events: List<EventDto>, onSignUpClick: (String) -> Unit) {
    for (bucketName in listOf("Today", "This Week", "Later")) {
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
                EventCard(event = event, onSignUpClick = onSignUpClick)
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun PhotosTab(repository: KofcRepository, pinManager: PinManager) {
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
            Text(
                text = "Share Your Event Photos",
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
                AsyncImage(
                    model = photo.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { enlargedPhotoUrl = photo.mediumUrl },
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
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
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    var showAddToCalendarSheet by remember { mutableStateOf(false) }
    var isGoing by remember(event.id) { mutableStateOf(RsvpStore.isGoing(context, event.id)) }

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
            // Feed the Homeless reuses one signup link across every
            // occurrence, so it always opens whatever sheet is currently
            // live rather than the date shown here.
            if (event.title == "Feed the Homeless") {
                Text(
                    text = "⚠️ This event uses a recurring signup link that doesn't change. Please make sure the signup sheet matches this date before signing up.",
                    fontSize = 12.sp,
                    color = Color(0xFF666666),
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            FlowRow(
                modifier = Modifier.padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (!event.signupUrl.isNullOrBlank()) {
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

