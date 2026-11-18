package com.dominoes.app.feedback

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

enum class HapticKind {
    PICKUP,
    PLACE,
    SCORE,
    WIN
}

class HapticManager(context: Context) {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        manager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    fun trigger(kind: HapticKind, enabled: Boolean) {
        if (!enabled) return
        val v = vibrator ?: return
        if (!v.hasVibrator()) return

        runCatching {
            val effect = when (kind) {
                HapticKind.PICKUP -> VibrationEffect.createOneShot(18L, 80)
                HapticKind.PLACE -> VibrationEffect.createOneShot(32L, 160)
                HapticKind.SCORE -> VibrationEffect.createWaveform(
                    longArrayOf(0L, 45L, 55L, 65L),
                    intArrayOf(0, 210, 0, 255),
                    -1
                )
                HapticKind.WIN -> VibrationEffect.createWaveform(
                    longArrayOf(0L, 70L, 60L, 90L, 60L, 170L),
                    intArrayOf(0, 180, 0, 220, 0, 255),
                    -1
                )
            }
            v.vibrate(effect)
        }
    }
}
