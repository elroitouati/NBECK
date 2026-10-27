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
 *  stats       https://site.web.api.espn.com/apis/common/v3/sports/basketball/nba/statistics/byathlete
 *  draft year  https://sports.core.api.espn.com/v2/sports/basketball/leagues/nba/athletes/{id}
 *  headshots   https://a.espncdn.com/i/headshots/nba/players/full/{id}.png
 *
 * Draft year never changes, so it is only downloaded for players we have not seen before.
 * Championship rings come from the bundled assets/rings.json (built by tools/fetch_players.py
 * from every NBA Finals box score since 2004).
 */
class EspnSource(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build(),
) {
    private val json = Json { ignoreUnknownKeys = true }

    /** [known]: players from the previous data set, reused for draft years. [ringIds]: title winners. */
    suspend fun fetchAll(known: Map<String, Player>, ringIds: Set<String>): PlayerBundle = withContext(Dispatchers.IO) {
        coroutineScope {
            val gate = Semaphore(6)
            val teams = Teams.all.map { t -> async { gate.withPermit { team(t.espnId) } } }
            val stats = async { gate.withPermit { stats() } }
            val st = stats.await()
            val roster = teams.awaitAll().flatten()
            val drafts = roster.map { p ->
                async {
                    val prev = known[p.id]
                    if (prev != null) prev.draftYear else gate.withPermit { draftYear(p.id) }
                }
            }.awaitAll()
            val worn = runCatching { liveJerseys() }.getOrDefault(emptyMap())
            val players = roster.zip(drafts) { p, draft ->
                p.copy(
                    draftYear = draft,
                    ring = p.id in ringIds,
                    // The number worn in the team's latest game beats ESPN's (sometimes stale) profile.
                    jersey = worn[p.teamId to p.id] ?: p.jersey,
                    // ESPN lacks a birthplace for some international players; keep the previous one.
                    country = p.country ?: known[p.id]?.country,
                )
            }
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
                position = position(a.obj("position")?.str("abbreviation")),
                birthDate = birth,
                jersey = a.str("jersey")?.takeIf { it.isNotBlank() },
                country = country(a.obj("birthPlace")),
                headshot = a.obj("headshot")?.str("href"),
            )
        }
    }

    /**
     * (team id, player id) -> jersey worn in that team's most recent game. ESPN player profiles lag
     * behind number changes; the game lineup does not. Walks back up to 7 days of scoreboards until
     * every team has a game, then reads each game's lineup (small core-API documents).
     */
    private suspend fun liveJerseys(): Map<Pair<Int, String>, String> = coroutineScope {
        val latest = LinkedHashMap<Int, String>()
        var day = java.time.LocalDate.now()
        repeat(7) {
            if (latest.size >= Teams.all.size) return@repeat
            val board = runCatching { get("$SITE/scoreboard?dates=${day.format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE)}") }.getOrNull()
            board?.arr("events")?.forEach { e ->
                val ev = e.jsonObject
                val done = ev.obj("status")?.obj("type")?.str("completed") == "true"
                if (!done) return@forEach
                val id = ev.str("id") ?: return@forEach
                ev.arr("competitions").firstOrNull()?.jsonObject?.arr("competitors")?.forEach { c ->
                    c.jsonObject.obj("team")?.str("id")?.toIntOrNull()?.let { latest.putIfAbsent(it, id) }
                }
            }
            day = day.minusDays(1)
        }
        val gate = Semaphore(6)
        latest.map { (team, event) ->
            async {
                gate.withPermit {
                    runCatching {
                        get("$CORE/events/$event/competitions/$event/competitors/$team/roster").arr("entries").mapNotNull { x ->
                            val o = x.jsonObject
                            val pid = o.str("playerId") ?: return@mapNotNull null
                            val j = o.str("jersey")?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                            (team to pid) to j
                        }
                    }.getOrDefault(emptyList())
                }
            }
        }.awaitAll().flatten().toMap()
    }

    /** Absent "draft" on the athlete = undrafted. */
    private fun draftYear(id: String): Int? =
        runCatching { get("$CORE/athletes/$id").obj("draft")?.str("year")?.toIntOrNull() }.getOrNull()

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
        private const val CORE = "https://sports.core.api.espn.com/v2/sports/basketball/leagues/nba"

        /** ESPN sometimes uses PG/SG/SF/PF; the game only knows G, F and C. */
        fun position(abbr: String?): String = when (abbr?.uppercase()) {
            "G", "PG", "SG" -> "G"
            "C" -> "C"
            else -> "F"
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
