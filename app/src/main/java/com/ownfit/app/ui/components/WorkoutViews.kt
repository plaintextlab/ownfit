package com.ownfit.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ownfit.app.data.AppState
import com.ownfit.app.data.Section
import com.ownfit.app.data.Workout

/** Compact list of a workout's moves grouped by section. */
@Composable
fun WorkoutList(state: AppState, w: Workout, onExercise: (String) -> Unit) {
    var lastSection: Section? = null
    var n = 0
    Column {
        w.items.forEach { item ->
            if (item.section != lastSection && w.items.map { it.section }.distinct().size > 1) {
                lastSection = item.section
                Text(
                    item.section.label.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                )
            }
            n++
            val e = state.exercise(item.exerciseId)
            Row(
                Modifier.fillMaxWidth().clickable { onExercise(item.exerciseId) }.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.size(30.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("$n", style = MaterialTheme.typography.labelMedium)
                    }
                }
                Text(
                    e?.name ?: item.exerciseId,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                )
                Text(item.label(e), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
