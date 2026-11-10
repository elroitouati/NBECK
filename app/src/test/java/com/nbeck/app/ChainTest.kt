package com.nbeck.app

import com.nbeck.app.game.CHAIN_ATTEMPTS
import com.nbeck.app.game.ChainMove
import com.nbeck.app.game.ChainPuzzle
import com.nbeck.app.game.ChainRound
import com.nbeck.app.game.ChainStats
import com.nbeck.app.game.TeammateGraph
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ChainTest {
    // a—b on BOS 2020, b—c on LAL 2022, c—d on NYK 2024; x shares nothing with anyone.
    private fun p(id: String, vararg stints: String) = player(id = id, name = "Player $id").copy(stints = stints.toList())
    private val g = TeammateGraph(listOf(
        p("a", "2020:2"),
        p("b", "2020:2", "2022:13"),
        p("c", "2022:13", "2024:18"),
        p("d", "2024:18"),
        p("x", "2019:9"),
    ))

    @Test fun teammatesNeedSameTeamAndSeason() {
        assertTrue(g.areTeammates("a", "b"))
        assertFalse(g.areTeammates("a", "c"))
        assertFalse(g.areTeammates("b", "d"))           // same franchise ids don't matter across seasons
        assertEquals(13, g.link("b", "c")!!.teamId)
        assertEquals("2021-22", g.link("b", "c")!!.seasonLabel)
        assertNull(g.link("a", "x"))
    }

    @Test fun shortestPath() {
        assertEquals(listOf("a", "b", "c", "d"), g.shortestPath("a", "d"))
        assertNull(g.shortestPath("a", "x"))
    }

    @Test fun winByReachingGoalsTeammate() {
        var r = ChainRound(ChainPuzzle("a", "d", 2))
        var m: ChainMove
        r.guess("b", g).let { (n, mv) -> r = n; m = mv }
        assertEquals(ChainMove.LINKED, m)
        r.guess("c", g).let { (n, mv) -> r = n; m = mv }   // c played with d -> d joins by itself
        assertEquals(ChainMove.LINKED_AND_DONE, m)
        assertTrue(r.won); assertEquals(2, r.attempts); assertEquals(2, r.between)
    }

    @Test fun missCostsAnAttemptAndFiveMissesLose() {
        var r = ChainRound(ChainPuzzle("a", "d", 2))
        val (n, m) = r.guess("c", g)                          // c never played with a
        assertEquals(ChainMove.NOT_TEAMMATES, m); assertEquals(1, n.attempts); assertEquals(listOf("c"), n.misses)
        r = n
        repeat(CHAIN_ATTEMPTS - 1) { r = r.guess("x", g).first }
        assertTrue(r.lost)
        assertEquals(ChainMove.ALREADY_USED, r.guess("b", g).second)   // finished
    }

    @Test fun repeatsAreIgnored() {
        val r = ChainRound(ChainPuzzle("a", "d", 2)).guess("b", g).first
        assertEquals(ChainMove.ALREADY_USED, r.guess("b", g).second)
        assertEquals(1, r.attempts)
    }

    @Test fun giveUpLoses() {
        val r = ChainRound(ChainPuzzle("a", "d", 2)).guess("b", g).first.giveUp()
        assertTrue(r.lost); assertTrue(r.gaveUp); assertFalse(r.won)
        assertEquals(ChainMove.ALREADY_USED, r.guess("c", g).second)
        assertEquals(0, ChainStats(currentStreak = 3).record(r).currentStreak)
    }

    @Test fun guessingTheGoalDirectlyWorksWhenLinked() {
        val r = ChainRound(ChainPuzzle("a", "c", 1)).guess("b", g).first   // b-c teammates -> won
        assertTrue(r.won)
    }

    @Test fun statsAndPerfect() {
        val won = ChainRound(ChainPuzzle("a", "d", 2)).guess("b", g).first.guess("c", g).first
        val s = ChainStats().record(won)
        assertEquals(1, s.wins); assertEquals(1, s.perfect); assertEquals(1, s.currentStreak)
        val lost = (1..CHAIN_ATTEMPTS).fold(ChainRound(ChainPuzzle("a", "d", 2))) { r, _ -> r.guess("x", g).first }
        assertEquals(0, s.record(lost).currentStreak)
    }

    @Test fun puzzlesFromRealDataAreSolvable() {
        val players = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
            .decodeFromString<com.nbeck.app.data.PlayerBundle>(java.io.File("src/main/assets/players.json").readText()).players
        val real = TeammateGraph(players)
        val rnd = Random(42)
        repeat(30) {
            val pz = real.newPuzzle(rnd)
            val path = real.shortestPath(pz.startId, pz.goalId)!!
            assertEquals(pz.minBetween, path.size - 2)
            assertTrue(pz.minBetween in 1..2)
            assertFalse(real.areTeammates(pz.startId, pz.goalId))
        }
    }
}
