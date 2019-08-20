package com.jrvermeer.psalter.Core.Models

import java.util.ArrayList

/**
 * Created by Jonathan on 9/4/2017.
 */

class SqLiteQuery {
    var queryText: String? = null

    val parameters: Array<String>
        get() = queryParameters.toTypedArray()

    private val queryParameters: MutableList<String>

    init {
        queryParameters = ArrayList()
    }

    fun addParameter(value: String) {
        queryParameters.add(value)
    }
}
