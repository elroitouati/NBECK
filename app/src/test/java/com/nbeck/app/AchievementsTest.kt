package com.nbeck.app

import com.nbeck.app.game.Achievement
import com.nbeck.app.game.AchievementState
import com.nbeck.app.game.Achievements
import com.nbeck.app.game.Engine
import com.nbeck.app.game.FinishedGame
import com.nbeck.app.game.Mode
import com.nbeck.app.game.ModeStats
import com.nbeck.app.game.Stats
import com.nbeck.app.game.latestDraftYear
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class AchievementsTest {
    private val day = LocalDate.of(2026, 10, 27)
    private val noon = LocalDateTime.of(2026, 10, 27, 12, 0)

    private fun win(answer: com.nbeck.app.data.Player, guesses: Int, archive: Boolean = false, at: LocalDateTime = noon, mode: Mode = Mode.DAILY): List<Achievement> {
        val miss = player(id = "x", team = 13, pos = "C", born = "1980-01-01", jersey = "99", country = "Japan")
        val results = List(guesses - 1) { Engine.grade(miss, answer, day) } + Engine.grade(answer, answer, day)
        val g = FinishedGame(mode, true, results, answer, day, archive, at)
        return Achievements.evaluate(AchievementState(), Stats(daily = ModeStats(played = 1, wins = 1)), g, day).second
    }

    @Test fun secondTry() {
        assertTrue(Achievement.SECOND_TRY in win(player(), 2))
        assertFalse(Achievement.SECOND_TRY in win(player(), 3))
    }
    @Test fun blueWhite() = assertTrue(Achievement.BLUE_WHITE in win(player(country = "Israel"), 4))
    @Test fun ringHunter() = assertTrue(Achievement.RING_HUNTER in win(player().copy(ring = true), 4))
    @Test fun undrafted() {
        assertTrue(Achievement.UNDRAFTED in win(player().copy(draftYear = null), 4))
        assertFalse(Achievement.UNDRAFTED in win(player().copy(draftYear = 2019), 4))
    }
    @Test fun oldSchool() {
        assertTrue(Achievement.OLD_SCHOOL in win(player(born = "1988-03-14"), 4))
        assertFalse(Achievement.OLD_SCHOOL in win(player(born = "1995-03-14"), 4))
    }
    @Test fun rookieHunter() {
        assertEquals(2026, latestDraftYear(day))
        assertEquals(2025, latestDraftYear(LocalDate.of(2026, 3, 1)))
        assertTrue(Achievement.ROOKIE_HUNTER in win(player().copy(draftYear = 2026), 4))
        assertFalse(Achievement.ROOKIE_HUNTER in win(player().copy(draftYear = 2025), 4))
    }
    @Test fun nightOwl() {
        assertTrue(Achievement.NIGHT_OWL in win(player(), 4, at = LocalDateTime.of(2026, 10, 27, 1, 30)))
        assertFalse(Achievement.NIGHT_OWL in win(player(), 4))
    }
    @Test fun hotStart() {
        val answer = player(team = 2, pos = "F", born = "2000-01-01", jersey = "10", country = "USA")
        val close = player(id = "c", team = 2, pos = "F", born = "2000-06-01", jersey = "40", country = "Canada")
        val results = listOf(Engine.grade(close, answer, day), Engine.grade(answer, answer, day))
        val g = FinishedGame(Mode.FREE, true, results, answer, day, false, noon)
        assertTrue(Achievement.HOT_START in Achievements.evaluate(AchievementState(), Stats(), g, day).second)
    }
    @Test fun historianCountsArchiveGames() {
        var s = AchievementState()
        repeat(5) {
            val g = FinishedGame(Mode.DAILY, false, emptyList(), player(), day.minusDays(3), archive = true, endedAt = noon)
            val (n, earned) = Achievements.evaluate(s, Stats(), g, day)
            s = n
            assertEquals(it == 4, Achievement.HISTORIAN in earned)
        }
        assertEquals(5, s.archiveFinished)
    }
    @Test fun ambassadorCountsNonUsaWins() {
        var s = AchievementState()
        repeat(10) {
            val g = FinishedGame(Mode.FREE, true, listOf(Engine.grade(player(country = "France"), player(country = "France"), day)), player(country = "France"), day, false, noon)
            s = Achievements.evaluate(s, Stats(), g, day).first
        }
        assertEquals(10, s.internationalWins)
        assertTrue(s.isUnlocked(Achievement.INTERNATIONAL_10))
    }
    @Test fun freePlayStreakAndWins() {
        val stats = Stats(free = ModeStats(played = 30, wins = 25, maxStreak = 5))
        val earned = Achievements.evaluate(AchievementState(), stats, null, day).second
        assertTrue(Achievement.FREE_STREAK_5 in earned && Achievement.FREE_WINS_25 in earned)
    }
}

class BundledDataTest {
    private val assets = java.io.File("src/main/assets")
    private val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }

    @Test fun correctionsApplied() {
        val players = json.decodeFromString<com.nbeck.app.data.PlayerBundle>(assets.resolve("players.json").readText()).players.associateBy { it.name }
        assertEquals("1", players.getValue("Jamal Shead").jersey)
        assertEquals("Israel", players.getValue("Ben Saraf").country)
        assertEquals("2002-05-14", players.getValue("Zach Edey").birthDate)
        assertEquals("Montenegro", players.getValue("Nikola Vucevic").country)
        assertTrue("every player has a country", players.values.all { it.country != null })
    }
}

class ReleaseManifestTest {
    /** release/latest.json must describe this build, or installed apps won't hear about it. */
    @Test fun manifestMatchesThisVersion() {
        val f = java.io.File("../release/latest.json")
        val r = kotlinx.serialization.json.Json.decodeFromString(com.nbeck.app.data.LatestRelease.serializer(), f.readText())
        assertEquals(BuildConfig.VERSION_CODE, r.versionCode)
        assertEquals(BuildConfig.VERSION_NAME, r.versionName)
        assertEquals(com.nbeck.app.data.Changelog.releases.first().code, r.versionCode)
        assertTrue(r.notesHe.isNotEmpty() && r.notesEn.isNotEmpty())
    }
}

class OnlineRulesTest {
    @Test fun leaguePoints() {
        assertEquals(10, com.nbeck.app.data.Online.leaguePoints(true, 1))
        assertEquals(3, com.nbeck.app.data.Online.leaguePoints(true, 8))
        assertEquals(0, com.nbeck.app.data.Online.leaguePoints(false, 8))
    }
    @Test fun dailyRankOrder() {
        val r = com.nbeck.app.data.Online::rankKey
        assertTrue(r(true, 3, 500) < r(true, 4, 10))   // fewer guesses beats faster
        assertTrue(r(true, 3, 40) < r(true, 3, 90))    // then time
        assertTrue(r(true, 8, 999) < r(false, 1, 1))   // any win beats a loss
        assertEquals(3_000_095L, r(true, 3, 95))       // matches firestore.rules
    }
    @Test fun nicknames() {
        val ok = com.nbeck.app.data.Online::isValidNickname
        assertTrue(ok("אלרואי")); assertTrue(ok("Elroi_23")); assertTrue(ok("Dr. J"))
        assertFalse(ok("a")); assertFalse(ok("x".repeat(17))); assertFalse(ok("<script>")); assertFalse(ok("  "))
    }
    @Test fun friendCodeStable() {
        val c = com.nbeck.app.data.Online.friendCodeFor("abc123")
        assertEquals(6, c.length)
        assertEquals(c, com.nbeck.app.data.Online.friendCodeFor("abc123"))
    }
    @Test fun monthKey() = assertEquals("2026-10", com.nbeck.app.data.Online.monthKey(LocalDate.of(2026, 10, 29)))
}
