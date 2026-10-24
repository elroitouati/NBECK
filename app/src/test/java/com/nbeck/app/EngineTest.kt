package com.nbeck.app

import com.nbeck.app.game.Arrow
import com.nbeck.app.game.Engine
import com.nbeck.app.game.Mark
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** One test per rule in the spec table (section 4). */
class EngineTest {
    private val day = LocalDate.of(2026, 10, 24)

    // ---- Team: green same team, no yellow, gray otherwise
    @Test fun team_same_isGreen() = assertEquals(Mark.GREEN, Engine.team(player(team = 2), player(team = 2)).mark)
    @Test fun team_sameDivisionOtherTeam_isGray_noYellow() = assertEquals(Mark.GRAY, Engine.team(player(team = 18), player(team = 2)).mark)
    @Test fun team_other_isGray() = assertEquals(Mark.GRAY, Engine.team(player(team = 13), player(team = 2)).mark)

    // ---- Division: green same, yellow other division same conference, gray other conference
    @Test fun division_same_isGreen() = assertEquals(Mark.GREEN, Engine.division(player(team = 18), player(team = 2)).mark)
    @Test fun division_sameConference_isYellow() = assertEquals(Mark.YELLOW, Engine.division(player(team = 4), player(team = 2)).mark)
    @Test fun division_otherConference_isGray() = assertEquals(Mark.GRAY, Engine.division(player(team = 13), player(team = 2)).mark)
    @Test fun division_west_sameDivision_isGreen() = assertEquals(Mark.GREEN, Engine.division(player(team = 9), player(team = 13)).mark)

    // ---- Position: only G, F, C — same green, different gray
    @Test fun position_same_isGreen() = assertEquals(Mark.GREEN, Engine.position("C", "C").mark)
    @Test fun position_different_isGray() = assertEquals(Mark.GRAY, Engine.position("G", "F").mark)
    @Test fun position_neverYellow() {
        for (g in listOf("G", "F", "C")) for (a in listOf("G", "F", "C"))
            assertTrue(Engine.position(g, a).mark != Mark.YELLOW)
    }

    // ---- Age: equal green, within 3 yellow, more gray; arrow toward answer when not green
    @Test fun age_equal_isGreen_noArrow() {
        val c = Engine.grade(player(born = "2000-05-01"), player(id = "2", born = "2000-02-01"), day).age
        assertEquals(Mark.GREEN, c.mark); assertEquals(Arrow.NONE, c.arrow)
    }
    @Test fun age_answerOlderBy3_isYellow_up() {
        val c = Engine.grade(player(born = "2000-01-01"), player(id = "2", born = "1997-01-01"), day).age
        assertEquals(Mark.YELLOW, c.mark); assertEquals(Arrow.UP, c.arrow)
    }
    @Test fun age_answerYoungerBy1_isYellow_down() {
        val c = Engine.grade(player(born = "2000-01-01"), player(id = "2", born = "2001-01-01"), day).age
        assertEquals(Mark.YELLOW, c.mark); assertEquals(Arrow.DOWN, c.arrow)
    }
    @Test fun age_diff4_isGray_withArrow() {
        val up = Engine.grade(player(born = "2000-01-01"), player(id = "2", born = "1996-01-01"), day).age
        assertEquals(Mark.GRAY, up.mark); assertEquals(Arrow.UP, up.arrow)
        val down = Engine.grade(player(born = "1990-01-01"), player(id = "2", born = "2004-01-01"), day).age
        assertEquals(Mark.GRAY, down.mark); assertEquals(Arrow.DOWN, down.arrow)
    }
    @Test fun age_isComputedOnGameDay() {
        val p = player(born = "2000-10-25")
        assertEquals(25, p.ageOn(day))                    // birthday is tomorrow
        assertEquals(26, p.ageOn(day.plusDays(1)))
    }

    // ---- Number: equal green, within 3 yellow, more gray; arrows; missing = gray
    @Test fun number_equal_isGreen() = assertEquals(Mark.GREEN, Engine.jersey("23", "23").mark)
    @Test fun number_within3_isYellow_up() {
        val c = Engine.jersey("20", "23"); assertEquals(Mark.YELLOW, c.mark); assertEquals(Arrow.UP, c.arrow)
    }
    @Test fun number_within3_isYellow_down() {
        val c = Engine.jersey("8", "7"); assertEquals(Mark.YELLOW, c.mark); assertEquals(Arrow.DOWN, c.arrow)
    }
    @Test fun number_diff4_isGray_withArrow() {
        val c = Engine.jersey("1", "5"); assertEquals(Mark.GRAY, c.mark); assertEquals(Arrow.UP, c.arrow)
        val d = Engine.jersey("77", "0"); assertEquals(Mark.GRAY, d.mark); assertEquals(Arrow.DOWN, d.arrow)
    }
    @Test fun number_missingGuess_isGray_noArrow() {
        val c = Engine.jersey(null, "5"); assertEquals(Mark.GRAY, c.mark); assertEquals(Arrow.NONE, c.arrow)
    }
    @Test fun number_missingAnswer_isGray_noArrow() {
        val c = Engine.jersey("5", null); assertEquals(Mark.GRAY, c.mark); assertEquals(Arrow.NONE, c.arrow)
    }
    @Test fun number_bothMissing_isGray() = assertEquals(Mark.GRAY, Engine.jersey(null, null).mark)
    @Test fun number_zeroVsDoubleZero_isYellow() = assertEquals(Mark.YELLOW, Engine.jersey("0", "00").mark)

    // ---- Country: same green, no yellow, else gray
    @Test fun country_same_isGreen() = assertEquals(Mark.GREEN, Engine.country("France", "France").mark)
    @Test fun country_other_isGray() = assertEquals(Mark.GRAY, Engine.country("Canada", "USA").mark)
    @Test fun country_missing_isGray() = assertEquals(Mark.GRAY, Engine.country(null, null).mark)

    // ---- Draft year: equal green, within 3 yellow, more gray, arrows; UND only matches UND
    @Test fun draft_same_isGreen() = assertEquals(Mark.GREEN, Engine.draft(2020, 2020).mark)
    @Test fun draft_within3_isYellow_up() {
        val c = Engine.draft(2018, 2020); assertEquals(Mark.YELLOW, c.mark); assertEquals(Arrow.UP, c.arrow)
    }
    @Test fun draft_far_isGray_down() {
        val c = Engine.draft(2020, 2003); assertEquals(Mark.GRAY, c.mark); assertEquals(Arrow.DOWN, c.arrow)
    }
    @Test fun draft_bothUndrafted_isGreen() = assertEquals(Mark.GREEN, Engine.draft(null, null).mark)
    @Test fun draft_oneUndrafted_isGray_noArrow() {
        val c = Engine.draft(null, 2020); assertEquals(Mark.GRAY, c.mark); assertEquals(Arrow.NONE, c.arrow)
        assertEquals(Mark.GRAY, Engine.draft(2020, null).mark)
    }

    // ---- Ring: same status green, different gray
    @Test fun ring_bothChampions_isGreen() = assertEquals(Mark.GREEN, Engine.ring(true, true).mark)
    @Test fun ring_bothWithout_isGreen() = assertEquals(Mark.GREEN, Engine.ring(false, false).mark)
    @Test fun ring_different_isGray() {
        assertEquals(Mark.GRAY, Engine.ring(true, false).mark); assertEquals(Mark.GRAY, Engine.ring(false, true).mark)
    }

    // ---- Whole row
    @Test fun samePlayer_allGreen() {
        val p = player(team = 13, pos = "C", jersey = "3", country = "Slovenia").copy(draftYear = 2018, ring = true)
        assertTrue(Engine.grade(p, p, day).isWin)
    }
    @Test fun differentPlayer_notWin() {
        val r = Engine.grade(player(team = 13), player(id = "2", team = 2), day)
        assertFalse(r.isWin)
        assertEquals(8, r.cells.size)
    }
}
