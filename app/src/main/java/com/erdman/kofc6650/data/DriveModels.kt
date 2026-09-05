package com.erdman.kofc6650.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DriveFilesResponse(
    val files: List<DriveFileDto> = emptyList(),
    val nextPageToken: String? = null,
)

@JsonClass(generateAdapter = true)
data class DriveFileDto(
    val id: String,
    val name: String,
    val mimeType: String,
    val webViewLink: String? = null,
    val modifiedTime: String? = null,
)
