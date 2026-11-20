/*
 * Dominoes
 * Copyright (c) 2026 Fouez Mahrouk
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */


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
