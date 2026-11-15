package com.nbeck.app.game

import com.nbeck.app.data.Player
import kotlin.random.Random

/** Rounds per game; a game needs one more player than rounds. */
const val HL_ROUNDS = 20

/** The 9 categories. Values are the current-season numbers from ESPN, as stored on [Player]. */
enum class HlCategory(val en: String, val percent: Boolean, val value: (Player) -> Double?) {
    PPG("Points Per Game", false, { it.ppg }),
    RPG("Rebounds Per Game", false, { it.rpg }),
    APG("Assists Per Game", false, { it.apg }),
    SPG("Steals Per Game", false, { it.spg }),
    BPG("Blocks Per Game", false, { it.bpg }),
    FG_PCT("Field Goal %", true, { it.fgPct }),
    THREE_PCT("Three Point %", true, { it.tpPct }),
    FT_PCT("Free Throw %", true, { it.ftPct }),
    TOPG("Turnovers Per Game", false, { it.topg });

    /** "25.4" or "48.9%" — the same number the comparison uses. */
    fun format(v: Double) = "%.1f".format(v) + if (percent) "%" else ""
}

enum class HlChoice { HIGHER, LOWER }

/** One finished comparison, frozen with the values that were shown. */
data class HlRound(
    val number: Int,
    val category: HlCategory,
    val top: Player,
    val bottom: Player,
    val topValue: Double,
    val bottomValue: Double,
    val choice: HlChoice,
) {
    val correct get() = if (choice == HlChoice.HIGHER) bottomValue > topValue else bottomValue < topValue
}

/**
 * Higher or Lower: one random category for the whole game, 21 different players with a value in it,
 * no two neighbours with the same value (each pair is one comparison: the bottom becomes the next top).
 * A wrong answer doesn't end the game; it always runs [HL_ROUNDS] comparisons.
 */
class HigherLowerGame private constructor(
    val category: HlCategory,
    /** players[i] is on top in round i+1, players[i+1] on the bottom. */
    val players: List<Player>,
) {
    private val _rounds = mutableListOf<HlRound>()
    val rounds: List<HlRound> get() = _rounds
    val score get() = _rounds.count { it.correct }
    /** 0-based index of the comparison being played; == HL_ROUNDS when the game is over. */
    val index get() = _rounds.size
    val finished get() = index >= HL_ROUNDS
    val top get() = players[index.coerceAtMost(HL_ROUNDS - 1)]
    val bottom get() = players[index.coerceAtMost(HL_ROUNDS - 1) + 1]
    fun value(p: Player): Double = category.value(p)!!

    /** Records the answer for the current comparison. Ignored once the game is over. */
    fun answer(choice: HlChoice): HlRound? {
        if (finished) return null
        val r = HlRound(index + 1, category, top, bottom, value(top), value(bottom), choice)
        _rounds += r
        return r
    }

    companion object {
        /**
         * New game. [previousCategory] is not picked again (a new game gets a new category);
         * players from [avoid] (the previous game) are only used if there aren't enough others.
         */
        fun create(
            all: List<Player>,
            random: Random = Random.Default,
            previousCategory: HlCategory? = null,
            avoid: Set<String> = emptySet(),
        ): HigherLowerGame {
            val categories = HlCategory.entries.filter { it != previousCategory }.shuffled(random)
            for (cat in categories) {
                val withValue = all.filter { cat.value(it) != null }.distinctBy { it.id }
                val fresh = withValue.filter { it.id !in avoid }
                for (pool in listOf(fresh, withValue)) {
                    pick(pool, cat, random)?.let { return HigherLowerGame(cat, it) }
                }
            }
            error("not enough players with current-season stats")
        }

        /** 21 different players, each one's value different from the previous one's. */
        private fun pick(pool: List<Player>, cat: HlCategory, random: Random): List<Player>? {
            if (pool.size < HL_ROUNDS + 1) return null
            repeat(20) {
                val left = pool.shuffled(random).toMutableList()
                val chosen = mutableListOf(left.removeAt(0))
                while (chosen.size < HL_ROUNDS + 1) {
                    val prev = cat.value(chosen.last())!!
                    val i = left.indexOfFirst { cat.value(it)!! != prev }
                    if (i < 0) break
                    chosen += left.removeAt(i)
                }
                if (chosen.size == HL_ROUNDS + 1) return chosen
            }
            return null
        }

        /** For tests: a game over an explicit player order. */
        internal fun of(category: HlCategory, players: List<Player>) = HigherLowerGame(category, players)
    }
}
