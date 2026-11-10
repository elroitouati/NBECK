package com.nbeck.app.game

import com.nbeck.app.data.Player
import com.nbeck.app.data.Teams
import kotlinx.serialization.Serializable
import kotlin.random.Random

const val CHAIN_ATTEMPTS = 5

/** One shared team-season between two players: the reason they're linked. */
data class Link(val season: Int, val teamId: Int) {
    /** "2025-26" */
    val seasonLabel get() = "${season - 1}-${(season % 100).toString().padStart(2, '0')}"
    val team get() = Teams.find(teamId)
}

/**
 * Players Chain: connect two players through teammates. Two players are teammates when they were on
 * the same team in the same season (from each player's season-by-season team history).
 */
class TeammateGraph(players: List<Player>) {
    val byId: Map<String, Player> = players.associateBy { it.id }
    private val stints: Map<String, Set<Pair<Int, Int>>> = players.associate { p ->
        p.id to p.stints.mapNotNull { s ->
            val (y, t) = s.split(':').takeIf { it.size == 2 } ?: return@mapNotNull null
            (y.toIntOrNull() ?: return@mapNotNull null) to (t.toIntOrNull() ?: return@mapNotNull null)
        }.toSet()
    }
    private val adjacency: Map<String, Set<String>> = run {
        val rosters = HashMap<Pair<Int, Int>, MutableSet<String>>()
        stints.forEach { (id, ss) -> ss.forEach { rosters.getOrPut(it) { mutableSetOf() } += id } }
        val adj = HashMap<String, MutableSet<String>>()
        rosters.values.forEach { ids -> ids.forEach { a -> adj.getOrPut(a) { mutableSetOf() } += ids - a } }
        players.associate { it.id to (adj[it.id] ?: emptySet<String>()) }
    }

    fun teammates(id: String): Set<String> = adjacency[id].orEmpty()
    fun areTeammates(a: String, b: String) = b in teammates(a)

    /** Most recent team-season the two shared, or null. */
    fun link(a: String, b: String): Link? =
        (stints[a].orEmpty() intersect stints[b].orEmpty()).maxWithOrNull(compareBy({ it.first }, { it.second }))
            ?.let { Link(it.first, it.second) }

    /** Shortest chain from [from] to [to], both included (BFS), or null when not connected. */
    fun shortestPath(from: String, to: String): List<String>? {
        if (from == to) return listOf(from)
        val prev = HashMap<String, String>()
        val queue = ArrayDeque(listOf(from))
        val seen = hashSetOf(from)
        while (queue.isNotEmpty()) {
            val x = queue.removeFirst()
            for (y in teammates(x)) {
                if (!seen.add(y)) continue
                prev[y] = x
                if (y == to) return generateSequence(to) { prev[it] }.toList().reversed()
                queue.addLast(y)
            }
        }
        return null
    }

    private fun distancesFrom(from: String): Map<String, Int> {
        val dist = hashMapOf(from to 0)
        val queue = ArrayDeque(listOf(from))
        while (queue.isNotEmpty()) {
            val x = queue.removeFirst()
            for (y in teammates(x)) if (y !in dist) { dist[y] = dist.getValue(x) + 1; queue.addLast(y) }
        }
        return dist
    }

    /**
     * A new round: two players 2 or 3 links apart (1 or 2 players needed in between), so it is
     * solvable within [CHAIN_ATTEMPTS]. Endpoints prefer players with some history (2+ seasons).
     */
    fun newPuzzle(random: Random = Random.Default, avoid: Set<String> = emptySet()): ChainPuzzle {
        val pool = byId.values.filter { stints[it.id].orEmpty().size >= 2 && teammates(it.id).isNotEmpty() && it.id !in avoid }
            .ifEmpty { byId.values.filter { teammates(it.id).isNotEmpty() } }
        repeat(50) {
            val a = pool[random.nextInt(pool.size)]
            val dist = distancesFrom(a.id)
            val wanted = if (random.nextFloat() < 0.4f) 2 else 3
            val candidates = pool.filter { dist[it.id] == wanted }.ifEmpty { pool.filter { dist[it.id] in 2..3 } }
            if (candidates.isNotEmpty()) {
                val b = candidates[random.nextInt(candidates.size)]
                return ChainPuzzle(a.id, b.id, dist.getValue(b.id) - 1)
            }
        }
        error("no puzzle found")
    }
}

/** [minBetween] = fewest players needed between start and goal. */
@Serializable
data class ChainPuzzle(val startId: String, val goalId: String, val minBetween: Int)

enum class ChainMove { LINKED, LINKED_AND_DONE, NOT_TEAMMATES, ALREADY_USED }

/** A round in progress. [chain] starts with the start player; the goal joins when the last link reaches it. */
@Serializable
data class ChainRound(
    val puzzle: ChainPuzzle,
    val chain: List<String> = listOf(puzzle.startId),
    val misses: List<String> = emptyList(),
    val attempts: Int = 0,
    val won: Boolean = false,
    val lost: Boolean = false,
) {
    val finished get() = won || lost
    val attemptsLeft get() = CHAIN_ATTEMPTS - attempts
    /** Players between start and goal in a finished chain. */
    val between get() = (chain.size - 1).coerceAtLeast(0)

    /** Every guess costs an attempt; a teammate of the chain's last player extends it. */
    fun guess(id: String, g: TeammateGraph): Pair<ChainRound, ChainMove> {
        if (finished || id in chain) return this to ChainMove.ALREADY_USED
        val used = attempts + 1
        val last = chain.last()
        if (!g.areTeammates(last, id)) {
            val next = copy(attempts = used, misses = misses + id, lost = used >= CHAIN_ATTEMPTS)
            return next to ChainMove.NOT_TEAMMATES
        }
        if (id == puzzle.goalId) return copy(attempts = used, won = true) to ChainMove.LINKED_AND_DONE
        val extended = chain + id
        // The goal joins by itself once the new player was its teammate.
        if (g.areTeammates(id, puzzle.goalId)) return copy(chain = extended, attempts = used, won = true) to ChainMove.LINKED_AND_DONE
        return copy(chain = extended, attempts = used, lost = used >= CHAIN_ATTEMPTS) to ChainMove.LINKED
    }

    /** Drop the last link (free) — for when a path turns out to be a dead end. */
    fun undo(): ChainRound = if (finished || chain.size <= 1) this else copy(chain = chain.dropLast(1))
}

@Serializable
data class ChainStats(
    val played: Int = 0,
    val wins: Int = 0,
    val currentStreak: Int = 0,
    val maxStreak: Int = 0,
    /** Wins with the fewest possible players in between and no misses. */
    val perfect: Int = 0,
) {
    val winPct get() = if (played == 0) 0 else Math.round(wins * 100f / played)

    fun record(r: ChainRound): ChainStats {
        val streak = if (r.won) currentStreak + 1 else 0
        val isPerfect = r.won && r.misses.isEmpty() && r.between <= r.puzzle.minBetween
        return copy(
            played = played + 1, wins = wins + if (r.won) 1 else 0,
            currentStreak = streak, maxStreak = maxOf(maxStreak, streak),
            perfect = perfect + if (isPerfect) 1 else 0,
        )
    }
}
