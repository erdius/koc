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
    // Set once in KofcRepository, rather than re-matching event.title ==
    // "Feed the Homeless" at every call site -- so there's only one place
    // that can miss it, not several. Missing from an offline cache written
    // before this field existed defaults to false via Moshi's default-value
    // support for absent keys (verified: reflection-based KotlinJsonAdapterFactory
    // honors constructor defaults, unlike Swift's Codable).
    @Json(name = "isFeedTheHomeless") val isFeedTheHomeless: Boolean = false,
)

data class CouncilEvents(
    val signupEvents: List<EventDto>,
    val allEvents: List<EventDto>,
)
