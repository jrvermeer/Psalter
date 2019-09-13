package com.jrvermeer.psalter.models

import android.support.v4.media.session.MediaControllerCompat

abstract class MediaServiceCallbacks : MediaControllerCompat.Callback() {
    open fun onAudioUnavailable(psalter: Psalter) { }
    open fun onBeginShuffling() { }
}