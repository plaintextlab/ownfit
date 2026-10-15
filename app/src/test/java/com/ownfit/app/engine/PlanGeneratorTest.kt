package com.ownfit.app.engine

import com.ownfit.app.data.Equipment
import com.ownfit.app.data.ExerciseLibrary
import com.ownfit.app.data.Focus
import com.ownfit.app.data.Goal
import com.ownfit.app.data.Injury
import com.ownfit.app.data.Level
import com.ownfit.app.data.Profile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanGeneratorTest {
    private val all = ExerciseLibrary.all
    private fun ex(id: String) = ExerciseLibrary.builtIn(id)!!

    @Test fun libraryIdsAreUnique() {
        assertEquals(all.size, all.map { it.id }.toSet().size)
    }

    @Test fun bodyweightUserNeverGetsEquipment() {
        val p = Profile(level = Level.ADVANCED, equipment = emptyList())
        Focus.entries.forEach { f ->
            val w = PlanGenerator.generate(p, all, f, 30, seed = 42)
            w.items.forEach { assertTrue("$f -> ${it.exerciseId}", ex(it.exerciseId).kit.availableWith(emptySet())) }
        }
    }

    @Test fun injuriesAreRespected() {
        val p = Profile(level = Level.ADVANCED, injuries = listOf(Injury.KNEE, Injury.WRIST),
            equipment = Equipment.entries.toList())
        repeat(20) { seed ->
            Focus.entries.forEach { f ->
                PlanGenerator.generate(p, all, f, 45, seed = seed.toLong()).items.forEach {
                    val e = ex(it.exerciseId)
                    assertTrue("${e.id} unsafe", Injury.KNEE !in e.avoidWith && Injury.WRIST !in e.avoidWith)
                    assertTrue("${e.id} high impact", !e.highImpact)
                }
            }
        }
    }

    @Test fun beginnersOnlyGetLevelOneMoves() {
        val p = Profile(level = Level.BEGINNER, equipment = Equipment.entries.toList())
        repeat(10) { seed ->
            PlanGenerator.generate(p, all, Focus.FULL, 30, seed = seed.toLong()).items.forEach {
                assertEquals(1, ex(it.exerciseId).level)
            }
        }
    }

    @Test fun dumbbellOwnerGetsDumbbellWork() {
        val p = Profile(level = Level.INTERMEDIATE, goal = Goal.BUILD_MUSCLE, equipment = listOf(Equipment.DUMBBELL_PAIR))
        val w = PlanGenerator.generate(p, all, Focus.UPPER, 30, seed = 1)
        assertTrue(w.items.any { ex(it.exerciseId).kit.name.contains("DUMBBELL") })
    }

    @Test fun durationIsRoughlyRespected() {
        listOf(10, 20, 30, 45, 60).forEach { mins ->
            val p = Profile(level = Level.INTERMEDIATE, equipment = Equipment.entries.toList(), minutes = mins)
            Focus.entries.filter { it != Focus.BREATHE }.forEach { f ->
                val est = PlanGenerator.estimateMinutes(PlanGenerator.generate(p, all, f, mins, seed = 3)) { ExerciseLibrary.builtIn(it) }
                assertTrue("$f $mins min -> $est", est in (mins * 0.6).toInt()..(mins * 1.5 + 4).toInt())
            }
        }
    }

    @Test fun yogaAndBreathingTogglesChangeCooldown() {
        val on = Profile(includeYoga = true, includeBreathing = true)
        val off = Profile(includeYoga = false, includeBreathing = false)
        val a = PlanGenerator.generate(on, all, Focus.FULL, 25, seed = 5).items.map { ex(it.exerciseId).category.name }
        val b = PlanGenerator.generate(off, all, Focus.FULL, 25, seed = 5).items.map { ex(it.exerciseId).category.name }
        assertTrue("YOGA" in a && "BREATHING" in a)
        assertTrue("YOGA" !in b && "BREATHING" !in b && "STRETCH" in b)
    }

    @Test fun weeklySplitMatchesDays() {
        (2..6).forEach { d ->
            val plan = PlanGenerator.buildPlan(Profile(daysPerWeek = d), all)
            assertEquals(d, plan.days.size)
            assertTrue(plan.days.all { it.workout.items.isNotEmpty() })
        }
    }
}
