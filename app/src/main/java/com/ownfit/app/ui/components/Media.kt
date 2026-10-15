package com.ownfit.app.ui.components

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.ownfit.app.OwnFitApp
import com.ownfit.app.data.Exercise
import com.ownfit.app.data.Gender
import com.ownfit.app.data.Media

object YouTube {
    private val patterns = listOf(
        Regex("youtu\\.be/([\\w-]{6,})"),
        Regex("[?&]v=([\\w-]{6,})"),
        Regex("/shorts/([\\w-]{6,})"),
        Regex("/embed/([\\w-]{6,})"),
        Regex("/live/([\\w-]{6,})"),
    )

    fun id(url: String?): String? {
        if (url.isNullOrBlank()) return null
        patterns.forEach { r -> r.find(url)?.let { return it.groupValues[1] } }
        return url.takeIf { Regex("^[\\w-]{11}$").matches(it) }
    }

    fun open(ctx: android.content.Context, url: String) {
        val id = id(url) ?: return
        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=$id")))
    }
}

/** User GIF/image if set, otherwise the built-in animation with a small muscle map. */
@Composable
fun ExerciseVisual(exercise: Exercise?, media: Media?, gender: Gender, modifier: Modifier = Modifier, playing: Boolean = true) {
    val model = OwnFitApp.instance.store.resolveMedia(media?.image)
    Box(modifier, contentAlignment = Alignment.Center) {
        if (model != null) {
            AsyncImage(
                model = model,
                contentDescription = exercise?.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp).clip(RoundedCornerShape(24.dp)),
            )
        } else {
            ExerciseAnimation(exercise, gender, Modifier.fillMaxSize(), playing = playing)
            if (exercise?.muscles?.isNotEmpty() == true) {
                BodyFigure(gender, exercise.muscles, Modifier.align(Alignment.TopEnd).padding(4.dp).width(34.dp))
            }
        }
    }
}

/** Inline YouTube player (privacy-enhanced embed). */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubeEmbed(url: String, modifier: Modifier = Modifier) {
    val id = YouTube.id(url) ?: return
    val ctx = LocalContext.current
    val html = """
        <html><head><meta name="viewport" content="width=device-width, initial-scale=1">
        <style>html,body{margin:0;padding:0;background:#000;height:100%}iframe{border:0;width:100%;height:100%}</style></head>
        <body><iframe src="https://www.youtube-nocookie.com/embed/$id?playsinline=1&rel=0&modestbranding=1"
          allow="autoplay; encrypted-media; picture-in-picture" allowfullscreen></iframe></body></html>
    """.trimIndent()
    var webView: WebView? = null
    AndroidView(
        factory = {
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                webChromeClient = WebChromeClient()
                loadDataWithBaseURL("https://ownfit.app", html, "text/html", "utf-8", null)
                webView = this
            }
        },
        modifier = modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(16.dp)),
    )
    DisposableEffect(id) { onDispose { webView?.destroy() } }
}
