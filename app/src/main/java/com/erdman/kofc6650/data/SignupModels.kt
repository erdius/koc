package com.erdman.kofc6650.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SignupSlotDto(
    val label: String,
    val name: String?,
)

@JsonClass(generateAdapter = true)
data class SignupStatusDto(
    val date: String,
    val slots: List<SignupSlotDto> = emptyList(),
    val filledCount: Int = 0,
    val totalCount: Int = 0,
)

// Up to 4 dates can be open at once (Kris opens several months ahead so
// volunteers who miss the closest date filling up can still claim a later
// one themselves), soonest first; empty when nothing is currently open.
@JsonClass(generateAdapter = true)
data class SignupStatusResponseDto(
    val openDates: List<SignupStatusDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class ClaimSlotRequest(
    val action: String = "claim",
    val date: String,
    val name: String,
    val email: String,
    val whatsapp: String,
    val asAlternate: Boolean,
)
