package com.jrvermeer.psalter.ui.adaptors

import android.content.Context
import androidx.core.text.HtmlCompat
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.jrvermeer.psalter.models.Psalter
import com.jrvermeer.psalter.infrastructure.Logger
import com.jrvermeer.psalter.infrastructure.PsalterDb
import com.jrvermeer.psalter.*
import com.jrvermeer.psalter.helpers.DownloadHelper
import kotlinx.android.synthetic.main.psalter_layout.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Created by Jonathan on 3/27/2017.
 */

class PsalterPagerAdapter(private val context: Context,
                          private val scope: CoroutineScope,
                          private val psalterDb: PsalterDb,
                          private val downloader: DownloadHelper,
                          private var showScore: Boolean,
                          private val nightMode: Boolean) : androidx.viewpager.widget.PagerAdapter() {

    private val views = ConcurrentHashMap<Int, View>()

    fun getView(i: Int): View? {
        return views[i]
    }

    override fun instantiateItem(collection: ViewGroup, position: Int): Any {
        try {
            val psalter = psalterDb.getIndex(position)!!
            Logger.d("Viewpager building page for psalter ${psalter.title}")
            val inflater = LayoutInflater.from(context)
            val layout = inflater.inflate(R.layout.psalter_layout, collection, false) as ViewGroup

            scope.launch { setScoreAndLyrics(psalter, layout) }

            layout.tvPagerHeading.text = psalter.heading
            layout.tvPagerPsalm.text = HtmlCompat.fromHtml(psalter.bibleLink, HtmlCompat.FROM_HTML_MODE_LEGACY)
            layout.tvPagerPsalm.movementMethod = LinkMovementMethod.getInstance()

            views[position] = layout
            collection.addView(layout)

            return layout
        }
        catch (ex: Exception) {
            Logger.e("Error in PsalterPagerAdapter.instantiateItem", ex)
            return collection
        }
    }

    override fun destroyItem(container: ViewGroup, position: Int, item: Any) {
        if (item is View) {
            container.removeView(item)
            views.remove(position)
        }
    }

    override fun getCount(): Int {
        return psalterDb.getCount()
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return psalterDb.getIndex(position)?.title ?: "?"
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view === `object`
    }

    private suspend fun setScoreAndLyrics(psalter: Psalter, layout: View) {
        // load audio in bg. viewpager never needs audio, but user could request it at tap of a button
        scope.launch { psalter.loadAudio(downloader) }
        var text = psalter.lyrics
        if (showScore) {
            layout.scoreProgress.show()
            val score = psalter.loadScore(downloader)
            layout.scoreProgress.hide()
            if (score != null) {
                if (nightMode) score.invertColors()
                layout.imgScore.setImageDrawable(score)

                val lyricStartIndex = psalter.lyrics.indexOf((psalter.numVersesInsideStaff + 1).toString() + ". ")
                text = if (lyricStartIndex < 0) "" else psalter.lyrics.substring(lyricStartIndex)
            }
        }
        else {
            layout.imgScore.setImageDrawable(null)
            // load score in bg. viewpager doesn't need it *now*, but could at the tap of a button
            scope.launch { psalter.loadScore(downloader) }
        }
        layout.tvPagerLyrics.text = text
    }

    fun toggleScore()  {
        showScore = !showScore
        for((i, view) in views) {
            scope.launch { setScoreAndLyrics(psalterDb.getIndex(i)!!, view) }
        }
    }


}
