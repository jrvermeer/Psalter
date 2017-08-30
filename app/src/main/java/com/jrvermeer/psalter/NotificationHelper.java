package com.jrvermeer.psalter;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Parcel;
import android.support.v7.app.NotificationCompat;
import android.widget.RemoteViews;

/**
 * Created by jonathanv on 8/30/2017.
 */

public class NotificationHelper {
    private static final int NOTIFICATION_ID = 0;
    protected static Context mContext;
    private static NotificationManager notificationManager;
    public static void notify(Context context){
        mContext = context;
        notificationManager = (NotificationManager)mContext.getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext);
        RemoteViews view = new NotificationView(context.getPackageName(), R.layout.notification_layout);
        builder.setSmallIcon(R.drawable.ic_smallicon);
        builder.setContent(view);
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }
    public static void clearNotification(){
        if(notificationManager != null){
            notificationManager.cancel(NOTIFICATION_ID);
        }
    }
    private static class NotificationView extends RemoteViews{
        //public static final String ACTION_PLAY = "com.jrvermeer.psalter.ACTION_PLAY";
        //public static final String ACTION_NEXT = "com.jrvermeer.psalter.ACTION_NEXT";

        public NotificationView(String packageName, int layoutId) {
            super(packageName, layoutId);

            Intent openActivity = new Intent(mContext, MainActivity.class);
            PendingIntent intent = PendingIntent.getActivity(mContext, 0, openActivity, 0);
            setOnClickPendingIntent(R.id.notification_linearlayout, intent);
            //Intent playIntent = new Intent(ACTION_PLAY);
            //Intent nextIntent = new Intent(ACTION_NEXT);
        }
    }
}


