package com.jrvermeer.psalter.Core.Models

import android.provider.MediaStore

/**
 * Created by Jonathan on 3/27/2017.
 */

data class Psalter(
    var id: Int = 0,
    var number: Int = 0,
    var psalm: Int = 0,
    var title: String = "",
    var lyrics: String = "",
    var numverses: Int = 0,
    var numVersesInsideStaff: Int = 0,
    var heading: String = "",
    var audioFileName: String = "",
    var scoreFileName: String = ""
) {
    private val passage get() = if (psalm == 0) "Matthew+6:9-13" else "Psalm+$psalm";
    val subtitleText get() = if (psalm == 0) "Lords Prayer" else "Psalm $psalm"
    val subtitleLink get() = "<a href=https://www.biblegateway.com/passage?search=$passage>$subtitleText</a>"

}
