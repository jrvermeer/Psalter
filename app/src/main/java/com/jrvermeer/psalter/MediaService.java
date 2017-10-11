package com.jrvermeer.psalter;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.NotificationCompat;

import com.jrvermeer.psalter.Models.Psalter;

import java.util.Random;

/**
 * Created by Jonathan on 4/3/2017.
 */

public class MediaService extends Service implements AudioManager.OnAudioFocusChangeListener, MediaPlayer.OnCompletionListener {
    private MediaBinder mBinder = new MediaBinder();
    private IMediaCallbacks mediaCallbacks = null;

    private PsalterDb db;
    private AudioManager audioManager;
    private MediaPlayer mediaPlayer = new MediaPlayer();
    private State mState = null;
    private Random rand = new Random();

    private final int MS_BETWEEN_VERSES = 700;
    private final int NOTIFICATION_ID = 1234;
    private final String ACTION = "action";
    private static int ACTION_STOP = 5;
    private static int ACTION_NEXT = 89;

    @Override
    public void onCreate() {
        audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        db = new PsalterDb(this);
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getAction().equals(AudioManager.ACTION_AUDIO_BECOMING_NOISY)){
                    stopMedia(false);
                }
            }
        }, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent != null ){
            if(intent.getIntExtra(ACTION, -1) == ACTION_STOP && isPlaying()){
                stopMedia(false);
            }
            else if(intent.getIntExtra(ACTION, -1) == ACTION_NEXT){
                playRandomNumber();
            }
        }
        stopSelf();
        return START_NOT_STICKY;
    }

    public void setCallbacks(@NonNull final IMediaCallbacks callbacks){
        mediaCallbacks = callbacks;
    }

    public boolean playPsalterNumber(int number){
        Psalter psalter = db.getPsalter(number);
        return playPsalter(psalter, false);
    }
    public boolean shuffleAllAudio(int firstNumber){
        Psalter psalter = db.getPsalter(firstNumber);
        return playPsalter(psalter, true);
    }
    private void playRandomNumber(){
        int nextIndex = rand.nextInt(db.getCount());
        Psalter psalter = db.getPsalter(nextIndex + 1);
        playPsalter(psalter, true);
        if(mediaCallbacks != null){
            mediaCallbacks.setCurrentNumber(psalter.getNumber());
        }
    }
    private boolean playPsalter(Psalter psalter, boolean shuffle){
        try{
            if(audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                mediaPlayer.reset();
                mState = new State(psalter, shuffle);
                AssetFileDescriptor afd = getAssetFileDescriptor(psalter);
                mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                afd.close();
                mediaPlayer.setOnCompletionListener(this);
                mediaPlayer.prepare();
                mediaPlayer.start();
                startForeground(NOTIFICATION_ID, getNotification());
                return true;
            }
            else return false;
        }
        catch(Exception ex) {
            return false;
        }
    }
    private boolean playNextVerse(){
        if(mState != null){
            mState.currentVerse++;
            if(mState.currentVerse <= mState.psalter.getNumverses()){
                mState.betweenVerses = true;
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if(mState != null && mState.betweenVerses){ // media could have been stopped between verses
                            mediaPlayer.start();
                            startForeground(NOTIFICATION_ID, getNotification());
                            mState.betweenVerses = false;
                        }
                    }
                }, MS_BETWEEN_VERSES);
                return true;
            }
        }
        return false;
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        if(!playNextVerse()){
            if(mState.isShuffling){
                playRandomNumber();
            }
            else playbackStopped(true);
        }
    }

    private AssetFileDescriptor getAssetFileDescriptor(Psalter psalter) {
        int resID = getResources().getIdentifier(psalter.getAudioFileName(), "raw", getPackageName());
        return getApplicationContext().getResources().openRawResourceFd(resID);
    }

    public void stopMedia(){
        stopMedia(true);
    }
    private void stopMedia(boolean removeNotification){
        mState = null;
        if(mediaPlayer.isPlaying()) mediaPlayer.stop();
        playbackStopped(removeNotification);
    }

    public boolean isPlaying(int number){
        return isPlaying() && mState.psalter.getNumber() == number;
    }
    public boolean isPlaying(){
        return mediaPlayer.isPlaying() || (mState != null && mState.betweenVerses);
    }

    private void playbackStopped(boolean removeNotification){
        stopForeground(true);
//        if(!removeNotification){
//            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, getNotification());
//        }
        audioManager.abandonAudioFocus(this);
        mediaPlayer.reset();
        if(mediaCallbacks != null){
            mediaCallbacks.playerFinished();
        }
    }

    @Override
    public void onAudioFocusChange(int i) {
        if(i == AudioManager.AUDIOFOCUS_LOSS || i == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT){
            stopMedia(false);
        }
    }
    private Notification getNotification() {
        Intent openActivity = new Intent(this, MainActivity.class);
        PendingIntent openActivityOnTouch = PendingIntent.getActivity(this, 1, openActivity, 0);

        Intent stopPlayback = new Intent(this, MediaService.class).putExtra(ACTION, ACTION_STOP);
        PendingIntent stopPlaybackOnTouch = PendingIntent.getService(this, 2, stopPlayback, 0);
        int numActions = 0;
        NotificationCompat.Builder builder = new NotificationCompat.Builder(MediaService.this);
        builder.setSmallIcon(R.drawable.ic_smallicon)
                //.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                .setContentTitle(mState.psalter.getDisplayTitle())
                .setContentText(String.format("Verse %d of %d", mState.currentVerse, mState.psalter.getNumverses()))
                .setSubText(mState.psalter.getDisplaySubtitle())
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(openActivityOnTouch)
                .addAction(R.drawable.ic_stop_white_36dp, "Stop", stopPlaybackOnTouch)
                .setShowWhen(false);
        numActions++;
        if(mState.isShuffling){
            Intent playNext = new Intent(this, MediaService.class).putExtra(ACTION, ACTION_NEXT);
            PendingIntent playNextOnTouch = PendingIntent.getService(this, 3, playNext, 0);

            builder.addAction(R.drawable.ic_skip_next_white_36dp, "Next", playNextOnTouch);
            numActions++;
        }
        builder.setStyle(new NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(Util.CreateRange(0, numActions - 1)));

        return  builder.build();
    }

    private Handler handler = new Handler();


    public class MediaBinder extends Binder{
        public MediaService getServiceInstance(){
            return MediaService.this;
        }
    }

    private class State {
        public State(Psalter p, boolean isShuffling){
            psalter = p;
            currentVerse = 1;
            betweenVerses = false;
            this.isShuffling = isShuffling;
        }
        private int currentVerse;
        private boolean betweenVerses;
        private Psalter psalter;
        private boolean isShuffling;
    }

    public interface IMediaCallbacks {
        void playerFinished();
        void setCurrentNumber(int number);
    }
}
