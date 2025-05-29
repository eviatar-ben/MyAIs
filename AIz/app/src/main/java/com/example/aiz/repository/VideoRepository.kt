package com.example.aiz.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.aiz.model.ProcessMediaResponse
import com.example.aiz.network.RetrofitInstance
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File
import java.io.FileOutputStream

class VideoRepository(private val context: Context) {
    /**
     * Uploads video to /upload, returns the list of keyframe filenames.
     */
    suspend fun uploadVideo(uri: Uri): ProcessMediaResponse = with(context) {
        // copy URI → temp file
        val input = contentResolver.openInputStream(uri)
        val tempFile = File.createTempFile("upload", ".mp4", cacheDir)
        FileOutputStream(tempFile).use { output -> input?.copyTo(output) }

        // prepare multipart body
        val requestFile = RequestBody.create("video/mp4".toMediaTypeOrNull(), tempFile)
        val body = MultipartBody.Part.createFormData("video", tempFile.name, requestFile)

        // call server
        Log.d("Chachy", body.body.toString())
        Log.d("Chachy", body.headers.toString())
        val response = RetrofitInstance.api.uploadVideo(body)
        Log.d("Chachy", response.toString())
        return response
    }

    suspend fun uploadMedia(videoUri: Uri, audioFile: File?): ProcessMediaResponse = with(context) {
        // --- 1) Prepare video part ---
        // copy URI → temp file
        val videoInput = contentResolver.openInputStream(videoUri)
        val videoFile = File.createTempFile("upload", ".mp4", cacheDir)
        FileOutputStream(videoFile).use { out -> videoInput?.copyTo(out) }
        val videoBody = RequestBody.create("video/mp4".toMediaTypeOrNull(), videoFile)
        val videoPart = MultipartBody.Part.createFormData("video", videoFile.name, videoBody)

        // --- 2) Prepare audio part ----
        var audioPart:  MultipartBody.Part? = null
        if (audioFile != null) {
            val audioBody = RequestBody.create("audio/mpeg".toMediaTypeOrNull(), audioFile)
            audioPart = MultipartBody.Part.createFormData("audio", audioFile.name, audioBody)
        }

        val response = RetrofitInstance.api.uploadMedia(videoPart, audioPart)
        Log.d("Chachy", response.toString())
        response

        // call server
//        return if (audioPart != null) {
//            val response = RetrofitInstance.api.uploadMedia(videoPart, audioPart)
//            Log.d("Chachy", response.toString())
//            response
//        } else {
//            val response = RetrofitInstance.api.uploadVideo(videoPart)
//            Log.d("Chachy", response.toString())
//            response
//        }
    }

    suspend fun ping() {
        Log.d("Chachy", RetrofitInstance.api.ping().toString())
    }
}
