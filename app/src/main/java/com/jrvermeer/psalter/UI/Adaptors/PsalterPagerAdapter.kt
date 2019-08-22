package com.jrvermeer.psalter.UI.Adaptors

import android.content.Context
import android.support.v4.text.HtmlCompat
import android.support.v4.view.PagerAdapter
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.jrvermeer.psalter.Core.Contracts.IPsalterRepository
import com.jrvermeer.psalter.Core.invertColors
import com.jrvermeer.psalter.Infrastructure.Logger
import com.jrvermeer.psalter.R
import kotlinx.android.synthetic.main.psalter_layout.view.*

/**
 * Created by Jonathan on 3/27/2017.
 */

class PsalterPagerAdapter(private val context: Context,
                          private val psalterRepository: IPsalterRepository,
                          private var showScore: Boolean,
                          private val nightMode: Boolean) : PagerAdapter() {

    private var pageCreated: ((View, Int) -> Unit)? = null

    fun onPageCreated(handler: ((v: View, i: Int) -> Unit)) : PsalterPagerAdapter {
        pageCreated = handler
        return this
    }

    override fun instantiateItem(collection: ViewGroup, position: Int): Any {
        try {
            val psalter = psalterRepository.getIndex(position)!!
            val inflater = LayoutInflater.from(context)
            val layout = inflater.inflate(R.layout.psalter_layout, collection, false) as ViewGroup

            var text = psalter.lyrics
            if (showScore) {
                val score = psalterRepository.getScore(psalter)
                if (score != null) {
                    if (nightMode) score.invertColors()
                    layout.imgScore.setImageDrawable(score)
                    val lyricStartIndex = psalter.lyrics.indexOf((psalter.numVersesInsideStaff + 1).toString() + ". ")
                    if (lyricStartIndex > 0) text = psalter.lyrics.substring(lyricStartIndex)
                }
            }
            layout.tvPagerLyrics.text = text
            layout.tvPagerHeading.text = psalter.heading
            layout.tvPagerPsalm.text = HtmlCompat.fromHtml(psalter.subtitleLink, HtmlCompat.FROM_HTML_MODE_LEGACY)
            layout.tvPagerPsalm.movementMethod = LinkMovementMethod.getInstance()
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
        if (item is View) container.removeView(item)
    }

    override fun getCount(): Int {
        return psalterRepository.getCount()
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return psalterRepository.getIndex(position)?.title ?: "?"
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view === `object`
    }

    fun toggleScore() {
        showScore = !showScore
        notifyDataSetChanged()
    }


}
