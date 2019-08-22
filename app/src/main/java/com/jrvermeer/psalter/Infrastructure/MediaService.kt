package com.jrvermeer.psalter.Infrastructure

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.support.v4.app.NotificationManagerCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaButtonReceiver
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import android.support.v4.media.app.NotificationCompat.MediaStyle
import android.util.Log
import android.widget.Toast

import com.jrvermeer.psalter.Core.Contracts.IPsalterRepository
import com.jrvermeer.psalter.Core.Models.Psalter
import com.jrvermeer.psalter.Core.duck
import com.jrvermeer.psalter.Core.shortToast
import com.jrvermeer.psalter.Core.unduck
import com.jrvermeer.psalter.UI.MainActivity
import com.jrvermeer.psalter.R

/**
 * Created by Jonathan on 4/3/2017.
 */

class MediaService : Service(), AudioManager.OnAudioFocusChangeListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {
    companion object {
        private const val MS_BETWEEN_VERSES = 700
        private const val NOTIFICATION_ID = 1234
        private const val NOTIFICATION_CHANNEL_ID = "DefaultChannel"
        private const val NOTIFICATION_CHANNEL_NAME = "Playback Notification"

        private const val ACTION_DELETE = "ACTION_DELETE"
    }

    private lateinit var binder: MediaServiceBinder
    private lateinit var psalterRepository: IPsalterRepository
    private lateinit var audioManager: AudioManager
    private lateinit var notificationManager: NotificationManagerCompat
    private lateinit var mediaSession: MediaSessionCompat
    private val mediaPlayer = MediaPlayer()
    private val handler = Handler()

    private var audioFocusRequest: AudioFocusRequest? = null
    private var psalter: Psalter? = null
    private var currentVerse = 1
    private var resumePlaybackOnFocusGain = false

    override fun onCreate() {
        Logger.d("MediaService created")
        psalterRepository = PsalterDb(this)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        notificationManager = NotificationManagerCompat.from(this)
        createNotificationChannel()
        mediaPlayer.setOnCompletionListener(this)

        mediaSession = MediaSessionCompat(this, "MediaService")
        mediaSession.setCallback(mMediaSessionCallback)
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)

        binder = MediaServiceBinder(mediaSession)

        updatePlaybackState(PlaybackStateCompat.STATE_NONE)
        registerReceiver(becomingNoisyReceiver, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Logger.d("MediaService started")
        if(intent?.action == ACTION_DELETE) stopSelf()
        else MediaButtonReceiver.handleIntent(mediaSession, intent)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        Logger.d("MediaService destroyed, releasing resources.")
        mediaPlayer.release()
        mediaSession.release()
        mMediaSessionCallback
        unregisterReceiver(becomingNoisyReceiver)
    }

    private val notification: Notification
        get() {
            val iOpenActivity = Intent(this, MainActivity::class.java)
            val pendingOpenActivity = PendingIntent.getActivity(this, 0, iOpenActivity, PendingIntent.FLAG_UPDATE_CURRENT)

            val iDelete = Intent(this, MediaService::class.java).setAction(ACTION_DELETE)
            val pendingDelete = PendingIntent.getService(this, 0, iDelete, PendingIntent.FLAG_UPDATE_CURRENT)

            var numActions = 0
            val builder = NotificationCompat.Builder(this@MediaService, NOTIFICATION_CHANNEL_ID)
            val metadata = mediaSession.controller.metadata
            builder.setSmallIcon(R.drawable.ic_smallicon)
                    .setLargeIcon(psalterRepository.getScore(psalter!!)!!.bitmap)
                    .setContentTitle(metadata.getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE))
                    .setContentText(metadata.getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION))
                    .setSubText(metadata.getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE))
                    .setContentIntent(pendingOpenActivity)
                    .setColor(ContextCompat.getColor(this, R.color.colorAccent))
                    .setShowWhen(false)
                    .setDeleteIntent(pendingDelete)
            if (mediaSession.controller.playbackState.state == PlaybackStateCompat.STATE_PLAYING) {
                builder.addAction(R.drawable.ic_stop_white_36dp, "Stop", MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_STOP))
                        .priority = NotificationCompat.PRIORITY_HIGH
            } else {
                builder.addAction(R.drawable.ic_play_arrow_white_36dp, "Play", MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PLAY))
            }
            numActions++

            if (binder.isShuffling) {
                builder.addAction(R.drawable.ic_skip_next_white_36dp, "Next", MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_NEXT))
                numActions++
            }
            builder.setStyle(MediaStyle()
                    .setShowActionsInCompactView(*(0 until numActions).toList().toIntArray()))

            return builder.build()
        }

    private val becomingNoisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (binder.isPlaying) binder.pause()
        }
    }

    private val mMediaSessionCallback = object : MediaSessionCompat.Callback() {
        override fun onPlay() {
            if (mediaSession.controller.playbackState.state == PlaybackStateCompat.STATE_PAUSED && audioFocusGranted()) {
                mediaPlayer.start()
                updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
                updateNotification()
            } else if (psalter != null) {
                playPsalter(psalter, currentVerse)
            }
        }

        override fun onStop() {
            if (mediaPlayer.isPlaying) { //between verses this could be false
                mediaPlayer.stop()
            }
            val state = mediaSession.controller.playbackState.state
            if (state == PlaybackStateCompat.STATE_PLAYING || state == PlaybackStateCompat.STATE_PAUSED) {
                playbackEnded()
            }
        }

        override fun onSetShuffleMode(shuffleMode: Int) {
            Logger.d("Setting shuffle mode $shuffleMode")
            mediaSession.setShuffleMode(shuffleMode)
            if (shuffleMode == PlaybackStateCompat.SHUFFLE_MODE_ALL) {
                shortToast("Shuffling")
            }
        }

        override fun onPause() {
            if (mediaSession.controller.playbackState.state == PlaybackStateCompat.STATE_PLAYING) {
                if (mediaPlayer.isPlaying) {
                    mediaPlayer.pause()
                }
                updatePlaybackState(PlaybackStateCompat.STATE_PAUSED)
                updateNotification()
            }
        }

        override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
            playPsalter(psalterRepository.getIndex(Integer.valueOf(mediaId!!)), 1)
        }

        override fun onPlayFromSearch(query: String?, extras: Bundle?) {
            val hits = psalterRepository.searchPsalter(query!!)
            if (hits.isNotEmpty()) {
                playPsalter(hits[0], 1)
            }
        }

        override fun onSkipToNext() {
            playPsalter(psalterRepository.getRandom(), 1)
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return binder
    }

    private fun playPsalter(psalter: Psalter?, currentVerse: Int): Boolean {
        try {
            if (audioFocusGranted()) {
                this.psalter = psalter
                this.currentVerse = currentVerse
                mediaPlayer.reset()
                val afd = psalterRepository.getAudioDescriptor(psalter!!)
                if (afd == null) {
                    if (binder.isShuffling) binder.skipToNext()
                    return false
                }
                mediaPlayer.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                mediaPlayer.prepare()
                mediaPlayer.start()
                updateMetaData()
                updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
                startForeground(NOTIFICATION_ID, notification)
                mediaSession.isActive = true
                return true
            } else
                return false
        } catch (ex: Exception) {
            Logger.e("error playing psalter", ex)
            return false
        }
    }

    private fun playNextVerse(): Boolean {
        if (currentVerse < psalter!!.numverses) {
            handler.postDelayed({
                if (binder.isPlaying) { // media could have been stopped between verses
                    currentVerse++
                    updateMetaData()
                    mediaPlayer.start()
                    startForeground(NOTIFICATION_ID, notification)
                }
            }, MS_BETWEEN_VERSES.toLong())
            return true
        }
        return false
    }

    override fun onCompletion(mediaPlayer: MediaPlayer) {
        if (!playNextVerse()) {
            if (binder.isShuffling)
                binder.skipToNext()
            else
                playbackEnded()
        }
    }

    private fun playbackEnded() {
        Log.d("Psalter", "Playback Ended")
        updatePlaybackState(PlaybackStateCompat.STATE_STOPPED)
        currentVerse = 1
        updateMetaData()
        updateNotification()
        abandonAudioFocus()
        mediaSession.isActive = false
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.abandonAudioFocusRequest(audioFocusRequest!!)
        } else {
            audioManager.abandonAudioFocus(this)
        }
    }

    override fun onAudioFocusChange(i: Int) {
        when(i){
            AudioManager.AUDIOFOCUS_LOSS -> {
                resumePlaybackOnFocusGain = false
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
                    binder.pause()
                }
            }
        }
    }

    private fun updatePlaybackState(state: Int) {
        var actions = PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID or PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH
        if (binder.isShuffling) actions = actions or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
        when (state) {
            PlaybackStateCompat.STATE_PAUSED -> actions = (actions
                    or PlaybackStateCompat.ACTION_PLAY
                    or PlaybackStateCompat.ACTION_STOP)
            PlaybackStateCompat.STATE_STOPPED -> actions = actions or PlaybackStateCompat.ACTION_PLAY
            PlaybackStateCompat.STATE_PLAYING -> actions = (actions or PlaybackStateCompat.ACTION_PAUSE or PlaybackStateCompat.ACTION_STOP)
        }
        mediaSession.setPlaybackState(PlaybackStateCompat.Builder()
                .setState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1f)
                .setActions(actions)
                .build())
    }

    private fun updateMetaData() {
        val title = psalter!!.title + " - " + psalter!!.heading
        mediaSession.setMetadata(MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, psalter!!.id.toString())
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, title)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, psalter!!.subtitleText)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, "The Psalter")
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, psalter!!.subtitleText)
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION, "Verse $currentVerse of ${psalter!!.numverses}")
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, psalterRepository.getScore(psalter!!)!!.bitmap)
                .build())
    }

    private fun updateNotification() {
        if (!binder.isPlaying) {
            stopForeground(false)
        }
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    override fun onError(mediaPlayer: MediaPlayer, what: Int, extra: Int): Boolean {
        mediaPlayer.reset()
        Logger.e("MediaPlayer error. ($what, $extra)", null)
        return false
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun audioFocusGranted(): Boolean {
        val requestResult: Int
        if (Build.VERSION.SDK_INT >= 26) {
            if (audioFocusRequest == null) {
                audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).run {
                    setOnAudioFocusChangeListener(this@MediaService)
                    setAudioAttributes(AudioAttributes.Builder().run {
                        setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        setUsage(AudioAttributes.USAGE_MEDIA)
                        build()
                    })
                    build()
                }
            }
            requestResult = audioManager.requestAudioFocus(audioFocusRequest!!)
        }
        else requestResult = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        return requestResult == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }
}
