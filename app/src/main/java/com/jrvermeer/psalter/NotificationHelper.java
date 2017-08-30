package com.jrvermeer.psalter;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Parcel;
import android.support.v7.app.NotificationCompat;
import android.widget.ImageButton;
import android.widget.RemoteViews;

/**
 * Created by jonathanv on 8/30/2017.
 */

public class NotificationHelper {
    public static final String ACTION_PLAY = "com.jrvermeer.psalter.ACTION_PLAY";
    //public static final String ACTION_NEXT = "com.jrvermeer.psalter.ACTION_NEXT";

    private static final int NOTIFICATION_ID = 0;
    private static NotificationManager notificationManager;

    public static void notify(Context context, Psalter psalter){
        notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);

        //if(Build.VERSION.SDK_INT < 22){
        buildNotificationApiUnder22(context, psalter);
        //}
    }

    public static void clearNotification(){
        if(notificationManager != null){
            notificationManager.cancel(NOTIFICATION_ID);
        }
    }

    private static void buildNotificationApiUnder22(Context context, Psalter psalter) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setSmallIcon(R.drawable.ic_smallicon);
        builder.setContentTitle(psalter.getDisplayTitle());
        builder.setContentText(psalter.getDisplaySubtitle());
        builder.setOngoing(true);

        Intent openActivity = new Intent(context, MainActivity.class);
        PendingIntent intent = PendingIntent.getActivity(context, 0, openActivity, 0);

        builder.setContentIntent(intent);
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }


//    private static class NotificationView extends RemoteViews{
//        public NotificationView(Context context, int layoutId, Psalter psalter) {
//            super(context.getPackageName(), layoutId);
//
//            setTextViewText(R.id.notification_titleText, "#" + psalter.getNumber());
//            String subtitle = null;
//            if(psalter.getPsalm() == 0){
//                subtitle = "Lords Prayer";
//            } else subtitle = "Psalm " + psalter.getPsalm();
//
//            setTextViewText(R.id.notification_subtitleText, subtitle);
//            setImageViewResource(R.id.notification_playButton, R.drawable.ic_stop_black_48dp);
//
//            Intent openActivity = new Intent(context, MainActivity.class);
//            PendingIntent intent = PendingIntent.getActivity(context, 0, openActivity, 0);
//            setOnClickPendingIntent(R.id.notification_linearlayout, intent);
//
//            Intent playIntent = new Intent(context, MediaService.class);
//            playIntent.setAction(ACTION_PLAY);
//            PendingIntent pendingIntentPlay = PendingIntent.getService(context, 0, playIntent, 0);
//            setOnClickPendingIntent(R.id.notification_playButton, pendingIntentPlay);
//
//            //Intent nextIntent = new Intent(ACTION_NEXT);
//        }
//    }
}


