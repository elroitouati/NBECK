package com.nbeck.app.game

import com.nbeck.app.data.Player
import java.text.Normalizer

/**
 * Autocomplete over first, last and full names. Case- and accent-insensitive; from 3 letters on it
 * also forgives small typos (edit distance 1, or 2 for queries of 6+ letters).
 */
class PlayerSearch(players: List<Player>) {

    private class Entry(val player: Player, val full: String, val tokens: List<String>)

    private val entries = players.map { p ->
        val full = normalize(p.name)
        Entry(p, full, full.split(' ').filter { it.isNotEmpty() })
    }

    fun search(query: String, exclude: Set<String> = emptySet(), limit: Int = 8): List<Player> {
        val q = normalize(query)
        if (q.isEmpty()) return emptyList()
        val qTokens = q.split(' ').filter { it.isNotEmpty() }
        return entries.asSequence()
            .filter { it.player.id !in exclude }
            .mapNotNull { e -> score(e, q, qTokens)?.let { e to it } }
            .sortedWith(compareBy<Pair<Entry, Int>> { it.second }
                .thenByDescending { it.first.player.ppg ?: -1.0 }
                .thenBy { it.first.full })
            .take(limit)
            .map { it.first.player }
            .toList()
    }

    /** Lower is better; null = no match. */
    private fun score(e: Entry, q: String, qTokens: List<String>): Int? {
        if (e.full.startsWith(q)) return 0
        if (e.tokens.any { it.startsWith(q) }) return 1
        if (qTokens.size > 1 && matchesInOrder(e.tokens, qTokens, fuzzy = false)) return 1
        if (q.length < 3) return null
        val allowed = if (q.length >= 6) 2 else 1
        var best = Int.MAX_VALUE
        for (t in e.tokens) best = minOf(best, prefixDistance(q, t))
        best = minOf(best, prefixDistance(q, e.full))
        if (qTokens.size > 1 && matchesInOrder(e.tokens, qTokens, fuzzy = true)) best = minOf(best, 1)
        return if (best <= allowed) 2 + best else null
    }

    private fun matchesInOrder(tokens: List<String>, q: List<String>, fuzzy: Boolean): Boolean {
        var i = 0
        for (t in tokens) {
            if (i == q.size) break
            val part = q[i]
            val ok = t.startsWith(part) || (fuzzy && part.length >= 3 && prefixDistance(part, t) <= 1)
            if (ok) i++
        }
        return i == q.size
    }

    companion object {
        fun normalize(s: String): String =
            Normalizer.normalize(s, Normalizer.Form.NFD)
                .replace(Regex("\\p{M}+"), "")
                .lowercase()
                .replace(Regex("[^a-z0-9 ]"), "")
                .replace(Regex("\\s+"), " ")
                .trim()

        /** Edit distance between [q] and the best-matching prefix of [t] (lengths q±1). */
        fun prefixDistance(q: String, t: String): Int {
            var best = Int.MAX_VALUE
            for (len in (q.length - 1)..(q.length + 1)) {
                if (len < 1 || len > t.length) continue
                best = minOf(best, levenshtein(q, t.substring(0, len)))
            }
            return best
        }

        /** Optimal-string-alignment distance: insert, delete, substitute, swap adjacent letters. */
        fun levenshtein(a: String, b: String): Int {
            val d = Array(a.length + 1) { IntArray(b.length + 1) }
            for (i in 0..a.length) d[i][0] = i
            for (j in 0..b.length) d[0][j] = j
            for (i in 1..a.length) for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                var v = minOf(d[i - 1][j] + 1, d[i][j - 1] + 1, d[i - 1][j - 1] + cost)
                if (i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1]) v = minOf(v, d[i - 2][j - 2] + 1)
                d[i][j] = v
            }
            return d[a.length][b.length]
        }
    }
}
