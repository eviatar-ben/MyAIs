package com.example.aiz.network

import com.example.aiz.model.SceneAnalysisResponse
import okhttp3.MultipartBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {
    @Multipart
    @POST("process")
    suspend fun uploadVideo(
        @Part video: MultipartBody.Part
    ): SceneAnalysisResponse
}