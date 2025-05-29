package com.example.aiz.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.aiz.UserPrefs
import com.example.aiz.databinding.FragmentMainBinding
import com.example.aiz.model.ProcessMediaResponse
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import java.io.File

class MainFragment : Fragment() {
    private var _b: FragmentMainBinding? = null
    private val b get() = _b!!
    private val vm: MainViewModel by viewModels()

    private var recorder: MediaRecorder? = null
    private var audioFile: File? = null
    private var already_sent_audio: Boolean = false

    private val recordLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { res -> res.data?.data?.let { handleContent(it) } }

    private val pickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { res ->
        res.data?.data?.let { uri ->
            val cursor = requireContext().contentResolver.query(
                uri, arrayOf(MediaStore.Video.Media.DURATION), null, null, null
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    val dur = it.getLong(0)
                    if (dur > MainViewModel.MAX_VIDEO_LENGTH_SEC * 1000) {
                        Snackbar.make(b.root, "Video too long", Snackbar.LENGTH_LONG).show()
                        return@registerForActivityResult
                    }
                }
            }
            handleContent(uri)
        }
    }

    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        if (!perms.entries.all { it.value }) {
            val x = 5
        }
//            Snackbar.make(b.root, "Permissions required for $perms", Snackbar.LENGTH_LONG).show()
    }

    override fun onCreateView(
        inflater: LayoutInflater, c: ViewGroup?, s: Bundle?
    ) = FragmentMainBinding.inflate(inflater, c, false).also { _b = it }.root

    @SuppressLint("ClickableViewAccessibility", "IntentReset")
    override fun onViewCreated(view: View, s: Bundle?) {
        super.onViewCreated(view, s)
        permLauncher.launch(
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        )

        collectFlows()

        b.btnRecord.setOnClickListener {
            val i = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
                .putExtra(MediaStore.EXTRA_DURATION_LIMIT, MainViewModel.MAX_VIDEO_LENGTH_SEC)
            recordLauncher.launch(i)
        }
        b.btnUpload.setOnClickListener {
            val i = Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
                .setType("video/*")
            pickerLauncher.launch(i)
        }

        // When user presses & holds, start recording; when they lift, stop
        b.btnAudio.setOnTouchListener { _, ev ->
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> startAudioRecording()
                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> stopAudioRecording()
            }
            true
        }
    }

    private fun startAudioRecording() {
        already_sent_audio = false
        with (File(requireContext().cacheDir, "request_audio.m4a")) {
            clear()
            recorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(absolutePath)
                prepare()
                start()
            }
            Snackbar.make(b.root, "Recording…", Snackbar.LENGTH_SHORT).show()
            audioFile = this
        }
    }

    private fun handleContent(videoUri: Uri) {
        if (audioFile == null){
            vm.uploadMedia(videoUri, null)
        } else if (already_sent_audio) {
            audioFile = null
            vm.uploadMedia(videoUri, null)
        }
        else {
            vm.uploadMedia(videoUri, audioFile)
            already_sent_audio = true
        }

        // Now call ViewModel with both

    }

    private fun stopAudioRecording() {
        recorder?.run {
            stop()
            reset()
            release()
        }
        recorder = null
        Snackbar.make(b.root, "Audio saved", Snackbar.LENGTH_SHORT).show()
    }

    private fun collectFlows() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                launch {
                    vm.loading.collectLatest {

                        b.progressBar.visibility = if (it) View.VISIBLE else View.GONE

                        // Don't show when loading response
                        b.btnRecord.visibility = if (it) View.INVISIBLE else View.VISIBLE
                        b.btnUpload.visibility = if (it) View.INVISIBLE else View.VISIBLE
                        b.btnAudio.visibility = if (it) View.INVISIBLE else View.VISIBLE
                        b.responseCard.visibility = if (it) View.INVISIBLE else b.responseCard.visibility
                    }
                }
                launch {
                    vm.error.collectLatest {
                        it?.let { e ->
                            Snackbar.make(b.root, e, Snackbar.LENGTH_LONG).show()
                            Log.d("Chachy", e)
                        }
                    }
                }
                launch {
                    vm.response.collectLatest { resp ->
                        resp?.let { show(it) }
                    }
                }
            }
        }
    }

    private fun show(resp: ProcessMediaResponse) {
        when (UserPrefs.getMode(requireContext())) {
            "text" -> displayText(resp.textResponse)
            "audio" -> play(resp.audioBase64)
            "both" -> {
                displayText(resp.textResponse)
                play(resp.audioBase64)
            }
        }
    }

    private fun displayText(responseText: String) {
        b.responseCard.visibility = View.VISIBLE
        b.resultText.text = responseText
    }

    private fun play(audioBase64: String) {
        // Decode base64-encoded MP3 and write to temp file
        val audioBytes = android.util.Base64.decode(audioBase64, android.util.Base64.DEFAULT)
        val tempFile = File(requireContext().cacheDir, "response.mp3")
        tempFile.outputStream().use { it.write(audioBytes) }

        // Play the MP3 file
        MediaPlayer().apply {
            setDataSource(tempFile.absolutePath)
            setOnPreparedListener { start() }
            prepareAsync()
        }
    }

    override fun onDestroyView() {
        audioFile?.delete()
        super.onDestroyView(); _b = null
    }

    private fun File.clear() {
        writeText("")
        outputStream().use { it.write(ByteArray(0)) }
    }
}