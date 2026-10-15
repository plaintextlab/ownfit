package com.ownfit.app.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

data class AppState(
    val loaded: Boolean = false,
    val profile: Profile? = null,
    val plan: Plan? = null,
    val settings: Settings = Settings(),
    val secrets: Secrets = Secrets(),
    val customExercises: List<Exercise> = emptyList(),
    val workouts: List<Workout> = emptyList(),
    val media: Map<String, Media> = emptyMap(),
    val history: List<Session> = emptyList(),
) {
    val exercises: List<Exercise> by lazy {
        ExerciseLibrary.all + customExercises.filter { !it.deleted }
    }
    private val index by lazy { exercises.associateBy { it.id } }
    fun exercise(id: String): Exercise? = index[id] ?: customExercises.firstOrNull { it.id == id }
    val activeWorkouts get() = workouts.filter { !it.deleted }
    /** The chosen trainer, defaulting to one matching the user's gender. */
    val trainer: Trainer get() = settings.trainer ?: Trainer.defaultFor(profile?.gender)
}

/**
 * Tiny JSON-file store. Everything the user creates lives under [dataDir] as plain JSON so it
 * can be committed to a git repo as-is. Device-only settings and secrets live in [privateDir].
 */
class Store(context: Context) {
    val dataDir = File(context.filesDir, "data").apply { mkdirs() }
    val mediaDir = File(dataDir, "media").apply { mkdirs() }
    private val privateDir = File(context.filesDir, "private").apply { mkdirs() }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1))
    private val writeLock = Mutex()
    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()

    companion object {
        val json = Json { ignoreUnknownKeys = true; prettyPrint = true; encodeDefaults = false; explicitNulls = false }
        const val PROFILE = "profile.json"
        const val PLAN = "plan.json"
        const val EXERCISES = "custom_exercises.json"
        const val WORKOUTS = "workouts.json"
        const val MEDIA = "media.json"
        const val HISTORY = "history.json"
        val mediaMapSerializer = MapSerializer(String.serializer(), Media.serializer())
        fun newId(): String = UUID.randomUUID().toString().substring(0, 8)
    }

    init {
        scope.launch { load() }
    }

    private fun <T> read(dir: File, name: String, s: KSerializer<T>): T? = try {
        File(dir, name).takeIf { it.exists() }?.readText()?.let { json.decodeFromString(s, it) }
    } catch (e: Exception) {
        null
    }

    fun load() {
        val st = AppState(
            loaded = true,
            profile = read(dataDir, PROFILE, Profile.serializer()),
            plan = read(dataDir, PLAN, Plan.serializer()),
            settings = read(privateDir, "settings.json", Settings.serializer()) ?: Settings(),
            secrets = read(privateDir, "secrets.json", Secrets.serializer()) ?: Secrets(),
            customExercises = read(dataDir, EXERCISES, ListSerializer(Exercise.serializer())) ?: emptyList(),
            workouts = read(dataDir, WORKOUTS, ListSerializer(Workout.serializer())) ?: emptyList(),
            media = read(dataDir, MEDIA, mediaMapSerializer) ?: emptyMap(),
            history = read(dataDir, HISTORY, ListSerializer(Session.serializer())) ?: emptyList(),
        )
        _state.value = st
    }

    private fun <T> write(dir: File, name: String, s: KSerializer<T>, value: T?) {
        scope.launch {
            writeLock.withLock {
                val f = File(dir, name)
                if (value == null) f.delete()
                else {
                    val tmp = File(dir, "$name.tmp")
                    tmp.writeText(json.encodeToString(s, value))
                    tmp.renameTo(f)
                }
            }
        }
    }

    private fun now() = System.currentTimeMillis()

    fun setProfile(p: Profile) {
        val v = p.copy(updatedAt = now())
        _state.update { it.copy(profile = v) }
        write(dataDir, PROFILE, Profile.serializer(), v)
    }

    fun setPlan(p: Plan?) {
        _state.update { it.copy(plan = p) }
        write(dataDir, PLAN, Plan.serializer(), p)
    }

    fun updateSettings(f: (Settings) -> Settings) {
        val v = f(_state.value.settings)
        _state.update { it.copy(settings = v) }
        write(privateDir, "settings.json", Settings.serializer(), v)
    }

    fun updateSecrets(f: (Secrets) -> Secrets) {
        val v = f(_state.value.secrets)
        _state.update { it.copy(secrets = v) }
        write(privateDir, "secrets.json", Secrets.serializer(), v)
    }

    fun upsertExercise(e: Exercise) {
        val v = e.copy(updatedAt = now(), custom = true)
        val list = _state.value.customExercises.filter { it.id != v.id } + v
        _state.update { it.copy(customExercises = list) }
        write(dataDir, EXERCISES, ListSerializer(Exercise.serializer()), list)
    }

    fun deleteExercise(id: String) {
        _state.value.customExercises.firstOrNull { it.id == id }?.let { upsertExercise(it.copy(deleted = true)) }
    }

    fun upsertWorkout(w: Workout) {
        val v = w.copy(updatedAt = now(), custom = true)
        val list = _state.value.workouts.filter { it.id != v.id } + v
        _state.update { it.copy(workouts = list) }
        write(dataDir, WORKOUTS, ListSerializer(Workout.serializer()), list)
    }

    fun deleteWorkout(id: String) {
        _state.value.workouts.firstOrNull { it.id == id }?.let { upsertWorkout(it.copy(deleted = true, items = emptyList())) }
    }

    fun setMedia(exerciseId: String, m: Media) {
        val v = m.copy(updatedAt = now())
        val map = _state.value.media + (exerciseId to v)
        _state.update { it.copy(media = map) }
        write(dataDir, MEDIA, mediaMapSerializer, map)
    }

    fun addSession(s: Session) {
        val list = _state.value.history + s
        _state.update { it.copy(history = list) }
        write(dataDir, HISTORY, ListSerializer(Session.serializer()), list)
    }

    /** Replace all syncable data at once (used after a git merge). */
    fun replaceSynced(
        profile: Profile?, plan: Plan?, exercises: List<Exercise>, workouts: List<Workout>,
        media: Map<String, Media>, history: List<Session>,
    ) {
        _state.update {
            it.copy(profile = profile, plan = plan, customExercises = exercises, workouts = workouts, media = media, history = history)
        }
        write(dataDir, PROFILE, Profile.serializer(), profile)
        write(dataDir, PLAN, Plan.serializer(), plan)
        write(dataDir, EXERCISES, ListSerializer(Exercise.serializer()), exercises)
        write(dataDir, WORKOUTS, ListSerializer(Workout.serializer()), workouts)
        write(dataDir, MEDIA, mediaMapSerializer, media)
        write(dataDir, HISTORY, ListSerializer(Session.serializer()), history)
    }

    /** Wait for pending writes to hit disk. */
    suspend fun flush() = writeLock.withLock { }

    fun resolveMedia(path: String?): Any? = when {
        path.isNullOrBlank() -> null
        path.startsWith("http") -> path
        else -> File(dataDir, path).takeIf { it.exists() }
    }

    fun resetAll() {
        dataDir.listFiles()?.forEach { if (it.isFile) it.delete() }
        mediaDir.listFiles()?.forEach { it.delete() }
        _state.update { AppState(loaded = true, settings = it.settings, secrets = it.secrets) }
    }
}
