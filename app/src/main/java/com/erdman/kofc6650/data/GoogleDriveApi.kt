package com.erdman.kofc6650.data

import retrofit2.http.GET
import retrofit2.http.Query

interface GoogleDriveApi {
    @GET("drive/v3/files")
    suspend fun listFiles(
        @Query("q") query: String,
        @Query("key") apiKey: String,
        @Query("fields") fields: String = "files(id,name,mimeType,webViewLink,modifiedTime)",
        @Query("orderBy") orderBy: String = "name desc",
        @Query("pageSize") pageSize: Int = 100,
    ): DriveFilesResponse
}
