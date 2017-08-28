package com.jrvermeer.psalter;

import android.app.Service;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Created by Jonathan on 4/3/2017.
 */

public class MediaService extends Service {
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new MediaBinder();
    }

    public class MediaBinder extends Binder implements MediaPlayer.OnCompletionListener {
        public MediaBinder(){
            mediaPlayer = new MediaPlayer();
        }
        private MediaPlayer mediaPlayer;
        private IMediaCallbacks mediaCallbacks;
        public void setCallbacks(@NonNull final IMediaCallbacks callbacks){
            mediaCallbacks = callbacks;
            mediaPlayer.setOnCompletionListener(this);
        }

        public boolean playMedia(int psalterNumber){
            try{
                stopMedia();

                int resID = getResources().getIdentifier("_" + psalterNumber, "raw", getPackageName());
                AssetFileDescriptor afd = getApplicationContext().getResources().openRawResourceFd(resID);
                mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                afd.close();
                mediaPlayer.prepare();
                mediaPlayer.start();
                return true;
            } catch (Exception ex){
                return  false;
            }
        }

        public void stopMedia(){
            if(isPlaying()){
                mediaPlayer.stop();
                onCompletion(mediaPlayer);
            }
        }

        public boolean isPlaying(){
            return mediaPlayer != null && mediaPlayer.isPlaying();
        }

        @Override
        public void onCompletion(MediaPlayer mediaPlayer) {
            mediaPlayer.reset();
            if(mediaCallbacks != null){
                mediaCallbacks.playerFinished();
            }
        }
    }

    public interface IMediaCallbacks {
        void playerFinished();
    }
}
