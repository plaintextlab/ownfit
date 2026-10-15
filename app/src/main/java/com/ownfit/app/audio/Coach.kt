package com.ownfit.app.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.ownfit.app.data.Exercise
import com.ownfit.app.data.Trainer
import com.ownfit.app.data.WorkoutItem
import java.util.Locale

/**
 * The trainer's voice. Plays pre-recorded neural-voice clips (assets/voice/<trainer>/<key>.ogg)
 * in sequence; anything without a clip (custom exercise names) falls back to on-device TTS,
 * pitched to match the trainer. Also handles beeps and haptics.
 */
class Coach(context: Context, private val trainer: Trainer, private val voice: Boolean, private val sounds: Boolean) {
    private val ctx = context.applicationContext
    private val dir = "voice/${trainer.id}"
    private val clips: Set<String> = runCatching { ctx.assets.list(dir)?.toSet() }.getOrNull().orEmpty()

    private sealed interface Part
    private data class Clip(val key: String) : Part
    private data class Speech(val text: String) : Part

    private var queue = ArrayDeque<Part>()
    private var player: MediaPlayer? = null
    private var ttsReady = false
    private var tts: TextToSpeech? = null
    private val tone: ToneGenerator? = if (sounds) runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 70) }.getOrNull() else null
    private val vibrator: Vibrator? = runCatching {
        if (Build.VERSION.SDK_INT >= 31) (ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        else @Suppress("DEPRECATION") (ctx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator)
    }.getOrNull()

    init {
        if (voice) tts = TextToSpeech(ctx) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                tts?.setPitch(if (trainer == Trainer.FEMALE) 1.12f else 0.82f)
                tts?.setSpeechRate(1.02f)
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(id: String?) {}
                    override fun onDone(id: String?) { android.os.Handler(ctx.mainLooper).post { playNext() } }
                    @Deprecated("Deprecated in Java") override fun onError(id: String?) { android.os.Handler(ctx.mainLooper).post { playNext() } }
                })
                ttsReady = true
            }
        }
    }

    private fun has(key: String) = "$key.ogg" in clips

    private fun clipOr(key: String, text: String): Part = if (has(key)) Clip(key) else Speech(text)

    private fun say(vararg parts: Part) {
        if (!voice) return
        stopSpeech()
        queue = ArrayDeque(parts.toList())
        Music.duck(true)
        playNext()
    }

    private fun playNext() {
        player?.release()
        player = null
        val part = queue.removeFirstOrNull()
        if (part == null) { Music.duck(false); return }
        when (part) {
            is Clip -> runCatching {
                val mp = MediaPlayer()
                mp.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                ctx.assets.openFd("$dir/${part.key}.ogg").use { mp.setDataSource(it.fileDescriptor, it.startOffset, it.length) }
                mp.setOnCompletionListener { playNext() }
                mp.prepare()
                mp.start()
                player = mp
            }.onFailure { playNext() }
            is Speech -> if (ttsReady) tts?.speak(part.text, TextToSpeech.QUEUE_FLUSH, null, "c") else playNext()
        }
    }

    private fun stopSpeech() {
        queue.clear()
        player?.release()
        player = null
        tts?.stop()
    }

    // ---- What the coach says ----

    private fun name(e: Exercise?): Part = if (e == null) Speech("") else clipOr("ex_${e.id}", e.name)

    private fun amount(item: WorkoutItem): Part {
        item.seconds?.let { s ->
            val key = "sec_$s"
            return clipOr(key, "$s seconds")
        }
        val r = item.reps ?: 10
        return clipOr("reps_$r", "$r reps")
    }

    fun ready(first: Exercise?) = say(Clip("get_ready"), Clip("first_up"), name(first))

    fun work(e: Exercise?, item: WorkoutItem, side: Int?, lastSet: Boolean) {
        go()
        val parts = mutableListOf(name(e), amount(item))
        when {
            side == 1 -> parts += Clip("first_side")
            side == 2 -> parts += Clip("other_side")
            e?.perSide == true -> parts += Clip("each_side")
        }
        if (lastSet && item.sets > 1 && side != 2) parts += Clip("last_set")
        say(*parts.toTypedArray())
    }

    fun rest(next: Exercise?) = say(Clip("rest"), Clip("next"), name(next))

    fun countdown(n: Int) {
        if (voice) when (n) {
            3 -> say(Clip("three")); 2 -> say(Clip("two")); 1 -> say(Clip("one"))
        } else tone?.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
    }

    fun encourage(halfway: Boolean) = say(Clip(if (halfway) "halfway" else listOf("keep_going", "almost").random()))

    fun breath(phase: String) = say(
        Clip(
            when (phase) {
                "Breathe in" -> "breathe_in"
                "Breathe out" -> "breathe_out"
                else -> "hold"
            },
        ),
    )

    fun done() = say(Clip("done"))

    fun intro() = say(Clip("intro"))

    private fun go() {
        tone?.startTone(ToneGenerator.TONE_PROP_BEEP2, 200)
        runCatching { vibrator?.vibrate(VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE)) }
    }

    fun release() {
        stopSpeech()
        Music.duck(false)
        tts?.shutdown()
        tone?.release()
    }
}
