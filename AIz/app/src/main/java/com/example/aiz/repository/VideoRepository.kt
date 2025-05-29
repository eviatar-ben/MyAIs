package com.example.aiz.repository

import android.content.Context
import android.net.Uri
import com.example.aiz.network.RetrofitInstance
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File
import java.io.FileOutputStream

class VideoRepository(private val context: Context) {
    suspend fun uploadVideo(uri: Uri) = with(context) {
        val input = contentResolver.openInputStream(uri)
        val tempFile = File.createTempFile("upload", ".mp4", cacheDir)
        FileOutputStream(tempFile).use { output -> input?.copyTo(output) }

        val requestFile = RequestBody.create("video/mp4".toMediaTypeOrNull(), tempFile)
        val body = MultipartBody.Part.createFormData("video", tempFile.name, requestFile)

        RetrofitInstance.api.uploadVideo(body)
    }
}
