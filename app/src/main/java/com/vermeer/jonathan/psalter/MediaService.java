package com.vermeer.jonathan.psalter;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;

import java.io.File;
import java.io.FileOutputStream;

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
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                if(mediaCallbacks != null){
                    mediaCallbacks.playerFinished();
                }
            }
        });
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

//                if(audioBytes == null) return false;
//
//                File teMp3 = File.createTempFile("kachow", ".mp3", getCacheDir());
//                teMp3.deleteOnExit();
//                FileOutputStream fos = new FileOutputStream(teMp3);
//                fos.write(audioBytes);
//                fos.close();

                //mediaPlayer.reset();
                //mediaPlayer.setDataSource(teMp3.getAbsolutePath());
                //mediaPlayer.prepare();
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
