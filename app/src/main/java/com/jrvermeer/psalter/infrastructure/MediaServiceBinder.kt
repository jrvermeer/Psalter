package com.jrvermeer.psalter.infrastructure

import android.content.Context
import android.content.Intent
import android.os.Binder
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.jrvermeer.psalter.models.Psalter

class MediaServiceBinder(private val mediaSession: MediaSessionCompat) : Binder() {

    private val player = mediaSession.controller.transportControls

    val isPlaying get() = mediaSession.controller.playbackState?.state == PlaybackStateCompat.STATE_PLAYING
    val isShuffling get() = mediaSession.controller.shuffleMode == PlaybackStateCompat.SHUFFLE_MODE_ALL
    val currentMediaId get() = mediaSession.controller.metadata.description.mediaId?.toInt()

    fun play() { player.play() }
    fun pause() { player.pause() }
    fun stop() { player.stop() }
    fun skipToNext() { player.skipToNext() }
    fun play(psalter: Psalter, shuffling: Boolean){
        player.setShuffleMode( if (shuffling) PlaybackStateCompat.SHUFFLE_MODE_ALL else PlaybackStateCompat.SHUFFLE_MODE_NONE)
        if(!isPlaying){
            player.playFromMediaId(psalter.id.toString(), null)
            Logger.playbackStarted(psalter.title, shuffling)
        }
    }
    fun startService(context: Context){
        context.startService(Intent(context, MediaService::class.java))
    }

    fun registerCallback(callback: MediaControllerCompat.Callback) {
        mediaSession.controller.registerCallback(callback)
        callback.onPlaybackStateChanged(mediaSession.controller.playbackState)
        callback.onMetadataChanged(mediaSession.controller.metadata)
    }
}