package com.jrvermeer.psalter.ui.adaptors

import android.support.v4.text.HtmlCompat
import android.support.v4.view.PagerAdapter
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.jrvermeer.psalter.models.Psalter
import com.jrvermeer.psalter.infrastructure.Logger
import com.jrvermeer.psalter.infrastructure.PsalterDb
import com.jrvermeer.psalter.*
import com.jrvermeer.psalter.ui.MainActivity
import kotlinx.android.synthetic.main.psalter_layout.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Created by Jonathan on 3/27/2017.
 */

class PsalterPagerAdapter(private val context: MainActivity,
                          private val psalterDb: PsalterDb,
                          private var showScore: Boolean,
                          private val nightMode: Boolean) : PagerAdapter() {

    private var pageCreated: ((View, Int) -> Unit)? = null
    private val views = ConcurrentHashMap<Int, View>()

    fun onPageCreated(handler: ((v: View, i: Int) -> Unit)) : PsalterPagerAdapter {
        pageCreated = handler
        return this
    }

    override fun instantiateItem(collection: ViewGroup, position: Int): Any {
        try {
            Logger.d("Viewpager building page $position")
            val psalter = psalterDb.getIndex(position)!!
            val inflater = LayoutInflater.from(context)
            val layout = inflater.inflate(R.layout.psalter_layout, collection, false) as ViewGroup

            context.launch { setScoreAndLyrics(psalter, layout) }

            layout.tvPagerHeading.text = psalter.heading
            layout.tvPagerPsalm.text = HtmlCompat.fromHtml(psalter.subtitleLink, HtmlCompat.FROM_HTML_MODE_LEGACY)
            layout.tvPagerPsalm.movementMethod = LinkMovementMethod.getInstance()

            views[position] = layout
            collection.addView(layout)

            pageCreated?.invoke(layout, position)
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

    private suspend fun setScoreAndLyrics(psalter: Psalter, layout: View){
        layout.tvPagerLyrics.text = psalter.lyrics
        if (showScore) {
            layout.scoreProgress.show()
            val score = psalterDb.getScore(psalter)
            if (score != null) {
                if (nightMode) score.invertColors()
                layout.imgScore.setImageDrawable(score)
                layout.scoreProgress.hide()

                val lyricStartIndex = psalter.lyrics.indexOf((psalter.numVersesInsideStaff + 1).toString() + ". ")
                layout.tvPagerLyrics.text = if (lyricStartIndex < 0) "" else psalter.lyrics.substring(lyricStartIndex)
            }
        }
        else layout.imgScore.setImageDrawable(null)
    }

    fun toggleScore()  {
        showScore = !showScore
        for((i, view) in views){
            context.launch { setScoreAndLyrics(psalterDb.getIndex(i)!!, view) }
        }
    }


}
