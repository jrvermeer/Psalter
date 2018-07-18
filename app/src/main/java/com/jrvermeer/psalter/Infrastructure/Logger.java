package com.jrvermeer.psalter.Infrastructure;

import android.content.Context;

import com.flurry.android.FlurryAgent;
import com.jrvermeer.psalter.Core.Models.LogEvent;
import com.jrvermeer.psalter.Core.Models.SearchMode;
import com.jrvermeer.psalter.R;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Jonathan on 7/17/2018.
 */

public class Logger {

    public Logger(Context context){
        new FlurryAgent.Builder()
                .withLogEnabled(true)
                .build(context, context.getResources().getString(R.string.secret_flurry));
    }
    public void changeScore(boolean scoreVisible){
        Map<String, String> params = new HashMap<>();
        params.put("ScoreVisible", String.valueOf(scoreVisible));
        FlurryAgent.logEvent(LogEvent.ChangeScore.name(), params);
    }
    public void changeTheme(boolean nightMode){
        Map<String, String> params = new HashMap<>();
        params.put("NightMode", String.valueOf(nightMode));
        FlurryAgent.logEvent(LogEvent.ChangeTheme.name(), params);
    }
    public void playbackStarted(String numberTitle, boolean shuffling){
        Map<String, String> params = new HashMap<>();
        params.put("Number", numberTitle);
        params.put("Shuffling", String.valueOf(shuffling));
        FlurryAgent.logEvent(LogEvent.PlayPsalter.name(), params);
    }

    public void searchSubmitted(SearchMode mode, String query){
        Map<String, String> params = new HashMap<>();
        params.put("SearchMode", mode.name());
        params.put("Query", query);
        FlurryAgent.logEvent(LogEvent.SearchSubmitted.name(), params);
    }

    public void searchResultSelected(String query, String selectedTitle){
        Map<String, String> params = new HashMap<>();
        params.put("Query", query);
        params.put("Number", selectedTitle);
        FlurryAgent.logEvent(LogEvent.SearchResultSelected.name(), params);
    }

    public void event(LogEvent event){
        FlurryAgent.logEvent(event.name());
    }
    public void error(Throwable ex){
        FlurryAgent.onError("Error", "", ex);
    }
    public void error(String message){
        FlurryAgent.onError("Error", message, "");
    }
}
