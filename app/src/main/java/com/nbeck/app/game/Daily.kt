package com.nbeck.app.game

import com.nbeck.app.data.Player
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.random.Random

/**
 * Same secret player for everyone on the same date, with no server.
 *
 * Rendezvous hashing: every player gets a score hash(seed, date, id) and the highest wins. Unlike
 * "index = hash % size", a friend whose roster cache is a day older (a trade, a new signing) still
 * gets the same answer unless the winner itself is missing from their list.
 */
object Daily {
    // Kept from the app's first name so the daily player sequence did not change with the rename.
    private const val SEED = "NBECK-v1"

    fun dateKey(date: LocalDate): String = date.toString()

    fun pick(players: List<Player>, date: LocalDate): Player =
        players.maxBy { score("$SEED|${dateKey(date)}|${it.id}") }

    fun untilNext(now: LocalDateTime): Duration =
        Duration.between(now, now.toLocalDate().plusDays(1).atStartOfDay())

    fun randomPick(players: List<Player>, avoid: Set<String>, random: Random = Random.Default): Player {
        val pool = players.filter { it.id !in avoid }.ifEmpty { players }
        return pool[random.nextInt(pool.size)]
    }

    /** FNV-1a 64 + splitmix finalizer — stable across devices and Android versions. */
    fun score(s: String): Long {
        var h = -0x340d631b7bdddcdbL
        for (b in s.toByteArray(Charsets.UTF_8)) {
            h = h xor (b.toLong() and 0xff)
            h *= 0x100000001b3L
        }
        h = (h xor (h ushr 30)) * -0x40a7b892e31b1a47L
        h = (h xor (h ushr 27)) * -0x6b2fb644ecceee15L
        return h xor (h ushr 31)
    }
}
