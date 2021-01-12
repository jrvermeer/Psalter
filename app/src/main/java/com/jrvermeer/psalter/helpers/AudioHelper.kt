package com.jrvermeer.psalter.helpers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import androidx.media.AudioAttributesCompat
import androidx.media.AudioFocusRequestCompat
import androidx.media.AudioManagerCompat
import com.jrvermeer.psalter.infrastructure.IPlayer

import com.jrvermeer.psalter.infrastructure.Logger
import com.jrvermeer.psalter.infrastructure.MediaServiceBinder

class AudioHelper(context: Context, private val binder: MediaServiceBinder, private val mediaPlayer: IPlayer): AudioManager.OnAudioFocusChangeListener {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var resumePlaybackOnFocusGain = false
    private val audioFocusRequest = AudioFocusRequestCompat.Builder(AudioManagerCompat.AUDIOFOCUS_GAIN).run {
        setOnAudioFocusChangeListener(this@AudioHelper)
        setAudioAttributes(AudioAttributesCompat.Builder().run {
            setContentType(AudioAttributesCompat.CONTENT_TYPE_MUSIC)
            setUsage(AudioAttributesCompat.USAGE_MEDIA)
            build()
        })
        build()
    }

    // get notified when playback is about to go through phone speakers. Users expect audio to pause when this happens
    val becomingNoisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (binder.isPlaying) {
                Logger.d("Pausing from BroadcastReceiver (${intent.action})")
                binder.pause()
            }
        }
    }

    fun focusGranted(): Boolean {
        return AudioManagerCompat.requestAudioFocus(audioManager, audioFocusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }
    fun abandonFocus() {
        AudioManagerCompat.abandonAudioFocusRequest(audioManager, audioFocusRequest)
    }

    override fun onAudioFocusChange(i: Int) {
        when(i){
            AudioManager.AUDIOFOCUS_LOSS -> {
                resumePlaybackOnFocusGain = false
                Logger.d("Pausing from AudioFocusChange (AUDIOFOCUS_LOSS)")
                binder.pause()
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                mediaPlayer.unduck()
                if (resumePlaybackOnFocusGain && !binder.isPlaying) {
                    binder.play()
                    resumePlaybackOnFocusGain = false
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> mediaPlayer.duck()
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                if (binder.isPlaying) {
                    resumePlaybackOnFocusGain = true
                    Logger.d("Pausing from AudioFocusChange (AUDIOFOCUS_LOSS_TRANSIENT)")
                    binder.pause()
                }
            }
        }
    }
}