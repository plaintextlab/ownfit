package com.ownfit.app.ui.components

import android.content.Context
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Real-person exercise photos (start + end position) bundled in assets/photos, from the
 * public-domain free-exercise-db dataset (see scripts/fetch_exercise_photos.py).
 */
object ExercisePhotos {
    private var index: Map<String, Int>? = null

    @Synchronized
    fun index(ctx: Context): Map<String, Int> = index ?: runCatching {
        val text = ctx.assets.open("photos/index.json").bufferedReader().use { it.readText() }
        Json.parseToJsonElement(text).jsonObject.mapValues { (_, v) -> v.jsonObject["frames"]!!.jsonPrimitive.int }
    }.getOrDefault(emptyMap()).also { index = it }

    fun has(ctx: Context, id: String) = id in index(ctx)
    fun uri(id: String, frame: Int) = "file:///android_asset/photos/${id}_$frame.webp"
}

/** Alternates the start and end photos with a soft crossfade, like a slow-motion demo. */
@Composable
fun ExercisePhotoLoop(exerciseId: String, modifier: Modifier = Modifier, playing: Boolean = true) {
    val ctx = LocalContext.current
    val frames = remember(exerciseId) { ExercisePhotos.index(ctx)[exerciseId] ?: 0 }
    if (frames == 0) return
    var frame by remember(exerciseId) { mutableIntStateOf(0) }
    LaunchedEffect(exerciseId, playing) {
        while (playing && frames > 1) {
            delay(1100)
            frame = (frame + 1) % frames
        }
    }
    Crossfade(targetState = frame, animationSpec = tween(450), modifier = modifier, label = "photo") { f ->
        AsyncImage(
            ExercisePhotos.uri(exerciseId, f), null,
            Modifier.fillMaxSize().clip(RoundedCornerShape(24.dp)),
            contentScale = ContentScale.Fit,
        )
    }
}
