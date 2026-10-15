package com.ownfit.app.engine

import com.ownfit.app.data.Category
import com.ownfit.app.data.Exercise
import com.ownfit.app.data.Focus
import com.ownfit.app.data.Goal
import com.ownfit.app.data.Level
import com.ownfit.app.data.Muscle
import com.ownfit.app.data.Plan
import com.ownfit.app.data.PlanDay
import com.ownfit.app.data.Profile
import com.ownfit.app.data.Section
import com.ownfit.app.data.Session
import com.ownfit.app.data.Workout
import com.ownfit.app.data.WorkoutItem
import java.time.LocalDate
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * On-device coach. Deterministic per (day, focus) so "today's workout" is stable,
 * but varies day to day. Filters by equipment, injuries, impact and level.
 */
object PlanGenerator {

    fun eligible(p: Profile, all: List<Exercise>): List<Exercise> {
        val owned = p.equipment.toSet()
        return all.filter { e ->
            !e.deleted &&
                e.kit.availableWith(owned) &&
                e.avoidWith.none { it in p.injuries } &&
                !(e.highImpact && p.lowImpact) &&
                e.level <= p.level.rank
        }
    }

    /** Weekly split based on days per week. */
    fun splitFor(p: Profile): List<Focus> {
        if (p.goal == Goal.FLEXIBILITY) return when {
            p.daysPerWeek <= 2 -> listOf(Focus.YOGA, Focus.FULL)
            p.daysPerWeek == 3 -> listOf(Focus.YOGA, Focus.FULL, Focus.YOGA)
            else -> listOf(Focus.YOGA, Focus.UPPER, Focus.YOGA, Focus.LOWER, Focus.YOGA, Focus.CORE).take(p.daysPerWeek)
        }
        val base = when (p.daysPerWeek) {
            1, 2 -> listOf(Focus.FULL, Focus.FULL)
            3 -> listOf(Focus.FULL, Focus.FULL, Focus.FULL)
            4 -> listOf(Focus.UPPER, Focus.LOWER, Focus.UPPER, Focus.LOWER)
            5 -> listOf(Focus.UPPER, Focus.LOWER, Focus.CORE, Focus.UPPER, Focus.LEGS)
            else -> listOf(Focus.CHEST, Focus.BACK, Focus.LEGS, Focus.SHOULDERS, Focus.ARMS, Focus.CORE)
        }.take(p.daysPerWeek.coerceAtLeast(1))
        // Fat loss: swap one day for cardio when training 4+ days.
        return if (p.goal == Goal.LOSE_FAT && base.size >= 4) base.dropLast(1) + Focus.CARDIO else base
    }

    fun buildPlan(p: Profile, all: List<Exercise>): Plan {
        val split = splitFor(p)
        val days = split.mapIndexed { i, f ->
            val w = generate(p, all, f, p.minutes, seed = (i + 1) * 7919L)
            PlanDay("Day ${i + 1}", f, w.copy(name = "Day ${i + 1} · ${f.label}"))
        }
        return Plan(days, source = "on-device", notes = notesFor(p), createdAt = System.currentTimeMillis())
    }

    fun notesFor(p: Profile): String = buildList {
        add("${p.daysPerWeek} days/week, ~${p.minutes} min, ${p.level.label.lowercase()} level, goal: ${p.goal.label.lowercase()}.")
        if (p.equipment.isEmpty()) add("Fully bodyweight — no equipment needed.")
        if (p.lowImpact) add("Low-impact: no jumping, joint-friendly choices.")
        if (p.injuries.isNotEmpty()) add("Avoiding exercises that load: ${p.injuries.joinToString { it.label.lowercase() }}.")
        if (p.injuries.any { it.name == "RECENT_SURGERY" || it.name == "HEART" || it.name == "PREGNANCY" })
            add("Please get your doctor's OK before starting, and stop if anything hurts.")
    }.joinToString(" ")

    /** Which plan day is "today" — rotates by number of plan sessions completed. */
    fun todayIndex(plan: Plan, history: List<Session>): Int =
        if (plan.days.isEmpty()) 0 else history.count { it.fromPlan } % plan.days.size

    fun generate(
        p: Profile,
        all: List<Exercise>,
        focus: Focus,
        minutes: Int,
        seed: Long = LocalDate.now().toEpochDay() * 31 + focus.ordinal,
    ): Workout {
        val rnd = Random(seed)
        val pool = eligible(p, all)
        val items = when (focus) {
            Focus.YOGA -> yoga(p, pool, minutes, rnd)
            Focus.BREATHE -> breathing(pool, minutes, rnd)
            else -> strength(p, pool, focus, minutes, rnd)
        }
        return Workout(
            id = "gen-${focus.name.lowercase()}-$seed",
            name = "${focus.label} · $minutes min",
            items = items,
            focus = focus,
        )
    }

    // ---- building blocks ----

    private data class Dose(val sets: Int, val reps: Int, val work: Int, val rest: Int)

    private fun dose(p: Profile): Dose {
        var d = when (p.level) {
            Level.BEGINNER -> Dose(2, 10, 30, 40)
            Level.INTERMEDIATE -> Dose(3, 12, 40, 30)
            Level.ADVANCED -> Dose(4, 14, 45, 25)
        }
        d = when (p.goal) {
            Goal.BUILD_MUSCLE -> d.copy(reps = (d.reps - 2).coerceAtLeast(8), rest = d.rest + 20)
            Goal.LOSE_FAT -> d.copy(rest = (d.rest - 10).coerceAtLeast(15), work = d.work + 5)
            else -> d
        }
        if (p.age >= 55 || p.lowImpact) d = d.copy(rest = d.rest + 15)
        // Feedback-driven adjustment (-2..+2)
        val k = 1f + p.intensity * 0.1f
        return d.copy(
            reps = (d.reps * k).roundToInt().coerceIn(5, 25),
            work = (d.work * k).roundToInt().coerceIn(20, 75),
            sets = (d.sets + if (p.intensity >= 2) 1 else if (p.intensity <= -2) -1 else 0).coerceIn(1, 5),
        )
    }

    private fun itemFor(e: Exercise, d: Dose, section: Section = Section.MAIN): WorkoutItem {
        val reps = if (e.timed) null else if (e.perSide) (d.reps * 0.8f).roundToInt() else d.reps
        val secs = if (e.timed) d.work else null
        return WorkoutItem(e.id, sets = d.sets, reps = reps, seconds = secs, restSec = d.rest, section = section)
    }

    /** Roughly how long an item takes, in seconds. */
    fun itemSeconds(i: WorkoutItem, e: Exercise?): Int {
        val side = if (e?.perSide == true) 2 else 1
        val work = (i.seconds ?: ((i.reps ?: 10) * 3)) * side
        return i.sets * work + (i.sets - 1).coerceAtLeast(0) * i.restSec + i.restSec
    }

    fun estimateMinutes(w: Workout, lookup: (String) -> Exercise?): Int =
        (w.items.sumOf { itemSeconds(it, lookup(it.exerciseId)) } / 60f).roundToInt().coerceAtLeast(1)

    private fun pickWeighted(cands: List<Exercise>, used: Set<String>, rnd: Random, preferKit: Boolean): Exercise? {
        val fresh = cands.filter { it.id !in used }.ifEmpty { return null }
        // Prefer equipment exercises when the user owns gear and wants muscle; prefer harder moves for higher levels.
        val weights = fresh.map { e ->
            var w = 1f + e.level * 0.6f
            if (preferKit && e.kit != com.ownfit.app.data.Kit.NONE) w += 2f
            w
        }
        var r = rnd.nextFloat() * weights.sum()
        fresh.forEachIndexed { i, e -> r -= weights[i]; if (r <= 0) return e }
        return fresh.last()
    }

    private fun warmup(pool: List<Exercise>, rnd: Random, count: Int): List<WorkoutItem> =
        pool.filter { it.category == Category.WARMUP }.shuffled(rnd).take(count)
            .map { WorkoutItem(it.id, sets = 1, seconds = 30, restSec = 5, section = Section.WARMUP) }

    private fun cooldown(p: Profile, pool: List<Exercise>, focus: Focus, rnd: Random): List<WorkoutItem> {
        val out = mutableListOf<WorkoutItem>()
        val target = focus.muscles.toSet()
        val cat = if (p.includeYoga) Category.YOGA else Category.STRETCH
        val stretches = pool.filter { it.category == cat && it.id != "savasana" }
            .sortedByDescending { e -> e.muscles.count { it in target } + rnd.nextFloat() }
            .take(2)
        stretches.forEach { out += WorkoutItem(it.id, sets = 1, seconds = 30, restSec = 5, section = Section.COOLDOWN) }
        if (p.includeBreathing) {
            pool.firstOrNull { it.id == "recovery_breath" }?.let {
                out += WorkoutItem(it.id, sets = 1, seconds = 60, restSec = 0, section = Section.COOLDOWN)
            }
        }
        return out
    }

    private fun strength(p: Profile, pool: List<Exercise>, focus: Focus, minutes: Int, rnd: Random): List<WorkoutItem> {
        val d = dose(p)
        val wu = warmup(pool, rnd, if (minutes >= 20) 3 else 2)
        val cd = cooldown(p, pool, focus, rnd)
        val fixedSec = (wu.size * 35) + cd.sumOf { (it.seconds ?: 30) + 5 }
        val budget = (minutes * 60 - fixedSec).coerceAtLeast(5 * 60)
        val main = mutableListOf<WorkoutItem>()
        val used = mutableSetOf<String>()
        val preferKit = p.goal == Goal.BUILD_MUSCLE || p.level != Level.BEGINNER
        val trainable = pool.filter {
            it.category == Category.STRENGTH || it.category == Category.CORE || it.category == Category.CARDIO
        }
        var spent = 0
        var slot = 0
        val slots = focus.muscles.ifEmpty { Focus.FULL.muscles }
        while (spent < budget && slot < 14) {
            val m = slots[slot % slots.size]
            slot++
            val cands = trainable.filter { e ->
                if (m == Muscle.CARDIO) e.category == Category.CARDIO
                else e.muscles.firstOrNull() == m || (e.muscles.contains(m) && e.category != Category.CARDIO)
            }
            val e = pickWeighted(cands, used, rnd, preferKit) ?: continue
            val item = if (e.category == Category.CARDIO && p.goal != Goal.LOSE_FAT)
                itemFor(e, d).copy(sets = (d.sets - 1).coerceAtLeast(1)) else itemFor(e, d)
            val cost = itemSeconds(item, e)
            if (spent + cost > budget + 60 && main.size >= 3) break
            main += item
            used += e.id
            spent += cost
        }
        return wu + main + cd
    }

    private fun yoga(p: Profile, pool: List<Exercise>, minutes: Int, rnd: Random): List<WorkoutItem> {
        val hold = when (p.level) { Level.BEGINNER -> 30; Level.INTERMEDIATE -> 45; Level.ADVANCED -> 60 }
        val poses = pool.filter { it.category == Category.YOGA }
        val opener = poses.filter { it.id in setOf("mountain_pose", "cat_cow", "sun_salutation") }
        val closer = poses.filter { it.id in setOf("happy_baby", "legs_up_wall", "savasana") }
        val middle = (poses - opener.toSet() - closer.toSet()).shuffled(rnd)
        val out = mutableListOf<WorkoutItem>()
        var spent = 0
        val budget = minutes * 60
        fun add(e: Exercise, sec: Int = hold) {
            val it = WorkoutItem(e.id, sets = 1, seconds = sec, restSec = 5, section = Section.MAIN)
            out += it; spent += itemSeconds(it, e)
        }
        opener.forEach { add(it) }
        // Long sessions repeat the flow with longer holds rather than running out of poses.
        var round = 0
        while (spent < budget - 150 && round < 3 && middle.isNotEmpty()) {
            val hold2 = hold + round * 15
            for (e in if (round == 0) middle else middle.shuffled(rnd)) { if (spent > budget - 150) break; add(e, hold2) }
            round++
        }
        closer.filter { it.id != "savasana" }.forEach { if (spent < budget - 90) add(it) }
        poses.firstOrNull { it.id == "savasana" }?.let { add(it, 60) }
        if (p.includeBreathing) pool.firstOrNull { it.id == "coherent" }?.let {
            out.add(0, WorkoutItem(it.id, sets = 1, seconds = 60, restSec = 5, section = Section.WARMUP))
        }
        return out
    }

    private fun breathing(pool: List<Exercise>, minutes: Int, rnd: Random): List<WorkoutItem> {
        val b = pool.filter { it.category == Category.BREATHING }.shuffled(rnd)
        val n = (minutes / 3).coerceIn(1, b.size)
        val each = (minutes * 60 / n).coerceIn(60, 300)
        return b.take(n).map { WorkoutItem(it.id, sets = 1, seconds = each, restSec = 10, section = Section.MAIN) }
    }

    /** MET-based calorie estimate. */
    fun kcal(p: Profile?, focus: Focus, seconds: Int): Int {
        val met = when (focus) {
            Focus.BREATHE -> 1.3f
            Focus.YOGA -> 2.8f
            Focus.CARDIO -> 7.5f
            else -> 5f
        }
        val kg = p?.weightKg ?: 70f
        return (met * 3.5f * kg / 200f * seconds / 60f).roundToInt()
    }
}
