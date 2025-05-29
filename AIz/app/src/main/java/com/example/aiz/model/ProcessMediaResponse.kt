package com.example.aiz.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ProcessMediaResponse(
    @Json(name = "message")
    val message: String,
    @Json(name = "gemini_text_response")
    val textResponse: String,
    @Json(name = "audio_base64")
    val audioBase64: String
)
