package com.jrvermeer.psalter.Core

import java.util.ArrayList

fun MutableList<Int>.swap(index1: Int, index2: Int) {
    val tmp = this[index1] // 'this' corresponds to the list
    this[index1] = this[index2]
    this[index2] = tmp
}

fun String.allIndexesOf(query: String): List<Int> {
    val rtn = ArrayList<Int>()
    var index = this.indexOf(query)
    while (index >= 0) {
        rtn.add(index)
        index = this.indexOf(query, index + 1)
    }
    return rtn
}