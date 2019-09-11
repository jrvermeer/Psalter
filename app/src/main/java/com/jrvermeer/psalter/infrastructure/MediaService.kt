package com.jrvermeer.psalter.infrastructure

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.KeyEvent
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.media.app.NotificationCompat.MediaStyle

import com.jrvermeer.psalter.models.Psalter
import com.jrvermeer.psalter.ui.MainActivity
import com.jrvermeer.psalter.R
import com.jrvermeer.psalter.helpers.AudioHelper
import com.jrvermeer.psalter.helpers.StorageHelper
import com.jrvermeer.psalter.models.MessageLength
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Created by Jonathan on 4/3/2017.
 */

class MediaService : Service(), CoroutineScope by MainScope() {
    companion object {
        private const val MS_BETWEEN_VERSES = 700
        private const val NOTIFICATION_ID = 1234
        private const val NOTIFICATION_CHANNEL_ID = "DefaultChannel"
        private const val NOTIFICATION_CHANNEL_NAME = "Playback Notification"
        private const val MAX_RETRY_COUNT = 5

        // MediaButtonReceiver.buildMediaButtonPendingIntent doesn't work for instant apps?
        const val ACTION_SKIP_TO_NEXT = "ACTION_SKIP_TO_NEXT"
        const val ACTION_PLAY = "ACTION_PLAY"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_DELETE = "ACTION_DELETE"
    }

    private lateinit var binder: MediaServiceBinder
    private lateinit var audioHelper: AudioHelper
    private lateinit var psalterDb: PsalterDb
    private lateinit var notificationManager: NotificationManagerCompat
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var storage: StorageHelper
    private val mutex = Mutex()
    private val mediaPlayer = MediaPlayer()
    private var psalter: Psalter? = null
    private var currentVerse = 1

    override fun onCreate() {
        Logger.d("MediaService created")
        psalterDb = PsalterDb(this, this)
        notificationManager = NotificationManagerCompat.from(this)
        createNotificationChannel()
        mediaPlayer.setOnCompletionListener { this@MediaService.mediaPlayerCompleted() }
        mediaPlayer.setOnErrorListener { mp, what, extra -> this@MediaService.mediaPlayerError(mp, what, extra) }

        mediaSession = MediaSessionCompat(this, "MediaService")
        mediaSession.setCallback(mMediaSessionCallback)

        binder = MediaServiceBinder(mediaSession)
        audioHelper = AudioHelper(this, binder, mediaPlayer)

        storage = StorageHelper(this)

        updatePlaybackState(PlaybackStateCompat.STATE_NONE)
        registerReceiver(audioHelper.becomingNoisyReceiver, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Logger.d("MediaService started (${intent?.action})")
        when(intent?.action) {
            ACTION_DELETE -> stopSelf()
            ACTION_STOP -> binder.stop()
            ACTION_PLAY -> binder.play()
            ACTION_SKIP_TO_NEXT -> {
                Logger.skipToNext(psalter)
                binder.skipToNext()
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        Logger.d("MediaService destroyed")
        mediaPlayer.release()
        mediaSession.release()
        unregisterReceiver(audioHelper.becomingNoisyReceiver)
        cancel()
    }


    private val mMediaSessionCallback = object : MediaSessionCompat.Callback() {
        override fun onPlay() {
            Logger.d("OnPlay: ${psalter?.title}")
            if (audioHelper.focusGranted()) {
                launch {
                    // can't transition from stopped to playing, must prepare
                    if(mediaSession.controller.playbackState.state == PlaybackStateCompat.STATE_STOPPED){
                        prepareNewPsalter(psalter)
                    }
                    updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
                    mediaPlayer.start()
                    startForeground(NOTIFICATION_ID, notification)
                    mediaSession.isActive = true
                }
            }
        }

        override fun onStop() {
            Logger.d("OnStop")
            if (mediaPlayer.isPlaying) {
                mediaPlayer.stop()
            }
            updatePlaybackState(PlaybackStateCompat.STATE_STOPPED)
            currentVerse = 1
            updateMetaData(psalter!!)
            updateNotification()
            audioHelper.abandonFocus()
            mediaSession.isActive = false
        }

        private fun showShuffleMessage(){
            binder.onBeginShuffling()
        }

        override fun onSetShuffleMode(shuffleMode: Int) {
            mediaSession.setShuffleMode(shuffleMode)
            if(binder.isPlaying) {
                updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
                updateNotification()
                if (binder.isShuffling) showShuffleMessage()
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
            val psalter = psalterDb.getIndex(mediaId?.toInt()!!) ?: return
            Logger.playbackStarted(psalter.title, binder.isShuffling)
            launch {
                if(prepareNewPsalter(psalter)) {
                    binder.play()
                    storage.playCount++
                    if(binder.isShuffling) showShuffleMessage()
                }
                else binder.onAudioUnavailable(psalter)
            }
        }

        // Gotcha: if user tries to SkipToNext multiple times before the first ones finish downloading, the audio and viewpager
        // can get out of sync really easily. So we need to hold on to current skipping job, and cancel it when a new one is started.
        var skipping: Job? = null
        override fun onSkipToNext() {
            launch {
                val oldJob = skipping
                skipping = this.coroutineContext[Job] // we need to update current job before cancelAndJoin
                oldJob?.cancelAndJoin() // cancel previous skip (if any) and wait for it to finish (should be really quick)
                var retryNum = 0
                while(retryNum++ < MAX_RETRY_COUNT && isActive) {
                    if(prepareNewPsalter(psalterDb.getRandom())) {
                        binder.play()
                        break
                    }
                }
            }
        }

        // we're only overriding this so we can log the skipToNext event
        override fun onMediaButtonEvent(mediaButtonEvent: Intent?): Boolean {
            (mediaButtonEvent?.getParcelableExtra(Intent.EXTRA_KEY_EVENT) as KeyEvent?)?.run {
                if(action == KeyEvent.ACTION_DOWN // action fires twice: button down && up.
                        && keyCode == KeyEvent.KEYCODE_MEDIA_NEXT // skip to next
                        && ((mediaSession.controller.playbackState.actions and PlaybackStateCompat.ACTION_SKIP_TO_NEXT) != 0L)) { // next is a valid action
                    Logger.skipToNext(psalter)
                }
            }
            return super.onMediaButtonEvent(mediaButtonEvent)
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return binder
    }

    private suspend fun prepareNewPsalter(psalter: Psalter?): Boolean {
        if(psalter == null) return false
        mediaPlayer.reset() // stop audio, we're going to a different page
        currentVerse = 1
        updateMetaData(psalter)
        val audioUri = psalter.loadAudio(psalterDb.downloader) ?: return false
        psalter.loadScore(psalterDb.downloader)
        this.psalter = psalter
        mutex.withLock { // cya: multiple coroutines could be trying to set datasource at the same time here, and who knows what state mediaPlayer is in after suspending
            mediaPlayer.reset()
            mediaPlayer.setDataSource(this, audioUri)
            mediaPlayer.prepare()
        }
        return true
    }

    private fun playNextVerse(): Boolean {
        if (currentVerse < psalter!!.numverses) {
            Handler().postDelayed({
                if (binder.isPlaying) { // media could have been stopped between verses
                    currentVerse++
                    updateMetaData(psalter!!)
                    binder.play()
                }
            }, MS_BETWEEN_VERSES.toLong())
            return true
        }
        return false
    }

    private fun mediaPlayerCompleted() {
        if (!playNextVerse()) {
            if (binder.isShuffling) binder.skipToNext()
            else binder.stop()
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

    private fun updateMetaData(psalter: Psalter) {
        val title = psalter.title + " - " + psalter.heading
        val builder = MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, psalter.id.toString())
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, title)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, psalter.subtitleText)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, "The Psalter")
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, psalter.subtitleText)
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION, "Verse $currentVerse of ${psalter.numverses}")
        if(psalter.score != null) builder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, psalter.score?.bitmap)

        mediaSession.setMetadata(builder.build())
    }

    private fun updateNotification() {
        if (!binder.isPlaying) {
            stopForeground(false)
        }
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    private val notification: Notification
        get() {
            val iOpenActivity = Intent(this, MainActivity::class.java)
            val pendingOpenActivity = PendingIntent.getActivity(this, 0, iOpenActivity, PendingIntent.FLAG_UPDATE_CURRENT)

            var numActions = 0
            val builder = NotificationCompat.Builder(this@MediaService, NOTIFICATION_CHANNEL_ID)
            val metadata = mediaSession.controller.metadata
            builder.setSmallIcon(R.drawable.ic_smallicon)
                    .setContentTitle(metadata.getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE))
                    .setContentText(metadata.getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION))
                    .setSubText(metadata.getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE))
                    .setContentIntent(pendingOpenActivity)
                    .setColor(ContextCompat.getColor(this, R.color.colorAccent))
                    .setShowWhen(false)
                    .setDeleteIntent(getPendingIntent(ACTION_DELETE, 0))
            if(psalter?.score != null) builder.setLargeIcon(psalter?.score?.bitmap)

            if (mediaSession.controller.playbackState.state == PlaybackStateCompat.STATE_PLAYING) {
                builder.addAction(R.drawable.ic_stop_white_36dp, "Stop", getPendingIntent(ACTION_STOP, 1))
                        .priority = NotificationCompat.PRIORITY_HIGH
            } else {
                builder.addAction(R.drawable.ic_play_arrow_white_36dp, "Play", getPendingIntent(ACTION_PLAY, 2))
            }
            numActions++

            if (binder.isShuffling) {
                builder.addAction(R.drawable.ic_skip_next_white_36dp, "Next", getPendingIntent(ACTION_SKIP_TO_NEXT, 3))
                numActions++
            }
            builder.setStyle(MediaStyle()
                    .setShowActionsInCompactView(*(0 until numActions).toList().toIntArray()))

            return builder.build()
        }

    private fun mediaPlayerError(mediaPlayer: MediaPlayer, what: Int, extra: Int): Boolean {
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

    private fun getPendingIntent(action: String?, i: Int): PendingIntent {
        val intent = Intent(this, MediaService::class.java).setAction(action)
        return PendingIntent.getService(this, i, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }
}
