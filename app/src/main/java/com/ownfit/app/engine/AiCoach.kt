package com.ownfit.app.engine

import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.OutputConfig
import com.anthropic.models.messages.StopReason
import com.ownfit.app.data.Exercise
import com.ownfit.app.data.Focus
import com.ownfit.app.data.Plan
import com.ownfit.app.data.PlanDay
import com.ownfit.app.data.Profile
import com.ownfit.app.data.Section
import com.ownfit.app.data.Workout
import com.ownfit.app.data.WorkoutItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Optional Claude-powered coach. It only ever chooses from exercises that already passed the
 * on-device safety filter (equipment, injuries, impact, level), so the AI can't suggest
 * something the user shouldn't do. Any failure falls back to [PlanGenerator].
 */
object AiCoach {
    private const val MODEL = "claude-opus-5-5"

    @Serializable private data class AiItem(
        val id: String, val sets: Int = 1, val reps: Int? = null, val seconds: Int? = null,
        val rest: Int = 30, val section: String = "MAIN",
    )
    @Serializable private data class AiDay(val title: String, val focus: String, val items: List<AiItem>)
    @Serializable private data class AiPlan(val notes: String = "", val days: List<AiDay>)

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private const val SYSTEM = """You are an experienced, safety-conscious personal trainer and yoga teacher.
You design home workout plans. You may ONLY use exercise ids from the catalogue you are given.
Respect the user's injuries, health notes, level and available time. Prefer joint-friendly options when in doubt.
Each day: a short warm-up (section WARMUP), the main block (MAIN), and a cool-down (COOLDOWN).
Respond with a single JSON object and nothing else, shaped like:
{"notes": "<2-3 sentence coaching note for the user>",
 "days": [{"title": "Day 1 · Upper body", "focus": "<one of FULL, UPPER, LOWER, CHEST, BACK, SHOULDERS, ARMS, CORE, LEGS, CARDIO, YOGA, BREATHE>",
   "items": [{"id": "<exercise id>", "sets": 3, "reps": 12, "rest": 30, "section": "MAIN"},
             {"id": "<timed exercise id>", "sets": 1, "seconds": 40, "rest": 15, "section": "COOLDOWN"}]}]}
Use "seconds" for exercises marked timed and "reps" otherwise."""

    private fun describe(p: Profile) = buildString {
        appendLine("User profile:")
        if (p.name.isNotBlank()) appendLine("- name: ${p.name}")
        appendLine("- gender: ${p.gender.label}, age ${p.age}, height ${p.heightCm} cm, weight ${p.weightKg} kg (BMI ${"%.1f".format(p.bmi)})")
        appendLine("- goal: ${p.goal.label}; level: ${p.level.label}")
        appendLine("- trains ${p.daysPerWeek} days/week, about ${p.minutes} minutes per session")
        appendLine("- equipment: ${p.equipment.joinToString { it.label }.ifEmpty { "none (bodyweight only)" }}")
        appendLine("- injuries/conditions: ${p.injuries.joinToString { it.label }.ifEmpty { "none" }}")
        if (p.healthNotes.isNotBlank()) appendLine("- health notes from the user: ${p.healthNotes}")
        appendLine("- include yoga: ${p.includeYoga}; include breathing exercises: ${p.includeBreathing}")
        if (p.intensity != 0) appendLine("- recent feedback says workouts were ${if (p.intensity > 0) "too easy" else "too hard"} (adjust ${p.intensity})")
    }

    private fun catalogue(pool: List<Exercise>) = pool.joinToString("\n") { e ->
        "${e.id} | ${e.name} | ${e.category.name} | ${e.muscles.joinToString("/") { it.name }}" +
            (if (e.timed) " | timed" else "") + (if (e.perSide) " | per side" else "")
    }

    suspend fun buildPlan(apiKey: String, p: Profile, all: List<Exercise>): Result<Plan> = withContext(Dispatchers.IO) {
        runCatching {
            val pool = PlanGenerator.eligible(p, all)
            val prompt = describe(p) + "\nExercise catalogue (id | name | category | muscles):\n" + catalogue(pool) +
                "\n\nCreate a ${p.daysPerWeek}-day weekly plan."
            val client = AnthropicOkHttpClient.builder().apiKey(apiKey).build()
            try {
                val params = MessageCreateParams.builder()
                    .model(MODEL)
                    .maxTokens(16000L)
                    .system(SYSTEM)
                    .outputConfig(OutputConfig.builder().effort(OutputConfig.Effort.MEDIUM).build())
                    .addUserMessage(prompt)
                    .build()
                val msg = client.messages().create(params)
                if (msg.stopReason().orElse(null) == StopReason.REFUSAL) error("The AI coach declined this request.")
                val text = msg.content().mapNotNull { b -> b.text().orElse(null)?.text() }.joinToString("")
                val start = text.indexOf('{')
                val end = text.lastIndexOf('}')
                require(start >= 0 && end > start) { "AI response had no plan" }
                toPlan(json.decodeFromString(AiPlan.serializer(), text.substring(start, end + 1)), pool)
            } finally {
                client.close()
            }
        }
    }

    private fun toPlan(ai: AiPlan, pool: List<Exercise>): Plan {
        val byId = pool.associateBy { it.id }
        val days = ai.days.mapIndexedNotNull { i, d ->
            val focus = runCatching { Focus.valueOf(d.focus.uppercase()) }.getOrDefault(Focus.FULL)
            val items = d.items.mapNotNull { it ->
                val e = byId[it.id] ?: return@mapNotNull null
                WorkoutItem(
                    exerciseId = e.id,
                    sets = it.sets.coerceIn(1, 6),
                    reps = if (e.timed) null else (it.reps ?: 10).coerceIn(3, 40),
                    seconds = if (e.timed) (it.seconds ?: 30).coerceIn(10, 600) else null,
                    restSec = it.rest.coerceIn(0, 180),
                    section = runCatching { Section.valueOf(it.section.uppercase()) }.getOrDefault(Section.MAIN),
                )
            }
            if (items.isEmpty()) null
            else PlanDay(d.title, focus, Workout(id = "ai-day-$i", name = d.title, items = items, focus = focus))
        }
        require(days.isNotEmpty()) { "AI plan was empty" }
        return Plan(days, source = "Claude", notes = ai.notes, createdAt = System.currentTimeMillis())
    }
}
