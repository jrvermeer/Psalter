package com.jrvermeer.psalter.Infrastructure;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.StringRes;

import com.jrvermeer.psalter.R;
import com.jrvermeer.psalter.UI.PsalterApplication;

public class SimpleStorage {
    public SimpleStorage(){
        c = PsalterApplication.getContext();
        sPref = c.getSharedPreferences("settings", Context.MODE_PRIVATE);

    }
    private SharedPreferences sPref;
    private Context c;

    public boolean getBoolean(@StringRes int id){
        return sPref.getBoolean(c.getString(id), false);
    }
    public void setBoolean(@StringRes int id, boolean val){
        sPref.edit().putBoolean(c.getString(id), val).apply();
    }
    public boolean toggleBoolean(@StringRes int id){
        boolean newVal = !getBoolean(id);
        setBoolean(id, newVal);
        return newVal;
    }

    private int getInt(@StringRes int id){
        return sPref.getInt(c.getString(id), 0);
    }
    private void setInt(@StringRes int id, int val){
        sPref.edit().putInt(c.getString(id), val).apply();
    }

    public boolean isNightMode(){
        return getBoolean(R.string.pref_nightmode);
    }
    public boolean toggleNightMode(){
        return toggleBoolean(R.string.pref_nightmode);
    }

    public boolean scoreShown(){
        return getBoolean(R.string.pref_showScore);
    }
    public boolean toggleScore(){
        return  toggleBoolean(R.string.pref_showScore);
    }

    public int getPageIndex(){
        return getInt(R.string.pref_lastindex);
    }
    public void setPageIndex(int i){
        setInt(R.string.pref_lastindex, i);
    }
}
