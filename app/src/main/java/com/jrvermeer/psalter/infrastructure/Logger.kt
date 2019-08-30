package com.jrvermeer.psalter.infrastructure

import android.content.Context
import android.util.Log

import com.flurry.android.FlurryAgent
import com.jrvermeer.psalter.models.LogEvent
import com.jrvermeer.psalter.models.SearchMode
import com.jrvermeer.psalter.R

/**
 * Created by Jonathan on 7/17/2018.
 */

object Logger {
    private const val TAG = "PsalterLog"

    fun init(context: Context) {
        FlurryAgent.Builder()
                .withLogEnabled(true)
                .build(context, context.resources.getString(R.string.secret_flurry))
    }

    fun d(message: String) {
        Log.d(TAG, message)
    }

    fun e(message: String, ex: Throwable?) {
        Log.e(TAG, message, ex)
        FlurryAgent.onError("Error", message, ex)
    }

    fun changeScore(scoreVisible: Boolean) {
        val params = mapOf("ScoreVisible" to scoreVisible.toString())
        FlurryAgent.logEvent(LogEvent.ChangeScore.name, params)
    }

    fun changeTheme(nightMode: Boolean) {
        val params = mapOf("NightMode" to nightMode.toString())
        FlurryAgent.logEvent(LogEvent.ChangeTheme.name, params)
    }

    fun playbackStarted(numberTitle: String, shuffling: Boolean) {
        val params = mapOf("Number" to numberTitle)
        FlurryAgent.logEvent(if (shuffling) LogEvent.ShufflePsalter.name else LogEvent.PlayPsalter.name, params)
    }

    fun searchPsalter(number: Int){
        event(LogEvent.SearchPsalter, mapOf("Number" to number.toString()))
    }
    fun searchPsalm(psalm: Int, psalterChosen: Int? = null){
        val params = mutableMapOf("Psalm" to psalm.toString())
        if(psalterChosen != null) params["PsalterChosen"] = psalterChosen.toString()
        event(LogEvent.SearchPsalm, params);
    }
    fun searchLyrics(query: String, psalterChosen: Int? = null){
        val params = mutableMapOf("Query" to query)
        if(psalterChosen != null) params["PsalterChosen"] = psalterChosen.toString()
        event(LogEvent.SearchLyrics, params)
    }
    fun searchEvent(searchMode: SearchMode, query: String, psalterChosen: Int? = null){
        when (searchMode){
            SearchMode.Lyrics -> searchLyrics(query, psalterChosen)
            SearchMode.Psalter -> searchPsalter(query.toInt())
            SearchMode.Psalm -> searchPsalm(query.toInt(), psalterChosen)
        }
    }

    fun event(event: LogEvent, params: Map<String, String>? = null) {
        FlurryAgent.logEvent(event.name, params)
    }
}