package com.nbeck.app

import com.nbeck.app.data.PlayerBundle
import com.nbeck.app.game.HL_ROUNDS
import com.nbeck.app.game.HigherLowerGame
import com.nbeck.app.game.HlCategory
import com.nbeck.app.game.HlChoice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class HigherLowerTest {
    private val players = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
        .decodeFromString<PlayerBundle>(java.io.File("src/main/assets/players.json").readText()).players

    /** Rules 1, 2, 4, 5: 21 players -> 20 comparisons, one category, no repeats, no equal neighbours. */
    @Test fun everyCategoryBuildsAValidGame() {
        for (cat in HlCategory.entries) {
            repeat(20) { seed ->
                val g = HigherLowerGame.create(players, Random(seed * 31 + cat.ordinal), previousCategory = null)
                    .takeIf { it.category == cat } ?: return@repeat
                assertEquals(HL_ROUNDS + 1, g.players.size)
                assertEquals(HL_ROUNDS + 1, g.players.map { it.id }.toSet().size)
                g.players.zipWithNext().forEach { (a, b) ->
                    // Equal only when the top is at 100% (then the choice is LOWER / EQUAL).
                    if (!cat.isCapped(cat.value(a)!!)) assertNotEquals("${cat.name}: ${a.name} vs ${b.name}", cat.value(a), cat.value(b))
                }
                assertTrue(g.players.all { cat.eligible(it) })
                assertTrue(g.players.none { cat.value(it) == 0.0 })
                cat.min?.let { m -> assertTrue(g.players.all { cat.value(it)!! >= m }) }
            }
        }
    }

    @Test fun allNineCategoriesCanComeUp() {
        val seen = (0 until 400).map { HigherLowerGame.create(players, Random(it)).category }.toSet()
        assertEquals(HlCategory.entries.toSet(), seen)
    }

    /** Rules 7, 8, 1: wrong answers don't stop the game, the bottom becomes the top, exactly 20 rounds. */
    @Test fun twentyRoundsWrongAnswersContinueBottomMovesUp() {
        val g = HigherLowerGame.create(players, Random(7))
        var expectedTop = g.players[0]
        repeat(HL_ROUNDS) { i ->
            assertEquals(expectedTop.id, g.top.id)
            val bottom = g.bottom
            val wrong = wrongChoice(g)
            val r = g.answer(wrong)!!
            assertFalse(r.correct)
            assertEquals(i + 1, r.number)
            assertEquals(g.category, r.category)
            expectedTop = bottom
        }
        assertTrue(g.finished)
        assertEquals(0, g.score)
        assertNull(g.answer(HlChoice.HIGHER))           // no 21st round
        assertEquals(HL_ROUNDS, g.rounds.size)
    }

    /** Scoring + the summary holds exactly the values that were compared. */
    @Test fun scoringAndSummaryValues() {
        val g = HigherLowerGame.create(players, Random(3))
        repeat(HL_ROUNDS) {
            g.answer(if (it % 4 == 0) wrongChoice(g) else rightChoice(g))
        }
        assertEquals(15, g.score)
        g.rounds.forEach { r ->
            assertEquals(g.category.value(r.top), r.topValue)
            assertEquals(g.category.value(r.bottom), r.bottomValue)
        }
    }

    /** Rule 3 / new game: a different category, and none of the previous game's players when avoidable. */
    @Test fun newGameNewCategoryAndFreshPlayers() {
        val first = HigherLowerGame.create(players, Random(1))
        val second = HigherLowerGame.create(players, Random(2), previousCategory = first.category, avoid = first.players.map { it.id }.toSet())
        assertNotEquals(first.category, second.category)
        assertTrue((first.players.map { it.id } intersect second.players.map { it.id }.toSet()).isEmpty())
    }

    private fun rightChoice(g: HigherLowerGame): HlChoice {
        val t = g.value(g.top); val b = g.value(g.bottom)
        return when { b > t -> HlChoice.HIGHER; b < t -> HlChoice.LOWER; else -> HlChoice.EQUAL }
    }
    private fun wrongChoice(g: HigherLowerGame): HlChoice = when (rightChoice(g)) {
        HlChoice.HIGHER -> HlChoice.LOWER
        HlChoice.LOWER -> if (g.capped) HlChoice.EQUAL else HlChoice.HIGHER
        HlChoice.EQUAL -> HlChoice.LOWER
    }

    /** A 100% top: the answer is LOWER or EQUAL, and EQUAL is right when the bottom is 100% too. */
    @Test fun hundredPercentTop() {
        val top = player(id = "t").copy(ftPct = 100.0)
        val same = player(id = "s").copy(ftPct = 100.0)
        val lower = player(id = "l").copy(ftPct = 85.0)
        val g = HigherLowerGame.of(HlCategory.FT_PCT, listOf(top, same, lower) + List(19) { player(id = "x$it").copy(ftPct = 70.0 + it) })
        assertTrue(g.capped)
        assertTrue(g.answer(HlChoice.EQUAL)!!.correct)       // 100 vs 100
        assertTrue(g.capped)                                  // the 100% bottom moved up
        assertTrue(g.answer(HlChoice.LOWER)!!.correct)       // 100 vs 85
        assertFalse(g.capped)
    }

    @Test fun thresholdsKeepItRelevant() {
        val blockers = players.filter { HlCategory.BPG.eligible(it) }
        assertTrue(blockers.size >= HL_ROUNDS + 1)
        assertTrue(blockers.all { it.bpg!! >= 0.8 })
        assertFalse(HlCategory.BPG.eligible(player().copy(bpg = 0.0)))
        assertFalse(HlCategory.THREE_PCT.eligible(player().copy(tpPct = 0.0)))
        assertTrue(HlCategory.THREE_PCT.eligible(player().copy(tpPct = 12.5)))   // % categories: everyone with attempts
    }

    @Test fun percentagesNeedAttempts() {
        // ESPN shows 0.0% with no attempts; those are stored as missing, so they never appear.
        assertTrue(players.filter { it.tpPct != null }.size < players.filter { it.ppg != null }.size)
        assertEquals("48.9%", HlCategory.FG_PCT.format(48.9))
        assertEquals("25.0", HlCategory.PPG.format(25.0))
    }
}
