package com.jrvermeer.psalter;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.media.app.NotificationCompat.MediaStyle;
import android.widget.Toast;

import com.jrvermeer.psalter.Models.Psalter;

import java.util.Random;

/**
 * Created by Jonathan on 4/3/2017.
 */

public class MediaService extends Service implements AudioManager.OnAudioFocusChangeListener, MediaPlayer.OnCompletionListener {
    private MediaBinder mBinder = new MediaBinder();

    private PsalterDb db;
    private AudioManager audioManager;
    private AudioFocusRequest audioFocusRequest = null;
    private NotificationManagerCompat notificationManager;
    private MediaPlayer mediaPlayer = new MediaPlayer();
    private Random rand = new Random();
    private Handler handler = new Handler();

    private MediaSessionCompat mediaSession;
    private MediaControllerCompat.TransportControls controls;

    private Psalter psalter;
    private int currentVerse = 1;
    private boolean resumePlaybackOnFocusGain = false;

    private static final int MS_BETWEEN_VERSES = 700;
    private static final int NOTIFICATION_ID = 1234;
    private static String NOTIFICATION_CHANNEL_ID = "DefaultChannel";
    private static String NOTIFICATION_CHANNEL_NAME = "Playback Notification";

    private static final String ACTION_STOP = "ACTION_STOP";
    private static final String ACTION_NEXT = "ACTION_NEXT";
    private static final String ACTION_PLAY = "ACTION_PLAY";

    @Override
    public void onCreate() {
        audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        notificationManager = NotificationManagerCompat.from(this);
        createNotificationChannel();
        db = new PsalterDb(this);
        mediaPlayer.setOnCompletionListener(this);
        mediaSession = new MediaSessionCompat(this, "MediaService");
        mediaSession.setCallback(mMediaSessionCallback);
        mediaSession.setFlags( MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS );
        controls = mediaSession.getController().getTransportControls();
        updatePlaybackState(PlaybackStateCompat.STATE_NONE);
    }

    @Override
    public void onDestroy(){
        mediaPlayer.release();
        notificationManager.cancelAll();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent != null ){
            String action = intent.getAction();
            if(action != null){
                if(action.equals(ACTION_STOP)){
                    controls.stop();
                }
                else if(action.equals(ACTION_NEXT)){
                    controls.skipToNext();
                }
                else if(action.equals(ACTION_PLAY)){
                    controls.play();
                }
            }
            else MediaButtonReceiver.handleIntent(mediaSession, intent);
        }
        stopSelf();
        return START_NOT_STICKY;
    }
    private boolean playPsalter(Psalter psalter, int currentVerse){
        try{
            if(audioFocusGranted()) {
                this.psalter = psalter;
                this.currentVerse = currentVerse;
                mediaPlayer.reset();
                AssetFileDescriptor afd = getAssetFileDescriptor(psalter);
                mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                afd.close();
                mediaPlayer.prepare();
                mediaPlayer.start();
                updateMetaData();
                updatePlaybackState(PlaybackStateCompat.STATE_PLAYING);
                startForeground(NOTIFICATION_ID, getNotification());
                mediaSession.setActive(true);
                registerReceiver(becomingNoisyReceiver, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
                return true;
            }
            else return false;
        }
        catch(Exception ex) {
            return false;
        }
    }
    private boolean playNextVerse(){
        if(currentVerse < psalter.getNumverses()){
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if(getPlaybackState() == PlaybackStateCompat.STATE_PLAYING){ // media could have been stopped between verses
                        currentVerse++;
                        updateMetaData();
                        mediaPlayer.start();
                        startForeground(NOTIFICATION_ID, getNotification());
                    }
                }
            }, MS_BETWEEN_VERSES);
            return true;
        }
        return false;
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        if(!playNextVerse()){
            if(mediaSession.getController().getShuffleMode() == PlaybackStateCompat.SHUFFLE_MODE_ALL){
                controls.skipToNext();
            }
            else playbackEnded();
        }
    }

    private AssetFileDescriptor getAssetFileDescriptor(Psalter psalter) {
        int resID = getResources().getIdentifier(psalter.getAudioFileName(), "raw", getPackageName());
        return getApplicationContext().getResources().openRawResourceFd(resID);
    }

    private void playbackEnded(){
        stopForeground(false);
        unregisterReceiver(becomingNoisyReceiver);
        updatePlaybackState(PlaybackStateCompat.STATE_STOPPED);
        currentVerse = 1;
        updateMetaData();
        updateNotification();
        abandonAudioFocus();
        mediaSession.setActive(false);
    }

    private int getPlaybackState(){
        return mediaSession.getController().getPlaybackState().getState();
    }

    public boolean audioFocusGranted(){
        int requestResult;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            if(audioFocusRequest == null){
                audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                        .setOnAudioFocusChangeListener(this)
                        .setAudioAttributes(new AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build())
                        .build();
            }
            requestResult = audioManager.requestAudioFocus(audioFocusRequest);
        }
        else{
            requestResult = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        }
        return  requestResult == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }
    public void abandonAudioFocus(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            audioManager.abandonAudioFocusRequest(audioFocusRequest);
        }
        else{
            audioManager.abandonAudioFocus(this);
        }

    }

    @Override
    public void onAudioFocusChange(int i) {
        if(i == AudioManager.AUDIOFOCUS_LOSS){
            resumePlaybackOnFocusGain = false;
            controls.pause();
        }
        else if(i == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT){
            // having focus doesn't mean media is playing. only resume playback on focus regained if media was playing originally.
            if(getPlaybackState() == PlaybackStateCompat.STATE_PLAYING){
                resumePlaybackOnFocusGain = true;
                controls.pause();
            }
        }
        else if(i == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK){
            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.O) { //ducking is handled by the system in Oreo
                mediaPlayer.setVolume(0.1f, 0.1f);
            }
        }
        else if(i == AudioManager.AUDIOFOCUS_GAIN){
            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.O) { //ducking is handled by the system in Oreo
                mediaPlayer.setVolume(1f, 1f);
            }
            if(resumePlaybackOnFocusGain && getPlaybackState() != PlaybackStateCompat.STATE_PLAYING){
                controls.play();
                resumePlaybackOnFocusGain = false;
            }
        }
    }
    private Notification getNotification() {
        Intent openActivity = new Intent(this, MainActivity.class);
        PendingIntent openActivityOnTouch = PendingIntent.getActivity(this, 0, openActivity, PendingIntent.FLAG_UPDATE_CURRENT);

        int numActions = 0;
        NotificationCompat.Builder builder = new NotificationCompat.Builder(MediaService.this, NOTIFICATION_CHANNEL_ID);
        MediaMetadataCompat metadata = mediaSession.getController().getMetadata();
        builder.setSmallIcon(R.drawable.ic_smallicon)
                .setContentTitle(metadata.getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE))
                .setContentText(metadata.getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION))
                .setSubText(metadata.getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE))
                .setContentIntent(openActivityOnTouch)
                .setShowWhen(false);
        if(getPlaybackState() == PlaybackStateCompat.STATE_PLAYING){
            Intent stopPlayback = new Intent(this, MediaService.class).setAction(ACTION_STOP);
            PendingIntent stopPlaybackOnTouch = PendingIntent.getService(this, 1, stopPlayback, PendingIntent.FLAG_UPDATE_CURRENT);
            builder.addAction(R.drawable.ic_stop_white_36dp, "Stop", stopPlaybackOnTouch)
                .setPriority(NotificationCompat.PRIORITY_HIGH);
        }
        else{
            Intent startPlayback = new Intent(this, MediaService.class).setAction(ACTION_PLAY);
            PendingIntent startPlaybackOnTouch = PendingIntent.getService(this, 2, startPlayback, PendingIntent.FLAG_UPDATE_CURRENT);
            builder.addAction(R.drawable.ic_play_arrow_white_36dp, "Play", startPlaybackOnTouch);
        }
        numActions++;

        if(mediaSession.getController().getShuffleMode() == PlaybackStateCompat.SHUFFLE_MODE_ALL){ // todo: use transport actions
            Intent playNext = new Intent(this, MediaService.class).setAction(ACTION_NEXT);
            PendingIntent playNextOnTouch = PendingIntent.getService(this, 3, playNext, PendingIntent.FLAG_UPDATE_CURRENT);

            builder.addAction(R.drawable.ic_skip_next_white_36dp, "Next", playNextOnTouch);
            numActions++;
        }
        builder.setStyle(new MediaStyle()
                .setShowActionsInCompactView(Util.CreateRange(0, numActions - 1)));

        return  builder.build();
    }

    private void updatePlaybackState(int state){
        long actions = PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID | PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH;
        if(mediaSession.getController().getShuffleMode() == PlaybackStateCompat.SHUFFLE_MODE_ALL){
            actions = actions | PlaybackStateCompat.ACTION_SKIP_TO_NEXT;
        }
        if(state == PlaybackStateCompat.STATE_PAUSED){
            actions = actions
                    | PlaybackStateCompat.ACTION_PLAY
                    | PlaybackStateCompat.ACTION_STOP;
        }
        else if(state == PlaybackStateCompat.STATE_STOPPED){
            actions = actions | PlaybackStateCompat.ACTION_PLAY;

        }
        else if(state == PlaybackStateCompat.STATE_PLAYING){
            actions = PlaybackStateCompat.ACTION_PAUSE
                    | PlaybackStateCompat.ACTION_STOP;
        }
        mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                .setState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1)
                .setActions(actions)
                .build());
    }
    private void updateMetaData(){
        mediaSession.setMetadata(new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, String.valueOf(psalter.getNumber()))
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, "#" + psalter.getNumber() + " - " + psalter.getHeading())
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, psalter.getSubtitleText())
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION, String.format("Verse %d of %d", currentVerse, psalter.getNumverses()))
                .build());
    }
    private void updateNotification(){
        notificationManager.notify(NOTIFICATION_ID, getNotification());
    }

    private BroadcastReceiver becomingNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(AudioManager.ACTION_AUDIO_BECOMING_NOISY)){
                controls.pause();
            }
        }
    };

    private MediaSessionCompat.Callback mMediaSessionCallback = new MediaSessionCompat.Callback() {

        @Override
        public void onPlay() {
            if(getPlaybackState() == PlaybackStateCompat.STATE_PAUSED && audioFocusGranted()){
                mediaPlayer.start();
                updatePlaybackState(PlaybackStateCompat.STATE_PLAYING);
                updateNotification();
            }
            else if(psalter != null){
                playPsalter(psalter, currentVerse);
            }

        }

        @Override
        public void onStop() {
            if(mediaPlayer.isPlaying()) { //between verses this could be false
                mediaPlayer.stop();
            }

            int state = getPlaybackState();
            if(state == PlaybackStateCompat.STATE_PLAYING || state == PlaybackStateCompat.STATE_PAUSED){
                playbackEnded();
            }
        }

        @Override
        public void onSetShuffleMode(int shuffleMode) {
            mediaSession.setShuffleMode(shuffleMode);
            if(shuffleMode == PlaybackStateCompat.SHUFFLE_MODE_ALL){
                Toast.makeText(MediaService.this, "Shuffling", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onPause() {
            if(getPlaybackState() == PlaybackStateCompat.STATE_PLAYING) {
                if(mediaPlayer.isPlaying()){
                    mediaPlayer.pause();
                }
                updatePlaybackState(PlaybackStateCompat.STATE_PAUSED);
                updateNotification();
            }
        }

        @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras) {
            playPsalter(db.getPsalter(Integer.valueOf(mediaId)), 1);
        }

        @Override
        public void onPlayFromSearch(String query, Bundle extras) {
            Psalter[] hits = db.searchPsalter(query);
            if(hits.length > 0){
                playPsalter(hits[0], 1);
            }
        }

        @Override
        public void onSkipToNext() {
            int nextIndex = rand.nextInt(db.getCount());
            playPsalter(db.getPsalter(nextIndex + 1), 1);
        }
    };

    public class MediaBinder extends Binder{
        public MediaSessionCompat.Token getSessionToken(){
            return mediaSession.getSessionToken(); }
    }

    private void createNotificationChannel(){
        if(Build.VERSION.SDK_INT >= 26){
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
            manager.createNotificationChannel(channel);
        }
    }
}
