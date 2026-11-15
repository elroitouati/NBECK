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
                    assertNotEquals("${cat.name}: ${a.name} vs ${b.name}", cat.value(a), cat.value(b))
                }
                assertTrue(g.players.all { cat.value(it) != null })
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
            val wrong = if (g.value(bottom) > g.value(g.top)) HlChoice.LOWER else HlChoice.HIGHER
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
            val right = if (g.value(g.bottom) > g.value(g.top)) HlChoice.HIGHER else HlChoice.LOWER
            g.answer(if (it % 4 == 0) (if (right == HlChoice.HIGHER) HlChoice.LOWER else HlChoice.HIGHER) else right)
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

    @Test fun percentagesNeedAttempts() {
        // ESPN shows 0.0% with no attempts; those are stored as missing, so they never appear.
        assertTrue(players.filter { it.tpPct != null }.size < players.filter { it.ppg != null }.size)
        assertEquals("48.9%", HlCategory.FG_PCT.format(48.9))
        assertEquals("25.0", HlCategory.PPG.format(25.0))
    }
}
