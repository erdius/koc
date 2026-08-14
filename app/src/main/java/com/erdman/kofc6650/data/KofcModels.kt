package com.erdman.kofc6650.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class EventDto(
    val id: String,
    val title: String,
    val date: String,
    val time: String?,
    val location: String?,
    val description: String?,
    @Json(name = "signupUrl") val signupUrl: String?,
    @Json(name = "linkUrl") val linkUrl: String? = null,
)

data class CouncilEvents(
    val signupEvents: List<EventDto>,
    val allEvents: List<EventDto>,
)
