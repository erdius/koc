package com.erdman.kofc6650.data

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface GoogleCalendarApi {
    @GET("calendar/v3/calendars/{calendarId}/events")
    suspend fun getEvents(
        @Path("calendarId") calendarId: String,
        @Query("key") apiKey: String,
        @Query("timeMin") timeMin: String,
        @Query("timeMax") timeMax: String,
        @Query("singleEvents") singleEvents: Boolean = true,
        @Query("orderBy") orderBy: String = "startTime",
        @Query("maxResults") maxResults: Int = 100,
        @Query("pageToken") pageToken: String? = null,
    ): CalendarEventsResponse
}
