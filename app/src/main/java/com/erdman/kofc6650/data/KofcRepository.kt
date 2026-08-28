package com.erdman.kofc6650.data

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
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

    // Photo uploads can involve several full-resolution images over a mobile
    // connection, so this needs much more headroom than the quick JSON
    // GETs the client above is tuned for.
    private val uploadHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(GOOGLE_CALENDAR_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val api = retrofit.create(GoogleCalendarApi::class.java)
    private val recentPhotosApi = retrofit.create(RecentPhotosApi::class.java)
    private val signupApi = retrofit.create(SignupApi::class.java)
    private val driveApi = retrofit.create(GoogleDriveApi::class.java)

    suspend fun getRecentPhotos(): List<RecentPhotoDto> =
        recentPhotosApi.getRecentPhotos(RECENT_PHOTOS_API_URL)

    suspend fun getPhotoArchiveMonths(): List<ArchiveMonthDto> =
        recentPhotosApi.getArchiveMonths(PHOTOS_ARCHIVE_API_URL)

    suspend fun getArchivedPhotos(month: String): List<RecentPhotoDto> =
        recentPhotosApi.getRecentPhotos("$PHOTOS_ARCHIVE_API_URL/$month")

    suspend fun getFeedTheHomelessStatus(): List<SignupStatusDto> =
        signupApi.getStatus(SIGNUP_API_URL).openDates

    // Excludes subfolders (e.g. "Archive") -- this is a flat list of the
    // current minutes only, not a full file browser.
    suspend fun getMinutesFiles(): List<DriveFileDto> {
        val query = "'$MINUTES_FOLDER_ID' in parents and mimeType != 'application/vnd.google-apps.folder' and trashed = false"
        return driveApi.listFiles(query = query, apiKey = API_KEY).files
    }

    suspend fun claimFeedTheHomelessSlot(date: String, name: String, email: String, whatsapp: String, asAlternate: Boolean) {
        try {
            signupApi.claim(SIGNUP_API_URL, ClaimSlotRequest(date = date, name = name, email = email, whatsapp = whatsapp, asAlternate = asAlternate))
        } catch (e: Exception) {
            // Apps Script's POST response redirect is occasionally unreliable
            // to read back directly; the action itself still lands
            // server-side, so callers always re-fetch getFeedTheHomelessStatus()
            // afterward rather than trust this call to succeed or throw.
        }
    }

    suspend fun uploadPhotos(
        pin: String,
        name: String,
        caption: String,
        files: List<PhotoUploadFile>,
    ): PhotoUploadResponseDto = withContext(Dispatchers.IO) {
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("pin", pin)
            .addFormDataPart("name", name)
            .addFormDataPart("caption", caption)
            .apply {
                files.forEach { file ->
                    addFormDataPart(
                        "photos",
                        file.filename,
                        file.bytes.toRequestBody(file.mimeType.toMediaTypeOrNull()),
                    )
                }
            }
            .build()

        val request = Request.Builder().url(PHOTOS_UPLOAD_API_URL).post(body).build()
        uploadHttpClient.newCall(request).execute().use { response ->
            val bodyStr = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val message = runCatching {
                    moshi.adapter(UploadErrorResponseDto::class.java).fromJson(bodyStr)?.error
                }.getOrNull() ?: "Upload failed (HTTP ${response.code})"
                throw PhotoUploadException(message)
            }
            moshi.adapter(PhotoUploadResponseDto::class.java).fromJson(bodyStr)
                ?: throw PhotoUploadException("Unexpected response from server")
        }
    }

    /**
     * Fetches the calendar once and returns both views the app needs:
     * every upcoming event (Calendar tab) and just the subset with a
     * SignUpGenius link (Volunteer Sign Ups tab). Both tabs used to make
     * their own identical API call; this merges them into one round trip.
     */
    suspend fun getCouncilEvents(): CouncilEvents {
        val all = fetchEvents()
        val signupOnly = all.filter { !it.signupUrl.isNullOrBlank() }
        return CouncilEvents(signupEvents = signupOnly, allEvents = all)
    }

    private suspend fun fetchEvents(): List<EventDto> {
        // Start of 14 days ago, not today -- the month view keeps showing
        // the last 2 weeks of past events for context, so the fetch has to
        // reach back that far even though the Sign Ups/Calendar list views
        // still filter down to today-or-later themselves in MainActivity.kt.
        val timeMin = LocalDate.now().minusDays(14).atStartOfDay(ZoneId.systemDefault())
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
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
        val signupUrl = extractSignupUrl(event.description)
        // Non-SignupGenius links (e.g. a Zoom join link) aren't "volunteer
        // sign ups", so they're kept out of signupUrl (which drives the
        // Volunteer Sign Ups tab filter) and surfaced separately instead.
        val linkUrl = if (signupUrl == null) extractGenericUrl(event.description) else null
        return EventDto(
            id = event.id,
            title = event.summary ?: "Untitled",
            date = date,
            time = time,
            location = event.location,
            description = cleanDescription(event.description),
            signupUrl = signupUrl,
            linkUrl = linkUrl,
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
        private const val RECENT_PHOTOS_API_URL = "https://koc-photos.erdcloud.org/api/photos"
        private const val PHOTOS_ARCHIVE_API_URL = "https://koc-photos.erdcloud.org/api/photos/archive"
        private const val PHOTOS_UPLOAD_API_URL = "https://koc-photos.erdcloud.org/api/upload"
        private const val SIGNUP_API_URL = "https://script.google.com/macros/s/AKfycbynrbC2qHqgjItUpxvFKabHXSawnglZYRvQQymVKstZRd4T6mt-fM1eCfcgAylJClMZ/exec"
        private const val MINUTES_FOLDER_ID = "1XIWKahCrq08qfRtrVWK33GCgWoQnG8TH"

        private val TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
        private val SIGNUP_URL_REGEX =
            Regex("""https?://(?:www\.)?signupgenius\.com/[^\s<>"']*""", RegexOption.IGNORE_CASE)
        private val BR_TAG_REGEX = Regex("<br\\s*/?>", RegexOption.IGNORE_CASE)
        private val HTML_TAG_REGEX = Regex("<[^>]+>")
        private val BARE_URL_REGEX = Regex("""https?://\S+""")
        private val EXTRA_BLANK_LINES_REGEX = Regex("\n{3,}")

        private fun extractSignupUrl(text: String?): String? {
            if (text.isNullOrBlank()) return null
            return SIGNUP_URL_REGEX.find(text)?.value?.let(::unescapeHtmlEntities)
        }

        private fun extractGenericUrl(text: String?): String? {
            if (text.isNullOrBlank()) return null
            return BARE_URL_REGEX.find(text)?.value?.trimEnd('.', ',', ')', ']')?.let(::unescapeHtmlEntities)
        }

        // URLs are extracted from the raw (still-HTML) description, which
        // encodes multi-param query strings as "...&amp;startdate=..." --
        // opening that literally sends "amp;startdate" as the param name,
        // silently breaking any link with more than one query parameter.
        private fun unescapeHtmlEntities(text: String): String =
            text.replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")

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
