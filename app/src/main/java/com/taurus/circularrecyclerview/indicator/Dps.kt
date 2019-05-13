package com.taurus.circularrecyclerview.indicator

import android.content.res.Resources

inline val Int.dp: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toInt()

inline val Float.dp: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toInt()
