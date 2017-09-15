package com.jrvermeer.psalter;

import android.app.Notification;
import android.app.NotificationManager;
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
    private final int NOTIFICATION_ID = 1234;
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        if(binder == null){
            binder = new MediaBinder();
        }
        return binder;
    }

    public class MediaBinder extends Binder implements MediaPlayer.OnCompletionListener, AudioManager.OnAudioFocusChangeListener{
        private MediaBinder(){
            mediaPlayer = new MediaPlayer();
            audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        }
        private MediaPlayer mediaPlayer;
        private IMediaCallbacks mediaCallbacks;
        private AudioManager audioManager;

        private Handler handler = new Handler();
        private Runnable playNextVerse = new Runnable() {
            @Override
            public void run() {
                if(betweenVerses){ // media could have been stopped between verses
                    mediaPlayer.start();
                    betweenVerses = false;
                }
            }
        };
        private int iterationDelay_ms = 700;
        private int numberIterationsToPlay = 1;
        private int currentIteration = 0;
        private boolean betweenVerses = false;

        public void setCallbacks(@NonNull final IMediaCallbacks callbacks){
            mediaCallbacks = callbacks;
            mediaPlayer.setOnCompletionListener(this);
        }

        public boolean playMedia(Psalter psalter){
            try{
                stopMedia();
                if(audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED){
                    int resID = getResources().getIdentifier("_" + psalter.getNumber(), "raw", getPackageName());
                    AssetFileDescriptor afd = getApplicationContext().getResources().openRawResourceFd(resID);
                    mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                    afd.close();
                    numberIterationsToPlay = psalter.getNumverses();
                    currentIteration = 0;
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
            betweenVerses = false;
            if(mediaPlayer.isPlaying()) mediaPlayer.stop();
            playbackStopped();
        }

        public boolean isPlaying(){
            return mediaPlayer.isPlaying() || betweenVerses;
        }


        @Override
        public void onCompletion(final MediaPlayer mediaPlayer) {
            currentIteration++;
            if(currentIteration < numberIterationsToPlay){
                betweenVerses = true;
                handler.postDelayed(playNextVerse, iterationDelay_ms);
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
            NotificationCompat.Builder builder = new NotificationCompat.Builder(MediaService.this);
            builder.setSmallIcon(R.drawable.ic_smallicon);
            builder.setContentTitle(psalter.getDisplayTitle());
            builder.setContentText(psalter.getDisplaySubtitle());
            builder.setOngoing(true);

            Intent openActivity = new Intent(MediaService.this, MainActivity.class);
            PendingIntent intent = PendingIntent.getActivity(MediaService.this, 0, openActivity, 0);

            builder.setContentIntent(intent);

            return builder.build();
        }
    }

    public interface IMediaCallbacks {
        void playerFinished();
    }
}
