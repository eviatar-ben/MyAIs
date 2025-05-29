package com.example.aiz.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.aiz.model.SceneAnalysisResponse
import com.example.aiz.repository.VideoRepository
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = VideoRepository(app)
    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading
    private val _response = MutableLiveData<SceneAnalysisResponse?>()
    val response: LiveData<SceneAnalysisResponse?> = _response
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun uploadVideo(uri: Uri) {
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                _response.value = repo.uploadVideo(uri)
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
