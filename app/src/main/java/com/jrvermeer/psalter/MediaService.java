package com.jrvermeer.psalter;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;

/**
 * Created by Jonathan on 4/3/2017.
 */

public class MediaService extends Service {
    MediaBinder mediaBinder;
    MediaPlayer mediaPlayer;
    IMediaCallbacks mediaCallbacks;

    @Override
    public void onCreate() {
        super.onCreate();
        mediaBinder = new MediaBinder();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mediaBinder;
    }

    public class MediaBinder extends Binder{
        public void setCallbacks(IMediaCallbacks callbacks){
            mediaCallbacks = callbacks;
        }

        public boolean playMedia(int psalterNumber){
            try{
                int resID = getResources().getIdentifier("_" + psalterNumber, "raw", getPackageName());
                stopMedia();
                mediaPlayer = MediaPlayer.create(getApplicationContext(), resID);
                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                                                        @Override
                                                        public void onCompletion(MediaPlayer mediaPlayer) {
                                                            if (mediaCallbacks != null) {
                                                                mediaCallbacks.playerFinished();
                                                            }
                                                        }
                                                    });
                mediaPlayer.start();
                return true;
            } catch (Exception ex){
                return  false;
            }
        }

        public void stopMedia(){
            if(isPlaying()){
                mediaPlayer.stop();
                if(mediaCallbacks != null){
                    mediaCallbacks.playerFinished();
                }
            }
        }

        public boolean isPlaying(){
            return mediaPlayer != null && mediaPlayer.isPlaying();
        }
    }

    public interface IMediaCallbacks {
        void playerFinished();
    }
}
