package com.jrvermeer.psalter.Infrastructure;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.graphics.drawable.DrawableCompat;

import com.jrvermeer.psalter.R;
import com.jrvermeer.psalter.UI.PsalterApplication;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jonathan on 2/22/2019.
 */

public class Helpers {
    public static void selectFab(boolean selected, FloatingActionButton fab, boolean nightMode){
        Drawable drawable = DrawableCompat.wrap(fab.getDrawable());
        int color;
        Resources res = PsalterApplication.getContext().getResources();
        if(selected && nightMode) color = Color.WHITE;
        else if(selected && !nightMode) color = res.getColor(R.color.colorAccent);
        else if(!selected && nightMode) color = res.getColor(R.color.colorUnselectedInverse);
        else /*if(!selected && !nightMode)*/ color = res.getColor(R.color.colorUnselected);

        DrawableCompat.setTint(drawable, color);
        fab.setImageDrawable(drawable);
    }

    // get all indexes of a string in another string
    public static List<Integer> allIndexesOf(String lyrics, String query ){
        List<Integer> rtn = new ArrayList<>();
        int index = lyrics.indexOf(query);
        while (index >= 0) {
            rtn.add(index);
            index = lyrics.indexOf(query, index + 1);
        }
        return  rtn;
    }
}
