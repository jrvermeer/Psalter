package com.jrvermeer.psalter.Core.Contracts

import android.content.res.AssetFileDescriptor
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable

import com.jrvermeer.psalter.Core.Models.Psalter

/**
 * Created by Jonathan on 6/15/2018.
 */

interface IPsalterRepository {
    fun getCount(): Int
    fun getRandom(): Psalter
    fun getIndex(index: Int): Psalter?
    fun getPsalter(number: Int): Psalter?
    fun getPsalm(psalmNumber: Int): Array<Psalter>
    fun searchPsalter(searchText: String): Array<Psalter>
    fun getAudioDescriptor(psalter: Psalter): AssetFileDescriptor?
    fun getScore(psalter: Psalter): BitmapDrawable?

}
