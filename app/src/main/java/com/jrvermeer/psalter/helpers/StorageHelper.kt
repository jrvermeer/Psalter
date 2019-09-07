package com.jrvermeer.psalter.helpers

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.StringRes

import com.jrvermeer.psalter.R

class StorageHelper(private val context: Context) {
    private val sPref: SharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var nightMode
        get() = getBoolean(R.string.pref_nightmode)
        set(b) = setBoolean(R.string.pref_nightmode, b)

    var scoreShown
        get() = getBoolean(R.string.pref_showScore)
        set(b) = setBoolean(R.string.pref_showScore, b)

    var pageIndex
        get() = getInt(R.string.pref_lastindex)
        set(i) = setInt(R.string.pref_lastindex, i)

    var offlineEnabled
        get() = getBoolean(R.string.pref_enableOffline)
        set(b) = setBoolean(R.string.pref_enableOffline, b)

    fun getBoolean(@StringRes id: Int): Boolean {
        return sPref.getBoolean(context.getString(id), false)
    }
    fun setBoolean(@StringRes id: Int, `val`: Boolean) {
        sPref.edit().putBoolean(context.getString(id), `val`).apply()
    }

    private fun getInt(@StringRes id: Int): Int {
        return sPref.getInt(context.getString(id), 0)
    }
    private fun setInt(@StringRes id: Int, `val`: Int) {
        sPref.edit().putInt(context.getString(id), `val`).apply()
    }
}
