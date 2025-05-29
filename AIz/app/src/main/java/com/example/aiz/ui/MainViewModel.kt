package com.example.aiz.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.aiz.model.ProcessMediaResponse
import com.example.aiz.repository.VideoRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = VideoRepository(app)
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    // ONE-SHOT event flow: no replay, new collectors see only new emissions
    private val _response = MutableSharedFlow<ProcessMediaResponse>(
        replay = 0,
        extraBufferCapacity = 1
    )
    val response: SharedFlow<ProcessMediaResponse?> = _response.asSharedFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun uploadMedia(uri: Uri, audioFile: File?) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val response = repo.uploadMedia(uri, audioFile)
                _response.emit(response)
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _loading.value = false
            }
        }
    }

    companion object {
        const val MAX_VIDEO_LENGTH_SEC = 5
    }
}
