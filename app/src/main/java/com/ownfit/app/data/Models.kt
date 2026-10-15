package com.ownfit.app.data

import kotlinx.serialization.Serializable

@Serializable
enum class Gender(val label: String) { MALE("Male"), FEMALE("Female"), OTHER("Prefer not to say") }

@Serializable
enum class Level(val label: String, val rank: Int) {
    BEGINNER("Beginner", 1), INTERMEDIATE("Intermediate", 2), ADVANCED("Advanced", 3)
}

@Serializable
enum class Goal(val label: String, val blurb: String) {
    LOSE_FAT("Lose fat", "Burn calories, keep moving"),
    BUILD_MUSCLE("Build muscle", "Get stronger and bigger"),
    GET_FIT("Get fit", "Balanced strength and stamina"),
    FLEXIBILITY("Move better", "Mobility, posture, calm"),
}

/** Equipment the user owns. */
@Serializable
enum class Equipment(val label: String) {
    DUMBBELL_PAIR("Pair of dumbbells"),
    SINGLE_DUMBBELL("Single dumbbell"),
    ADJUSTABLE_DUMBBELLS("Adjustable dumbbells"),
    KETTLEBELL("Kettlebell"),
    RESISTANCE_BAND("Resistance band"),
    PULLUP_BAR("Pull-up bar"),
    BENCH("Bench or sturdy chair"),
    MAT("Yoga mat"),
}

/** What an exercise needs. */
@Serializable
enum class Kit(val label: String) {
    NONE("No equipment"),
    ONE_DUMBBELL("1 dumbbell"),
    TWO_DUMBBELLS("2 dumbbells"),
    KETTLEBELL("Kettlebell"),
    BAND("Resistance band"),
    PULLUP_BAR("Pull-up bar"),
    BENCH("Bench / chair");

    fun availableWith(owned: Set<Equipment>): Boolean = when (this) {
        NONE -> true
        ONE_DUMBBELL -> owned.any {
            it == Equipment.DUMBBELL_PAIR || it == Equipment.SINGLE_DUMBBELL ||
                it == Equipment.ADJUSTABLE_DUMBBELLS || it == Equipment.KETTLEBELL
        }
        TWO_DUMBBELLS -> Equipment.DUMBBELL_PAIR in owned || Equipment.ADJUSTABLE_DUMBBELLS in owned
        KETTLEBELL -> Equipment.KETTLEBELL in owned
        BAND -> Equipment.RESISTANCE_BAND in owned
        PULLUP_BAR -> Equipment.PULLUP_BAR in owned
        BENCH -> Equipment.BENCH in owned
    }
}

@Serializable
enum class Muscle(val label: String) {
    CHEST("Chest"), BACK("Back"), SHOULDERS("Shoulders"), ARMS("Arms"),
    CORE("Core"), GLUTES("Glutes"), LEGS("Legs"), CARDIO("Cardio")
}

@Serializable
enum class Category(val label: String) {
    WARMUP("Warm-up"), STRENGTH("Strength"), CARDIO("Cardio"), CORE("Core"),
    STRETCH("Stretch"), YOGA("Yoga"), BREATHING("Breathing")
}

/** Joints / conditions. An exercise lists the ones it loads heavily. */
@Serializable
enum class Injury(val label: String) {
    NECK("Neck"), SHOULDER("Shoulder"), ELBOW("Elbow"), WRIST("Wrist"),
    LOWER_BACK("Lower back"), HIP("Hip"), KNEE("Knee"), ANKLE("Ankle"),
    HEART("Heart / blood pressure"), PREGNANCY("Pregnancy"), RECENT_SURGERY("Recent surgery / operation")
}

@Serializable
data class BreathPattern(val inhale: Int, val hold1: Int, val exhale: Int, val hold2: Int) {
    val cycle get() = inhale + hold1 + exhale + hold2
}

@Serializable
data class Exercise(
    val id: String,
    val name: String,
    val category: Category,
    val muscles: List<Muscle> = emptyList(),
    val kit: Kit = Kit.NONE,
    /** Minimum level (1..3). */
    val level: Int = 1,
    /** Injuries that make this exercise unsuitable. */
    val avoidWith: List<Injury> = emptyList(),
    val highImpact: Boolean = false,
    /** Hold / time based rather than reps. */
    val timed: Boolean = false,
    val perSide: Boolean = false,
    val description: String = "",
    val breath: BreathPattern? = null,
    val custom: Boolean = false,
    val deleted: Boolean = false,
    val updatedAt: Long = 0,
)

/** User-attached demo media for an exercise. */
@Serializable
data class Media(
    val youtube: String? = null,
    /** http(s) url or "media/<file>" relative to the app data dir. */
    val image: String? = null,
    val updatedAt: Long = 0,
)

@Serializable
enum class Section(val label: String) { WARMUP("Warm-up"), MAIN("Workout"), COOLDOWN("Cool-down") }

@Serializable
data class WorkoutItem(
    val exerciseId: String,
    val sets: Int = 1,
    val reps: Int? = null,
    val seconds: Int? = null,
    val restSec: Int = 30,
    val section: Section = Section.MAIN,
)

@Serializable
data class Workout(
    val id: String,
    val name: String,
    val items: List<WorkoutItem>,
    val focus: Focus = Focus.FULL,
    val custom: Boolean = false,
    val deleted: Boolean = false,
    val updatedAt: Long = 0,
)

@Serializable
enum class Focus(val label: String, val muscles: List<Muscle>) {
    FULL("Full body", listOf(Muscle.LEGS, Muscle.CHEST, Muscle.BACK, Muscle.GLUTES, Muscle.SHOULDERS, Muscle.CORE, Muscle.ARMS, Muscle.CARDIO)),
    UPPER("Upper body", listOf(Muscle.CHEST, Muscle.BACK, Muscle.SHOULDERS, Muscle.ARMS, Muscle.CHEST, Muscle.BACK, Muscle.ARMS, Muscle.CORE)),
    LOWER("Lower body", listOf(Muscle.LEGS, Muscle.GLUTES, Muscle.LEGS, Muscle.GLUTES, Muscle.CORE, Muscle.LEGS, Muscle.GLUTES, Muscle.CARDIO)),
    CHEST("Chest", listOf(Muscle.CHEST, Muscle.CHEST, Muscle.SHOULDERS, Muscle.ARMS, Muscle.CHEST, Muscle.CORE, Muscle.CHEST)),
    BACK("Back", listOf(Muscle.BACK, Muscle.BACK, Muscle.ARMS, Muscle.BACK, Muscle.CORE, Muscle.BACK, Muscle.SHOULDERS)),
    SHOULDERS("Shoulders", listOf(Muscle.SHOULDERS, Muscle.SHOULDERS, Muscle.BACK, Muscle.SHOULDERS, Muscle.ARMS, Muscle.CORE, Muscle.SHOULDERS)),
    ARMS("Arms", listOf(Muscle.ARMS, Muscle.ARMS, Muscle.CHEST, Muscle.ARMS, Muscle.BACK, Muscle.ARMS, Muscle.CORE)),
    CORE("Core", listOf(Muscle.CORE, Muscle.CORE, Muscle.CORE, Muscle.GLUTES, Muscle.CORE, Muscle.CORE, Muscle.CARDIO)),
    LEGS("Legs & glutes", listOf(Muscle.LEGS, Muscle.GLUTES, Muscle.LEGS, Muscle.GLUTES, Muscle.LEGS, Muscle.CORE, Muscle.GLUTES)),
    CARDIO("Cardio", listOf(Muscle.CARDIO, Muscle.LEGS, Muscle.CARDIO, Muscle.CORE, Muscle.CARDIO, Muscle.GLUTES, Muscle.CARDIO)),
    YOGA("Yoga", emptyList()),
    BREATHE("Breathe", emptyList()),
}

@Serializable
data class Profile(
    val name: String = "",
    val gender: Gender = Gender.OTHER,
    val age: Int = 30,
    val heightCm: Int = 170,
    val weightKg: Float = 70f,
    val goal: Goal = Goal.GET_FIT,
    val level: Level = Level.BEGINNER,
    val daysPerWeek: Int = 3,
    val minutes: Int = 25,
    val equipment: List<Equipment> = emptyList(),
    val injuries: List<Injury> = emptyList(),
    val healthNotes: String = "",
    val includeYoga: Boolean = true,
    val includeBreathing: Boolean = true,
    /** -2..+2, nudged by post-workout feedback. */
    val intensity: Int = 0,
    val updatedAt: Long = 0,
) {
    val bmi: Float get() = weightKg / ((heightCm / 100f) * (heightCm / 100f))
    val lowImpact: Boolean
        get() = age >= 60 || bmi >= 32f || injuries.any {
            it == Injury.KNEE || it == Injury.ANKLE || it == Injury.HEART ||
                it == Injury.PREGNANCY || it == Injury.RECENT_SURGERY || it == Injury.HIP
        }
}

@Serializable
data class PlanDay(val title: String, val focus: Focus, val workout: Workout)

@Serializable
data class Plan(
    val days: List<PlanDay>,
    val source: String = "on-device",
    val notes: String = "",
    val createdAt: Long = 0,
)

@Serializable
data class Session(
    val id: String,
    val workoutName: String,
    val focus: Focus,
    val startedAt: Long,
    val durationSec: Int,
    val exercises: Int,
    val kcal: Int,
    val feedback: Int = 0,
    val fromPlan: Boolean = false,
)

@Serializable
enum class ThemeMode(val label: String) { SYSTEM("System"), LIGHT("Light"), DARK("Dark") }

/** Device-local settings (not synced). */
@Serializable
data class Settings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val voice: Boolean = true,
    val sounds: Boolean = true,
    val metric: Boolean = true,
    val gitRemote: String = "",
    val gitBranch: String = "main",
    val gitUser: String = "",
    val autoSync: Boolean = false,
    val lastSync: Long = 0,
    val aiEnabled: Boolean = false,
)

/** Secrets — never written to the git repo. */
@Serializable
data class Secrets(val gitToken: String = "", val anthropicKey: String = "")
