package com.jrvermeer.psalter.Infrastructure

import android.content.Context
import android.content.Intent
import android.os.Binder
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.jrvermeer.psalter.Core.Models.Psalter

class MediaServiceBinder(private val mediaSession: MediaSessionCompat) : Binder() {

    val isPlaying get() = mediaSession.controller.playbackState?.state == PlaybackStateCompat.STATE_PLAYING
    val isShuffling get() = mediaSession.controller.shuffleMode == PlaybackStateCompat.SHUFFLE_MODE_ALL
    val currentMediaId get() = mediaSession.controller.metadata.description.mediaId?.toInt()

    fun play() { mediaSession.controller.transportControls.play() }
    fun pause() { mediaSession.controller.transportControls.pause() }
    fun stop() { mediaSession.controller.transportControls.stop() }
    fun skipToNext() { mediaSession.controller.transportControls.skipToNext() }
    fun play(context: Context, psalter: Psalter, shuffling: Boolean){
       context.startService(Intent(context, MediaService::class.java)
                .setAction(MediaService.ACTION_START)
                .putExtra(MediaService.EXTRA_MEDIA_ID, psalter.id)
                .putExtra(MediaService.EXTRA_SHUFFLING, shuffling))
    }

    fun registerCallback(callback: MediaControllerCompat.Callback) {
        mediaSession.controller.registerCallback(callback)
        callback.onPlaybackStateChanged(mediaSession.controller.playbackState)
        callback.onMetadataChanged(mediaSession.controller.metadata)
    }
}