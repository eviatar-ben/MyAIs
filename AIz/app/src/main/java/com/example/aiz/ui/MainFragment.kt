package com.example.aiz.ui

import android.Manifest
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.aiz.UserPrefs
import com.example.aiz.databinding.FragmentMainBinding
import com.example.aiz.model.SceneAnalysisResponse
import com.google.android.material.snackbar.Snackbar

class MainFragment : Fragment() {
    private var _b: FragmentMainBinding? = null
    private val b get() = _b!!
    private val vm: MainViewModel by viewModels()

    private val recordLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { res -> res.data?.data?.let { handleVideo(it) } }

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
            handleVideo(uri)
        }
    }

    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        if (!perms.entries.all { it.value })
            Snackbar.make(b.root, "Permissions required", Snackbar.LENGTH_LONG).show()
    }

    override fun onCreateView(
        inflater: LayoutInflater, c: ViewGroup?, s: Bundle?
    ) = FragmentMainBinding.inflate(inflater, c, false).also { _b = it }.root

    override fun onViewCreated(view: View, s: Bundle?) {
        super.onViewCreated(view, s)
        permLauncher.launch(
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        )

        vm.loading.observe(viewLifecycleOwner) { b.progressBar.visibility = if (it) View.VISIBLE else View.GONE }
        vm.error.observe(viewLifecycleOwner) { it?.let { e -> Snackbar.make(b.root, e, Snackbar.LENGTH_LONG).show() }}
        vm.response.observe(viewLifecycleOwner) { resp -> resp?.let { show(resp) } }

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
    }

    private fun handleVideo(uri: Uri) = vm.uploadVideo(uri)

    private fun show(resp: SceneAnalysisResponse) {
        when (UserPrefs.getMode(requireContext())) {
            "text" -> b.resultText.text = resp.text ?: ""
            "audio" -> resp.audioUrl?.let { play(it) }
            else -> {
                b.resultText.text = resp.text ?: ""
                resp.audioUrl?.let { play(it) }
            }
        }
    }

    private fun play(url: String) {
        MediaPlayer().apply {
            setDataSource(url)
            prepareAsync()
            setOnPreparedListener { start() }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView(); _b = null
    }
}