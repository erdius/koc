package com.erdman.kofc6650.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SignupSlotDto(
    val label: String,
    val name: String?,
)

@JsonClass(generateAdapter = true)
data class SignupStatusDto(
    val open: Boolean,
    val date: String? = null,
    val slots: List<SignupSlotDto> = emptyList(),
    val filledCount: Int = 0,
    val totalCount: Int = 0,
)

@JsonClass(generateAdapter = true)
data class ClaimSlotRequest(
    val action: String = "claim",
    val name: String,
    val email: String,
    val whatsapp: String,
    val asAlternate: Boolean,
)
