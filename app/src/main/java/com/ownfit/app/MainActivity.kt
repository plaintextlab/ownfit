package com.ownfit.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ownfit.app.ui.OwnFitRoot
import com.ownfit.app.ui.theme.OwnFitTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val app = application as OwnFitApp
        installSplashScreen().setKeepOnScreenCondition { !app.store.state.value.loaded }
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val state by app.store.state.collectAsStateWithLifecycle()
            OwnFitTheme(state.settings.theme, state.settings.dynamicColor) {
                OwnFitRoot(state)
            }
        }
    }
}
