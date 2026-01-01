package com.tiborlaszlo.keywave.core

import android.view.KeyEvent
import com.tiborlaszlo.keywave.data.CustomKeybind
import com.tiborlaszlo.keywave.data.HardwareKey
import com.tiborlaszlo.keywave.data.KeybindStep
import com.tiborlaszlo.keywave.data.PressType

class CustomKeybindMatcher {
  private val downTimes = mutableMapOf<HardwareKey, Long>()
  private var candidates: List<Candidate> = emptyList()

  fun onKeyEvent(event: KeyEvent?, keybinds: List<CustomKeybind>, onMatch: (CustomKeybind) -> Unit) {
    if (event == null) return
    val key = when (event.keyCode) {
      KeyEvent.KEYCODE_VOLUME_UP -> HardwareKey.VOLUME_UP
      KeyEvent.KEYCODE_VOLUME_DOWN -> HardwareKey.VOLUME_DOWN
      else -> return
    }

    when (event.action) {
      KeyEvent.ACTION_DOWN -> {
        downTimes[key] = event.eventTime
      }
      KeyEvent.ACTION_UP -> {
        val downTime = downTimes.remove(key) ?: return
        val duration = (event.eventTime - downTime).coerceAtLeast(0)
        val input = StepInput(key = key, durationMs = duration, timeMs = event.eventTime)
        processInput(input, keybinds, onMatch)
      }
    }
  }

  private fun processInput(input: StepInput, keybinds: List<CustomKeybind>, onMatch: (CustomKeybind) -> Unit) {
    val nextCandidates = ArrayList<Candidate>()

    candidates.forEach { candidate ->
      val prevStep = candidate.keybind.steps[candidate.nextIndex - 1]
      val elapsed = input.timeMs - candidate.lastTimeMs
      val maxDelay = if (prevStep.delayAfterMs == 0L) {
        SIMULTANEOUS_WINDOW_MS
      } else {
        prevStep.delayAfterMs
      }
      if (elapsed > maxDelay) return@forEach
      val step = candidate.keybind.steps[candidate.nextIndex]
      if (matches(step, input)) {
        if (candidate.nextIndex == candidate.keybind.steps.lastIndex) {
          onMatch(candidate.keybind)
        } else {
          nextCandidates.add(
            Candidate(
              keybind = candidate.keybind,
              nextIndex = candidate.nextIndex + 1,
              lastTimeMs = input.timeMs,
            )
          )
        }
      }
    }

    keybinds.forEach { keybind ->
      if (!keybind.enabled) return@forEach
      if (keybind.steps.isEmpty()) return@forEach
      val firstStep = keybind.steps.first()
      if (matches(firstStep, input)) {
        if (keybind.steps.size == 1) {
          onMatch(keybind)
        } else {
          nextCandidates.add(
            Candidate(
              keybind = keybind,
              nextIndex = 1,
              lastTimeMs = input.timeMs,
            )
          )
        }
      }
    }

    candidates = nextCandidates
  }

  private fun matches(step: KeybindStep, input: StepInput): Boolean {
    if (step.key != input.key) return false
    val threshold = step.longPressMs.coerceAtLeast(1)
    return when (step.pressType) {
      PressType.SHORT -> input.durationMs < threshold
      PressType.LONG -> input.durationMs >= threshold
    }
  }

  private data class StepInput(
    val key: HardwareKey,
    val durationMs: Long,
    val timeMs: Long,
  )

  private data class Candidate(
    val keybind: CustomKeybind,
    val nextIndex: Int,
    val lastTimeMs: Long,
  )

  companion object {
    private const val SIMULTANEOUS_WINDOW_MS = 120L
  }
}
