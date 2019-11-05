package com.jrvermeer.psalter.infrastructure

import android.content.Context
import android.util.Log

import com.flurry.android.FlurryAgent
import com.jrvermeer.psalter.models.LogEvent
import com.jrvermeer.psalter.models.SearchMode
import com.jrvermeer.psalter.R
import com.jrvermeer.psalter.models.Psalter

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
        event(LogEvent.ChangeScore, params)
    }

    fun changeTheme(nightMode: Boolean) {
        val params = mapOf("NightMode" to nightMode.toString())
        event(LogEvent.ChangeTheme, params)
    }

    fun playbackStarted(numberTitle: String, shuffling: Boolean) {
        val params = mapOf("Number" to numberTitle, "Shuffling" to shuffling.toString())
        event(LogEvent.PlayPsalter, params)
    }

    fun skipToNext(skippedPsalter: Psalter?){
        val params = mapOf("SkippedPsalter" to (skippedPsalter?.title ?: "null"))
        event(LogEvent.SkipToNext, params)
    }

    fun searchPsalter(number: Int){
        event(LogEvent.SearchPsalter, mapOf("Number" to number.toString()))
    }
    fun searchPsalm(psalm: Int, psalterChosen: Int? = null){
        val params = mutableMapOf("Psalm" to psalm.toString())
        if(psalterChosen != null) params["PsalterChosen"] = psalterChosen.toString()
        event(LogEvent.SearchPsalm, params)
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
    fun feedbackCancelled(enjoying: Boolean? = null){
        val params = mapOf("Enjoying" to getFeedbackEnjoyingResponse(enjoying))
        event(LogEvent.Feedback, params)
    }
    fun feedback(enjoying: Boolean, doSomethingAboutIt: Boolean?){
        val action1 = getFeedbackEnjoyingResponse(enjoying)
        val param2 = if(enjoying) "Rate" else "SuggestImprovements"
        val action2: String = when (doSomethingAboutIt){
            null -> "AlreadyDid"
            true -> "Yes"
            false -> "MaybeLater"
        }
        val params = mutableMapOf("Enjoying" to action1, param2 to action2)
        event(LogEvent.Feedback, params)
    }
    private fun getFeedbackEnjoyingResponse(enjoying: Boolean?): String {
        return when(enjoying) {
            null -> "Cancelled"
            true -> "Yes!"
            false -> "Could be better"
        }
    }

    fun event(event: LogEvent, params: Map<String, String>? = null) {
        FlurryAgent.logEvent(event.name, params)
    }
}
