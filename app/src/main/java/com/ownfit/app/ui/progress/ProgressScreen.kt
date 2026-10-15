package com.ownfit.app.ui.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.ownfit.app.data.AppState
import com.ownfit.app.data.Session
import com.ownfit.app.ui.components.SectionTitle
import com.ownfit.app.ui.components.fmtMinutes
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

data class Stats(
    val streak: Int, val thisWeek: Int, val total: Int, val minutes: Int, val kcal: Int,
    val perDay: Map<LocalDate, Int>,
) {
    companion object {
        fun day(s: Session): LocalDate = Instant.ofEpochMilli(s.startedAt).atZone(ZoneId.systemDefault()).toLocalDate()

        fun of(history: List<Session>): Stats {
            val perDay = history.groupingBy { day(it) }.eachCount()
            val today = LocalDate.now()
            var d = if (today in perDay) today else today.minusDays(1)
            var streak = 0
            while (d in perDay) { streak++; d = d.minusDays(1) }
            val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            return Stats(
                streak = streak,
                thisWeek = history.count { !day(it).isBefore(monday) },
                total = history.size,
                minutes = history.sumOf { it.durationSec } / 60,
                kcal = history.sumOf { it.kcal },
                perDay = perDay,
            )
        }
    }
}

@Composable
fun ProgressScreen(state: AppState) {
    val stats = remember(state.history) { Stats.of(state.history) }
    val fmt = remember { DateTimeFormatter.ofPattern("EEE d MMM") }
    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 24.dp),
    ) {
        item {
            Text("Progress", style = MaterialTheme.typography.headlineMedium)
            Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("🔥 ${stats.streak}", "day streak", Modifier.weight(1f))
                StatCard("${stats.total}", "workouts", Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("${stats.minutes}", "minutes", Modifier.weight(1f))
                StatCard("${stats.kcal}", "kcal (est.)", Modifier.weight(1f))
            }
            SectionTitle("Last 16 weeks")
            Heatmap(stats.perDay)
            SectionTitle("History")
            if (state.history.isEmpty()) Text(
                "Your finished workouts will show up here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        items(state.history.sortedByDescending { it.startedAt }.take(100), key = { it.id }) { s ->
            ListItem(
                headlineContent = { Text(s.workoutName) },
                supportingContent = { Text("${Stats.day(s).format(fmt)} · ${fmtMinutes(s.durationSec)} · ${s.kcal} kcal") },
                trailingContent = { Text(when (s.feedback) { 1 -> "😌"; -1 -> "🥵"; else -> "💪" }) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )
        }
    }
}

@Composable
private fun StatCard(value: String, label: String, modifier: Modifier) {
    Card(
        modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(value, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Heatmap(perDay: Map<LocalDate, Int>) {
    val weeks = 16
    val today = LocalDate.now()
    val start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks((weeks - 1).toLong())
    val cs = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        for (w in 0 until weeks) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                for (d in 0 until 7) {
                    val date = start.plusDays((w * 7 + d).toLong())
                    val n = perDay[date] ?: 0
                    val color = when {
                        date.isAfter(today) -> Color.Transparent
                        n == 0 -> cs.surfaceVariant
                        else -> lerp(cs.primary.copy(alpha = 0.55f), cs.primary, (n - 1).coerceAtMost(2) / 2f)
                    }
                    Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(3.dp)).background(color))
                }
            }
        }
    }
}
