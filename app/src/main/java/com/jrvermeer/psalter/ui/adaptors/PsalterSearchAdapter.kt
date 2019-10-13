package com.jrvermeer.psalter.ui.adaptors

import android.content.Context
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.core.content.ContextCompat

import com.jrvermeer.psalter.models.Psalter
import com.jrvermeer.psalter.R
import com.jrvermeer.psalter.allIndexesOf
import com.jrvermeer.psalter.infrastructure.PsalterDb

import java.util.ArrayList
import kotlinx.android.synthetic.main.search_results_layout.view.*

/**
 * Created by Jonathan on 4/4/2017.
 */

//uses data from PsalterDb class to fill search "screen" with items
class PsalterSearchAdapter(private val appContext: Context, private val psalterDb: PsalterDb) : ArrayAdapter<Psalter>(appContext, R.layout.search_results_layout) {
    private lateinit var query: String
    private val inflater: LayoutInflater = LayoutInflater.from(appContext)
    private var psalmSearch: Boolean = false

    fun queryPsalter(searchQuery: String) {
        query = getFilteredQuery(searchQuery.toLowerCase())
        showResults(psalterDb.searchPsalter(query))
        psalmSearch = false
    }

    fun getAllFromPsalm(psalm: Int) {
        showResults(psalterDb.getPsalm(psalm))
        psalmSearch = true
    }

    private fun showResults(results: Array<Psalter>) {
        clear()
        addAll(*results)
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View? {
        var convertView = convertView
        try {
            val holder: ViewHolder
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.search_results_layout, parent, false)
                holder = ViewHolder(convertView)
                convertView!!.tag = holder
            } else
                holder = convertView.tag as ViewHolder

            val psalter = getItem(position)
            holder.tvNumber!!.text = psalter!!.number.toString()
            if (psalmSearch) {
                holder.tvLyrics!!.text = psalter.lyrics.substring(0, getVerseEndIndex(psalter.lyrics, 0))
            } else { //lyric search
                val filterLyrics = psalter.lyrics.toLowerCase()

                // build SpannableStringBuilder of only the verses that contain query
                val viewText = SpannableStringBuilder()
                val verseHitIndices = ArrayList<Int>() // could be verse number, could be chorus, so build a list of indexes
                var first = true
                for (i in filterLyrics.allIndexesOf(query)) {
                    val iStart = getVerseStartIndex(filterLyrics, i)

                    if (!verseHitIndices.contains(iStart)) { // if 1st occurrence of query in this verse, add verse to display
                        verseHitIndices.add(iStart)
                        val iEnd = getVerseEndIndex(filterLyrics, i)
                        if (!first) viewText.append("\n\n")
                        first = false
                        viewText.append(psalter.lyrics.substring(iStart, iEnd))
                    }
                }
                // highlight all instances of query
                val filterText = viewText.toString().toLowerCase()
                var iHighlightStart = filterText.indexOf(query)
                while (iHighlightStart >= 0) {
                    val iHighlightEnd = iHighlightStart + query.length
                    viewText.setSpan(StyleSpan(Typeface.BOLD), iHighlightStart, iHighlightEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
//                    viewText.setSpan(AbsoluteSizeSpan((holder.tvLyrics!!.textSize * 1.5).toInt()), iHighlightStart, iHighlightEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    viewText.setSpan(ForegroundColorSpan(ContextCompat.getColor(context, R.color.colorAccent)), iHighlightStart, iHighlightEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    iHighlightStart = filterText.indexOf(query, iHighlightStart + 1)
                }

                holder.tvLyrics!!.text = viewText
            }
            return convertView

        } catch (ex: Exception) {
            return convertView
        }

    }

    //filter out characters
    private fun getFilteredQuery(rawQuery: String): String {
        var filteredQuery = rawQuery
        for (ignore in appContext.resources.getStringArray(R.array.search_ignore_strings)) {
            filteredQuery = filteredQuery.replace(ignore, "")
        }
        return filteredQuery
    }


    //given random index in lyrics string, return index of the beginning of that verse
    private fun getVerseStartIndex(lyrics: String, queryStartIndex: Int): Int {
        val startIndex = lyrics.lastIndexOf("\n\n", queryStartIndex)
        return if (startIndex < 0) 0
        else startIndex + 2 // don't need to display the 2 newline chars
    }

    //given random index in lyrics string, return index of the end of that verse
    private fun getVerseEndIndex(lyrics: String, queryStartIndex: Int): Int {
        val i = lyrics.indexOf("\n\n", queryStartIndex)
        return if (i > 0) i
        else lyrics.length
    }

    class ViewHolder(view: View) {
        var tvNumber: TextView = view.tvSearchNumber
        var tvLyrics: TextView = view.tvSearchLyrics
    }
}
