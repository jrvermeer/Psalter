package com.jrvermeer.psalter;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
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

/**
 * Created by Jonathan on 4/3/2017.
 */

public class MediaService extends Service {
    private MediaBinder binder;
    private final int ITERATION_DELAY_MS = 700;
    private final int NOTIFICATION_ID = 1234;
    private final String ACTION = "action";
    private final int ACTION_STOP = 1253283478;
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        if(binder == null){
            binder = new MediaBinder();
        }
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent.getIntExtra(ACTION, -1) == ACTION_STOP && binder != null && binder.isPlaying()){
            binder.stopMedia();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    public class MediaBinder extends Binder implements MediaPlayer.OnCompletionListener, AudioManager.OnAudioFocusChangeListener{
        private MediaBinder(){
            mediaPlayer = new MediaPlayer();
            audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        }
        private MediaPlayer mediaPlayer;
        private IMediaCallbacks mediaCallbacks;
        private AudioManager audioManager;
        private State state;

        private Handler handler = new Handler();
        private Runnable playNextVerse = new Runnable() {
            @Override
            public void run() {
                if(state != null){ // media could have been stopped between verses
                    mediaPlayer.start();
                    startForeground(NOTIFICATION_ID, getNotification(state.psalter, state.currentVerse));
                    state.betweenVerses = false;
                }
            }
        };



        public void setCallbacks(@NonNull final IMediaCallbacks callbacks){
            mediaCallbacks = callbacks;
            mediaPlayer.setOnCompletionListener(this);
        }

        public boolean playMedia(Psalter psalter){
            try{
                stopMedia();
                if(audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED){
                    int resID = getResources().getIdentifier(psalter.getAudioFileName(), "raw", getPackageName());
                    AssetFileDescriptor afd = getApplicationContext().getResources().openRawResourceFd(resID);
                    mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                    afd.close();
                    state = new State(psalter);
                    mediaPlayer.prepare();
                    mediaPlayer.start();
                    startForeground(NOTIFICATION_ID, getNotification(psalter));
                }
                return true;
            } catch (Exception ex){
                return false;
            }
        }


        public void stopMedia(){
            state = null;
            if(mediaPlayer.isPlaying()) mediaPlayer.stop();
            playbackStopped();
        }

        public boolean isPlaying(){
            return mediaPlayer.isPlaying() || (state != null && state.betweenVerses);
        }


        @Override
        public void onCompletion(final MediaPlayer mediaPlayer) {
            state.currentVerse++;
            if(state.currentVerse <= state.psalter.getNumverses()){
                state.betweenVerses = true;
                handler.postDelayed(playNextVerse, ITERATION_DELAY_MS);
            }
            else playbackStopped();

        }
        private void playbackStopped(){
            stopForeground(true);
            audioManager.abandonAudioFocus(this);
            mediaPlayer.reset();
            if(mediaCallbacks != null){
                mediaCallbacks.playerFinished();
            }
        }

        @Override
        public void onAudioFocusChange(int i) {
            if(i == AudioManager.AUDIOFOCUS_LOSS || i == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT){
                stopMedia();
            }
        }

        public Notification getNotification(Psalter psalter) {
            return getNotification(psalter, 1);
        }
        public Notification getNotification(Psalter psalter, int currentVerse) {
            Intent openActivity = new Intent(MediaService.this, MainActivity.class);
            PendingIntent openActivityOnTouch = PendingIntent.getActivity(MediaService.this, 0, openActivity, 0);

            Intent stopPlayback = new Intent(MediaService.this, MediaService.class).putExtra(ACTION, ACTION_STOP);
            PendingIntent stopPlaybackOnTouch = PendingIntent.getService(MediaService.this, 0, stopPlayback, 0);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(MediaService.this);
            builder.setSmallIcon(R.drawable.ic_smallicon)
                    //.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                    .setContentTitle(psalter.getDisplayTitle())
                    .setContentText(psalter.getDisplaySubtitle())
                    .setContentIntent(openActivityOnTouch)
                    .addAction(R.drawable.ic_stop_black_36dp, "Stop", stopPlaybackOnTouch)
                    .setSubText(String.format("Verse %d of %d", currentVerse, psalter.getNumverses()))
                    .setShowWhen(false).setPriority(100)
                    .setStyle(new NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(0));
            return builder.build();
        }

        private class State {
            public State(Psalter p){
                psalter = p;
                currentVerse = 1;
                betweenVerses = false;
            }
            private int currentVerse;
            private boolean betweenVerses;
            private Psalter psalter;
        }
    }

    public interface IMediaCallbacks {
        void playerFinished();
    }
}
