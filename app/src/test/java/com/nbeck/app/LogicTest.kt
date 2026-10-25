package com.nbeck.app

import com.nbeck.app.data.EspnSource
import com.nbeck.app.data.Teams
import com.nbeck.app.game.Daily
import com.nbeck.app.game.DivisionBoard
import com.nbeck.app.game.Engine
import com.nbeck.app.game.Mode
import com.nbeck.app.game.ModeStats
import com.nbeck.app.game.PlayerSearch
import com.nbeck.app.game.Share
import com.nbeck.app.game.TeamState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class SearchTest {
    private val players = listOf(
        player("1", "Nikola Jokic", ppg = 26.0),
        player("2", "Stephen Curry", ppg = 25.0),
        player("3", "Seth Curry", ppg = 6.0),
        player("4", "Luka Dončić", ppg = 28.0),
        player("5", "De'Aaron Fox", ppg = 22.0),
        player("6", "Giannis Antetokounmpo", ppg = 30.0),
    )
    private val search = PlayerSearch(players)
    private fun names(q: String, ex: Set<String> = emptySet()) = search.search(q, ex).map { it.name }

    @Test fun oneLetter_matchesFirstAndLastNames() {
        val r = names("s")
        assertTrue("Stephen Curry" in r && "Seth Curry" in r)
    }
    @Test fun lastName_prefix() = assertEquals(listOf("Stephen Curry", "Seth Curry"), names("cur"))
    @Test fun caseInsensitive() = assertEquals("Nikola Jokic", names("JOK").first())
    @Test fun accentsIgnored() = assertEquals("Luka Dončić", names("doncic").first())
    @Test fun punctuationIgnored() = assertEquals("De'Aaron Fox", names("deaaron").first())
    @Test fun typo_substitution() = assertEquals("Nikola Jokic", names("jokuc").first())
    @Test fun typo_transposition() = assertEquals("Giannis Antetokounmpo", names("antetoknoumpo").first())
    @Test fun fullName_withSpace() = assertEquals("Stephen Curry", names("steph cur").first())
    @Test fun alreadyGuessed_excluded() = assertEquals(listOf("Seth Curry"), names("curry", setOf("2")))
    @Test fun nonsense_noMatch() = assertTrue(names("zzzzq").isEmpty())
    @Test fun emptyQuery_noMatch() = assertTrue(names("  ").isEmpty())
}

class DailyTest {
    private val players = (1..500).map { player(it.toString()) }

    @Test fun sameDate_samePlayer() {
        val d = LocalDate.of(2026, 10, 24)
        assertEquals(Daily.pick(players, d).id, Daily.pick(players.shuffled(), d).id)
    }
    @Test fun differentDates_vary() {
        val ids = (0 until 30).map { Daily.pick(players, LocalDate.of(2026, 11, 1).plusDays(it.toLong())).id }.toSet()
        assertTrue(ids.size > 20)
    }
    @Test fun robustToSmallRosterChanges() {
        val d = LocalDate.of(2026, 12, 25)
        val winner = Daily.pick(players, d)
        val changed = players.filter { it.id != "17" && it.id != winner.id }.plus(winner) + player("9999")
        val sameUnlessNewcomerWins = Daily.pick(changed, d).id
        assertTrue(sameUnlessNewcomerWins == winner.id || sameUnlessNewcomerWins == "9999")
    }
    @Test fun countdownToMidnight() {
        assertEquals(3600L, Daily.untilNext(LocalDateTime.of(2026, 10, 24, 23, 0)).seconds)
    }
}

class DivisionBoardTest {
    private val day = LocalDate.of(2026, 10, 24)
    private fun states(guess: Int, answer: Int) =
        DivisionBoard.states(listOf(player(team = guess) to Engine.grade(player(team = guess), player(id = "2", team = answer), day)))

    @Test fun grayDivision_dimsGuessConference() {
        val s = states(guess = 13, answer = 2) // LAL guess, BOS answer
        Teams.all.forEach { t -> assertEquals(t.abbr, if (t.conference.name == "WEST") TeamState.DIM else TeamState.NORMAL, s[t.espnId]) }
    }
    @Test fun yellowDivision_dimsGuessDivisionAndOtherConference() {
        val s = states(guess = 4, answer = 2) // CHI (Central) guess, BOS (Atlantic) answer
        Teams.all.forEach { t ->
            val expectDim = t.division.name == "CENTRAL" || t.conference.name == "WEST"
            assertEquals(t.abbr, if (expectDim) TeamState.DIM else TeamState.NORMAL, s[t.espnId])
        }
    }
    @Test fun greenDivision_dimsAllOtherDivisions_andGuessTeamIfGray() {
        val s = states(guess = 18, answer = 2) // NYK guess, BOS answer
        Teams.all.forEach { t ->
            val expect = when {
                t.abbr == "NYK" -> TeamState.DIM
                t.division.name == "ATLANTIC" -> TeamState.NORMAL
                else -> TeamState.DIM
            }
            assertEquals(t.abbr, expect, s[t.espnId])
        }
    }
    @Test fun greenTeam_highlighted() {
        val s = states(guess = 2, answer = 2)
        assertEquals(TeamState.HIGHLIGHT, s[2])
        assertEquals(TeamState.NORMAL, s[18])
        assertEquals(TeamState.DIM, s[4])
    }
}

class StatsTest {
    private val d = LocalDate.of(2026, 10, 24)

    @Test fun dailyStreak_continuesOnConsecutiveDays() {
        var s = ModeStats()
        s = s.record(Mode.DAILY, true, 3, d)
        s = s.record(Mode.DAILY, true, 5, d.plusDays(1))
        assertEquals(2, s.currentStreak); assertEquals(2, s.maxStreak)
        assertEquals(listOf(0, 0, 1, 0, 1, 0, 0, 0), s.dist)
        assertEquals(4f, s.avgGuesses)
    }
    @Test fun dailyStreak_resetsAfterSkippedDay() {
        var s = ModeStats().record(Mode.DAILY, true, 3, d)
        assertEquals(0, s.liveStreak(Mode.DAILY, d.plusDays(2)))
        s = s.record(Mode.DAILY, true, 3, d.plusDays(2))
        assertEquals(1, s.currentStreak)
    }
    @Test fun loss_resetsStreak_countsPlayed() {
        val s = ModeStats().record(Mode.FREE, true, 2, d).record(Mode.FREE, false, 8, d)
        assertEquals(2, s.played); assertEquals(1, s.wins); assertEquals(0, s.currentStreak); assertEquals(50, s.winPct)
    }
}

class ShareTest {
    @Test fun format() {
        val day = LocalDate.of(2026, 10, 24)
        val a = player(id = "a", team = 2, pos = "F", jersey = "0", country = "USA")
        val g = player(id = "g", team = 2, pos = "F", jersey = "0", country = "USA", born = "1990-01-01")
        val txt = Share.text("יומי", day, listOf(Engine.grade(g, a, day), Engine.grade(a, a, day)), won = true)
        assertEquals("DNBA יומי 24.10 — 2/8\n🟩🟩🟩⬜🟩🟩🟩🟩\n🟩🟩🟩🟩🟩🟩🟩🟩", txt)
    }
}

class EspnPositionTest {
    @Test fun onlyGfc() {
        assertEquals("G", EspnSource.position("PG")); assertEquals("G", EspnSource.position("SG"))
        assertEquals("F", EspnSource.position("SF")); assertEquals("F", EspnSource.position("PF"))
        assertEquals("C", EspnSource.position("C")); assertEquals("F", EspnSource.position(null))
    }
}
