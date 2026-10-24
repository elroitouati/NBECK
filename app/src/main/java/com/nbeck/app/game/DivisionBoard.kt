package com.nbeck.app.game

import com.nbeck.app.data.Player
import com.nbeck.app.data.Teams

enum class TeamState { NORMAL, DIM, HIGHLIGHT }

/** Smart division table (spec section 7): which of the 30 teams are still possible. */
object DivisionBoard {

    fun states(guesses: List<Pair<Player, GuessResult>>): Map<Int, TeamState> {
        val dim = mutableSetOf<Int>()
        val highlight = mutableSetOf<Int>()
        for ((p, r) in guesses) {
            val team = Teams[p.teamId]
            when (r.team.mark) {
                Mark.GREEN -> highlight += team.espnId
                else -> dim += team.espnId
            }
            when (r.division.mark) {
                // Other conference: everything in the guessed conference is out.
                Mark.GRAY -> dim += Teams.all.filter { it.conference == team.conference }.map { it.espnId }
                // Same conference, different division: the guessed division and the whole other conference are out.
                Mark.YELLOW -> dim += Teams.all
                    .filter { it.division == team.division || it.conference != team.conference }
                    .map { it.espnId }
                // Right division: every other division is out.
                Mark.GREEN -> dim += Teams.all.filter { it.division != team.division }.map { it.espnId }
            }
        }
        return Teams.all.associate {
            it.espnId to when (it.espnId) {
                in highlight -> TeamState.HIGHLIGHT
                in dim -> TeamState.DIM
                else -> TeamState.NORMAL
            }
        }
    }
}
