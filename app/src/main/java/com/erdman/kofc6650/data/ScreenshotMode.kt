package com.erdman.kofc6650.data

import android.content.Context
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Activated only via a manually-set SharedPreferences flag (never in a real
 * launch -- there's no UI to turn this on). Swaps live council data for
 * generic placeholders so Play Store screenshots don't expose real event
 * locations or real photos of real people.
 */
object ScreenshotMode {
    private const val PREFS_NAME = "kofc_screenshot_mode"
    private const val KEY_ACTIVE = "active"

    fun isActive(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_ACTIVE, false)

    private fun futureDate(daysAhead: Long): String =
        LocalDate.now().plusDays(daysAhead).format(DateTimeFormatter.ISO_LOCAL_DATE)

    val sampleSignupEvents: List<EventDto> = listOf(
        EventDto(
            id = "sample-signup-1", title = "Sample Volunteer Event",
            date = futureDate(7), time = null, location = null, description = null,
            signupUrl = "https://www.signupgenius.com/", linkUrl = null,
        ),
        EventDto(
            id = "sample-signup-2", title = "Another Volunteer Opportunity",
            date = futureDate(14), time = null, location = null, description = null,
            signupUrl = "https://www.signupgenius.com/", linkUrl = null,
        ),
        EventDto(
            id = "sample-signup-3", title = "Community Outreach Event",
            date = futureDate(21), time = null, location = null, description = null,
            signupUrl = null, linkUrl = null,
        ),
    )

    val sampleMinutesFiles: List<DriveFileDto> = listOf(
        DriveFileDto(
            id = "sample-minutes-1", name = "${futureDate(-30)} Regular Meeting.pdf",
            mimeType = "application/pdf", webViewLink = "https://drive.google.com/", modifiedTime = null,
        ),
        DriveFileDto(
            id = "sample-minutes-2", name = "${futureDate(-60)} Officers Meeting.pdf",
            mimeType = "application/pdf", webViewLink = "https://drive.google.com/", modifiedTime = null,
        ),
    )

    val sampleAllEvents: List<EventDto> = listOf(
        EventDto(
            id = "sample-cal-1", title = "Sample Community Event",
            date = futureDate(4), time = "6:00 PM", location = null,
            description = "This is a placeholder description for a sample community event. Real event " +
                "details, dates, and locations appear here in the app — removed from this screenshot for privacy.",
            signupUrl = "https://www.signupgenius.com/", linkUrl = null,
        ),
        EventDto(
            id = "sample-cal-2", title = "Sample Community Event",
            date = futureDate(9), time = "6:00 PM", location = null, description = null,
            signupUrl = null, linkUrl = null,
        ),
    )
}
