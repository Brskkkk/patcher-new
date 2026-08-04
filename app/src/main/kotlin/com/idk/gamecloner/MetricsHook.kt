package com.idk.gamecloner

import android.content.res.Resources
import android.util.DisplayMetrics

object MetricsHook {
    const val TARGET_WIDTH = 360
    const val TARGET_HEIGHT = 806
    const val TARGET_DPI = 160
    const val TARGET_DENSITY = 1.0f

    fun createSpoofedMetrics(): DisplayMetrics {
        return DisplayMetrics().apply {
            widthPixels = TARGET_WIDTH
            heightPixels = TARGET_HEIGHT
            densityDpi = TARGET_DPI
            density = TARGET_DENSITY
            scaledDensity = TARGET_DENSITY
            xdpi = TARGET_DPI.toFloat()
            ydpi = TARGET_DPI.toFloat()
        }
    }

    fun applySpoofedMetrics(resources: Resources?) {
        resources?.let {
            it.displayMetrics.apply {
                widthPixels = TARGET_WIDTH
                heightPixels = TARGET_HEIGHT
                densityDpi = TARGET_DPI
                density = TARGET_DENSITY
                scaledDensity = TARGET_DENSITY
                xdpi = TARGET_DPI.toFloat()
                ydpi = TARGET_DPI.toFloat()
            }
        }
    }
}
