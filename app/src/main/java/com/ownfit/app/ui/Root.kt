package com.ownfit.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.ownfit.app.data.AppState
import com.ownfit.app.data.Workout
import com.ownfit.app.ui.home.HomeScreen
import com.ownfit.app.ui.onboarding.OnboardingScreen
import com.ownfit.app.ui.player.PlayerScreen
import com.ownfit.app.ui.progress.ProgressScreen
import com.ownfit.app.ui.settings.SettingsScreen
import com.ownfit.app.ui.workouts.ExerciseDetailScreen
import com.ownfit.app.ui.workouts.WorkoutEditorScreen
import com.ownfit.app.ui.workouts.WorkoutsScreen

sealed interface Screen {
    data object Tabs : Screen
    data class Player(val workout: Workout, val fromPlan: Boolean) : Screen
    data class Editor(val workoutId: String?) : Screen
    data class ExerciseDetail(val id: String) : Screen
    data object Settings : Screen
    data object EditProfile : Screen
}

/** Minimal navigator — a plain back stack, no extra library. */
class Nav {
    val stack = mutableStateListOf<Screen>(Screen.Tabs)
    val current get() = stack.last()
    fun go(s: Screen) { stack.add(s) }
    fun back(): Boolean = if (stack.size > 1) { stack.removeAt(stack.lastIndex); true } else false
}

private enum class Tab(val label: String, val icon: ImageVector) {
    TODAY("Today", Icons.Outlined.WbSunny),
    WORKOUTS("Workouts", Icons.Outlined.FitnessCenter),
    PROGRESS("Progress", Icons.Outlined.Insights),
}

@Composable
fun OwnFitRoot(state: AppState) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        if (!state.loaded) return@Surface
        if (state.profile == null) {
            OnboardingScreen(state, initial = null, onDone = {})
            return@Surface
        }
        val nav = remember { Nav() }
        BackHandler(enabled = nav.stack.size > 1) { nav.back() }
        AnimatedContent(
            targetState = nav.current,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "nav",
        ) { screen ->
            when (screen) {
                Screen.Tabs -> TabsHost(state, nav)
                is Screen.Player -> PlayerScreen(state, screen.workout, screen.fromPlan, onExit = { nav.back() })
                is Screen.Editor -> WorkoutEditorScreen(state, screen.workoutId, nav)
                is Screen.ExerciseDetail -> ExerciseDetailScreen(state, screen.id, onBack = { nav.back() })
                Screen.Settings -> SettingsScreen(state, nav)
                Screen.EditProfile -> OnboardingScreen(state, initial = state.profile, onDone = { nav.back() })
            }
        }
    }
}

@Composable
private fun TabsHost(state: AppState, nav: Nav) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.entries.forEachIndexed { i, t ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = { Icon(t.icon, null) },
                        label = { Text(t.label) },
                    )
                }
            }
        },
    ) { pad ->
        Box(Modifier.padding(bottom = pad.calculateBottomPadding())) {
            when (Tab.entries[tab]) {
                Tab.TODAY -> HomeScreen(state, nav)
                Tab.WORKOUTS -> WorkoutsScreen(state, nav)
                Tab.PROGRESS -> ProgressScreen(state)
            }
        }
    }
}
