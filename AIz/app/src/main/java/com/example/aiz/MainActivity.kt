package com.example.aiz
import android.app.AlertDialog
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.aiz.ui.MainFragment

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val prefs = getSharedPreferences("AIzPrefs", MODE_PRIVATE)
        if (!prefs.contains("outputMode")) showModeDialog()
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, MainFragment())
                .commitNow()
        }
    }

    private fun showModeDialog() {
        val options = arrayOf("Text only", "Audio only", "Both")
        AlertDialog.Builder(this)
            .setTitle("Choose response mode")
            .setItems(options) { _, which ->
                val mode = when(which) {
                    0 -> "text"; 1 -> "audio"; else -> "both"
                }
                UserPrefs.setMode(this, mode)
            }
            .setCancelable(false)
            .show()
    }
}
