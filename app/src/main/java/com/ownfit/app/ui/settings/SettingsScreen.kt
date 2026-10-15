package com.ownfit.app.ui.settings

import android.os.Build
import android.text.format.DateUtils
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.ownfit.app.OwnFitApp
import com.ownfit.app.data.AppState
import com.ownfit.app.data.ThemeMode
import com.ownfit.app.ui.Nav
import com.ownfit.app.ui.Screen
import com.ownfit.app.ui.components.SectionTitle
import com.ownfit.app.ui.onboarding.Segmented
import com.ownfit.app.ui.onboarding.ToggleRow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(state: AppState, nav: Nav) {
    val app = OwnFitApp.instance
    val store = app.store
    val s = state.settings
    val profile = state.profile ?: return
    val scope = rememberCoroutineScope()
    val snack = remember { SnackbarHostState() }
    var busy by remember { mutableStateOf(false) }
    var syncing by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    var pickTrainer by remember { mutableStateOf(false) }
    if (pickTrainer) com.ownfit.app.ui.home.TrainerPickerSheet(state) { pickTrainer = false }
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val musicPicker = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
        if (uris.isNotEmpty()) {
            uris.forEach { u ->
                runCatching { ctx.contentResolver.takePersistableUriPermission(u, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            }
            store.updateSettings { st -> st.copy(myMusic = (st.myMusic + uris.map { it.toString() }).distinct(), music = "mine") }
        }
    }

    fun rebuild() {
        busy = true
        scope.launch {
            val msg = app.rebuildPlan()
            busy = false
            snack.showSnackbar(msg)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = { IconButton(onClick = { nav.back() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            )
        },
        snackbarHost = { SnackbarHost(snack) },
    ) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        ) {
            // ---- Training ----
            SectionTitle("Training")
            ListItem(
                headlineContent = { Text("Profile & goals") },
                supportingContent = {
                    Text("${profile.level.label} · ${profile.goal.label} · ${profile.daysPerWeek}×/week · ${profile.minutes} min")
                },
                trailingContent = { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null) },
                overlineContent = { Text("Profile, equipment & injuries") },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.clickable { nav.go(Screen.EditProfile) },
            )
            ToggleRow("Include yoga", "Yoga cool-downs and yoga days", profile.includeYoga) {
                store.setProfile(profile.copy(includeYoga = it)); rebuild()
            }
            ToggleRow("Include breathing", "Guided breathing to finish workouts", profile.includeBreathing) {
                store.setProfile(profile.copy(includeBreathing = it)); rebuild()
            }
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                FilledTonalButton(onClick = { rebuild() }, enabled = !busy) { Text("Regenerate my plan") }
                if (busy) CircularProgressIndicator(Modifier.padding(start = 12.dp).size(24.dp))
            }

            // ---- Appearance ----
            SectionTitle("Appearance")
            Segmented(ThemeMode.entries, s.theme, { it.label }) { t -> store.updateSettings { it.copy(theme = t) } }
            if (Build.VERSION.SDK_INT >= 31) ToggleRow("Dynamic color", "Match your wallpaper (Material You)", s.dynamicColor) { v ->
                store.updateSettings { it.copy(dynamicColor = v) }
            }

            // ---- Workout ----
            SectionTitle("Voice coach")
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                com.ownfit.app.ui.home.TrainerButton(state) { pickTrainer = true }
                Text(
                    "  ${state.trainer.blurb}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            SectionTitle("Music")
            ToggleRow("Play music during workouts", "Pause anytime from the workout screen", s.musicOn) { v ->
                store.updateSettings { it.copy(musicOn = v) }
            }
            if (s.musicOn) {
                com.ownfit.app.ui.components.ChipGroup(
                    com.ownfit.app.data.MusicTrack.entries.map { it.id } + "mine",
                    { it == s.music },
                    { id -> com.ownfit.app.data.MusicTrack.entries.firstOrNull { it.id == id }?.title ?: "My music (${s.myMusic.size})" },
                    { id ->
                        if (id == "mine" && s.myMusic.isEmpty()) musicPicker.launch(arrayOf("audio/*"))
                        else store.updateSettings { it.copy(music = id) }
                    },
                )
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text("Volume", style = MaterialTheme.typography.bodyMedium)
                    androidx.compose.material3.Slider(
                        value = s.musicVolume,
                        onValueChange = { v -> store.updateSettings { it.copy(musicVolume = v) }; com.ownfit.app.audio.Music.setVolume(v) },
                        modifier = Modifier.weight(1f).padding(start = 12.dp),
                    )
                }
                Row {
                    TextButton(onClick = { musicPicker.launch(arrayOf("audio/*")) }) { Text("Add my music") }
                    if (s.myMusic.isNotEmpty()) TextButton(onClick = {
                        store.updateSettings { it.copy(myMusic = emptyList(), music = if (it.music == "mine") "werq" else it.music) }
                    }) { Text("Clear my music") }
                }
                Text(
                    "Bundled music by Kevin MacLeod (incompetech.com), licensed CC BY 4.0.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            SectionTitle("During workouts")
            ToggleRow("Voice coach", "${state.trainer.displayName} announces exercises, counts down and motivates", s.voice) { v -> store.updateSettings { it.copy(voice = v) } }
            ToggleRow("Beeps & vibration", "3-2-1 countdown", s.sounds) { v -> store.updateSettings { it.copy(sounds = v) } }
            ToggleRow("Metric units", "kg / cm", s.metric) { v -> store.updateSettings { it.copy(metric = v) } }

            // ---- AI ----
            SectionTitle("AI coach")
            ToggleRow("Use Claude to build plans", "Sends your profile (not your name) to Anthropic. Off = on-device coach.", s.aiEnabled) { v ->
                store.updateSettings { it.copy(aiEnabled = v) }
            }
            if (s.aiEnabled) SecretField("Anthropic API key", state.secrets.anthropicKey) { k -> store.updateSecrets { it.copy(anthropicKey = k) } }

            // ---- Sync ----
            SectionTitle("Sync with git")
            Text(
                "Back up and sync your workouts, plan and history to any HTTPS git repo (GitHub, GitLab, Gitea…). " +
                    "Use a private repo and a personal access token with repo write access.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            var remote by remember { mutableStateOf(s.gitRemote) }
            var branch by remember { mutableStateOf(s.gitBranch) }
            var user by remember { mutableStateOf(s.gitUser) }
            OutlinedTextField(
                remote, { remote = it; store.updateSettings { st -> st.copy(gitRemote = it.trim()) } },
                label = { Text("Repository URL") }, placeholder = { Text("https://github.com/you/fitness-data.git") },
                singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                OutlinedTextField(
                    branch, { branch = it; store.updateSettings { st -> st.copy(gitBranch = it.trim()) } },
                    label = { Text("Branch") }, singleLine = true, modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    user, { user = it; store.updateSettings { st -> st.copy(gitUser = it.trim()) } },
                    label = { Text("Username") }, singleLine = true, modifier = Modifier.weight(1f),
                )
            }
            SecretField("Access token", state.secrets.gitToken) { k -> store.updateSecrets { it.copy(gitToken = k) } }
            ToggleRow("Auto-sync", "Sync after each workout and plan change", s.autoSync) { v -> store.updateSettings { it.copy(autoSync = v) } }
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Button(
                    enabled = !syncing && remote.startsWith("https://"),
                    onClick = {
                        syncing = true
                        app.syncNow { msg -> syncing = false; scope.launch { snack.showSnackbar(msg) } }
                    },
                ) {
                    Icon(Icons.Outlined.Sync, null); Text("  Sync now")
                }
                if (syncing) CircularProgressIndicator(Modifier.padding(start = 12.dp).size(24.dp))
                else if (s.lastSync > 0) Text(
                    "Last: " + DateUtils.getRelativeTimeSpanString(s.lastSync),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }

            // ---- Data ----
            SectionTitle("Data")
            TextButton(onClick = { confirmReset = true }) { Text("Reset all data on this device", color = MaterialTheme.colorScheme.error) }
            HorizontalDivider(Modifier.padding(vertical = 12.dp))
            Text(
                "OwnFit · Exercises are suggestions, not medical advice. Stop if you feel pain and consult a professional for injuries.\n\n" +
                    "Credits: exercise photos from free-exercise-db (public domain). Voices generated with Piper TTS " +
                    "(Joe – CC0, Kristin – public domain). Music by Kevin MacLeod (incompetech.com), CC BY 4.0.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(32.dp))
        }
    }

    if (confirmReset) AlertDialog(
        onDismissRequest = { confirmReset = false },
        title = { Text("Reset everything?") },
        text = { Text("Deletes your profile, plan, custom workouts and history on this device. Data already synced to git stays in the repo.") },
        confirmButton = {
            TextButton(onClick = {
                confirmReset = false
                runCatching { app.sync.reset() }
                store.resetAll()
            }) { Text("Reset", color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } },
    )
}

@Composable
private fun SecretField(label: String, value: String, onChange: (String) -> Unit) {
    var v by remember { mutableStateOf(value) }
    OutlinedTextField(
        v, { v = it.trim(); onChange(v) },
        label = { Text(label) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    )
}
