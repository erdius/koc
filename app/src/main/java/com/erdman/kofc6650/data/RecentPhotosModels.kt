package com.erdman.kofc6650.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SlidePhotoDto(
    val index: Int,
    val slideId: String,
    val imageUrl: String,
)
