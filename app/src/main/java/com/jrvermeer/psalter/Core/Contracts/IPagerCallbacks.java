package com.jrvermeer.psalter.Core.Contracts;

import android.view.View;

import com.jrvermeer.psalter.Core.Models.Psalter;

/**
 * Created by Jonathan on 6/16/2018.
 */

public interface IPagerCallbacks {
    void pageCreated(View page, int position);
}
