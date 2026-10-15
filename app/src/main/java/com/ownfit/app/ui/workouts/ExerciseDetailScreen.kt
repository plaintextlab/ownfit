package com.ownfit.app.ui.workouts

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ownfit.app.OwnFitApp
import com.ownfit.app.data.AppState
import com.ownfit.app.data.Category
import com.ownfit.app.data.Exercise
import com.ownfit.app.data.Kit
import com.ownfit.app.data.Media
import com.ownfit.app.data.Muscle
import com.ownfit.app.data.Store
import com.ownfit.app.ui.components.ChipGroup
import com.ownfit.app.ui.components.ExerciseVisual
import com.ownfit.app.ui.components.SectionTitle
import com.ownfit.app.ui.components.YouTube
import com.ownfit.app.ui.components.YouTubeEmbed
import com.ownfit.app.ui.onboarding.Segmented
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDetailScreen(state: AppState, id: String, onBack: () -> Unit) {
    val store = OwnFitApp.instance.store
    val ctx = LocalContext.current
    val e = state.exercise(id)
    val media = state.media[id] ?: Media()
    var yt by remember(id) { mutableStateOf(media.youtube.orEmpty()) }
    var imgUrl by remember(id) { mutableStateOf(media.image?.takeIf { it.startsWith("http") }.orEmpty()) }
    var editing by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            val type = ctx.contentResolver.getType(uri).orEmpty()
            val ext = when {
                "gif" in type -> "gif"; "png" in type -> "png"; "webp" in type -> "webp"; else -> "jpg"
            }
            val name = "$id-${System.currentTimeMillis()}.$ext"
            runCatching {
                ctx.contentResolver.openInputStream(uri)?.use { input ->
                    File(store.mediaDir, name).outputStream().use { input.copyTo(it) }
                }
                store.setMedia(id, media.copy(image = "media/$name"))
                imgUrl = ""
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(e?.name ?: "Exercise") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    if (e?.custom == true) {
                        IconButton(onClick = { editing = true }) { Icon(Icons.Outlined.Edit, "Edit") }
                        IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Outlined.Delete, "Delete") }
                    }
                },
            )
        },
    ) { pad ->
        if (e == null) return@Scaffold
        Column(
            Modifier.fillMaxSize().padding(pad).imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        ) {
            ExerciseVisual(e, media, state.trainer, Modifier.fillMaxWidth().height(320.dp))
            Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip({}, { Text(e.category.label) })
                AssistChip({}, { Text(e.kit.label) })
                AssistChip({}, { Text(listOf("", "Beginner", "Intermediate", "Advanced")[e.level.coerceIn(1, 3)]) })
            }
            if (e.muscles.isNotEmpty()) Text(
                e.muscles.joinToString { it.label },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp),
            )
            if (e.description.isNotBlank()) Text(e.description, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 12.dp))
            if (e.avoidWith.isNotEmpty()) Text(
                "Not recommended with: ${e.avoidWith.joinToString { it.label.lowercase() }} issues",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )

            SectionTitle("Demo video")
            OutlinedTextField(
                yt, { yt = it },
                label = { Text("YouTube link") },
                singleLine = true,
                isError = yt.isNotBlank() && YouTube.id(yt) == null,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                Button(
                    onClick = { store.setMedia(id, media.copy(youtube = yt.trim().ifBlank { null })) },
                    enabled = yt.trim() != media.youtube.orEmpty() && (yt.isBlank() || YouTube.id(yt) != null),
                ) { Text("Save link") }
                if (media.youtube != null) OutlinedButton(onClick = { YouTube.open(ctx, media.youtube) }) {
                    Icon(Icons.AutoMirrored.Outlined.OpenInNew, null); Text("  YouTube")
                }
            }
            media.youtube?.let { YouTubeEmbed(it, Modifier.padding(top = 12.dp)) }

            SectionTitle("Demo GIF or image")
            Text(
                "Shown during the workout instead of the figure.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                imgUrl, { imgUrl = it },
                label = { Text("GIF / image URL") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                Button(
                    onClick = { store.setMedia(id, media.copy(image = imgUrl.trim())) },
                    enabled = imgUrl.trim().startsWith("http") && imgUrl.trim() != media.image,
                ) { Text("Use URL") }
                OutlinedButton(onClick = { picker.launch(arrayOf("image/*")) }) {
                    Icon(Icons.Outlined.Image, null); Text("  From phone")
                }
                if (media.image != null) TextButton(onClick = {
                    media.image.takeIf { !it.startsWith("http") }?.let { File(store.dataDir, it).delete() }
                    store.setMedia(id, media.copy(image = null)); imgUrl = ""
                }) { Text("Remove") }
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    if (editing && e != null) ExerciseEditorDialog(e, onDismiss = { editing = false }) { editing = false }
    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text("Delete exercise?") },
        text = { Text("It will be removed from your library. Workouts using it will skip it.") },
        confirmButton = { TextButton(onClick = { store.deleteExercise(id); confirmDelete = false; onBack() }) { Text("Delete") } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
    )
}

/** Create or edit a custom exercise. Calls [onSaved] with its id. */
@Composable
fun ExerciseEditorDialog(initial: Exercise?, onDismiss: () -> Unit, onSaved: (String) -> Unit) {
    val store = OwnFitApp.instance.store
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var desc by remember { mutableStateOf(initial?.description.orEmpty()) }
    var cat by remember { mutableStateOf(initial?.category ?: Category.STRENGTH) }
    var muscles by remember { mutableStateOf(initial?.muscles.orEmpty()) }
    var kit by remember { mutableStateOf(initial?.kit ?: Kit.NONE) }
    var timed by remember { mutableStateOf(initial?.timed ?: false) }
    var perSide by remember { mutableStateOf(initial?.perSide ?: false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New exercise" else "Edit exercise") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(desc, { desc = it }, label = { Text("How to do it") }, minLines = 2, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                SectionTitle("Type")
                ChipGroup(Category.entries.filter { it != Category.BREATHING }, { it == cat }, { it.label }, { cat = it })
                SectionTitle("Muscles")
                ChipGroup(Muscle.entries, { it in muscles }, { it.label }, { m -> muscles = if (m in muscles) muscles - m else muscles + m })
                SectionTitle("Equipment")
                ChipGroup(Kit.entries, { it == kit }, { it.label }, { kit = it })
                SectionTitle("Measured by")
                Segmented(listOf(false, true), timed, { if (it) "Time" else "Reps" }) { timed = it }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                    Checkbox(perSide, { perSide = it }); Text("Done on each side")
                }
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = {
                val ex = (initial ?: Exercise(id = "c-" + Store.newId(), name = "", category = cat)).copy(
                    name = name.trim(), description = desc.trim(), category = cat, muscles = muscles,
                    kit = kit, timed = timed, perSide = perSide,
                )
                store.upsertExercise(ex)
                onSaved(ex.id)
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
