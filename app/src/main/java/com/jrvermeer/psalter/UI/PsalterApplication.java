package com.jrvermeer.psalter.UI;

import android.app.Application;
import android.content.Context;

/**
 * Created by Jonathan on 2/22/2019.
 */

public class PsalterApplication extends Application {
    private static Context appContext;

    @Override
    public void onCreate() {
        super.onCreate();
        appContext = getApplicationContext();
    }

    public static Context getContext() {
        return appContext;
    }
}
