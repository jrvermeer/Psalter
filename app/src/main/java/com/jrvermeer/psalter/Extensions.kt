package com.jrvermeer.psalter

import android.app.Activity
import android.content.Context
import android.graphics.ColorMatrixColorFilter
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.annotation.DrawableRes
import com.google.android.material.floatingactionbutton.FloatingActionButton
import android.view.View
import android.widget.Toast
import java.util.ArrayList

fun String.allIndexesOf(query: String): List<Int> {
    val rtn = ArrayList<Int>()
    var index = this.indexOf(query)
    while (index >= 0) {
        rtn.add(index)
        index = this.indexOf(query, index + 1)
    }
    return rtn
}

fun Drawable.invertColors(): Drawable {
    this.colorFilter = ColorMatrixColorFilter(floatArrayOf(
            -1.0f, 0f, 0f, 0f, 255f, // red
            0f, -1.0f, 0f, 0f, 255f, // green
            0f, 0f, -1.0f, 0f, 255f, // blue
            0f, 0f, 0f, 1.0f, 0f))  // alpha
    return this
}

fun Context.toast(message: String, length: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, length).show()
}

fun View.show(){
    this.visibility = View.VISIBLE
}
fun View.hide() {
    this.visibility = View.GONE
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