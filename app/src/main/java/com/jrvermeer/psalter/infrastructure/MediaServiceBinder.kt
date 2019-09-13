package com.jrvermeer.psalter.infrastructure

import android.content.Context
import android.content.Intent
import android.os.Binder
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.jrvermeer.psalter.models.MediaServiceCallbacks
import com.jrvermeer.psalter.models.Psalter

class MediaServiceBinder(private val mediaSession: MediaSessionCompat) : Binder() {
    private val _player = mediaSession.controller.transportControls
    private var _callbacks: MediaServiceCallbacks? = null

    val isPlaying get() = mediaSession.controller.playbackState?.state == PlaybackStateCompat.STATE_PLAYING
    val isShuffling get() = mediaSession.controller.shuffleMode == PlaybackStateCompat.SHUFFLE_MODE_ALL
    val currentMediaId get() = mediaSession.controller.metadata.description.mediaId?.toInt()

    fun play() { _player.play() }
    fun pause() { _player.pause() }
    fun stop() { _player.stop() }
    fun skipToNext() { _player.skipToNext() }
    fun play(psalter: Psalter, shuffling: Boolean){
        _player.setShuffleMode( if (shuffling) PlaybackStateCompat.SHUFFLE_MODE_ALL else PlaybackStateCompat.SHUFFLE_MODE_NONE)
        if(!isPlaying) _player.playFromMediaId(psalter.id.toString(), null)
    }
    fun startService(context: Context){
        context.startService(Intent(context, MediaService::class.java))
    }

    fun registerCallbacks(callbacks: MediaServiceCallbacks) {
        _callbacks = callbacks
        mediaSession.controller.registerCallback(callbacks)
        callbacks.onPlaybackStateChanged(mediaSession.controller.playbackState)
        callbacks.onMetadataChanged(mediaSession.controller.metadata)
    }
    fun unregisterCallbacks(){
        _callbacks?.let { mediaSession.controller.unregisterCallback(it) }
        _callbacks = null
    }

    fun onAudioUnavailable(psalter: Psalter) {_callbacks?.onAudioUnavailable(psalter) }
    fun onBeginShuffling() { _callbacks?.onBeginShuffling() }
}
