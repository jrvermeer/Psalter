package com.jrvermeer.psalter.Core.Contracts

import android.view.View


/**
 * Created by Jonathan on 6/16/2018.
 */

interface IPagerCallbacks {
    fun pageCreated(page: View, position: Int)
}
