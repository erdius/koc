package com.erdman.kofc6650

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.text.ClickableText
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.TextStyle as ComposeTextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.erdman.kofc6650.data.EventDto
import com.erdman.kofc6650.data.KofcRepository
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

private const val PHOTOS_FORM_URL =
    "https://docs.google.com/forms/d/e/1FAIpQLSemHls6xz9BRhMuy3QruxiSw6fcHOEYG94NBcuCWmnZ-S3j1A/viewform"
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
                PhotosTab(
                    onSubmitClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PHOTOS_FORM_URL)))
                    },
                )
            } else if (isLoadingPhotos) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = KofcNavy)
                }
            } else {
                RecentPhotosTab(photos = photos, isError = photosError)
            }
        }
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
private fun PhotosTab(onSubmitClick: () -> Unit) {
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
                        text = "Photos are collected through a short Google Form. Tap below to open it and upload your pictures.",
                        fontSize = 14.sp,
                        color = Color(0xFF555555),
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    Button(
                        onClick = onSubmitClick,
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = KofcNavy,
                            contentColor = KofcGold,
                        ),
                        modifier = Modifier.padding(top = 10.dp),
                    ) {
                        Text("Submit Photos →")
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentPhotosTab(
    photos: List<RecentPhotoDto>,
    isError: Boolean,
) {
    var enlargedPhotoUrl by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 32.dp),
    ) {
        item {
            Text(
                text = "Recent Photos",
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
                        text = "Could not load recent photos.",
                        color = Color(0xFFC0392B),
                        modifier = Modifier.padding(14.dp),
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        if (!isError && photos.isEmpty()) {
            item {
                Text(
                    text = "No photos yet.",
                    color = Color(0xFF999999),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                )
            }
        }

        items(photos) { photo ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                AsyncImage(
                    model = photo.imageUrl,
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
            }
        }
    }
}

