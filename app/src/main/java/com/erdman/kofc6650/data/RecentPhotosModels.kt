package com.erdman.kofc6650.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RecentPhotoDto(
    val id: String,
    val imageUrl: String,
    val caption: String? = null,
    val submittedBy: String? = null,
    val uploadedAt: String? = null,
)
