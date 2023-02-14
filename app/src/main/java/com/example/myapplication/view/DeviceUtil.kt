package com.example.myapplication.view

import android.content.Context

object DeviceUtil {
    fun dip2px(context: Context, dp: Float):Int {
        val scale = context.resources.displayMetrics.density
        return (dp * scale + 0.5f).toInt()
    }
}