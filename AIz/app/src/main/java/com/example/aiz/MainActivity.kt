package com.example.aiz
import android.app.AlertDialog
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.example.aiz.ui.MainFragment
import androidx.core.content.ContextCompat
import com.google.android.material.appbar.MaterialToolbar

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.statusBarColor = ContextCompat.getColor(this, R.color.gray_400)

        findViewById<MaterialToolbar>(R.id.toolbar).also {
            setSupportActionBar(it)
        }



        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, MainFragment())
                .commitNow()
        }

        // first‐run mode dialog
        val prefs = getSharedPreferences("AIzPrefs", MODE_PRIVATE)
        if (!prefs.contains("outputMode")) showModeDialog()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        // Tint the logo drawable to your blue color
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)

        toolbar.logo?.setColorFilter(
            ContextCompat.getColor(this, R.color.gray_480),
            PorterDuff.Mode.SRC_IN
        )
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_change_mode -> {
                showModeDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
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
