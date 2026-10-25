package com.nbeck.app.game

import java.time.LocalDate

object Share {
    fun square(m: Mark) = when (m) {
        Mark.GREEN -> "🟩"
        Mark.YELLOW -> "🟨"
        Mark.GRAY -> "⬜"
    }

    /** "DNBA יומי 24.10 — 4/8" + one row of squares per guess. No names, no spoilers. */
    fun text(modeLabel: String, date: LocalDate, results: List<GuessResult>, won: Boolean): String {
        val score = if (won) "${results.size}/$MAX_GUESSES" else "X/$MAX_GUESSES"
        val day = "%02d.%02d".format(date.dayOfMonth, date.monthValue)
        val grid = results.joinToString("\n") { r -> r.cells.joinToString("") { square(it.mark) } }
        return "DNBA $modeLabel $day — $score\n$grid"
    }
}
