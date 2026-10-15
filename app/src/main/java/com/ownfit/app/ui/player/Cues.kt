package com.ownfit.app.ui.player

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import java.util.Locale

/** Voice, beeps and haptics during a workout. Everything is best-effort and silent on failure. */
class Cues(context: Context, private val voice: Boolean, private val sounds: Boolean) {
    private var ready = false
    private var tts: TextToSpeech? = null
    private val tone: ToneGenerator? = if (sounds) runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 70) }.getOrNull() else null
    private val vibrator: Vibrator? = runCatching {
        if (Build.VERSION.SDK_INT >= 31)
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        else @Suppress("DEPRECATION") (context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator)
    }.getOrNull()

    init {
        if (voice) tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                runCatching { tts?.language = Locale.getDefault() }
                ready = true
            }
        }
    }

    fun say(text: String) {
        if (ready) tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "cue")
    }

    fun tick() { tone?.startTone(ToneGenerator.TONE_PROP_BEEP, 120) }

    fun go() {
        tone?.startTone(ToneGenerator.TONE_PROP_BEEP2, 250)
        runCatching { vibrator?.vibrate(VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE)) }
    }

    fun release() {
        tts?.stop(); tts?.shutdown()
        tone?.release()
    }
}
