package com.jrvermeer.psalter.helpers

import android.media.MediaPlayer
import java.lang.Exception

class SafeMediaPlayer : MediaPlayer() {
    override fun isPlaying(): Boolean {
        return try { super.isPlaying() }
        catch (ex: Exception) { false }
    }
}