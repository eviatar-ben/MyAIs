package com.example.aiz.network

import com.example.aiz.model.ProcessMediaResponse
import okhttp3.MultipartBody
import retrofit2.http.Multipart
import retrofit2.http.Part
import retrofit2.http.POST
import retrofit2.http.GET

interface ApiService {

    @Multipart
    @POST("process_video")
    suspend fun uploadVideo(
        @Part video: MultipartBody.Part
    ): ProcessMediaResponse

    @Multipart
    @POST("process_media")
    suspend fun uploadMedia(
        @Part video: MultipartBody.Part,
        @Part audio: MultipartBody.Part?
    ): ProcessMediaResponse


    /**
     * Optional health-check
     */
    @GET("ping")
    suspend fun ping(): Map<String, String>
}
