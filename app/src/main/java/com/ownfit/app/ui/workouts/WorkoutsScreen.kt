package com.ownfit.app.ui.workouts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.ownfit.app.data.AppState
import com.ownfit.app.data.Category
import com.ownfit.app.data.Exercise
import com.ownfit.app.data.Kit
import com.ownfit.app.data.Muscle
import com.ownfit.app.engine.PlanGenerator
import com.ownfit.app.ui.Nav
import com.ownfit.app.ui.Screen
import com.ownfit.app.ui.components.ExerciseAnimation
import com.ownfit.app.ui.onboarding.Segmented

@Composable
fun WorkoutsScreen(state: AppState, nav: Nav) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var newExercise by remember { mutableStateOf(false) }
    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { if (tab == 0) nav.go(Screen.Editor(null)) else newExercise = true },
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text(if (tab == 0) "New workout" else "New exercise") },
            )
        },
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
    ) { pad ->
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(pad)) {
            Text("Workouts", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(20.dp, 12.dp, 20.dp, 12.dp))
            Column(Modifier.padding(horizontal = 20.dp)) {
                Segmented(listOf(0, 1), tab, { if (it == 0) "My workouts" else "Exercises" }) { tab = it }
            }
            if (tab == 0) MyWorkouts(state, nav) else ExerciseLibraryList(state) { nav.go(Screen.ExerciseDetail(it.id)) }
        }
    }
    if (newExercise) ExerciseEditorDialog(null, onDismiss = { newExercise = false }) { id ->
        newExercise = false
        nav.go(Screen.ExerciseDetail(id))
    }
}

@Composable
private fun MyWorkouts(state: AppState, nav: Nav) {
    val list = state.activeWorkouts.sortedByDescending { it.updatedAt }
    if (list.isEmpty()) {
        Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Build your own routine", style = MaterialTheme.typography.titleMedium)
            Text(
                "Pick exercises, set reps or time, and play it with the guided timer.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        return
    }
    LazyColumn(contentPadding = PaddingValues(20.dp, 16.dp, 20.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(list, key = { it.id }) { w ->
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                onClick = { nav.go(Screen.Editor(w.id)) },
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(w.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${w.items.size} moves · ~${PlanGenerator.estimateMinutes(w) { state.exercise(it) }} min",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { nav.go(Screen.Editor(w.id)) }) { Icon(Icons.Outlined.Edit, "Edit") }
                    FilledIconButton(onClick = { nav.go(Screen.Player(w, fromPlan = false)) }, enabled = w.items.isNotEmpty()) {
                        Icon(Icons.Default.PlayArrow, "Start")
                    }
                }
            }
        }
    }
}

/** Searchable, filterable list of every exercise. Used by the library tab and the picker. */
@Composable
fun ExerciseLibraryList(
    state: AppState,
    selected: Set<String> = emptySet(),
    contentPadding: PaddingValues = PaddingValues(bottom = 96.dp),
    onClick: (Exercise) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var muscle by rememberSaveable { mutableStateOf<Muscle?>(null) }
    var cat by rememberSaveable { mutableStateOf<Category?>(null) }
    val list = remember(state.exercises, query, muscle, cat) {
        state.exercises.filter { e ->
            (query.isBlank() || e.name.contains(query, ignoreCase = true)) &&
                (muscle == null || muscle in e.muscles) &&
                (cat == null || e.category == cat)
        }.sortedBy { it.name }
    }
    Column {
        OutlinedTextField(
            query, { query = it },
            leadingIcon = { Icon(Icons.Outlined.Search, null) },
            placeholder = { Text("Search exercises") },
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth().padding(20.dp, 12.dp, 20.dp, 4.dp),
        )
        LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(Muscle.entries) { m ->
                FilterChip(muscle == m, { muscle = if (muscle == m) null else m }, { Text(m.label) })
            }
        }
        LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(Category.entries) { c ->
                FilterChip(cat == c, { cat = if (cat == c) null else c }, { Text(c.label) })
            }
        }
        LazyColumn(contentPadding = contentPadding) {
            items(list, key = { it.id }) { e ->
                val sel = e.id in selected
                ListItem(
                    leadingContent = {
                        ExerciseAnimation(
                            e, state.profile?.gender ?: com.ownfit.app.data.Gender.OTHER,
                            Modifier.size(52.dp).clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                            playing = false,
                        )
                    },
                    headlineContent = { Text(e.name) },
                    supportingContent = {
                        Text(
                            listOfNotNull(
                                e.category.label,
                                e.muscles.joinToString { it.label }.ifBlank { null },
                                e.kit.takeIf { it != Kit.NONE }?.label,
                                if (e.custom) "Custom" else null,
                            ).joinToString(" · "),
                        )
                    },
                    trailingContent = if (sel) ({ Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary) }) else null,
                    colors = ListItemDefaults.colors(
                        containerColor = if (sel) MaterialTheme.colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Transparent,
                    ),
                    modifier = Modifier.clickable { onClick(e) }.padding(horizontal = 8.dp),
                )
            }
        }
    }
}
