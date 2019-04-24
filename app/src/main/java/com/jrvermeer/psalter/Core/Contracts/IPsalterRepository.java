package com.jrvermeer.psalter.Core.Contracts;

import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import com.jrvermeer.psalter.Core.Models.Psalter;

/**
 * Created by Jonathan on 6/15/2018.
 */

public interface IPsalterRepository {
    int getCount();
    Psalter getIndex(int index);
    Psalter getPsalter(int number);
    Psalter getRandom();
    Psalter[] getPsalm(int psalmNumber);
    Psalter[] searchPsalter(String searchText);
    AssetFileDescriptor getAudioDescriptor(Psalter psalter);
    BitmapDrawable getScore(Psalter psalter);

}
