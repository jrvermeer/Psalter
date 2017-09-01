package com.jrvermeer.psalter;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Created by Jonathan on 4/3/2017.
 */

public class MediaService extends Service {
    private MediaBinder binder;
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
        if(binder != null && intent.getAction().equals(NotificationHelper.ACTION_PLAY)){
            // if playing, stop playback
            if(binder.isPlaying()){
                binder.stopMedia();
            }
            // if not playing, start playback
        }
        return super.onStartCommand(intent, flags, startId);
    }

    public class MediaBinder extends Binder implements MediaPlayer.OnCompletionListener {
        private MediaBinder(){
            mediaPlayer = new MediaPlayer();
        }
        private MediaPlayer mediaPlayer;
        private IMediaCallbacks mediaCallbacks;

        private Handler handler = new Handler();
        private Runnable playNextVerse = new Runnable() {
            @Override
            public void run() {
                mediaPlayer.start();
            }
        };
        private int iterationDelay_ms = 700;
        private int numberIterationsToPlay = 1;
        private int currentIteration = 0;

        public void setCallbacks(@NonNull final IMediaCallbacks callbacks){
            mediaCallbacks = callbacks;
            mediaPlayer.setOnCompletionListener(this);
        }

        public boolean playMedia(Psalter psalter){
            try{
                stopMedia();
                int resID = getResources().getIdentifier("_" + psalter.getNumber(), "raw", getPackageName());
                AssetFileDescriptor afd = getApplicationContext().getResources().openRawResourceFd(resID);
                mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                afd.close();
                numberIterationsToPlay = psalter.getNumverses();
                currentIteration = 0;
                mediaPlayer.prepare();
                mediaPlayer.start();
                NotificationHelper.notify(MediaService.this, psalter);
                return true;
            } catch (Exception ex){
                return false;
            }
        }

        public void stopMedia(){
            if(isPlaying()){
                mediaPlayer.stop();
                playbackStopped();
            }
        }

        public boolean isPlaying(){
            return mediaPlayer != null && mediaPlayer.isPlaying();
        }


        @Override
        public void onCompletion(final MediaPlayer mediaPlayer) {
            currentIteration++;
            if(currentIteration < numberIterationsToPlay){
                handler.postDelayed(playNextVerse, iterationDelay_ms);
            }
            else playbackStopped();

        }
        private void playbackStopped(){
            mediaPlayer.reset();
            if(mediaCallbacks != null){
                mediaCallbacks.playerFinished();
            }
            NotificationHelper.clearNotification();
        }
    }

    public interface IMediaCallbacks {
        void playerFinished();
    }
}
