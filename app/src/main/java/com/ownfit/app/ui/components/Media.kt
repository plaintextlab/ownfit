package com.ownfit.app.ui.components

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
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
import com.ownfit.app.data.Trainer

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
fun ExerciseVisual(exercise: Exercise?, media: Media?, trainer: Trainer, modifier: Modifier = Modifier, playing: Boolean = true) {
    val model = OwnFitApp.instance.store.resolveMedia(media?.image)
    val ctx = LocalContext.current
    val gender = if (trainer == Trainer.FEMALE) Gender.FEMALE else Gender.MALE
    Box(modifier, contentAlignment = Alignment.Center) {
        if (model != null) {
            AsyncImage(
                model = model,
                contentDescription = exercise?.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp).clip(RoundedCornerShape(24.dp)),
            )
        } else {
            if (exercise != null && ExercisePhotos.has(ctx, exercise.id)) {
                ExercisePhotoLoop(exercise.id, Modifier.fillMaxSize(), playing = playing)
            } else {
                ExerciseAnimation(exercise, gender, Modifier.fillMaxSize(), playing = playing)
            }
            if (exercise?.muscles?.isNotEmpty() == true) {
                BodyFigure(gender, exercise.muscles, Modifier.align(Alignment.TopEnd).padding(4.dp).width(34.dp))
            }
        }
    }
}

/**
 * Inline YouTube player.
 *
 * Notes for WebView playback:
 *  - never clip the WebView (video renders on its own hardware layer; clipping turns it black),
 *  - YouTube requires a referrer, so the page is served from an https origin with
 *    referrerpolicy + origin parameters,
 *  - fullscreen is handled by showing the player's custom view over the activity.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubeEmbed(url: String, modifier: Modifier = Modifier) {
    val id = YouTube.id(url) ?: return
    val ctx = LocalContext.current
    val origin = "https://${ctx.packageName}"
    val html = """
        <!DOCTYPE html><html><head>
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <meta name="referrer" content="strict-origin-when-cross-origin">
        <style>html,body{margin:0;padding:0;background:#000;height:100%;overflow:hidden}
        iframe{position:absolute;top:0;left:0;width:100%;height:100%;border:0}</style></head>
        <body><iframe
          src="https://www.youtube.com/embed/$id?playsinline=1&rel=0&enablejsapi=1&origin=$origin&widget_referrer=$origin"
          referrerpolicy="strict-origin-when-cross-origin"
          allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
          allowfullscreen></iframe></body></html>
    """.trimIndent()
    key(id) {
        val holder = remember { arrayOfNulls<WebView>(1) }
        Box(
            modifier.fillMaxWidth().aspectRatio(16f / 9f)
                .background(androidx.compose.ui.graphics.Color.Black, RoundedCornerShape(12.dp)),
        ) {
            AndroidView(
                factory = { c ->
                    WebView(c).apply {
                        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                        setBackgroundColor(android.graphics.Color.BLACK)
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        webChromeClient = FullscreenChromeClient(c.findActivity())
                        webViewClient = object : WebViewClient() {
                            // Links inside the player (YouTube logo, "watch on YouTube") open the YouTube app.
                            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                                val u = request.url.toString()
                                if (request.isForMainFrame && !u.startsWith(origin)) {
                                    runCatching { c.startActivity(Intent(Intent.ACTION_VIEW, request.url)) }
                                    return true
                                }
                                return false
                            }
                        }
                        loadDataWithBaseURL(origin, html, "text/html", "utf-8", null)
                        holder[0] = this
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
        DisposableEffect(Unit) {
            onDispose {
                holder[0]?.apply { stopLoading(); loadUrl("about:blank"); destroy() }
                holder[0] = null
            }
        }
    }
}

private fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

/** Shows YouTube's fullscreen view over the whole activity, hiding the system bars. */
private class FullscreenChromeClient(private val activity: Activity?) : WebChromeClient() {
    private var custom: View? = null
    private var callback: CustomViewCallback? = null

    override fun onShowCustomView(view: View, cb: CustomViewCallback) {
        val decor = activity?.window?.decorView as? FrameLayout
        if (decor == null || custom != null) { cb.onCustomViewHidden(); return }
        custom = view
        callback = cb
        view.setBackgroundColor(android.graphics.Color.BLACK)
        decor.addView(view, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        WindowCompat.getInsetsController(activity.window, decor).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun onHideCustomView() {
        val decor = activity?.window?.decorView as? FrameLayout ?: return
        custom?.let { decor.removeView(it) }
        custom = null
        WindowCompat.getInsetsController(activity.window, decor).show(WindowInsetsCompat.Type.systemBars())
        callback?.onCustomViewHidden()
        callback = null
    }

    // Avoid the grey placeholder poster before the player paints.
    override fun getDefaultVideoPoster(): Bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
}
