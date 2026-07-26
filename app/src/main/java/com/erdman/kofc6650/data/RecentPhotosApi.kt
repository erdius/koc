package com.erdman.kofc6650.data

import retrofit2.http.GET
import retrofit2.http.Url

interface RecentPhotosApi {
    @GET
    suspend fun getRecentPhotos(@Url url: String): List<SlidePhotoDto>
}
