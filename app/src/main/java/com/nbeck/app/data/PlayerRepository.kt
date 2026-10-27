package com.nbeck.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.time.Duration
import java.time.Instant

/**
 * Player list: cached copy from the last successful download (filesDir/players.json), falling back
 * to the bundled assets/players.json on first launch without network. Refreshed at most once a day.
 */
private const val OPEN_REFRESH_GAP = 10 * 60 * 1000L

class PlayerRepository(private val context: Context, private val source: EspnSource = EspnSource()) {

    private val json = Json { ignoreUnknownKeys = true }
    private val cacheFile get() = File(context.filesDir, "players.json")
    private val mutex = Mutex()

    private val _players = MutableStateFlow<List<Player>>(emptyList())
    val players: StateFlow<List<Player>> = _players

    private var fetchedAt: Instant = Instant.EPOCH

    suspend fun load(): List<Player> = mutex.withLock {
        if (_players.value.isNotEmpty()) return _players.value
        val bundle = withContext(Dispatchers.IO) {
            runCatching { json.decodeFromString<PlayerBundle>(cacheFile.readText()) }.getOrNull()
                ?.takeIf { it.schema >= PLAYER_SCHEMA }
                ?: json.decodeFromString<PlayerBundle>(context.assets.open("players.json").bufferedReader().use { it.readText() })
        }
        publish(bundle)
        _players.value
    }

    val isStale get() = Duration.between(fetchedAt, Instant.now()) > Duration.ofHours(24)

    /** Downloads fresh rosters. Returns true when new data was stored. */
    suspend fun refresh(force: Boolean = false): Boolean {
        load()
        if (!force && !isStale) return false
        val known = _players.value.associateBy { it.id }
        val bundle = runCatching { source.fetchAll(known, ringIds()) }.getOrNull() ?: return false
        val fixes = corrections()
        val valid = bundle.players.filter { Teams.find(it.teamId) != null }.map { p -> fixes[p.id]?.applyTo(p) ?: p }
        if (valid.size < 300) return false // partial outage — keep what we have
        val clean = bundle.copy(schema = PLAYER_SCHEMA, players = valid)
        withContext(Dispatchers.IO) {
            val tmp = File(context.filesDir, "players.json.tmp")
            tmp.writeText(json.encodeToString(PlayerBundle.serializer(), clean))
            tmp.renameTo(cacheFile)
        }
        mutex.withLock { publish(clean) }
        return true
    }

    private fun publish(bundle: PlayerBundle) {
        fetchedAt = runCatching { Instant.parse(bundle.fetchedAt) }.getOrDefault(Instant.EPOCH)
        _players.value = bundle.players.filter { Teams.find(it.teamId) != null }
    }

    @kotlinx.serialization.Serializable
    private class Rings(val champions: Map<String, List<String>> = emptyMap())

    private fun ringIds(): Set<String> = runCatching {
        json.decodeFromString<Rings>(context.assets.open("rings.json").bufferedReader().use { it.readText() })
            .champions.values.flatten().toSet()
    }.getOrDefault(emptySet())

    /** Hand-checked fixes (assets/corrections.json, built by tools/fetch_players.py); always win over ESPN. */
    @kotlinx.serialization.Serializable
    private class Correction(val country: String? = null, val birthDate: String? = null, val jersey: String? = null) {
        fun applyTo(p: Player) = p.copy(
            country = country ?: p.country,
            birthDate = birthDate ?: p.birthDate,
            jersey = jersey ?: p.jersey,
        )
    }

    private fun corrections(): Map<String, Correction> = runCatching {
        json.decodeFromString<Map<String, Correction>>(context.assets.open("corrections.json").bufferedReader().use { it.readText() })
    }.getOrDefault(emptyMap())

    /**
     * Called whenever the app comes to the foreground. Refreshes from ESPN unless the last attempt
     * was under [OPEN_REFRESH_GAP] ago (switching apps back and forth shouldn't re-download).
     */
    suspend fun refreshOnOpen() {
        val now = System.currentTimeMillis()
        if (now - lastOpenRefresh < OPEN_REFRESH_GAP) return
        lastOpenRefresh = now
        refresh(force = true)
    }

    @Volatile private var lastOpenRefresh = 0L

    fun byId(id: String): Player? = _players.value.firstOrNull { it.id == id }
}
