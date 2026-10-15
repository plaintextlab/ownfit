package com.ownfit.app

import android.app.Application
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.ownfit.app.data.Plan
import com.ownfit.app.data.Profile
import com.ownfit.app.data.Store
import com.ownfit.app.engine.AiCoach
import com.ownfit.app.engine.PlanGenerator
import com.ownfit.app.sync.GitSync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class OwnFitApp : Application(), ImageLoaderFactory {
    lateinit var store: Store
        private set

    /** JGit is heavy — only load it when sync is actually used. */
    val sync: GitSync by lazy { GitSync(this, store) }
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        instance = this
        store = Store(this)
    }

    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .components {
            if (Build.VERSION.SDK_INT >= 28) add(ImageDecoderDecoder.Factory()) else add(GifDecoder.Factory())
        }
        .crossfade(true)
        .build()

    /** Rebuilds the weekly plan. Uses Claude when enabled, otherwise (or on failure) the on-device coach. */
    suspend fun rebuildPlan(forProfile: Profile? = null): String {
        val st = store.state.value
        val profile = forProfile ?: st.profile ?: return "Set up your profile first"
        var msg = "Plan updated"
        val plan: Plan = if (st.settings.aiEnabled && st.secrets.anthropicKey.isNotBlank()) {
            AiCoach.buildPlan(st.secrets.anthropicKey, profile, st.exercises).getOrElse {
                msg = "AI unavailable (${it.message ?: it.javaClass.simpleName}) — used on-device coach"
                PlanGenerator.buildPlan(profile, st.exercises)
            }
        } else PlanGenerator.buildPlan(profile, st.exercises)
        store.setPlan(plan)
        if (plan.source == "Claude") msg = "AI plan ready"
        autoSync()
        return msg
    }

    fun syncNow(onDone: (String) -> Unit = {}) {
        val st = store.state.value
        val s = st.settings
        if (s.gitRemote.isBlank()) { onDone("Add a git remote in Settings first"); return }
        scope.launch {
            val r = sync.sync(s.gitRemote, s.gitBranch, s.gitUser, st.secrets.gitToken)
            if (r.isSuccess) store.updateSettings { it.copy(lastSync = System.currentTimeMillis()) }
            onDone(r.getOrElse { "Sync failed: ${it.message ?: it.javaClass.simpleName}" })
        }
    }

    fun autoSync() {
        val s = store.state.value.settings
        if (s.autoSync && s.gitRemote.isNotBlank()) syncNow()
    }

    companion object {
        lateinit var instance: OwnFitApp
            private set
    }
}
