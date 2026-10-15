package com.ownfit.app.ui.workouts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ownfit.app.OwnFitApp
import com.ownfit.app.data.AppState
import com.ownfit.app.data.Store
import com.ownfit.app.data.Workout
import com.ownfit.app.data.WorkoutItem
import com.ownfit.app.engine.PlanGenerator
import com.ownfit.app.ui.Nav
import com.ownfit.app.ui.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutEditorScreen(state: AppState, workoutId: String?, nav: Nav) {
    val store = OwnFitApp.instance.store
    val existing = workoutId?.let { id -> state.workouts.firstOrNull { it.id == id } }
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var items by remember { mutableStateOf(existing?.items ?: emptyList()) }
    var picking by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    fun current() = (existing ?: Workout(id = "w-" + Store.newId(), name = "", items = emptyList()))
        .copy(name = name.trim().ifBlank { "My workout" }, items = items)

    fun update(i: Int, f: (WorkoutItem) -> WorkoutItem) {
        items = items.toMutableList().also { it[i] = f(it[i]) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existing == null) "New workout" else "Edit workout") },
                navigationIcon = { IconButton(onClick = { nav.back() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    if (existing != null) IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Outlined.Delete, "Delete") }
                    TextButton(enabled = items.isNotEmpty(), onClick = { store.upsertWorkout(current()); nav.back() }) { Text("Save") }
                },
            )
        },
    ) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad).imePadding(),
            contentPadding = PaddingValues(20.dp, 8.dp, 20.dp, 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                OutlinedTextField(name, { name = it }, label = { Text("Workout name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                if (items.isNotEmpty()) Text(
                    "${items.size} moves · ~${PlanGenerator.estimateMinutes(current()) { state.exercise(it) }} min",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            itemsIndexed(items) { i, it ->
                val e = state.exercise(it.exerciseId)
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                ) {
                    Column(Modifier.padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(e?.name ?: it.exerciseId, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            IconButton(onClick = { if (i > 0) items = items.toMutableList().apply { add(i - 1, removeAt(i)) } }, enabled = i > 0) {
                                Icon(Icons.Outlined.KeyboardArrowUp, "Move up")
                            }
                            IconButton(onClick = { if (i < items.lastIndex) items = items.toMutableList().apply { add(i + 1, removeAt(i)) } }, enabled = i < items.lastIndex) {
                                Icon(Icons.Outlined.KeyboardArrowDown, "Move down")
                            }
                            IconButton(onClick = { items = items.toMutableList().apply { removeAt(i) } }) { Icon(Icons.Outlined.Delete, "Remove") }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Counter("Sets", it.sets, 1..10) { v -> update(i) { x -> x.copy(sets = v) } }
                            if (it.seconds != null) Counter("Secs", it.seconds, 5..600, step = 5) { v -> update(i) { x -> x.copy(seconds = v) } }
                            else Counter("Reps", it.reps ?: 10, 1..100) { v -> update(i) { x -> x.copy(reps = v) } }
                            Counter("Rest", it.restSec, 0..300, step = 5) { v -> update(i) { x -> x.copy(restSec = v) } }
                        }
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { picking = true }) { Icon(Icons.Default.Add, null); Text("  Add exercises") }
                    if (items.isNotEmpty()) Button(onClick = { nav.go(Screen.Player(current(), fromPlan = false)) }) {
                        Icon(Icons.Default.PlayArrow, null); Text("  Try it")
                    }
                }
            }
        }
    }

    if (picking) ExercisePicker(state, onDismiss = { picking = false }) { ids ->
        picking = false
        items = items + ids.mapNotNull { id ->
            val e = state.exercise(id) ?: return@mapNotNull null
            if (e.timed) WorkoutItem(id, sets = 1, seconds = 40, restSec = 20)
            else WorkoutItem(id, sets = 3, reps = 12, restSec = 30)
        }
    }

    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text("Delete workout?") },
        confirmButton = { TextButton(onClick = { existing?.let { store.deleteWorkout(it.id) }; confirmDelete = false; nav.back() }) { Text("Delete") } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
    )
}

@Composable
private fun Counter(label: String, value: Int, range: IntRange, step: Int = 1, onChange: (Int) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { onChange((value - step).coerceIn(range)) }, contentPadding = PaddingValues(0.dp), modifier = Modifier.width(36.dp)) { Text("−") }
            Text("$value", style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = { onChange((value + step).coerceIn(range)) }, contentPadding = PaddingValues(0.dp), modifier = Modifier.width(36.dp)) { Text("+") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExercisePicker(state: AppState, onDismiss: () -> Unit, onPicked: (List<String>) -> Unit) {
    var selected by remember { mutableStateOf(listOf<String>()) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(if (selected.isEmpty()) "Add exercises" else "${selected.size} selected") },
                    navigationIcon = { IconButton(onClick = onDismiss) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Close") } },
                    actions = { TextButton(enabled = selected.isNotEmpty(), onClick = { onPicked(selected) }) { Text("Add") } },
                )
            },
        ) { pad ->
            Column(Modifier.padding(pad)) {
                ExerciseLibraryList(state, selected.toSet()) { e ->
                    selected = if (e.id in selected) selected - e.id else selected + e.id
                }
            }
        }
    }
}
