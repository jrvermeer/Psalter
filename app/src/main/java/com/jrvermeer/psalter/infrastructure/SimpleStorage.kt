package com.jrvermeer.psalter.infrastructure

import android.content.Context
import android.content.SharedPreferences
import android.support.annotation.StringRes

import com.jrvermeer.psalter.R

class SimpleStorage(private val context: Context) {
    private val sPref: SharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    val isNightMode get() = getBoolean(R.string.pref_nightmode)
    val isScoreShown get() = getBoolean(R.string.pref_showScore)

    var pageIndex
        get() = getInt(R.string.pref_lastindex)
        set(i) = setInt(R.string.pref_lastindex, i)

    fun getBoolean(@StringRes id: Int): Boolean {
        return sPref.getBoolean(context.getString(id), false)
    }

    fun setBoolean(@StringRes id: Int, `val`: Boolean) {
        sPref.edit().putBoolean(context.getString(id), `val`).apply()
    }

    fun toggleBoolean(@StringRes id: Int): Boolean {
        val newVal = !getBoolean(id)
        setBoolean(id, newVal)
        return newVal
    }

    private fun getInt(@StringRes id: Int): Int {
        return sPref.getInt(context.getString(id), 0)
    }

    private fun setInt(@StringRes id: Int, `val`: Int) {
        sPref.edit().putInt(context.getString(id), `val`).apply()
    }

    fun toggleNightMode() {
        val nightMode = toggleBoolean(R.string.pref_nightmode)
        Logger.changeTheme(nightMode)
    }

    fun toggleScore(): Boolean {
        val shown = toggleBoolean(R.string.pref_showScore)
        Logger.changeScore(shown)
        return shown
    }
}
