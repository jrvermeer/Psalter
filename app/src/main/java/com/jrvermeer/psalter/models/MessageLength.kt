package com.jrvermeer.psalter.models

import com.google.android.material.snackbar.Snackbar

enum class MessageLength { Short, Long }

fun MessageLength.forSnack(): Int {
    return when(this){
        MessageLength.Long -> Snackbar.LENGTH_LONG
        MessageLength.Short -> Snackbar.LENGTH_SHORT
    }
}