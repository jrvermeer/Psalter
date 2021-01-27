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

    var fabLongPressCount
        get() = getInt(R.string.pref_fabLongPressCount)
        set(i) = setInt(R.string.pref_fabLongPressCount, i)

    var launchCount
        get() = getInt(R.string.pref_launchCount)
        set(i) = setInt(R.string.pref_launchCount, i)

    var lastRatePromptTime
        get() = getLong(R.string.pref_lastRatePromptShownTime)
        set(l) = setLong(R.string.pref_lastRatePromptShownTime, l)

    var ratePromptCount
        get() = getInt(R.string.pref_ratePromptCount)
        set(i) = setInt(R.string.pref_ratePromptCount, i)

    var allMediaDownloaded
        get() = getBoolean(R.string.pref_enableOffline)
        set(b) = setBoolean(R.string.pref_enableOffline, b)

    var textScale
        get() = getFloat(R.string.pref_textScale, 1f)
        set(i) = setFloat(R.string.pref_textScale, i)

    fun getBoolean(@StringRes id: Int): Boolean {
        return sPref.getBoolean(context.getString(id), false)
    }
    fun setBoolean(@StringRes id: Int, b: Boolean) {
        sPref.edit().putBoolean(context.getString(id), b).apply()
    }

    fun getInt(@StringRes id: Int, default: Int = 0): Int {
        return sPref.getInt(context.getString(id), default)
    }
    fun setInt(@StringRes id: Int, i: Int) {
        sPref.edit().putInt(context.getString(id), i).apply()
    }

    fun getFloat(@StringRes id: Int, default: Float = 0f): Float {
        return sPref.getFloat(context.getString(id), default)
    }
    fun setFloat(@StringRes id: Int, i: Float) {
        sPref.edit().putFloat(context.getString(id), i).apply()
    }

    fun getLong(@StringRes id: Int): Long {
        return sPref.getLong(context.getString(id), 0)
    }
    fun setLong(@StringRes id: Int, l: Long) {
        sPref.edit().putLong(context.getString(id), l).apply()
    }
}
