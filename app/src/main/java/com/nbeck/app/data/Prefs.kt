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

enum class Language { HE, EN }
enum class ThemeMode { SYSTEM, DARK, LIGHT }

@Serializable
data class Settings(
    val language: Language = Language.HE,
    val theme: ThemeMode = ThemeMode.DARK,
    val sound: Boolean = true,
    val haptics: Boolean = true,
    val seenHowTo: Boolean = false,
)

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

    fun game(mode: Mode): Flow<SavedGame?> = context.store.data.map { p ->
        p[gameKey(mode)]?.let { runCatching { json.decodeFromString(SavedGame.serializer(), it) }.getOrNull() }
    }

    suspend fun saveGame(g: SavedGame) = put(gameKey(g.mode), SavedGame.serializer(), g)

    private fun gameKey(mode: Mode) = stringPreferencesKey("game_${mode.name.lowercase()}")

    private companion object {
        val K_SETTINGS = stringPreferencesKey("settings")
        val K_STATS = stringPreferencesKey("stats")
        val K_ACH = stringPreferencesKey("achievements")
    }
}
