package com.ownfit.app

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat

/**
 * Android fixes an app's launcher label at install time, so we offer:
 *  - preset names, each an <activity-alias> in the manifest; we enable exactly one, and
 *  - any custom name, as a pinned home-screen shortcut (the launcher asks the user to confirm).
 */
object LauncherName {
    val presets = listOf("OwnFit", "Workout", "Fitness", "Gym", "Home Gym", "Train")

    private fun component(ctx: Context, name: String) =
        ComponentName(ctx.packageName, "com.ownfit.app.Launcher" + name.replace(" ", ""))

    fun current(ctx: Context): String = presets.firstOrNull { p ->
        val state = ctx.packageManager.getComponentEnabledSetting(component(ctx, p))
        state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED ||
            (state == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT && p == "OwnFit")
    } ?: "OwnFit"

    /** Switch the launcher entry. Enable the new one first so the app never has zero entries. */
    fun apply(ctx: Context, name: String) {
        val pm = ctx.packageManager
        pm.setComponentEnabledSetting(component(ctx, name), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
        presets.filter { it != name }.forEach {
            pm.setComponentEnabledSetting(component(ctx, it), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
        }
    }

    fun canPin(ctx: Context) = ShortcutManagerCompat.isRequestPinShortcutSupported(ctx)

    /** Ask the launcher to pin a shortcut labelled [label] that opens the app. */
    fun pinCustom(ctx: Context, label: String): Boolean {
        val intent = Intent(ctx, MainActivity::class.java).setAction(Intent.ACTION_MAIN)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val info = ShortcutInfoCompat.Builder(ctx, "custom-name")
            .setShortLabel(label.take(25))
            .setLongLabel(label)
            .setIcon(IconCompat.createWithResource(ctx, R.mipmap.ic_launcher))
            .setIntent(intent)
            .build()
        return runCatching { ShortcutManagerCompat.requestPinShortcut(ctx, info, null) }.getOrDefault(false)
    }
}
