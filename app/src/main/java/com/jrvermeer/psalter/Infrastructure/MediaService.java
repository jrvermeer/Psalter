package com.jrvermeer.psalter.Infrastructure;

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
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.media.app.NotificationCompat.MediaStyle;
import android.util.Log;
import android.widget.Toast;

import com.jrvermeer.psalter.Core.Contracts.IPsalterRepository;
import com.jrvermeer.psalter.Core.Models.Psalter;
import com.jrvermeer.psalter.Core.Util;
import com.jrvermeer.psalter.UI.MainActivity;
import com.jrvermeer.psalter.R;

/**
 * Created by Jonathan on 4/3/2017.
 */

public class MediaService extends Service
        implements AudioManager.OnAudioFocusChangeListener,
            MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {
    private MediaServiceBinder binder;

    private IPsalterRepository psalterRepository;
    private AudioManager audioManager;
    private AudioFocusRequest audioFocusRequest = null;
    private NotificationManagerCompat notificationManager;
    private MediaPlayer mediaPlayer = new MediaPlayer();
    private Handler handler = new Handler();

    private MediaSessionCompat mediaSession;

    private Psalter psalter;
    private int currentVerse = 1;
    private boolean resumePlaybackOnFocusGain = false;

    private static final int MS_BETWEEN_VERSES = 700;
    private static final int NOTIFICATION_ID = 1234;
    private static String NOTIFICATION_CHANNEL_ID = "DefaultChannel";
    private static String NOTIFICATION_CHANNEL_NAME = "Playback Notification";

    public static final String ACTION_STOP = "ACTION_STOP"; // stop from notification
    public static final String ACTION_NEXT = "ACTION_NEXT"; // next # from notification
    public static final String ACTION_PLAY = "ACTION_PLAY"; // play (resume) from notification
    public static final String ACTION_DELETE = "ACTION_DELETE"; // notification removed

    public static final String ACTION_START = "ACTION_START"; // start new number from UI
    public static final String EXTRA_MEDIA_ID = "EXTRA_MEDIA_ID";
    public static final String EXTRA_SHUFFLING = "EXTRA_SHUFFLING";


    @Override
    public void onCreate() {
        Logger.d("MediaService created");
        psalterRepository = new PsalterDb(this);
        audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        notificationManager = NotificationManagerCompat.from(this);
        createNotificationChannel();
        mediaPlayer.setOnCompletionListener(this);

        mediaSession = new MediaSessionCompat(this, "MediaService");
        mediaSession.setCallback(mMediaSessionCallback);
        mediaSession.setFlags( MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS );

        binder = new MediaServiceBinder(mediaSession);

        updatePlaybackState(PlaybackStateCompat.STATE_NONE);
    }

    @Override
    public void onDestroy(){
        Logger.d("MediaService destroyed");
        mediaPlayer.release();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent != null ){
            String action = intent.getAction();
            Logger.d("MediaService started from intent. Action: " + action);
            if(action != null){
                if(action.equals(ACTION_STOP)) binder.stop();
                else if(action.equals(ACTION_NEXT)) binder.skipToNext();
                else if(action.equals(ACTION_PLAY)) binder.play();
                else if(action.equals(ACTION_DELETE)) stopSelf();
                else if(action.equals(ACTION_START)) {
                    int mediaId = intent.getIntExtra(EXTRA_MEDIA_ID, 0);
                    boolean shuffling = intent.getBooleanExtra(EXTRA_SHUFFLING, false);
                    mediaSession.getController().getTransportControls().setShuffleMode(shuffling ? PlaybackStateCompat.SHUFFLE_MODE_ALL : PlaybackStateCompat.SHUFFLE_MODE_NONE);
                    mediaSession.getController().getTransportControls().playFromMediaId(String.valueOf(mediaId), null);
                }
            }
            else MediaButtonReceiver.handleIntent(mediaSession, intent);
        }
        return START_NOT_STICKY;
    }


    private boolean playPsalter(Psalter psalter, int currentVerse){
        try{
            if(audioFocusGranted()) {
                this.psalter = psalter;
                this.currentVerse = currentVerse;
                mediaPlayer.reset();
                AssetFileDescriptor afd = psalterRepository.getAudioDescriptor(psalter);
                if(afd == null) {
                    if(isShuffling()) binder.skipToNext();
                    return false;
                }
                mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                afd.close();
                mediaPlayer.prepare();
                mediaPlayer.start();
                updateMetaData();
                updatePlaybackState(PlaybackStateCompat.STATE_PLAYING);
                startForeground(NOTIFICATION_ID, getNotification());
                mediaSession.setActive(true);
                return true;
            }
            else return false;
        }
        catch(Exception ex) {
            Logger.e("error playing psalter", ex);
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
            if(binder.isShuffling()) binder.skipToNext();
            else playbackEnded();
        }
    }
    private boolean isShuffling(){
        return mediaSession.getController().getShuffleMode() == PlaybackStateCompat.SHUFFLE_MODE_ALL;
    }

    private void playbackEnded(){
        Log.d("Psalter", "Playback Ended");
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
            binder.pause();
        }
        else if(i == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT){
            // having focus doesn't mean media is playing. only resume playback on focus regained if media was playing originally.
            if(getPlaybackState() == PlaybackStateCompat.STATE_PLAYING){
                resumePlaybackOnFocusGain = true;
                binder.pause();
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
                binder.play();
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
                .setLargeIcon(psalterRepository.getScore(psalter).getBitmap())
                .setContentTitle(metadata.getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE))
                .setContentText(metadata.getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION))
                .setSubText(metadata.getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE))
                .setContentIntent(openActivityOnTouch)
                .setColor(getResources().getColor(R.color.colorAccent))
                .setShowWhen(false)
                .setDeleteIntent(getPendingIntent(ACTION_DELETE, 0));
        if(getPlaybackState() == PlaybackStateCompat.STATE_PLAYING){
            builder.addAction(R.drawable.ic_stop_white_36dp, "Stop", getPendingIntent(ACTION_STOP, 1))
                .setPriority(NotificationCompat.PRIORITY_HIGH);
        }
        else{
            builder.addAction(R.drawable.ic_play_arrow_white_36dp, "Play", getPendingIntent(ACTION_PLAY, 2));
        }
        numActions++;

        if(isShuffling()){
            builder.addAction(R.drawable.ic_skip_next_white_36dp, "Next", getPendingIntent(ACTION_NEXT, 3));
            numActions++;
        }
        builder.setStyle(new MediaStyle()
                .setShowActionsInCompactView(Util.CreateRange(0, numActions - 1)));

        return  builder.build();
    }

    private void updatePlaybackState(int state){
        if(state == PlaybackStateCompat.STATE_PLAYING) registerReceiver();
        else unregisterReceiver();

        long actions = PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID | PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH;
        if(isShuffling()){
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
            actions = actions | PlaybackStateCompat.ACTION_PAUSE
                    | PlaybackStateCompat.ACTION_STOP;
        }
        mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                .setState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1)
                .setActions(actions)
                .build());
    }
    private void updateMetaData(){
        String title = psalter.getTitle() + " - " + psalter.getHeading();
        mediaSession.setMetadata(new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, String.valueOf(psalter.getId()))
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, title)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, psalter.getSubtitleText())
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, "The Psalter")
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, psalter.getSubtitleText())
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION, "Verse " + currentVerse + " of " + psalter.getNumverses())
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, psalterRepository.getScore(psalter).getBitmap())
                .build());
    }
    private void updateNotification(){
        if(getPlaybackState() != PlaybackStateCompat.STATE_PLAYING){
            stopForeground(false);
        }
        notificationManager.notify(NOTIFICATION_ID, getNotification());
    }

    private BroadcastReceiver becomingNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(AudioManager.ACTION_AUDIO_BECOMING_NOISY)){
                binder.pause();
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
            Logger.d("Setting shuffle mode " + shuffleMode);
            mediaSession.setShuffleMode(shuffleMode);
            if(shuffleMode == PlaybackStateCompat.SHUFFLE_MODE_ALL){
                Toast.makeText(MediaService.this, "Shuffling", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onPause() {
            if(getPlaybackState() == PlaybackStateCompat.STATE_PLAYING) {
                if(mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                }
                updatePlaybackState(PlaybackStateCompat.STATE_PAUSED);
                updateNotification();
            }
        }

        @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras) {
            playPsalter(psalterRepository.getIndex(Integer.valueOf(mediaId)), 1);
        }

        @Override
        public void onPlayFromSearch(String query, Bundle extras) {
            Psalter[] hits = psalterRepository.searchPsalter(query);
            if(hits.length > 0){
                playPsalter(hits[0], 1);
            }
        }

        @Override
        public void onSkipToNext() {
            playPsalter(psalterRepository.getRandom(), 1);
        }
    };

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int what, int extra) {
        mediaPlayer.reset();
        Logger.e("MediaPlayer error. (" + what + ", " + extra + ")", null);
        return false;
    }

    private void createNotificationChannel(){
        if(Build.VERSION.SDK_INT >= 26){
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
            manager.createNotificationChannel(channel);
        }
    }

    private PendingIntent getPendingIntent(String action, int actionId){ // actionId is any number unique to string action
        Intent intent = new Intent(this, MediaService.class).setAction(action);
        return PendingIntent.getService(this, actionId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }
    private void registerReceiver() {
        try { registerReceiver(becomingNoisyReceiver, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)); }
        catch (Exception ex) { }
    }
    private void unregisterReceiver(){
        try { unregisterReceiver(becomingNoisyReceiver); }
        catch (Exception ex) { }
    }
}
