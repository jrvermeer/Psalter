package com.jrvermeer.psalter.Core;

import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.Drawable;

/**
 * Created by Jonathan on 9/27/2017.
 */

public class Util {
    private static final float[] NEGATIVE = {
            -1.0f,     0,     0,    0, 255, // red
            0, -1.0f,     0,    0, 255, // green
            0,     0, -1.0f,    0, 255, // blue
            0,     0,     0, 1.0f,   0  // alpha
    };

    public static int[] CreateRange(int start, int end){
        int[] range = new int[end - start + 1];
        for(int i = 0; i < range.length; i++){
            range[i] = i + start;
        }
        return range;
    }

    public static void invertColors(Drawable drawable){
        drawable.setColorFilter(new ColorMatrixColorFilter(NEGATIVE));
    }
}
