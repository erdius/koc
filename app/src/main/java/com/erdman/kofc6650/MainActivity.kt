package com.erdman.kofc6650

import android.content.Intent
import android.net.Uri
import android.os.Bundle
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.Density
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
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
import coil.compose.AsyncImage
import com.erdman.kofc6650.data.ArchiveMonthDto
import com.erdman.kofc6650.data.EventDto
import com.erdman.kofc6650.data.FontScalePreference
import com.erdman.kofc6650.data.KofcRepository
import com.erdman.kofc6650.data.LeadershipContact
import com.erdman.kofc6650.data.LeadershipDirectory
import com.erdman.kofc6650.data.PhotoUploadFile
import com.erdman.kofc6650.data.PinManager
import com.erdman.kofc6650.data.RecentPhotoDto
import com.erdman.kofc6650.ui.theme.KofC6650Theme
import com.erdman.kofc6650.ui.theme.KofcGold
import com.erdman.kofc6650.ui.theme.KofcGoldMuted
import com.erdman.kofc6650.ui.theme.KofcNavy
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

private const val SIGNUP_GENIUS_URL = "https://www.signupgenius.com/"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KofC6650Theme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    KofcApp()
                }
            }
        }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KofcApp() {
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
    var tabIndex by remember { mutableIntStateOf(0) }
    var events by remember { mutableStateOf<List<EventDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var eventsError by remember { mutableStateOf(false) }
    var allEvents by remember { mutableStateOf<List<EventDto>>(emptyList()) }
    var isLoadingAllEvents by remember { mutableStateOf(true) }
    var allEventsError by remember { mutableStateOf(false) }
    var photos by remember { mutableStateOf<List<RecentPhotoDto>>(emptyList()) }
    var isLoadingPhotos by remember { mutableStateOf(true) }
    var photosError by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableIntStateOf(0) }

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
        eventsError = false
        isLoadingAllEvents = true
        allEventsError = false
        isLoadingPhotos = true
        photosError = false
        coroutineScope {
            launch {
                try {
                    val bundle = repository.getCouncilEvents()
                    events = bundle.signupEvents
                    allEvents = bundle.allEvents
                } catch (e: Exception) {
                    eventsError = true
                    allEventsError = true
                } finally {
                    isLoading = false
                    isLoadingAllEvents = false
                }
            }
            launch {
                try {
                    photos = repository.getRecentPhotos()
                } catch (e: Exception) {
                    photosError = true
                } finally {
                    isLoadingPhotos = false
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
                        onClick = { tabIndex = 3 },
                        text = { Text("Recent Photos") },
                    )
                }
            }
        },
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (isLoading && tabIndex == 0) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = KofcNavy)
                }
            } else if (tabIndex == 0) {
                CalendarTab(
                    events = events,
                    isError = eventsError,
                    onSignUpClick = { url ->
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    },
                )
            } else if (tabIndex == 1 && isLoadingAllEvents) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = KofcNavy)
                }
            } else if (tabIndex == 1) {
                CalendarAgendaTab(
                    events = allEvents,
                    isError = allEventsError,
                    onSignUpClick = { url ->
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    },
                )
            } else if (tabIndex == 2) {
                PhotosTab(repository = repository, pinManager = pinManager)
            } else if (isLoadingPhotos) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = KofcNavy)
                }
            } else {
                RecentPhotosTab(repository = repository, photos = photos, isError = photosError)
            }
        }
    }

    if (showAbout) {
        AboutDialog(pinManager = pinManager, fontScalePref = fontScalePref, onDismiss = { showAbout = false })
    }

    if (showDirectorsOfficers) {
        DirectorsOfficersDialog(onDismiss = { showDirectorsOfficers = false })
    }

    } // CompositionLocalProvider(LocalDensity)
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

@Composable
private fun CreateSignUpLink(onClick: () -> Unit) {
    val annotatedText = buildAnnotatedString {
        pushStringAnnotation(tag = "URL", annotation = SIGNUP_GENIUS_URL)
        withStyle(
            style = SpanStyle(
                color = KofcGold,
                textDecoration = TextDecoration.Underline,
                fontWeight = FontWeight.SemiBold,
            ),
        ) {
            append("Click here")
        }
        pop()
        append(" to create a new sign-up")
    }
    ClickableText(
        text = annotatedText,
        style = ComposeTextStyle(
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground,
        ),
        onClick = { offset ->
            annotatedText.getStringAnnotations(tag = "URL", start = offset, end = offset)
                .firstOrNull()
                ?.let { onClick() }
        },
    )
}

@Composable
private fun CalendarTab(
    events: List<EventDto>,
    isError: Boolean,
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

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
    ) {
        item {
            Text(
                text = "Sign ups for Upcoming Volunteer Opportunities",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(8.dp))
            CreateSignUpLink(onClick = { onSignUpClick(SIGNUP_GENIUS_URL) })
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (isError) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF5F5))) {
                    Text(
                        text = "Could not load calendar events.",
                        color = Color(0xFFC0392B),
                        modifier = Modifier.padding(14.dp),
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        if (!isError && upcoming.isEmpty()) {
            item {
                Text(
                    text = "No upcoming events on the calendar.",
                    color = Color(0xFF999999),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                )
            }
        }

        items(upcoming) { event ->
            EventCard(event = event, onSignUpClick = onSignUpClick)
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun CalendarAgendaTab(
    events: List<EventDto>,
    isError: Boolean,
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

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
    ) {
        item {
            Text(
                text = "Upcoming Events",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (isError) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF5F5))) {
                    Text(
                        text = "Could not load calendar events.",
                        color = Color(0xFFC0392B),
                        modifier = Modifier.padding(14.dp),
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        if (!isError && upcoming.isEmpty()) {
            item {
                Text(
                    text = "No upcoming events on the calendar.",
                    color = Color(0xFF999999),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                )
            }
        }

        items(upcoming) { event ->
            EventCard(event = event, onSignUpClick = onSignUpClick)
            Spacer(modifier = Modifier.height(12.dp))
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
    isError: Boolean,
) {
    var enlargedPhotoUrl by remember { mutableStateOf<String?>(null) }

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
    val displayedIsError = if (viewingArchive) archiveError else isError
    val displayedIsLoading = viewingArchive && isLoadingArchivePhotos
    val currentMonthTitle = remember {
        val today = LocalDate.now()
        "${today.month.getDisplayName(TextStyle.FULL, Locale.US)} ${today.year}"
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
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
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (displayedIsLoading) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = KofcNavy)
                }
            }
        }

        if (displayedIsError) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF5F5))) {
                    Text(
                        text = if (viewingArchive) "Could not load photos for this month." else "Could not load recent photos.",
                        color = Color(0xFFC0392B),
                        modifier = Modifier.padding(14.dp),
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        if (!displayedIsLoading && !displayedIsError && displayedPhotos.isEmpty()) {
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
                        .clickable { enlargedPhotoUrl = photo.imageUrl },
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

                IconButton(
                    onClick = { enlargedPhotoUrl = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun EventCard(
    event: EventDto,
    onSignUpClick: (String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = event.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
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
                    modifier = Modifier.padding(top = 2.dp),
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
            if (!event.signupUrl.isNullOrBlank()) {
                Button(
                    onClick = { onSignUpClick(event.signupUrl) },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = KofcNavy,
                        contentColor = KofcGold,
                    ),
                    modifier = Modifier.padding(top = 10.dp),
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
                    modifier = Modifier.padding(top = 10.dp),
                ) {
                    Text("Open Link →")
                }
            }
        }
    }
}

