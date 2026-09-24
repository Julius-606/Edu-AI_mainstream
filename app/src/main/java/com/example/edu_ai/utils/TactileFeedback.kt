package com.example.edu_ai.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object TactileFeedback {
    /**
     * Triggers a subtle, light click haptic vibration (15ms duration) to make button presses feel tactile.
     */
    fun triggerSubtleClick(context: Context) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= 31) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            vibrator?.let {
                if (it.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= 26) {
                        it.vibrate(VibrationEffect.createOneShot(15, 110))
                    } else {
                        @Suppress("DEPRECATION")
                        it.vibrate(15)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
