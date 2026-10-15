package com.ownfit.app.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ownfit.app.data.MusicTrack
import com.ownfit.app.data.Settings

/** Background workout music: a looping playlist of bundled tracks or the user's own files. */
object Music {
    private sealed interface Src {
        data class Asset(val path: String) : Src
        data class File(val uri: Uri) : Src
    }

    private var ctx: Context? = null
    private var player: MediaPlayer? = null
    private var playlist: List<Src> = emptyList()
    private var index = 0
    private var volume = 0.45f
    private var ducked = false

    /** Observable for the play/pause button. */
    var playing by mutableStateOf(false)
        private set
    var active by mutableStateOf(false)
        private set

    fun start(context: Context, s: Settings) {
        stop()
        ctx = context.applicationContext
        volume = s.musicVolume
        val bundled = MusicTrack.entries.map { Src.Asset("music/${it.id}.ogg") }
        playlist = if (s.music == "mine" && s.myMusic.isNotEmpty()) s.myMusic.map { Src.File(Uri.parse(it)) }
        else {
            val first = MusicTrack.entries.indexOfFirst { it.id == s.music }.coerceAtLeast(0)
            bundled.drop(first) + bundled.take(first)
        }
        index = 0
        active = true
        if (s.musicOn) playCurrent()
    }

    private fun playCurrent() {
        val c = ctx ?: return
        player?.release()
        player = null
        if (playlist.isEmpty()) return
        val mp = MediaPlayer()
        runCatching {
            mp.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
            when (val src = playlist[index % playlist.size]) {
                is Src.Asset -> c.assets.openFd(src.path).use { mp.setDataSource(it.fileDescriptor, it.startOffset, it.length) }
                is Src.File -> mp.setDataSource(c, src.uri)
            }
            mp.setOnCompletionListener { index++; playCurrent() }
            mp.setOnPreparedListener { applyVolume(); it.start(); playing = true }
            mp.prepareAsync()
            player = mp
        }.onFailure {
            mp.release()
            // Skip unreadable files (e.g. a removed song) but don't spin forever.
            if (playlist.size > 1 && index < playlist.size * 2) { index++; playCurrent() } else playing = false
        }
    }

    private fun applyVolume() {
        val v = if (ducked) volume * 0.3f else volume
        runCatching { player?.setVolume(v, v) }
    }

    fun toggle() {
        val p = player
        when {
            p == null -> playCurrent()
            playing -> { runCatching { p.pause() }; playing = false }
            else -> { runCatching { p.start() }; playing = true }
        }
    }

    fun pause() { if (playing) toggle() }

    fun next() { index++; playCurrent() }

    fun setVolume(v: Float) { volume = v; applyVolume() }

    /** Lower the music while the coach speaks. */
    fun duck(on: Boolean) { ducked = on; applyVolume() }

    fun stop() {
        runCatching { player?.stop() }
        player?.release()
        player = null
        playing = false
        active = false
    }
}
