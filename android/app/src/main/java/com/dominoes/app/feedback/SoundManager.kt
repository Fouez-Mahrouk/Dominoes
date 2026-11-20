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

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

class SoundManager {

    private val sampleRate = 22050

    fun playPickup(enabled: Boolean) {
        if (!enabled) return
        playToneAsync(freqHz = 520.0, durationMs = 45, volume = 0.25)
    }

    fun playPlace(enabled: Boolean) {
        if (!enabled) return
        playSequenceAsync(listOf(210.0 to 40, 140.0 to 55), volume = 0.38)
    }

    fun playDraw(enabled: Boolean) {
        if (!enabled) return
        playToneAsync(freqHz = 380.0, durationMs = 55, volume = 0.22)
    }

    fun playPass(enabled: Boolean) {
        if (!enabled) return
        playToneAsync(freqHz = 240.0, durationMs = 70, volume = 0.2)
    }

    fun playScore(enabled: Boolean) {
        if (!enabled) return
        playSequenceAsync(listOf(587.33 to 80, 880.0 to 130), volume = 0.35)
    }

    fun playWin(enabled: Boolean) {
        if (!enabled) return
        playSequenceAsync(listOf(523.25 to 90, 659.25 to 90, 783.99 to 110, 1046.5 to 220), volume = 0.38)
    }

    private fun playToneAsync(freqHz: Double, durationMs: Int, volume: Double) {
        playSequenceAsync(listOf(freqHz to durationMs), volume)
    }

    private fun playSequenceAsync(notes: List<Pair<Double, Int>>, volume: Double) {
        thread(isDaemon = true) {
            runCatching {
                val totalSamples = notes.sumOf { (sampleRate * it.second) / 1000 }
                if (totalSamples <= 0) return@runCatching
                val pcm = ShortArray(totalSamples)
                var offset = 0
                for ((freq, ms) in notes) {
                    val count = (sampleRate * ms) / 1000
                    for (i in 0 until count) {
                        val t = i.toDouble() / sampleRate
                        val env = exp(-5.0 * i / count.coerceAtLeast(1))
                        val sample = sin(2.0 * PI * freq * t) * env * volume
                        pcm[offset + i] = (sample * Short.MAX_VALUE).toInt().coerceIn(
                            Short.MIN_VALUE.toInt(),
                            Short.MAX_VALUE.toInt()
                        ).toShort()
                    }
                    offset += count
                }

                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(pcm.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(pcm, 0, pcm.size)
                track.play()
                val totalMs = notes.sumOf { it.second }.toLong()
                Thread.sleep(totalMs + 40L)
                track.stop()
                track.release()
            }
        }
    }
}
