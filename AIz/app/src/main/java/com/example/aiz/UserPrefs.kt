package com.example.aiz
import android.content.Context
import androidx.core.content.edit

object UserPrefs {
    private const val NAME = "AIzPrefs"
    private const val KEY_MODE = "outputMode"

    fun getMode(ctx: Context): String =
        ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE)
            .getString(KEY_MODE, "both")!!
    fun setMode(ctx: Context, mode: String) =
        ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE)
            .edit { putString(KEY_MODE, mode) }
}
