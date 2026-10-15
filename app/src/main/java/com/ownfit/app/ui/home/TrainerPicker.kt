package com.ownfit.app.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ownfit.app.OwnFitApp
import com.ownfit.app.audio.Coach
import com.ownfit.app.data.AppState
import com.ownfit.app.data.Trainer

/** Compact pill on the home screen showing the current trainer. */
@Composable
fun TrainerButton(state: AppState, onClick: () -> Unit) {
    val t = state.trainer
    Surface(onClick = onClick, shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
        Row(Modifier.padding(start = 4.dp, end = 6.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(t, 32.dp)
            Text(t.displayName, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 8.dp))
            Icon(Icons.Default.ArrowDropDown, "Change trainer")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainerPickerSheet(state: AppState, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val store = OwnFitApp.instance.store
    val coaches = remember { Trainer.entries.associateWith { Coach(ctx, it, voice = true, sounds = false) } }
    DisposableEffect(Unit) { onDispose { coaches.values.forEach { it.release() } } }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp).navigationBarsPadding()) {
            Text("Choose your coach", style = MaterialTheme.typography.titleLarge)
            Text(
                "Your coach talks you through every workout: moves, reps, countdowns and encouragement.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Trainer.entries.forEach { t ->
                    val selected = t == state.trainer
                    val cs = MaterialTheme.colorScheme
                    Card(
                        onClick = {
                            store.updateSettings { it.copy(trainer = t) }
                            coaches[t]?.intro()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = if (selected) cs.primaryContainer else cs.surfaceContainerHigh),
                        border = if (selected) BorderStroke(2.dp, cs.primary) else null,
                    ) {
                        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Avatar(t, 88.dp)
                            Text(t.displayName, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 10.dp))
                            Text(t.blurb, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, color = cs.onSurfaceVariant)
                            TextButton(onClick = { coaches[t]?.intro() }) {
                                Icon(Icons.AutoMirrored.Outlined.VolumeUp, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Hear voice")
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Avatar(t: Trainer, size: androidx.compose.ui.unit.Dp) {
    val cs = MaterialTheme.colorScheme
    Surface(
        shape = CircleShape,
        color = if (t == Trainer.FEMALE) cs.tertiaryContainer else cs.primaryContainer,
        modifier = Modifier.size(size),
    ) {
        androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
            Text(
                t.displayName.take(1),
                style = if (size > 48.dp) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.titleSmall,
                color = if (t == Trainer.FEMALE) cs.onTertiaryContainer else cs.onPrimaryContainer,
            )
        }
    }
}
