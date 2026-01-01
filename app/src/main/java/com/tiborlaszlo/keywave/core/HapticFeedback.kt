package com.tiborlaszlo.keywave.core

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.tiborlaszlo.keywave.data.HapticPattern

class HapticFeedback(
    private val context: Context,
    private val isDebugEnabled: () -> Boolean = { false },
) {
    companion object {
        private const val TAG = "KeyWaveHaptic"
    }

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(VibratorManager::class.java)
        manager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Vibrator::class.java)
    }
    
    init {
        Log.w(TAG, "HapticFeedback init: vibrator=${vibrator != null}, hasVibrator=${vibrator?.hasVibrator()}")
    }

    fun perform(
        pattern: HapticPattern,
        intensity: Int,
        customPulseCount: Int,
        customPulseMs: Int,
        customGapMs: Int,
    ) {
        Log.w(TAG, ">>> perform() called: pattern=$pattern, intensity=$intensity")
        
        if (pattern == HapticPattern.OFF) {
            Log.w(TAG, "Haptic pattern is OFF - skipping")
            return
        }
        
        val device = vibrator
        if (device == null || !device.hasVibrator()) {
            Log.e(TAG, "ERROR: No vibrator available!")
            return
        }

        val amplitude = intensity.coerceIn(1, 255)
        
        val effect = when (pattern) {
            HapticPattern.OFF -> null
            HapticPattern.SHORT -> VibrationEffect.createOneShot(50, amplitude)
            HapticPattern.LONG -> VibrationEffect.createOneShot(100, amplitude)
            HapticPattern.DOUBLE -> VibrationEffect.createWaveform(
                longArrayOf(0, 40, 80, 40),
                intArrayOf(0, amplitude, 0, amplitude),
                -1,
            )
            HapticPattern.CUSTOM -> {
                val pulses = customPulseCount.coerceIn(1, 6)
                val pulseMs = customPulseMs.coerceIn(10, 200)
                val gapMs = customGapMs.coerceIn(10, 300)
                val timings = LongArray(pulses * 2 + 1)
                val amplitudes = IntArray(pulses * 2 + 1)
                timings[0] = 0
                amplitudes[0] = 0
                var idx = 1
                repeat(pulses) {
                    timings[idx] = pulseMs.toLong()
                    amplitudes[idx] = amplitude
                    idx++
                    timings[idx] = gapMs.toLong()
                    amplitudes[idx] = 0
                    idx++
                }
                VibrationEffect.createWaveform(timings, amplitudes, -1)
            }
        }

        if (effect == null) return

        try {
            // Use USAGE_ACCESSIBILITY on API 33+ for proper accessibility haptic feedback
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val attrs = VibrationAttributes.Builder()
                    .setUsage(VibrationAttributes.USAGE_ACCESSIBILITY)
                    .build()
                device.vibrate(effect, attrs)
                Log.w(TAG, ">>> Vibrated with USAGE_ACCESSIBILITY")
            } else {
                // For older devices, use USAGE_ASSISTANCE_ACCESSIBILITY
                val audioAttrs = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                @Suppress("DEPRECATION")
                device.vibrate(effect, audioAttrs)
                Log.w(TAG, ">>> Vibrated with USAGE_ASSISTANCE_ACCESSIBILITY")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Vibration error: ${e.message}", e)
            // Fallback to basic vibrate
            try {
                @Suppress("DEPRECATION")
                device.vibrate(effect)
                Log.w(TAG, ">>> Fallback vibration executed")
            } catch (e2: Exception) {
                Log.e(TAG, "Fallback also failed: ${e2.message}")
            }
        }
    }
}
