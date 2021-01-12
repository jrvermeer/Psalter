package com.jrvermeer.psalter.helpers

import android.media.MediaPlayer
import android.os.Build
import com.jrvermeer.psalter.infrastructure.IPlayer
import java.lang.Exception

class SafeMediaPlayer : MediaPlayer(), IPlayer {
    override fun isPlaying(): Boolean {
        return try { super.isPlaying() }
        catch (ex: Exception) { false }
    }

    override fun onComplete(listener: () -> Unit) {
        setOnCompletionListener { listener() }
    }

    override fun onError(listener: (what: Int, extra: Int) -> Unit) {
        setOnErrorListener { mp, what, extra -> listener(what, extra); return@setOnErrorListener true }
    }

    override fun duck() {
        if (Build.VERSION.SDK_INT < 26) {
            this.setVolume(0.1f, 0.1f)
        }
    }

    override fun unduck() {
        if (Build.VERSION.SDK_INT < 26) {
            this.setVolume(1f, 1f)
        }
    }
}