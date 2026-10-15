package com.ownfit.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ownfit.app.data.Exercise
import com.ownfit.app.data.WorkoutItem

fun WorkoutItem.label(e: Exercise?): String {
    val side = if (e?.perSide == true) " / side" else ""
    val each = seconds?.let { "${it}s" } ?: "${reps ?: 10} reps"
    return if (sets > 1) "$sets × $each$side" else "$each$side"
}

fun fmtClock(sec: Int): String = "%d:%02d".format(sec / 60, sec % 60)

fun fmtMinutes(sec: Int): String = if (sec < 60) "${sec}s" else "${(sec + 30) / 60} min"

@Composable
fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
) {
    androidx.compose.foundation.layout.Row(
        modifier.padding(top = 20.dp, bottom = 8.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        if (icon != null) Icon(icon, null, tint = tint, modifier = Modifier.padding(end = 8.dp).size(20.dp))
        Text(text, style = MaterialTheme.typography.titleSmall, color = tint)
    }
}

/** Wrapping chips. [selected] decides check state; [onToggle] fires on tap. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> ChipGroup(
    options: List<T>,
    selected: (T) -> Boolean,
    label: (T) -> String,
    onToggle: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
        options.forEach { o ->
            val sel = selected(o)
            FilterChip(
                selected = sel,
                onClick = { onToggle(o) },
                label = { Text(label(o)) },
                leadingIcon = if (sel) ({ Icon(Icons.Default.Check, null, Modifier.padding(0.dp)) }) else null,
                colors = FilterChipDefaults.filterChipColors(),
            )
        }
    }
}

@Composable
fun Labeled(title: String, content: @Composable () -> Unit) {
    Column {
        SectionTitle(title)
        content()
    }
}
