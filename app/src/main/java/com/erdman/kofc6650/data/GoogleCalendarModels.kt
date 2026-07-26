package com.erdman.kofc6650.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CalendarEventsResponse(
    val items: List<CalendarEventDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class CalendarEventDto(
    val id: String,
    val summary: String? = null,
    val description: String? = null,
    val location: String? = null,
    val status: String? = null,
    val start: CalendarEventDateTime? = null,
)

@JsonClass(generateAdapter = true)
data class CalendarEventDateTime(
    val date: String? = null,
    val dateTime: String? = null,
)
