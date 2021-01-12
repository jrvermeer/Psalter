package com.jrvermeer.psalter.infrastructure

import android.content.Context
import android.media.PlaybackParams
import android.net.Uri

interface IPlayer {
    fun start(): Unit
    fun stop(): Unit
    fun pause(): Unit
    fun isPlaying(): Boolean

    fun duck(): Unit
    fun unduck(): Unit

    fun setDataSource(context: Context, uri: Uri): Unit
    fun prepare(): Unit

    fun onComplete(listener: () -> Unit)
    fun onError(listener: (what: Int, extra: Int) -> Unit)

    fun release(): Unit
    fun reset(): Unit
}