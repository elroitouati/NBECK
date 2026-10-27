package com.nbeck.app.game

import kotlinx.serialization.Serializable
import com.nbeck.app.data.Player
import java.time.LocalDate
import java.time.LocalDateTime

enum class Mode { DAILY, FREE }

@Serializable
data class ModeStats(
    val played: Int = 0,
    val wins: Int = 0,
    val currentStreak: Int = 0,
    val maxStreak: Int = 0,
    val winGuessTotal: Int = 0,
    /** dist[i] = wins in i+1 guesses. */
    val dist: List<Int> = List(MAX_GUESSES) { 0 },
    /** Daily only: date of the last finished daily game (ISO). */
    val lastDate: String? = null,
) {
    val winPct get() = if (played == 0) 0 else Math.round(wins * 100f / played)
    val avgGuesses get() = if (wins == 0) 0f else winGuessTotal.toFloat() / wins

    /** Daily streak breaks if a whole day was skipped. */
    fun liveStreak(mode: Mode, today: LocalDate): Int {
        if (mode == Mode.FREE || lastDate == null) return currentStreak
        val last = LocalDate.parse(lastDate)
        return if (last >= today.minusDays(1)) currentStreak else 0
    }

    /**
     * [streak] = false for an archive daily (a past date): it counts toward played/wins/distribution
     * but must not touch the daily streak, which is about playing each day as it happens.
     */
    fun record(mode: Mode, won: Boolean, guesses: Int, date: LocalDate, streak: Boolean = true): ModeStats {
        if (!streak) return copy(
            played = played + 1,
            wins = wins + if (won) 1 else 0,
            winGuessTotal = winGuessTotal + if (won) guesses else 0,
            dist = if (won) dist.mapIndexed { i, v -> if (i == guesses - 1) v + 1 else v } else dist,
        )
        val continues = when (mode) {
            Mode.FREE -> true
            Mode.DAILY -> lastDate != null && LocalDate.parse(lastDate) == date.minusDays(1)
        }
        val streak = if (won) (if (continues) currentStreak + 1 else 1) else 0
        return copy(
            played = played + 1,
            wins = wins + if (won) 1 else 0,
            currentStreak = streak,
            maxStreak = maxOf(maxStreak, streak),
            winGuessTotal = winGuessTotal + if (won) guesses else 0,
            dist = if (won) dist.mapIndexed { i, v -> if (i == guesses - 1) v + 1 else v } else dist,
            lastDate = if (mode == Mode.DAILY) date.toString() else lastDate,
        )
    }
}

@Serializable
data class Stats(val daily: ModeStats = ModeStats(), val free: ModeStats = ModeStats()) {
    operator fun get(mode: Mode) = if (mode == Mode.DAILY) daily else free
    fun with(mode: Mode, s: ModeStats) = if (mode == Mode.DAILY) copy(daily = s) else copy(free = s)
    val totalWins get() = daily.wins + free.wins
}

enum class Achievement(val target: Int = 1) {
    FIRST_WIN,
    FIRST_TRY,
    THREE_POINTER,
    STREAK_3(3),
    STREAK_7(7),
    STREAK_30(30),
    NO_YELLOW,
    BUZZER_BEATER,
    WINS_10(10),
    WINS_50(50),
    WINS_100(100),
    ALL_TEAMS(30),
    GLOBETROTTER(15),
    SECOND_TRY,
    HOT_START,
    FREE_STREAK_5(5),
    FREE_WINS_25(25),
    HISTORIAN(5),
    BLUE_WHITE,
    INTERNATIONAL_10(10),
    ROOKIE_HUNTER,
    OLD_SCHOOL,
    RING_HUNTER,
    UNDRAFTED,
    NIGHT_OWL,
}

@Serializable
data class AchievementState(
    val unlocked: Map<String, String> = emptyMap(),
    val teamsGuessed: Set<Int> = emptySet(),
    val countriesGuessed: Set<String> = emptySet(),
    val threeOrLessWins: Int = 0,
    /** Finished past-date dailies (archive). */
    val archiveFinished: Int = 0,
    /** Wins where the secret player isn't from the USA. */
    val internationalWins: Int = 0,
) {
    fun isUnlocked(a: Achievement) = a.name in unlocked

    fun progress(a: Achievement, stats: Stats): Int = when (a) {
        Achievement.STREAK_3, Achievement.STREAK_7, Achievement.STREAK_30 -> stats.daily.maxStreak
        Achievement.WINS_10, Achievement.WINS_50, Achievement.WINS_100 -> stats.totalWins
        Achievement.ALL_TEAMS -> teamsGuessed.size
        Achievement.GLOBETROTTER -> countriesGuessed.size
        Achievement.FIRST_WIN -> stats.totalWins
        Achievement.FREE_STREAK_5 -> stats.free.maxStreak
        Achievement.FREE_WINS_25 -> stats.free.wins
        Achievement.HISTORIAN -> archiveFinished
        Achievement.INTERNATIONAL_10 -> internationalWins
        else -> if (isUnlocked(a)) 1 else 0
    }.coerceAtMost(a.target)
}

data class FinishedGame(
    val mode: Mode,
    val won: Boolean,
    val results: List<GuessResult>,
    val answer: Player? = null,
    /** The day the game is for (age is measured on it). */
    val date: LocalDate = LocalDate.now(),
    /** A past date from the archive. */
    val archive: Boolean = false,
    /** Wall-clock time the game ended (for the late-night achievement). */
    val endedAt: LocalDateTime = LocalDateTime.now(),
)

/** The most recent NBA draft: June of this year once the season has started, else last year's. */
fun latestDraftYear(today: LocalDate): Int = if (today.monthValue >= 7) today.year else today.year - 1

object Achievements {

    /** Called after every guess (teams/countries progress) and once more at game end. */
    fun afterGuess(s: AchievementState, teamId: Int, country: String?): AchievementState =
        s.copy(teamsGuessed = s.teamsGuessed + teamId, countriesGuessed = s.countriesGuessed + listOfNotNull(country))

    /** Returns the new state and the achievements unlocked by this update (for the toast). */
    fun evaluate(
        s: AchievementState,
        stats: Stats,
        game: FinishedGame?,
        today: LocalDate,
    ): Pair<AchievementState, List<Achievement>> {
        var state = s
        if (game != null && game.won && game.results.size <= 3) state = state.copy(threeOrLessWins = state.threeOrLessWins + 1)
        if (game != null && game.archive) state = state.copy(archiveFinished = state.archiveFinished + 1)
        if (game != null && game.won && game.answer?.country != null && game.answer.country != "USA") {
            state = state.copy(internationalWins = state.internationalWins + 1)
        }
        val earned = mutableListOf<Achievement>()
        fun check(a: Achievement, cond: Boolean) {
            if (cond && !state.isUnlocked(a)) earned += a
        }
        check(Achievement.FIRST_WIN, stats.totalWins >= 1)
        check(Achievement.STREAK_3, stats.daily.maxStreak >= 3)
        check(Achievement.STREAK_7, stats.daily.maxStreak >= 7)
        check(Achievement.STREAK_30, stats.daily.maxStreak >= 30)
        check(Achievement.WINS_10, stats.totalWins >= 10)
        check(Achievement.WINS_50, stats.totalWins >= 50)
        check(Achievement.WINS_100, stats.totalWins >= 100)
        check(Achievement.ALL_TEAMS, state.teamsGuessed.size >= 30)
        check(Achievement.GLOBETROTTER, state.countriesGuessed.size >= Achievement.GLOBETROTTER.target)
        check(Achievement.FREE_STREAK_5, stats.free.maxStreak >= 5)
        check(Achievement.FREE_WINS_25, stats.free.wins >= 25)
        check(Achievement.HISTORIAN, state.archiveFinished >= Achievement.HISTORIAN.target)
        check(Achievement.INTERNATIONAL_10, state.internationalWins >= Achievement.INTERNATIONAL_10.target)
        if (game != null && game.won) {
            val n = game.results.size
            check(Achievement.FIRST_TRY, n == 1)
            check(Achievement.THREE_POINTER, n <= 3)
            check(Achievement.BUZZER_BEATER, n == MAX_GUESSES)
            check(Achievement.NO_YELLOW, game.results.none { r -> r.cells.any { it.mark == Mark.YELLOW } })
            check(Achievement.SECOND_TRY, n == 2)
            check(Achievement.HOT_START, n > 1 && game.results.first().cells.count { it.mark == Mark.GREEN } >= 5)
            check(Achievement.NIGHT_OWL, game.endedAt.hour < 5)
            val a = game.answer
            if (a != null) {
                check(Achievement.BLUE_WHITE, a.country == "Israel")
                check(Achievement.ROOKIE_HUNTER, a.draftYear != null && a.draftYear == latestDraftYear(game.date))
                check(Achievement.OLD_SCHOOL, a.ageOn(game.date) >= 35)
                check(Achievement.RING_HUNTER, a.ring)
                check(Achievement.UNDRAFTED, a.draftYear == null)
            }
        }
        if (earned.isEmpty()) return state to earned
        val stamp = today.toString()
        return state.copy(unlocked = state.unlocked + earned.associate { it.name to stamp }) to earned
    }
}
