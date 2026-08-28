package com.erdman.kofc6650.data

import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Url

interface SignupApi {
    @GET
    suspend fun getStatus(@Url url: String): SignupStatusResponseDto

    // Returns the raw body rather than a parsed DTO -- the Apps Script
    // backend's POST response redirect is occasionally unreliable to read,
    // so callers ignore whatever comes back here and re-fetch getStatus()
    // afterward to see whether the claim actually landed.
    @POST
    suspend fun claim(@Url url: String, @Body body: ClaimSlotRequest): ResponseBody
}
