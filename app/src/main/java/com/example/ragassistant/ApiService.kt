package com.example.ragassistant

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

data class AskRequest(
    val question: String
)

data class AskResponse(
    val answer: String,
    val source: String? = null,
    val chunks_used: Int = 0
)

data class UploadResponse(
    val message: String,
    val filename: String,
    val chunks: Int
)

interface ApiService {

    @Multipart
    @POST("upload")
    suspend fun uploadPdf(
        @Part file: MultipartBody.Part
    ): Response<UploadResponse>

    @POST("ask")
    suspend fun askQuestion(
        @Body request: AskRequest
    ): Response<AskResponse>

}
