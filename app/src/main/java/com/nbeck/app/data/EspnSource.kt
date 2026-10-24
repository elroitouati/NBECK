package com.nbeck.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.Instant
import java.util.concurrent.TimeUnit

/**
 * Player data source: ESPN's public site API — free, no key, updated daily during the season.
 *
 *  rosters     https://site.api.espn.com/apis/site/v2/sports/basketball/nba/teams/{id}/roster
 *  depth chart https://site.api.espn.com/apis/site/v2/sports/basketball/nba/teams/{id}/depthcharts
 *  stats       https://site.web.api.espn.com/apis/common/v3/sports/basketball/nba/statistics/byathlete
 *  headshots   https://a.espncdn.com/i/headshots/nba/players/full/{id}.png
 *
 * ESPN rosters only say G/F/C, so combo positions (G-F, F-G, F-C, C-F) come from the depth chart:
 * a player in the top 3 of an adjacent position group gets it as a secondary position.
 * tools/fetch_players.py mirrors this logic to build the bundled assets/players.json.
 */
class EspnSource(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build(),
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetchAll(): PlayerBundle = withContext(Dispatchers.IO) {
        coroutineScope {
            val gate = Semaphore(6)
            val teams = Teams.all.map { t -> async { gate.withPermit { team(t.espnId) } } }
            val stats = async { gate.withPermit { stats() } }
            val st = stats.await()
            val players = teams.awaitAll().flatten()
                .map { p -> st[p.id]?.let { p.copy(ppg = it.ppg, rpg = it.rpg, apg = it.apg, gp = it.gp) } ?: p }
                .sortedBy { it.id }
            PlayerBundle(source = "ESPN public site API", fetchedAt = Instant.now().toString(), players = players)
        }
    }

    private fun get(url: String): JsonObject {
        val req = Request.Builder().url(url).header("Accept", "application/json").build()
        client.newCall(req).execute().use { res ->
            check(res.isSuccessful) { "HTTP ${res.code} for $url" }
            return json.parseToJsonElement(res.body!!.string()).jsonObject
        }
    }

    private fun team(teamId: Int): List<Player> {
        val roster = get("$SITE/teams/$teamId/roster")
        val depth = runCatching { get("$SITE/teams/$teamId/depthcharts") }.getOrNull()
        val ranks = depthRanks(depth)
        return roster.arr("athletes").mapNotNull { el ->
            val a = el.jsonObject
            val id = a.str("id") ?: return@mapNotNull null
            val birth = a.str("dateOfBirth")?.take(10) ?: return@mapNotNull null
            Player(
                id = id,
                name = a.str("fullName") ?: a.str("displayName") ?: return@mapNotNull null,
                firstName = a.str("firstName").orEmpty(),
                lastName = a.str("lastName").orEmpty(),
                teamId = teamId,
                position = position(a.obj("position")?.str("abbreviation"), ranks[id].orEmpty()),
                birthDate = birth,
                jersey = a.str("jersey")?.takeIf { it.isNotBlank() },
                country = country(a.obj("birthPlace")),
                headshot = a.obj("headshot")?.str("href"),
            )
        }
    }

    private class Line(val ppg: Double?, val rpg: Double?, val apg: Double?, val gp: Int)

    private fun stats(): Map<String, Line> {
        val out = HashMap<String, Line>()
        var page = 1
        while (true) {
            val d = runCatching { get("$WEB/statistics/byathlete?limit=200&page=$page") }.getOrNull() ?: break
            val names = d.arr("categories").associate { c ->
                c.jsonObject.str("name") to c.jsonObject.arr("names").map { it.jsonPrimitive.content }
            }
            for (row in d.arr("athletes")) {
                val r = row.jsonObject
                val vals = HashMap<String, String>()
                for (c in r.arr("categories")) {
                    val co = c.jsonObject
                    val ns = names[co.str("name")] ?: continue
                    ns.zip(co.arr("totals").map { it.jsonPrimitive.content }).forEach { (k, v) -> vals.putIfAbsent(k, v) }
                }
                val id = r.obj("athlete")?.str("id") ?: continue
                out[id] = Line(
                    vals["avgPoints"]?.toDoubleOrNull(), vals["avgRebounds"]?.toDoubleOrNull(),
                    vals["avgAssists"]?.toDoubleOrNull(), vals["gamesPlayed"]?.toDoubleOrNull()?.toInt() ?: 0,
                )
            }
            val pages = d.obj("pagination")?.str("pages")?.toIntOrNull() ?: 1
            if (page >= pages) break
            page++
        }
        return out
    }

    companion object {
        private const val SITE = "https://site.api.espn.com/apis/site/v2/sports/basketball/nba"
        private const val WEB = "https://site.web.api.espn.com/apis/common/v3/sports/basketball/nba"
        private const val DEPTH_TOP = 3
        private val SLOT_GROUP = mapOf("pg" to "G", "sg" to "G", "sf" to "F", "pf" to "F", "c" to "C")
        private val ADJ = mapOf("G" to listOf("F"), "F" to listOf("G", "C"), "C" to listOf("F"))

        /** athlete id -> (group -> best depth rank). */
        fun depthRanks(depth: JsonObject?): Map<String, Map<String, Int>> {
            val best = HashMap<String, HashMap<String, Int>>()
            depth?.arr("depthchart")?.forEach { chart ->
                chart.jsonObject.obj("positions")?.forEach { (slot, v) ->
                    val g = SLOT_GROUP[slot.lowercase()] ?: return@forEach
                    v.jsonObject.arr("athletes").forEachIndexed { rank, a ->
                        val id = a.jsonObject.str("id") ?: return@forEachIndexed
                        val m = best.getOrPut(id) { HashMap() }
                        m[g] = minOf(m[g] ?: Int.MAX_VALUE, rank)
                    }
                }
            }
            return best
        }

        fun position(abbr: String?, ranks: Map<String, Int>): String {
            val p = when (abbr?.uppercase()) {
                "G", "PG", "SG" -> "G"
                "C" -> "C"
                else -> "F"
            }
            val sec = ADJ.getValue(p)
                .mapNotNull { g -> ranks[g]?.takeIf { it < DEPTH_TOP }?.let { it to g } }
                .minWithOrNull(compareBy({ it.first }, { it.second }))
            return if (sec != null) "$p-${sec.second}" else p
        }

        fun country(bp: JsonObject?): String? {
            if (bp == null) return null
            val c = bp.str("country")
            if (c != null) return if (c == "Türkiye") "Turkey" else c
            return if (bp.str("state") != null) "USA" else null
        }

        private fun JsonObject.str(k: String): String? =
            (this[k] as? kotlinx.serialization.json.JsonPrimitive)?.content?.takeIf { it != "null" }
        private fun JsonObject.obj(k: String): JsonObject? = this[k] as? JsonObject
        private fun JsonObject.arr(k: String): List<JsonElement> = (this[k] as? JsonArray) ?: emptyList()
    }
}
