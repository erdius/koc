package com.erdman.kofc6650.data

import com.squareup.moshi.JsonClass

data class PhotoUploadFile(
    val bytes: ByteArray,
    val filename: String,
    val mimeType: String,
)

@JsonClass(generateAdapter = true)
data class PhotoUploadResponseDto(
    val ok: Boolean = false,
    val saved: Int = 0,
    val skipped: Int = 0,
)

@JsonClass(generateAdapter = true)
data class UploadErrorResponseDto(val error: String? = null)

class PhotoUploadException(message: String) : Exception(message)
