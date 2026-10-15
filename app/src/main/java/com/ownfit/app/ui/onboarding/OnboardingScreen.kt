package com.ownfit.app.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.ownfit.app.OwnFitApp
import com.ownfit.app.data.AppState
import com.ownfit.app.data.Equipment
import com.ownfit.app.data.Gender
import com.ownfit.app.data.Goal
import com.ownfit.app.data.Injury
import com.ownfit.app.data.Level
import com.ownfit.app.data.Profile
import com.ownfit.app.engine.PlanGenerator
import com.ownfit.app.ui.components.ExerciseAnimation
import com.ownfit.app.ui.components.ChipGroup
import com.ownfit.app.ui.components.SectionTitle
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val STEPS = 10

@Composable
fun OnboardingScreen(state: AppState, initial: Profile?, onDone: () -> Unit) {
    val app = OwnFitApp.instance
    val scope = rememberCoroutineScope()
    var step by rememberSaveable { mutableIntStateOf(0) }
    var p by remember { mutableStateOf(initial ?: Profile()) }
    var hasKit by remember { mutableStateOf(initial?.equipment?.isNotEmpty() ?: false) }
    var building by remember { mutableStateOf(false) }
    var buildMsg by remember { mutableStateOf("") }
    val metric = state.settings.metric

    fun next() {
        if (step == STEPS - 2) {
            building = true
            step++
            scope.launch {
                val profile = if (hasKit) p else p.copy(equipment = emptyList())
                p = profile
                buildMsg = app.rebuildPlan(profile)
                building = false
            }
        } else step++
    }

    BackHandler(enabled = step > 0 && !building) { step-- }

    Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (step > 0 && !building) IconButton(onClick = { step-- }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
            else if (initial != null) IconButton(onClick = onDone) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Close") }
            else Spacer(Modifier.size(48.dp))
            LinearProgressIndicator(
                progress = { (step + 1f) / STEPS },
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
            )
            Text("${step + 1}/$STEPS", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(end = 12.dp))
        }

        AnimatedContent(
            targetState = step,
            transitionSpec = { (slideInHorizontally { it / 4 } + fadeIn()) togetherWith fadeOut() },
            modifier = Modifier.weight(1f),
            label = "step",
        ) { s ->
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
            ) {
                when (s) {
                    0 -> {
                        Question("Hi! I'm your OwnFit coach.", "A few quick questions and I'll build a plan that fits your body, your space and your schedule.")
                        OutlinedTextField(
                            p.name, { p = p.copy(name = it) },
                            label = { Text("What should I call you? (optional)") },
                            singleLine = true, modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    1 -> {
                        Question("How do you identify?", "Used for calorie estimates and to pick your coach's voice (you can switch anytime).")
                        Gender.entries.forEach { g ->
                            ChoiceCard(g.label, null, p.gender == g) { p = p.copy(gender = g) }
                        }
                        Box(Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center) {
                            ExerciseAnimation(null, p.gender, Modifier.height(220.dp).fillMaxWidth(), motionId = "jumping_jacks")
                        }
                    }
                    2 -> {
                        Question("Tell me about your body", "This helps me pick the right intensity and impact level.")
                        UnitsToggle(metric) { m -> app.store.updateSettings { it.copy(metric = m) } }
                        Stepper("Age", "${p.age} yrs", p.age.toFloat(), 13f..90f) { p = p.copy(age = it.roundToInt()) }
                        Stepper(
                            "Height",
                            if (metric) "${p.heightCm} cm" else (p.heightCm / 2.54f).roundToInt().let { "${it / 12}' ${it % 12}\"" },
                            p.heightCm.toFloat(), 120f..220f,
                        ) { p = p.copy(heightCm = it.roundToInt()) }
                        Stepper(
                            "Weight",
                            if (metric) "${p.weightKg.roundToInt()} kg" else "${(p.weightKg * 2.2046f).roundToInt()} lb",
                            p.weightKg, 35f..200f,
                        ) { p = p.copy(weightKg = it.roundToInt().toFloat()) }
                        Text(
                            "BMI ${"%.1f".format(p.bmi)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    3 -> {
                        Question("What's your main goal?", null)
                        Goal.entries.forEach { g -> ChoiceCard(g.label, g.blurb, p.goal == g) { p = p.copy(goal = g) } }
                    }
                    4 -> {
                        Question("How experienced are you?", "Be honest — I'll adapt as you go based on your feedback.")
                        val blurbs = mapOf(
                            Level.BEGINNER to "New to training or coming back after a break",
                            Level.INTERMEDIATE to "Training regularly for a few months",
                            Level.ADVANCED to "Training consistently for a year or more",
                        )
                        Level.entries.forEach { l -> ChoiceCard(l.label, blurbs[l], p.level == l) { p = p.copy(level = l) } }
                    }
                    5 -> {
                        Question("Any equipment at home?", "No gear is totally fine — bodyweight training works.")
                        ChoiceCard("Freehand only", "Bodyweight exercises", !hasKit) { hasKit = false }
                        ChoiceCard("I have some equipment", "Dumbbells, bands, a bar…", hasKit) { hasKit = true }
                        if (hasKit) {
                            SectionTitle("Select everything you have")
                            ChipGroup(
                                Equipment.entries, { it in p.equipment }, { it.label },
                                onToggle = { e -> p = p.copy(equipment = p.equipment.toggle(e)) },
                            )
                        }
                    }
                    6 -> {
                        Question("Any injuries, conditions or operations?", "I'll leave out anything that could aggravate them.")
                        ChipGroup(
                            Injury.entries, { it in p.injuries }, { it.label },
                            onToggle = { i -> p = p.copy(injuries = p.injuries.toggle(i)) },
                        )
                        if (p.injuries.isEmpty()) Text(
                            "None selected — great!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        OutlinedTextField(
                            p.healthNotes, { p = p.copy(healthNotes = it) },
                            label = { Text("Anything else? e.g. knee surgery 2023") },
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                            minLines = 2,
                        )
                        if (p.injuries.any { it == Injury.HEART || it == Injury.PREGNANCY || it == Injury.RECENT_SURGERY }) {
                            Note("Please check with your doctor before starting. Stop any exercise that causes pain.")
                        }
                    }
                    7 -> {
                        Question("Mind & body", "Add yoga and breathing to your routine? You can change this anytime in Settings.")
                        ToggleRow("Yoga", "Mobility flows in cool-downs and dedicated yoga days", p.includeYoga) {
                            p = p.copy(includeYoga = it)
                        }
                        ToggleRow("Breathing exercises", "Guided breathing to recover and de-stress", p.includeBreathing) {
                            p = p.copy(includeBreathing = it)
                        }
                    }
                    8 -> {
                        Question("Your schedule", "Short and consistent beats long and rare.")
                        SectionTitle("Days per week")
                        Segmented((2..6).toList(), p.daysPerWeek, { "$it" }) { p = p.copy(daysPerWeek = it) }
                        SectionTitle("Minutes per workout")
                        Segmented(listOf(10, 15, 20, 30, 45, 60), p.minutes, { "$it" }) { p = p.copy(minutes = it) }
                        AiOptIn(state)
                    }
                    else -> PlanPreview(state, building, buildMsg)
                }
                Spacer(Modifier.height(24.dp))
            }
        }

        Row(Modifier.fillMaxWidth().padding(24.dp, 8.dp, 24.dp, 16.dp)) {
            if (step == STEPS - 1) {
                Button(
                    enabled = !building,
                    onClick = {
                        app.store.setProfile(p)
                        onDone()
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                ) { Text(if (initial == null) "Let's go" else "Save") }
            } else {
                Button(onClick = { next() }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    Text(if (step == STEPS - 2) "Build my plan" else "Next")
                }
            }
        }
    }
}

private fun <T> List<T>.toggle(v: T) = if (v in this) this - v else this + v

@Composable
private fun Question(title: String, sub: String?) {
    Row(Modifier.padding(top = 16.dp, bottom = 20.dp), verticalAlignment = Alignment.Top) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(40.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.AutoAwesome, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, style = MaterialTheme.typography.headlineSmall)
            if (sub != null) Text(
                sub, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
fun ChoiceCard(title: String, sub: String?, selected: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = if (selected) cs.primaryContainer else cs.surfaceContainerLow),
        border = if (selected) BorderStroke(2.dp, cs.primary) else null,
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (sub != null) Text(sub, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
        }
    }
}

@Composable
private fun Stepper(label: String, value: String, v: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Column(Modifier.padding(vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            TextButton(onClick = { onChange((v - 1).coerceIn(range)) }) { Text("−") }
            Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            TextButton(onClick = { onChange((v + 1).coerceIn(range)) }) { Text("+") }
        }
        Slider(value = v, onValueChange = onChange, valueRange = range)
    }
}

@Composable
private fun UnitsToggle(metric: Boolean, onChange: (Boolean) -> Unit) {
    Segmented(listOf(true, false), metric, { if (it) "Metric" else "Imperial" }, onChange)
}

@Composable
fun <T> Segmented(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { i, o ->
            SegmentedButton(
                selected = o == selected,
                onClick = { onSelect(o) },
                shape = SegmentedButtonDefaults.itemShape(i, options.size),
                icon = {},
            ) { Text(label(o)) }
        }
    }
}

@Composable
fun ToggleRow(title: String, sub: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (sub != null) Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked, onChange)
    }
}

@Composable
private fun Note(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
    ) {
        Text(text, Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun AiOptIn(state: AppState) {
    val store = OwnFitApp.instance.store
    var key by remember { mutableStateOf(state.secrets.anthropicKey) }
    SectionTitle("AI coach (optional)")
    ToggleRow(
        "Personalise with Claude AI",
        "Uses your Anthropic API key. Off = fast, private on-device coach.",
        state.settings.aiEnabled,
    ) { on -> store.updateSettings { it.copy(aiEnabled = on) } }
    if (state.settings.aiEnabled) {
        OutlinedTextField(
            key,
            { key = it.trim(); store.updateSecrets { s -> s.copy(anthropicKey = key) } },
            label = { Text("Anthropic API key") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun PlanPreview(state: AppState, building: Boolean, msg: String) {
    if (building) {
        Column(Modifier.fillMaxWidth().padding(top = 80.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Text(
                if (state.settings.aiEnabled) "Your AI coach is designing your plan…" else "Building your plan…",
                Modifier.padding(top = 20.dp), style = MaterialTheme.typography.titleMedium,
            )
        }
        return
    }
    val plan = state.plan ?: return
    Question("Your plan is ready", plan.notes.ifBlank { msg })
    if (msg.startsWith("AI unavailable")) Note(msg)
    plan.days.forEach { d ->
        val mins = PlanGenerator.estimateMinutes(d.workout) { state.exercise(it) }
        Card(
            Modifier.fillMaxWidth().padding(vertical = 5.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(d.title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text(d.focus.label, style = MaterialTheme.typography.titleMedium)
                }
                Text("${d.workout.items.size} moves · ~$mins min", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
    Text(
        "Built by: ${plan.source}",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp),
    )
    Spacer(Modifier.height(8.dp))
}
