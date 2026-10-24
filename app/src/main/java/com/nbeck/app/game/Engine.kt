package com.nbeck.app.game

import com.nbeck.app.data.Player
import com.nbeck.app.data.Teams
import java.time.LocalDate
import kotlin.math.abs

const val MAX_GUESSES = 8

/** Green = exact, yellow = close, gray = far. */
enum class Mark { GREEN, YELLOW, GRAY }

/** Which way the secret answer lies relative to the guess (only on non-green age/number). */
enum class Arrow { UP, DOWN, NONE }

data class Cell(val mark: Mark, val arrow: Arrow = Arrow.NONE)

enum class Category { TEAM, DIVISION, POSITION, AGE, NUMBER, COUNTRY }

data class GuessResult(
    val team: Cell,
    val division: Cell,
    val position: Cell,
    val age: Cell,
    val number: Cell,
    val country: Cell,
) {
    val cells get() = listOf(team, division, position, age, number, country)
    val isWin get() = cells.all { it.mark == Mark.GREEN }
}

/** Pure grading rules from the spec table (section 4). Every rule has a unit test. */
object Engine {

    fun grade(guess: Player, answer: Player, date: LocalDate): GuessResult = GuessResult(
        team = team(guess, answer),
        division = division(guess, answer),
        position = position(guess.position, answer.position),
        age = numeric(guess.ageOn(date), answer.ageOn(date)),
        number = jersey(guess.jersey, answer.jersey),
        country = country(guess.country, answer.country),
    )

    fun team(guess: Player, answer: Player) =
        Cell(if (guess.teamId == answer.teamId) Mark.GREEN else Mark.GRAY)

    fun division(guess: Player, answer: Player): Cell {
        val g = Teams[guess.teamId].division
        val a = Teams[answer.teamId].division
        return Cell(
            when {
                g == a -> Mark.GREEN
                g.conference == a.conference -> Mark.YELLOW
                else -> Mark.GRAY
            }
        )
    }

    /** Exact string = green; any shared letter (G/F/C) = yellow; none = gray. */
    fun position(guess: String, answer: String): Cell {
        if (guess == answer) return Cell(Mark.GREEN)
        val shared = letters(guess) intersect letters(answer)
        return Cell(if (shared.isNotEmpty()) Mark.YELLOW else Mark.GRAY)
    }

    private fun letters(pos: String) = pos.split('-').map { it.trim() }.filter { it.isNotEmpty() }.toSet()

    /** Equal = green; |diff| <= 3 = yellow; else gray. Arrow points toward the answer when not green. */
    fun numeric(guess: Int, answer: Int): Cell {
        if (guess == answer) return Cell(Mark.GREEN)
        val arrow = if (answer > guess) Arrow.UP else Arrow.DOWN
        return Cell(if (abs(answer - guess) <= 3) Mark.YELLOW else Mark.GRAY, arrow)
    }

    /** A missing number on either side is always gray with no arrow. */
    fun jersey(guess: String?, answer: String?): Cell {
        val g = guess?.toIntOrNull()
        val a = answer?.toIntOrNull()
        if (g == null || a == null) return Cell(Mark.GRAY)
        if (guess == answer) return Cell(Mark.GREEN)
        if (g == a) return Cell(Mark.YELLOW) // "0" vs "00": same value, different jersey
        return numeric(g, a)
    }

    fun country(guess: String?, answer: String?) =
        Cell(if (guess != null && guess == answer) Mark.GREEN else Mark.GRAY)
}
