package com.ownfit.app.sync

import android.content.Context
import android.os.Build
import com.ownfit.app.data.Exercise
import com.ownfit.app.data.Media
import com.ownfit.app.data.Plan
import com.ownfit.app.data.Profile
import com.ownfit.app.data.Session
import com.ownfit.app.data.Store
import com.ownfit.app.data.Workout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ResetCommand
import org.eclipse.jgit.lib.Config
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.storage.file.FileBasedConfig
import org.eclipse.jgit.transport.RefSpec
import org.eclipse.jgit.transport.RemoteRefUpdate
import org.eclipse.jgit.transport.URIish
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.eclipse.jgit.util.FS
import org.eclipse.jgit.util.SystemReader
import java.io.File

/**
 * Syncs the user's data folder with any HTTPS git remote (GitHub, GitLab, Gitea, ...).
 *
 * Data lives in `ownfit/` inside the repo, so an existing repo can be reused. Instead of
 * textual git merges, each sync fetches the remote, merges records semantically (newest
 * `updatedAt` wins per item, history is a union), commits and pushes — so two phones
 * never produce merge conflicts.
 */
class GitSync(private val context: Context, private val store: Store) {

    private val repoDir = File(context.filesDir, "sync")
    private val home = File(context.filesDir, "githome").apply { mkdirs() }

    init {
        installSystemReader(home)
    }

    suspend fun sync(remote: String, branch: String, user: String, token: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                require(remote.startsWith("https://")) { "Use an HTTPS remote URL, e.g. https://github.com/you/fitness-data.git" }
                var attempt = 0
                var msg: String
                while (true) {
                    attempt++
                    val (ok, m) = syncOnce(remote.trim(), branch.trim().ifEmpty { "main" }, user.trim(), token.trim())
                    msg = m
                    if (ok || attempt >= 2) break
                }
                msg
            }
        }

    private suspend fun syncOnce(remote: String, branch: String, user: String, token: String): Pair<Boolean, String> {
        val creds = UsernamePasswordCredentialsProvider(user.ifEmpty { "ownfit" }, token)
        val git = openOrInit(remote, branch)
        git.use { g ->
            val repo = g.repository
            // 1. Fetch and fast-forward the working tree to the remote state.
            g.fetch().setRemote("origin").setCredentialsProvider(creds)
                .setRefSpecs(RefSpec("+refs/heads/$branch:refs/remotes/origin/$branch")).call()
            val remoteRef = repo.exactRef("refs/remotes/origin/$branch")
            if (remoteRef != null) {
                val u = repo.updateRef("refs/heads/$branch")
                u.setNewObjectId(remoteRef.objectId)
                u.isForceUpdate = true
                u.forceUpdate()
                repo.updateRef(Constants.HEAD).link("refs/heads/$branch")
                g.reset().setMode(ResetCommand.ResetType.HARD).call()
            }

            // 2. Merge remote files with local data.
            val dir = File(repoDir, "ownfit").apply { mkdirs() }
            val mediaDir = File(dir, "media").apply { mkdirs() }
            store.flush()
            val local = store.state.value

            val rProfile = read(dir, Store.PROFILE, Profile.serializer())
            val rPlan = read(dir, Store.PLAN, Plan.serializer())
            val rEx = read(dir, Store.EXERCISES, ListSerializer(Exercise.serializer())).orEmpty()
            val rWo = read(dir, Store.WORKOUTS, ListSerializer(Workout.serializer())).orEmpty()
            val rMedia = read(dir, Store.MEDIA, Store.mediaMapSerializer).orEmpty()
            val rHist = read(dir, Store.HISTORY, ListSerializer(Session.serializer())).orEmpty()

            val profile = listOfNotNull(local.profile, rProfile).maxByOrNull { it.updatedAt }
            val plan = listOfNotNull(local.plan, rPlan).maxByOrNull { it.createdAt }
            val exercises = mergeById(local.customExercises, rEx, { it.id }, { it.updatedAt })
            val workouts = mergeById(local.workouts, rWo, { it.id }, { it.updatedAt })
            val media = (local.media.keys + rMedia.keys).associateWith { k ->
                listOfNotNull(local.media[k], rMedia[k]).maxBy(Media::updatedAt)
            }
            val history = (local.history + rHist).distinctBy { it.id }.sortedBy { it.startedAt }

            // Media files both ways.
            copyMissing(mediaDir, store.mediaDir)
            copyMissing(store.mediaDir, mediaDir)

            store.replaceSynced(profile, plan, exercises, workouts, media, history)
            store.flush()

            write(dir, Store.PROFILE, Profile.serializer(), profile)
            write(dir, Store.PLAN, Plan.serializer(), plan)
            write(dir, Store.EXERCISES, ListSerializer(Exercise.serializer()), exercises)
            write(dir, Store.WORKOUTS, ListSerializer(Workout.serializer()), workouts)
            write(dir, Store.MEDIA, Store.mediaMapSerializer, media)
            write(dir, Store.HISTORY, ListSerializer(Session.serializer()), history)
            File(repoDir, "ownfit/README.md").takeIf { !it.exists() }?.writeText(
                "# OwnFit data\n\nWorkout data synced by the OwnFit Android app. Plain JSON — safe to read, back up or edit.\n"
            )

            // 3. Commit and push.
            g.add().addFilepattern(".").call()
            val changed = !g.status().call().isClean
            if (changed) {
                val who = PersonIdent(user.ifEmpty { "OwnFit" }, "ownfit@users.noreply.local")
                g.commit().setAuthor(who).setCommitter(who)
                    .setMessage("OwnFit sync from ${Build.MODEL}").call()
            }
            if (!changed && remoteRef != null) return true to "Up to date"

            val results = g.push().setRemote("origin").setCredentialsProvider(creds)
                .setRefSpecs(RefSpec("refs/heads/$branch:refs/heads/$branch")).call()
            val statuses = results.flatMap { it.remoteUpdates }.map { it.status }
            return when {
                statuses.all { it == RemoteRefUpdate.Status.OK || it == RemoteRefUpdate.Status.UP_TO_DATE } ->
                    true to "Synced ✓"
                statuses.any { it == RemoteRefUpdate.Status.REJECTED_NONFASTFORWARD } ->
                    false to "Remote changed during sync — retry"
                else -> false to "Push failed: ${statuses.joinToString()}"
            }
        }
    }

    private fun openOrInit(remote: String, branch: String): Git {
        val git = if (File(repoDir, ".git").exists()) Git.open(repoDir)
        else {
            repoDir.deleteRecursively()
            repoDir.mkdirs()
            Git.init().setDirectory(repoDir).setInitialBranch(branch).call()
        }
        val cfg = git.repository.config
        if (cfg.getString("remote", "origin", "url") != remote) {
            if (cfg.getSubsections("remote").contains("origin")) {
                git.remoteSetUrl().setRemoteName("origin").setRemoteUri(URIish(remote)).call()
            } else {
                git.remoteAdd().setName("origin").setUri(URIish(remote)).call()
            }
        }
        return git
    }

    /** Forget the local clone (e.g. after changing remote). */
    fun reset() {
        repoDir.deleteRecursively()
    }

    private fun <T> mergeById(a: List<T>, b: List<T>, id: (T) -> String, ts: (T) -> Long): List<T> =
        (a + b).groupBy(id).values.map { group -> group.maxBy(ts) }

    private fun copyMissing(from: File, to: File) {
        from.listFiles()?.filter { it.isFile }?.forEach { f ->
            val t = File(to, f.name)
            if (!t.exists()) f.copyTo(t)
        }
    }

    private fun <T> read(dir: File, name: String, s: KSerializer<T>): T? = try {
        File(dir, name).takeIf { it.exists() }?.readText()?.let { Store.json.decodeFromString(s, it) }
    } catch (e: Exception) {
        null
    }

    private fun <T> write(dir: File, name: String, s: KSerializer<T>, v: T?) {
        val f = File(dir, name)
        if (v == null) f.delete() else f.writeText(Store.json.encodeToString(s, v))
    }

    companion object {
        @Volatile private var installed = false

        /** JGit expects a desktop-like environment (HOME, system gitconfig). Point it at app storage. */
        private fun installSystemReader(home: File) {
            if (installed) return
            synchronized(this) {
                if (installed) return
                val base = SystemReader.getInstance()
                SystemReader.setInstance(AndroidSystemReader(base, home))
                FS.DETECTED.setUserHome(home)
                installed = true
            }
        }
    }

    private class AndroidSystemReader(private val base: SystemReader, private val home: File) : SystemReader() {
        override fun getHostname(): String = "android"
        override fun getenv(variable: String?): String? = when (variable) {
            "HOME" -> home.path
            "GIT_CONFIG_NOSYSTEM" -> "true"
            else -> base.getenv(variable)
        }
        override fun getProperty(key: String?): String? = if (key == "user.home") home.path else base.getProperty(key)
        override fun openUserConfig(parent: Config?, fs: FS?): FileBasedConfig =
            FileBasedConfig(parent, File(home, ".gitconfig"), fs)
        override fun openSystemConfig(parent: Config?, fs: FS?): FileBasedConfig =
            object : FileBasedConfig(parent, null, fs) {
                override fun load() {}
                override fun isOutdated(): Boolean = false
            }
        override fun openJGitConfig(parent: Config?, fs: FS?): FileBasedConfig =
            FileBasedConfig(parent, File(home, "jgitconfig"), fs)
        override fun getCurrentTime(): Long = System.currentTimeMillis()
        override fun getTimezone(time: Long): Int = base.getTimezone(time)
    }
}
