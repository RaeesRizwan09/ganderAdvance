package com.arjun.gander

import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit

/**
 * The reader's own preferences, as opposed to [Recents], which is a record of what
 * they opened.
 *
 * Night mode for PDF pages and the page thumbnail strip live here because they are
 * reading choices, not one-shot toggles. App chrome (light/dark/system, brand vs
 * Material You) is the same kind of choice, so it shares the file.
 *
 * Not backed up: `allowBackup="false"` in the manifest covers this file along with the
 * recents list, for the reason given there.
 */
object Settings {

    private const val PREFS = "viewer"
    private const val KEY_NIGHT = "night"
    private const val KEY_PAGE_STRIP = "page_strip"
    private const val KEY_THEME = "theme_mode"

    /**
     * Chrome appearance. The five names are the ones on the Settings row.
     *
     * Dynamic and Custom pick a colour source; Dark, Light and System pick a night
     * mode. They share one control so the tab stays three rows. Dynamic and Custom
     * both follow the phone's light/dark; Dark and Light pin it. Custom and System
     * both use Gander's own palette — System is "do what the phone does" and Custom
     * is "use our colours" — which is the same look, named for the two ways people
     * ask for it.
     */
    enum class ThemeMode {
        DYNAMIC,
        CUSTOM,
        DARK,
        LIGHT,
        SYSTEM,
    }

    /** Whether PDF pages are drawn turned over. See issue #19 and `pdf.html`. */
    fun night(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_NIGHT, false)

    fun setNight(context: Context, on: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit { putBoolean(KEY_NIGHT, on) }
    }

    fun pageStrip(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_PAGE_STRIP, true)

    fun setPageStrip(context: Context, on: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit { putBoolean(KEY_PAGE_STRIP, on) }
    }

    fun themeMode(context: Context): ThemeMode {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_THEME, ThemeMode.SYSTEM.name)
        return ThemeMode.entries.firstOrNull { it.name == raw } ?: ThemeMode.SYSTEM
    }

    fun setThemeMode(context: Context, mode: ThemeMode) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit(commit = true) { putString(KEY_THEME, mode.name) }
        AppCompatDelegate.setDefaultNightMode(nightMode(mode))
    }

    fun nightMode(mode: ThemeMode): Int = when (mode) {
        ThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
        ThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
        ThemeMode.DYNAMIC, ThemeMode.CUSTOM, ThemeMode.SYSTEM ->
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    }

    fun usesDynamicColor(context: Context): Boolean =
        themeMode(context) == ThemeMode.DYNAMIC &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    /** Night mode for this process. Colour source is applied in [GanderApp]. */
    fun applyNightMode(context: Context) {
        AppCompatDelegate.setDefaultNightMode(nightMode(themeMode(context)))
    }
}
