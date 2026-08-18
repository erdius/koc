package com.erdman.kofc6650.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RecentPhotoDto(
    val id: String,
    val imageUrl: String,
    // Resized (max 800px wide) JPEG for grid/list display. Falls back to the
    // full-size imageUrl server-side for formats the thumbnailer can't
    // decode (real Apple HEIC), so this is always a valid URL to load.
    val thumbnailUrl: String,
    val caption: String? = null,
    val submittedBy: String? = null,
    val uploadedAt: String? = null,
)

@JsonClass(generateAdapter = true)
data class ArchiveMonthDto(
    val month: String,
    val label: String,
    val count: Int,
)
