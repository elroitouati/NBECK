package com.nbeck.app.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nbeck.app.game.AchievementState
import com.nbeck.app.game.Mode
import com.nbeck.app.game.Stats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDate

enum class Language { HE, EN }
enum class ThemeMode { SYSTEM, DARK, LIGHT }

@Serializable
data class Settings(
    val language: Language = Language.HE,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    /** False until the player picks a theme; until then the app follows the system setting. */
    val themeChosen: Boolean = false,
    val sound: Boolean = true,
    val haptics: Boolean = true,
    val seenHowTo: Boolean = false,
    /** versionCode whose "what's new" the player has seen; 0 = none yet (installs before 1.6.0). */
    val lastSeenVersion: Int = 0,
    /** Highest versionCode we already sent a "new version" notification for. */
    val notifiedVersion: Int = 0,
    /** Asked for the notification permission (Android 13+) once already. */
    val askedNotifications: Boolean = false,
    /** Evening reminder when today's daily isn't played yet. */
    val dailyReminder: Boolean = true,
)

/** Theme actually in effect: the system's until the player picks one in Settings. */
val Settings.effectiveTheme: ThemeMode get() = effectiveThemeOf(this)

fun effectiveThemeOf(s: Settings): ThemeMode = if (s.themeChosen) s.theme else ThemeMode.SYSTEM

/** A game in progress or finished. Daily games carry their date so a new day starts fresh. */
@Serializable
data class SavedGame(
    val mode: Mode,
    val date: String,
    val answerId: String,
    val guessIds: List<String> = emptyList(),
    val finished: Boolean = false,
    val won: Boolean = false,
    val gaveUp: Boolean = false,
    /** Wall-clock millis of the first guess (for the daily table's time tie-break); 0 = not started. */
    val startedAt: Long = 0,
    /** Stats already recorded for this game (guards against double counting). */
    val recorded: Boolean = false,
)

private val Context.store by preferencesDataStore("nbeck")

class Prefs(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private fun <T> flow(key: Preferences.Key<String>, ser: KSerializer<T>, default: T): Flow<T> =
        context.store.data.map { p -> p[key]?.let { runCatching { json.decodeFromString(ser, it) }.getOrNull() } ?: default }

    private suspend fun <T> put(key: Preferences.Key<String>, ser: KSerializer<T>, value: T) {
        context.store.edit { it[key] = json.encodeToString(ser, value) }
    }

    val settings = flow(K_SETTINGS, Settings.serializer(), Settings())
    val stats = flow(K_STATS, Stats.serializer(), Stats())
    val achievements = flow(K_ACH, AchievementState.serializer(), AchievementState())

    suspend fun updateSettings(f: (Settings) -> Settings) = put(K_SETTINGS, Settings.serializer(), f(settings.first()))
    suspend fun saveStats(s: Stats) = put(K_STATS, Stats.serializer(), s)
    suspend fun saveAchievements(s: AchievementState) = put(K_ACH, AchievementState.serializer(), s)

    /** Daily games are stored per date (archive); free play keeps one running game. */
    fun game(mode: Mode, date: LocalDate = LocalDate.now()): Flow<SavedGame?> = context.store.data.map { p ->
        val raw = p[gameKey(mode, date)]
            // Before the archive existed, today's daily lived under one key.
            ?: if (mode == Mode.DAILY) p[LEGACY_DAILY] else null
        raw?.let { runCatching { json.decodeFromString(SavedGame.serializer(), it) }.getOrNull() }
            ?.takeIf { mode == Mode.FREE || it.date == date.toString() }
    }

    /** Every stored daily game, by date (ISO). */
    val dailyGames: Flow<Map<String, SavedGame>> = context.store.data.map { p ->
        p.asMap().entries.mapNotNull { (k, v) ->
            if (k.name != LEGACY_DAILY.name && !k.name.startsWith("game_daily_")) return@mapNotNull null
            runCatching { json.decodeFromString(SavedGame.serializer(), v as String) }.getOrNull()
        }.associateBy { it.date }
    }

    suspend fun saveGame(g: SavedGame) {
        context.store.edit { p ->
            p[gameKey(g.mode, LocalDate.parse(g.date))] = json.encodeToString(SavedGame.serializer(), g)
            if (g.mode == Mode.DAILY) p.remove(LEGACY_DAILY) // migrated to the per-date key
        }
    }

    /** "Delete my data": statistics, achievements and all games. Settings (language, theme) stay. */
    suspend fun clearProgress() {
        context.store.edit { p ->
            val keep = p[K_SETTINGS]
            p.clear()
            if (keep != null) p[K_SETTINGS] = keep
        }
    }

    private fun gameKey(mode: Mode, date: LocalDate) =
        if (mode == Mode.DAILY) stringPreferencesKey("game_daily_$date") else stringPreferencesKey("game_free")

    private companion object {
        val K_SETTINGS = stringPreferencesKey("settings")
        val K_STATS = stringPreferencesKey("stats")
        val K_ACH = stringPreferencesKey("achievements")
        val LEGACY_DAILY = stringPreferencesKey("game_daily")
    }
}
