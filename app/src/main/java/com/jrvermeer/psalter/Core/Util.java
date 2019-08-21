package com.jrvermeer.psalter.Core;

import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.Drawable;

/**
 * Created by Jonathan on 9/27/2017.
 */

public class Util {

    public static int[] CreateRange(int start, int end){
        int[] range = new int[end - start + 1];
        for(int i = 0; i < range.length; i++){
            range[i] = i + start;
        }
        return range;
    }


}
