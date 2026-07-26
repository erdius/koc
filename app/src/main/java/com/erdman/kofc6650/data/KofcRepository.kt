package com.erdman.kofc6650.data

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Reads the council's public Google Calendar directly, the same way the
 * volunteer-signup.html widget embedded on kofc6650.org does. The
 * koc.erdcloud.org backend's own calendar parser never expands recurring
 * events past their first/edited occurrence, so it silently drops ongoing
 * monthly events; going straight to the Calendar API (singleEvents=true)
 * avoids that bug entirely.
 */
class KofcRepository {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(GOOGLE_CALENDAR_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val api = retrofit.create(GoogleCalendarApi::class.java)
    private val recentPhotosApi = retrofit.create(RecentPhotosApi::class.java)

    suspend fun getRecentPhotos(): List<SlidePhotoDto> =
        recentPhotosApi.getRecentPhotos(RECENT_PHOTOS_API_URL)

    /** Only events with a SignUpGenius link, for the Volunteer Sign Ups tab. */
    suspend fun getEvents(): List<EventDto> =
        fetchEvents().filter { !it.signupUrl.isNullOrBlank() }

    /** Every upcoming event, for the full agenda-style Calendar tab. */
    suspend fun getAllEvents(): List<EventDto> = fetchEvents()

    private suspend fun fetchEvents(): List<EventDto> {
        val timeMin = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        val response = api.getEvents(
            calendarId = CALENDAR_ID,
            apiKey = API_KEY,
            timeMin = timeMin,
        )
        return response.items
            .asSequence()
            .filter { it.status != "cancelled" }
            .mapNotNull { toEventDto(it) }
            .toList()
    }

    private fun toEventDto(event: CalendarEventDto): EventDto? {
        val start = event.start ?: return null
        val date = start.date ?: start.dateTime?.substringBefore("T") ?: return null
        val time = start.dateTime?.let { formatTime(it) }.orEmpty()
        return EventDto(
            id = event.id,
            title = event.summary ?: "Untitled",
            date = date,
            time = time,
            location = event.location,
            description = cleanDescription(event.description),
            signupUrl = extractSignupUrl(event.description),
        )
    }

    private fun formatTime(dateTime: String): String = try {
        OffsetDateTime.parse(dateTime).format(TIME_FORMATTER)
    } catch (e: DateTimeParseException) {
        ""
    }

    companion object {
        private const val GOOGLE_CALENDAR_BASE_URL = "https://www.googleapis.com/"
        private const val CALENDAR_ID = "3j9ina0035sbq5u2f7s4oafua4@group.calendar.google.com"
        private const val API_KEY = "AIzaSyDMVWRq8ykzhqKCVxiavbEfLLbvaIdahfU"
        private const val RECENT_PHOTOS_API_URL =
            "https://script.google.com/macros/s/AKfycbw-qJ3atc8-rZ5diar3Js67_ZAXusVsZ2I56yBz4rsLI4fKI0r0aTlCoxSyzNjSrU_V/exec"

        private val TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
        private val SIGNUP_URL_REGEX =
            Regex("""https?://(?:www\.)?signupgenius\.com/[^\s<>"']*""", RegexOption.IGNORE_CASE)
        private val BR_TAG_REGEX = Regex("<br\\s*/?>", RegexOption.IGNORE_CASE)
        private val HTML_TAG_REGEX = Regex("<[^>]+>")
        private val BARE_URL_REGEX = Regex("""https?://\S+""")
        private val EXTRA_BLANK_LINES_REGEX = Regex("\n{3,}")

        private fun extractSignupUrl(text: String?): String? {
            if (text.isNullOrBlank()) return null
            return SIGNUP_URL_REGEX.find(text)?.value
        }

        private fun cleanDescription(text: String?): String {
            if (text.isNullOrBlank()) return ""
            return text
                .replace(BR_TAG_REGEX, "\n")
                .replace(HTML_TAG_REGEX, "")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("\\n", "\n")
                .replace(BARE_URL_REGEX, "")
                .replace(EXTRA_BLANK_LINES_REGEX, "\n\n")
                .trim()
        }
    }
}
