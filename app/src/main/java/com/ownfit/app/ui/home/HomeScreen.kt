package com.ownfit.app.ui.home

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.ownfit.app.data.AppState
import com.ownfit.app.data.Focus
import com.ownfit.app.data.Quotes
import com.ownfit.app.data.Workout
import com.ownfit.app.engine.PlanGenerator
import com.ownfit.app.ui.Nav
import com.ownfit.app.ui.Screen
import com.ownfit.app.ui.components.BodyFigure
import com.ownfit.app.ui.components.SectionTitle
import com.ownfit.app.ui.components.WorkoutList
import com.ownfit.app.ui.progress.Stats
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun HomeScreen(state: AppState, nav: Nav) {
    val profile = state.profile ?: return
    val plan = state.plan
    val todayIdx = plan?.let { PlanGenerator.todayIndex(it, state.history) } ?: 0
    val today = plan?.days?.getOrNull(todayIdx)
    val stats = remember(state.history) { Stats.of(state.history) }

    var focus by rememberSaveable { mutableStateOf<Focus?>(null) }
    var minutes by rememberSaveable { mutableIntStateOf(profile.minutes) }
    var shuffle by rememberSaveable { mutableLongStateOf(0L) }
    var quote by remember { mutableStateOf(Quotes.today()) }

    val focusOptions = Focus.entries.filter {
        (it != Focus.YOGA || profile.includeYoga) && (it != Focus.BREATHE || profile.includeBreathing)
    }

    Column(
        Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
    ) {
        // Header
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(greeting() + if (profile.name.isNotBlank()) ", ${profile.name}" else "", style = MaterialTheme.typography.headlineMedium)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                    Icon(Icons.Outlined.LocalFireDepartment, null, Modifier.padding(end = 4.dp), tint = MaterialTheme.colorScheme.tertiary)
                    Text(
                        "${stats.streak}-day streak · ${stats.thisWeek}/${profile.daysPerWeek} this week",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(onClick = { nav.go(Screen.Settings) }) { Icon(Icons.Outlined.Settings, "Settings") }
        }

        // Quote
        Card(
            onClick = { quote = Quotes.random() },
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.FormatQuote, null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                Text(
                    quote, Modifier.padding(start = 10.dp),
                    style = MaterialTheme.typography.bodyLarge, fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
        }

        // Today's plan
        if (today != null) {
            SectionTitle("Today · ${today.title}")
            WorkoutCard(state, today.workout, today.focus, big = true, nav = nav, onStart = {
                nav.go(Screen.Player(today.workout, fromPlan = true))
            })
            if (plan.days.size > 1) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 10.dp)) {
                    items(plan.days.size) { i ->
                        val d = plan.days[i]
                        FilterChip(
                            selected = i == todayIdx,
                            onClick = { nav.go(Screen.Player(d.workout, fromPlan = i == todayIdx)) },
                            label = { Text("${d.title.substringBefore(" ·")}: ${d.focus.label}") },
                        )
                    }
                }
            }
        }

        // Quick pick
        SectionTitle("Or train something specific")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(focusOptions) { f ->
                FilterChip(selected = focus == f, onClick = { focus = if (focus == f) null else f }, label = { Text(f.label) })
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
            items(listOf(5, 10, 15, 20, 30, 45, 60)) { m ->
                FilterChip(selected = minutes == m, onClick = { minutes = m }, label = { Text("$m min") })
            }
        }
        focus?.let { f ->
            val w = remember(f, minutes, shuffle, state.profile, state.customExercises) {
                val seed = LocalDate.now().toEpochDay() * 31 + f.ordinal + shuffle * 1_000_003
                PlanGenerator.generate(profile, state.exercises, f, minutes, seed)
            }
            Spacer(Modifier.height(10.dp))
            WorkoutCard(state, w, f, big = false, nav = nav, onShuffle = { shuffle++ }, onStart = {
                nav.go(Screen.Player(w, fromPlan = false))
            })
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun WorkoutCard(
    state: AppState, w: Workout, focus: Focus, big: Boolean, nav: Nav,
    onStart: () -> Unit, onShuffle: (() -> Unit)? = null,
) {
    var expanded by rememberSaveable(w.id) { mutableStateOf(false) }
    val mins = PlanGenerator.estimateMinutes(w) { state.exercise(it) }
    val cs = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = if (big) cs.primaryContainer else cs.surfaceContainerHigh),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(focus.label, style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "${w.items.size} moves · ~$mins min",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (big) cs.onPrimaryContainer else cs.onSurfaceVariant,
                    )
                }
                if (focus != Focus.BREATHE) Box(Modifier.width(56.dp)) {
                    BodyFigure(state.profile!!.gender, focus.muscles.ifEmpty { com.ownfit.app.data.Muscle.entries.toList() }.toSet())
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = onStart, modifier = Modifier.height(52.dp)) {
                    Icon(Icons.Default.PlayArrow, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Start")
                }
                Spacer(Modifier.width(8.dp))
                if (onShuffle != null) FilledTonalButton(onClick = onShuffle, modifier = Modifier.height(52.dp)) {
                    Icon(Icons.Outlined.Shuffle, "Shuffle")
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "Hide" else "Preview") }
            }
            if (expanded) WorkoutList(state, w) { nav.go(Screen.ExerciseDetail(it)) }
        }
    }
}

private fun greeting(): String = when (LocalTime.now().hour) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    else -> "Good evening"
}

