package com.jrvermeer.psalter.Infrastructure;

import android.content.Context;
import android.util.Log;

import com.flurry.android.FlurryAgent;
import com.jrvermeer.psalter.Core.Models.LogEvent;
import com.jrvermeer.psalter.Core.Models.SearchMode;
import com.jrvermeer.psalter.R;
import com.jrvermeer.psalter.UI.PsalterApplication;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Jonathan on 7/17/2018.
 */

public class Logger {
    private Logger() { }

    public static void d(String message){
        Log.d("PsalterLog", message);
    }

    public static void e(String message, Throwable ex){
        Log.e("PsalterLog", message, ex);
    }

    static {
        Context c =  PsalterApplication.getContext();
        new FlurryAgent.Builder()
                .withLogEnabled(true)
                .build(c, c.getResources().getString(R.string.secret_flurry));
    }
    public static void changeScore(boolean scoreVisible){
        Map<String, String> params = new HashMap<>();
        params.put("ScoreVisible", String.valueOf(scoreVisible));
        FlurryAgent.logEvent(LogEvent.ChangeScore.name(), params);
    }
    public static void changeTheme(boolean nightMode){
        Map<String, String> params = new HashMap<>();
        params.put("NightMode", String.valueOf(nightMode));
        FlurryAgent.logEvent(LogEvent.ChangeTheme.name(), params);
    }
    public static void playbackStarted(String numberTitle, boolean shuffling){
        Map<String, String> params = new HashMap<>();
        params.put("Number", numberTitle);
        FlurryAgent.logEvent(shuffling ? LogEvent.ShufflePsalter.name() : LogEvent.PlayPsalter.name(), params);
    }

    public static void searchEvent(SearchMode mode, String query, String psalterChosen){
        Map<String, String> params = new HashMap<>();

        LogEvent event;
        if (mode == SearchMode.Psalter){
            event  = LogEvent.SearchPsalter;
            params.put("Psalter", query);
        }
        else {
            if(mode == SearchMode.Psalm){
                event = LogEvent.SearchPsalm;
                params.put("Psalm", query);
            }
            else { // SearchMode.Lyrics
                event = LogEvent.SearchLyrics;
                params.put("Query", query);
            }
            params.put("PsalterChosen", psalterChosen);
        }
        FlurryAgent.logEvent(event.name(), params);
    }

    public static void event(LogEvent event){
        FlurryAgent.logEvent(event.name());
    }
    public static void error(Throwable ex){
        FlurryAgent.onError("Error", "", ex);
    }
    public static void error(String message){
        FlurryAgent.onError("Error", message, "");
    }
}
