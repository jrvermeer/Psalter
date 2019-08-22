package com.jrvermeer.psalter.Core

import android.app.Activity
import android.content.Context
import android.graphics.ColorMatrixColorFilter
import android.graphics.drawable.Drawable
import android.media.MediaPlayer
import android.os.Build
import android.support.annotation.DrawableRes
import android.support.design.widget.FloatingActionButton
import android.view.View
import android.widget.Toast
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

fun Drawable.invertColors() {
    this.colorFilter = ColorMatrixColorFilter(floatArrayOf(
            -1.0f, 0f, 0f, 0f, 255f, // red
            0f, -1.0f, 0f, 0f, 255f, // green
            0f, 0f, -1.0f, 0f, 255f, // blue
            0f, 0f, 0f, 1.0f, 0f))  // alpha
}

fun Context.shortToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}
fun Context.longToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}

fun View.show(){
    this.visibility = View.VISIBLE
}
fun View.hide() {
    this.visibility = View.GONE
}

//ducking is handled by the system in Oreo
fun MediaPlayer.duck(){
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
        this.setVolume(0.1f, 0.1f)
    }
}
fun MediaPlayer.unduck(){
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
        this.setVolume(1f, 1f)
    }
}

// framework bug, setting images fails after toggling night mode. https://stackoverflow.com/a/52158081
fun FloatingActionButton.setImageResourceSafe(@DrawableRes id: Int) {
    this.setImageResource(id)
    if (this.isShown) {
        this.hide()
        this.show()
    }
}


// framework bug in api 23 calling recreate inside onOptionsItemSelected.
fun Activity.recreateSafe() {
    if (Build.VERSION.SDK_INT == 23) {
        finish()
        startActivity(intent)
    }
    else recreate()
}