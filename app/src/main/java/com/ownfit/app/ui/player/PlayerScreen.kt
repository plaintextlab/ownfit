package com.ownfit.app.ui.player

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.OndemandVideo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ownfit.app.OwnFitApp
import com.ownfit.app.data.AppState
import com.ownfit.app.data.Exercise
import com.ownfit.app.data.Session
import com.ownfit.app.data.Store
import com.ownfit.app.data.Workout
import com.ownfit.app.data.WorkoutItem
import com.ownfit.app.engine.PlanGenerator
import com.ownfit.app.ui.components.ExerciseVisual
import com.ownfit.app.ui.components.YouTubeEmbed
import com.ownfit.app.ui.components.fmtClock
import kotlinx.coroutines.delay
import com.ownfit.app.audio.Coach
import com.ownfit.app.audio.Music
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.MusicOff

private sealed interface Step {
    val seconds: Int?
    data class Ready(override val seconds: Int = 5, val next: Work) : Step
    data class Work(
        val item: WorkoutItem, val exercise: Exercise?, val set: Int, val sets: Int,
        val side: Int?, override val seconds: Int?,
    ) : Step
    data class Rest(override val seconds: Int, val next: Work) : Step
}

private fun buildSteps(w: Workout, state: AppState): List<Step> {
    val work = mutableListOf<Step.Work>()
    w.items.forEach { item ->
        val e = state.exercise(item.exerciseId)
        for (set in 1..item.sets.coerceAtLeast(1)) {
            if (e?.perSide == true && item.seconds != null) {
                work += Step.Work(item, e, set, item.sets, 1, item.seconds)
                work += Step.Work(item, e, set, item.sets, 2, item.seconds)
            } else work += Step.Work(item, e, set, item.sets, null, item.seconds)
        }
    }
    if (work.isEmpty()) return emptyList()
    val out = mutableListOf<Step>(Step.Ready(next = work.first()))
    work.forEachIndexed { i, s ->
        out += s
        val next = work.getOrNull(i + 1) ?: return@forEachIndexed
        // Rest after the work step (shorter transition between sides).
        val rest = if (s.side == 1) 5 else s.item.restSec
        if (rest > 0) out += Step.Rest(rest, next)
    }
    return out
}

@Composable
fun PlayerScreen(state: AppState, workout: Workout, fromPlan: Boolean, onExit: () -> Unit) {
    val app = OwnFitApp.instance
    val ctx = LocalContext.current
    val steps = remember(workout) { buildSteps(workout, state) }
    val trainer = state.trainer
    val cues = remember { Coach(ctx, trainer, state.settings.voice, state.settings.sounds) }
    DisposableEffect(Unit) {
        // Calm music for yoga and breathing unless the user picked their own songs
        val calm = workout.focus == com.ownfit.app.data.Focus.YOGA || workout.focus == com.ownfit.app.data.Focus.BREATHE
        Music.start(ctx, if (calm && state.settings.music != "mine") state.settings.copy(music = com.ownfit.app.data.MusicTrack.CALM.id) else state.settings)
        onDispose { cues.release(); Music.stop() }
    }

    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    var index by remember { mutableIntStateOf(0) }
    var remaining by remember { mutableIntStateOf(steps.firstOrNull()?.seconds ?: 0) }
    var paused by remember { mutableStateOf(false) }
    var elapsed by remember { mutableIntStateOf(0) }
    var finished by remember { mutableStateOf(steps.isEmpty()) }
    var confirmExit by remember { mutableStateOf(false) }
    var showVideo by remember { mutableStateOf(false) }
    val startedAt = remember { System.currentTimeMillis() }

    fun goTo(i: Int) {
        if (i >= steps.size) { finished = true; cues.done(); return }
        index = i.coerceAtLeast(0)
        remaining = steps[index].seconds ?: 0
        showVideo = false
        when (val s = steps[index]) {
            is Step.Ready -> cues.ready(s.next.exercise)
            is Step.Work -> cues.work(s.exercise, s.item, s.side, lastSet = s.set == s.sets)
            is Step.Rest -> cues.rest(s.next.exercise)
        }
    }

    LaunchedEffect(Unit) { if (steps.isNotEmpty()) goTo(0) }

    // Main clock
    LaunchedEffect(index, paused, finished, showVideo) {
        while (!paused && !finished && !showVideo) {
            delay(1000)
            elapsed++
            val s = steps[index]
            if (s.seconds != null) {
                remaining--
                // Spoken 3-2-1 only on work/longer rests so it doesn't cut off the "next up" call
                if (remaining in 1..3 && (s is Step.Work || (s.seconds ?: 0) >= 10)) cues.countdown(remaining)
                if (s is Step.Work && s.exercise?.breath == null && (s.seconds ?: 0) >= 30 && remaining == (s.seconds ?: 0) / 2) cues.encourage(halfway = true)
                if (remaining <= 0) { goTo(index + 1); break }
            }
        }
    }

    BackHandler { if (finished) onExit() else confirmExit = true }

    if (finished) {
        val done = steps.count { it is Step.Work } > 0
        Summary(state, workout, elapsed, done) { feedback ->
            val exercises = workout.items.map { it.exerciseId }.distinct().size
            app.store.addSession(
                Session(
                    id = Store.newId(), workoutName = workout.name, focus = workout.focus,
                    startedAt = startedAt, durationSec = elapsed, exercises = exercises,
                    kcal = PlanGenerator.kcal(state.profile, workout.focus, elapsed),
                    feedback = feedback, fromPlan = fromPlan,
                ),
            )
            val p = state.profile
            if (p != null && feedback != 0) {
                val np = p.copy(intensity = (p.intensity + feedback).coerceIn(-2, 2))
                if (np.intensity != p.intensity) {
                    app.store.setProfile(np)
                    // Keep on-device plans in step with feedback; AI plans pick it up on next rebuild.
                    if (state.plan?.source != "Claude") app.store.setPlan(PlanGenerator.buildPlan(np, state.exercises))
                }
            }
            app.autoSync()
            onExit()
        }
        return
    }

    val step = steps[index]
    val work: Step.Work = when (step) {
        is Step.Work -> step
        is Step.Rest -> step.next
        is Step.Ready -> step.next
    }
    val media = state.media[work.item.exerciseId]

    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        // Top bar
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { confirmExit = true }) { Icon(Icons.Default.Close, "End") }
            LinearProgressIndicator(
                progress = { (index + 1f) / steps.size },
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            )
            Text(fmtClock(elapsed), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 4.dp))
            if (Music.active) IconButton(onClick = { Music.toggle() }) {
                Icon(if (Music.playing) Icons.Outlined.MusicNote else Icons.Outlined.MusicOff, if (Music.playing) "Pause music" else "Play music")
            }
        }

        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val label = when (step) {
                is Step.Ready -> "GET READY"
                is Step.Rest -> "REST · UP NEXT"
                is Step.Work -> work.item.section.label.uppercase()
            }
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
            Text(
                work.exercise?.name ?: work.item.exerciseId,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp),
            )
            val meta = buildList {
                if (work.sets > 1) add("Set ${work.set} of ${work.sets}")
                work.side?.let { add(if (it == 1) "First side" else "Other side") }
                if (work.side == null && work.exercise?.perSide == true) add("Each side")
            }.joinToString(" · ")
            if (meta.isNotEmpty()) Text(meta, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(Modifier.height(16.dp))
            val breath = work.exercise?.breath
            if (step is Step.Work && breath != null) {
                androidx.compose.runtime.key(index) { BreathingGuide(breath, paused) { phase -> cues.breath(phase) } }
            } else if (showVideo && media?.youtube != null) {
                YouTubeEmbed(media.youtube)
            } else {
                ExerciseVisual(work.exercise, media, trainer, Modifier.fillMaxWidth().height(300.dp), playing = !paused)
            }

            if (media?.youtube != null && breath == null) {
                Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = {
                        showVideo = !showVideo
                        if (showVideo) Music.pause() // let the video's sound through
                    }) {
                        Icon(Icons.Outlined.OndemandVideo, null)
                        Text(if (showVideo) "  Hide video (timer paused)" else "  Watch demo")
                    }
                    // Some uploaders block embedding — the YouTube app always works.
                    TextButton(onClick = {
                        showVideo = false
                        paused = true
                        Music.pause()
                        com.ownfit.app.ui.components.YouTube.open(ctx, media.youtube)
                    }) { Text("Open in YouTube") }
                }
            }

            work.exercise?.description?.takeIf { it.isNotBlank() && !(step is Step.Work && breath != null) }?.let {
                Text(
                    it, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp),
                )
            }
        }

        // Big counter
        Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
            if (step.seconds != null) {
                TimerRing(remaining, step.seconds!!, if (step is Step.Work) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary)
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${work.item.reps ?: 10}", fontSize = 72.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text(if (work.exercise?.perSide == true) "reps each side" else "reps", style = MaterialTheme.typography.titleMedium)
                }
            }
        }

        // Controls
        Row(
            Modifier.fillMaxWidth().padding(24.dp, 8.dp, 24.dp, 24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { goTo(index - 1) }, enabled = index > 0) { Icon(Icons.Default.SkipPrevious, "Previous", Modifier.size(32.dp)) }
            if (step.seconds == null) {
                Button(onClick = { goTo(index + 1) }, modifier = Modifier.height(64.dp)) {
                    Icon(Icons.Outlined.CheckCircle, null)
                    Text("  Done", style = MaterialTheme.typography.titleMedium)
                }
            } else {
                FilledIconButton(
                    onClick = { paused = !paused },
                    modifier = Modifier.size(80.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(),
                ) {
                    Icon(if (paused) Icons.Default.PlayArrow else Icons.Default.Pause, if (paused) "Resume" else "Pause", Modifier.size(40.dp))
                }
            }
            IconButton(onClick = { goTo(index + 1) }) { Icon(Icons.Default.SkipNext, "Skip", Modifier.size(32.dp)) }
        }
        if (step is Step.Rest) {
            Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.Center) {
                OutlinedButton(onClick = { remaining += 15 }) { Text("+15s") }
            }
        }
    }

    if (confirmExit) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            title = { Text("End workout?") },
            text = { Text("You can save what you've done so far.") },
            confirmButton = { TextButton(onClick = { confirmExit = false; finished = true }) { Text("Finish & save") } },
            dismissButton = {
                Row {
                    TextButton(onClick = { confirmExit = false; onExit() }) { Text("Discard") }
                    TextButton(onClick = { confirmExit = false }) { Text("Keep going") }
                }
            },
        )
    }
}

@Composable
private fun TimerRing(remaining: Int, total: Int, color: androidx.compose.ui.graphics.Color) {
    val frac by animateFloatAsState(remaining.toFloat() / total.coerceAtLeast(1), tween(900), label = "ring")
    val track = MaterialTheme.colorScheme.surfaceVariant
    Box(Modifier.size(150.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val sw = 12.dp.toPx()
            drawArc(track, 0f, 360f, false, style = Stroke(sw))
            drawArc(color, -90f, 360f * frac, false, style = Stroke(sw, cap = StrokeCap.Round))
        }
        Text(fmtClock(remaining.coerceAtLeast(0)), fontSize = 40.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun BreathingGuide(p: com.ownfit.app.data.BreathPattern, paused: Boolean, onPhase: (String) -> Unit) {
    val phases = listOf(
        Triple("Breathe in", p.inhale, 1f), Triple("Hold", p.hold1, 1f),
        Triple("Breathe out", p.exhale, 0.45f), Triple("Hold", p.hold2, 0.45f),
    ).filter { it.second > 0 }
    var phase by remember { mutableIntStateOf(0) }
    var left by remember { mutableIntStateOf(phases[0].second) }
    LaunchedEffect(paused) {
        while (!paused) {
            if (left == phases[phase].second) onPhase(phases[phase].first)
            delay(1000)
            left--
            if (left <= 0) {
                phase = (phase + 1) % phases.size
                left = phases[phase].second
            }
        }
    }
    val (name, secs, target) = phases[phase]
    val scale by animateFloatAsState(target, tween(secs * 1000), label = "breath")
    val cs = MaterialTheme.colorScheme
    Box(Modifier.size(260.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension / 2
            drawCircle(cs.primaryContainer.copy(alpha = 0.5f), r * scale, Offset(size.width / 2, size.height / 2))
            drawCircle(cs.primary.copy(alpha = 0.85f), r * scale * 0.72f, Offset(size.width / 2, size.height / 2))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(name, style = MaterialTheme.typography.titleLarge, color = cs.onPrimary)
            Text("$left", style = MaterialTheme.typography.headlineMedium, color = cs.onPrimary)
        }
    }
}

@Composable
private fun Summary(state: AppState, w: Workout, seconds: Int, done: Boolean, onSave: (feedback: Int) -> Unit) {
    var feedback by remember { mutableIntStateOf(0) }
    val kcal = PlanGenerator.kcal(state.profile, w.focus, seconds)
    Column(
        Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("🎉", fontSize = 64.sp)
        Text(if (done) "Workout complete!" else "Nice effort!", style = MaterialTheme.typography.headlineMedium)
        Text(com.ownfit.app.data.Quotes.random(), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
        Row(Modifier.fillMaxWidth().padding(vertical = 28.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            Stat(fmtClock(seconds), "time")
            Stat("${w.items.map { it.exerciseId }.distinct().size}", "moves")
            Stat("$kcal", "kcal")
        }
        Text("How did that feel?", style = MaterialTheme.typography.titleMedium)
        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(1 to "Too easy", 0 to "Just right", -1 to "Too hard").forEach { (v, l) ->
                if (feedback == v) Button(onClick = { feedback = v }) { Text(l) }
                else FilledTonalButton(onClick = { feedback = v }) { Text(l) }
            }
        }
        Spacer(Modifier.height(32.dp))
        Button(onClick = { onSave(feedback) }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(28.dp)) {
            Text("Save")
        }
    }
}

@Composable
private fun Stat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

