package com.nbeck.app.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nbeck.app.NbeckApp
import com.nbeck.app.data.Player
import com.nbeck.app.data.SavedGame
import com.nbeck.app.game.Achievements
import com.nbeck.app.game.Daily
import com.nbeck.app.game.Engine
import com.nbeck.app.game.FinishedGame
import com.nbeck.app.game.GuessResult
import com.nbeck.app.game.MAX_GUESSES
import com.nbeck.app.game.Mode
import com.nbeck.app.game.PlayerSearch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class GameUi(
    val loading: Boolean = true,
    val mode: Mode = Mode.DAILY,
    val date: LocalDate = LocalDate.now(),
    val answer: Player? = null,
    val guesses: List<Player> = emptyList(),
    val results: List<GuessResult> = emptyList(),
    val finished: Boolean = false,
    val won: Boolean = false,
    /** Index of the row that should flip in (the one just guessed); -1 = render all static. */
    val animateRow: Int = -1,
    val query: String = "",
    val suggestions: List<Player> = emptyList(),
    /** True when this game ended during this screen session (play end effects); false when restored. */
    val endedLive: Boolean = false,
    val gaveUp: Boolean = false,
    /** Bumped on every new game so one-shot effects re-run. */
    val gameKey: Int = 0,
) {
    val guessCount get() = guesses.size
    val pairs get() = guesses.zip(results)
}

/** [day]: the daily's date (today, or a past date from the archive); ignored for free play. */
class GameViewModel(app: Application, private val mode: Mode, private val day: LocalDate) : AndroidViewModel(app) {
    private val nb = app as NbeckApp
    private val repo = nb.players
    private val prefs = nb.prefs

    private val _ui = MutableStateFlow(GameUi(mode = mode))
    val ui: StateFlow<GameUi> = _ui

    private var players: List<Player> = emptyList()
    private var search = PlayerSearch(emptyList())
    private var saved: SavedGame? = null

    init {
        viewModelScope.launch {
            players = repo.load()
            search = PlayerSearch(players)
            start(prefs.game(mode, day).first())
        }
    }

    private suspend fun start(prev: SavedGame?) {
        val today = if (mode == Mode.DAILY) day else LocalDate.now()
        val byId = players.associateBy { it.id }
        val restored = when (mode) {
            Mode.DAILY -> prev?.takeIf { it.date == today.toString() && byId.containsKey(it.answerId) }
            Mode.FREE -> prev?.takeIf { !it.finished && byId.containsKey(it.answerId) }
        }
        val game = restored ?: SavedGame(
            mode = mode,
            date = today.toString(),
            answerId = when (mode) {
                Mode.DAILY -> Daily.pick(players, today).id
                Mode.FREE -> Daily.randomPick(players, setOfNotNull(prev?.answerId)).id
            },
        )
        if (restored == null) prefs.saveGame(game)
        saved = game
        val date = LocalDate.parse(game.date)
        val answer = byId.getValue(game.answerId)
        val guesses = game.guessIds.mapNotNull { byId[it] }
        _ui.update {
            GameUi(
                loading = false,
                mode = mode,
                date = date,
                answer = answer,
                guesses = guesses,
                results = guesses.map { g -> Engine.grade(g, answer, date) },
                finished = game.finished,
                won = game.won,
                gaveUp = game.gaveUp,
                gameKey = it.gameKey + 1,
            )
        }
    }

    fun onQuery(q: String) {
        val s = _ui.value
        if (s.finished) return
        _ui.update { it.copy(query = q, suggestions = search.search(q, exclude = s.guesses.map { g -> g.id }.toSet(), limit = 40)) }
    }

    fun guess(p: Player) {
        val s = _ui.value
        val answer = s.answer ?: return
        if (s.finished || s.guesses.any { it.id == p.id }) return
        val result = Engine.grade(p, answer, s.date)
        val guesses = s.guesses + p
        val results = s.results + result
        val won = p.id == answer.id
        val finished = won || guesses.size >= MAX_GUESSES
        _ui.update {
            it.copy(
                guesses = guesses, results = results, finished = finished, won = won,
                animateRow = guesses.lastIndex, query = "", suggestions = emptyList(), endedLive = finished,
            )
        }
        viewModelScope.launch { persist(p, guesses, finished, won, results) }
    }

    /** The just-guessed row finished flipping: never flip it again (keyboard, scroll, recomposition). */
    fun rowAnimationDone(row: Int) {
        _ui.update { if (it.animateRow == row) it.copy(animateRow = -1) else it }
    }

    /** Give up: reveal the answer, counts as a loss. */
    fun giveUp() {
        val s = _ui.value
        if (s.finished || s.answer == null) return
        _ui.update { it.copy(finished = true, won = false, gaveUp = true, endedLive = true, animateRow = -1, query = "", suggestions = emptyList()) }
        saved = saved?.copy(gaveUp = true)
        viewModelScope.launch { persist(null, s.guesses, finished = true, won = false, results = s.results) }
    }

    private suspend fun persist(p: Player?, guesses: List<Player>, finished: Boolean, won: Boolean, results: List<GuessResult>) {
        val g = (saved ?: return).copy(guessIds = guesses.map { it.id }, finished = finished, won = won)
        var ach = prefs.achievements.first()
        if (p != null) ach = Achievements.afterGuess(ach, p.teamId, p.country)
        var stats = prefs.stats.first()
        var game: FinishedGame? = null
        val record = finished && !g.recorded
        if (record) {
            val today = LocalDate.parse(g.date)
            val live = mode == Mode.FREE || today == LocalDate.now()
            stats = stats.with(mode, stats[mode].record(mode, won, guesses.size, today, streak = live))
            prefs.saveStats(stats)
            game = FinishedGame(mode, won, results, answer = _ui.value.answer, date = today, archive = !live)
        }
        val (newAch, earned) = Achievements.evaluate(ach, stats, game, LocalDate.now())
        ach = newAch
        prefs.saveAchievements(ach)
        val done = g.copy(recorded = g.recorded || record)
        saved = done
        prefs.saveGame(done)
        if (earned.isNotEmpty()) nb.achievementEvents.emit(earned)
    }

    /** Free mode: a fresh random player. */
    fun newGame() {
        if (mode != Mode.FREE) return
        viewModelScope.launch {
            val prev = saved
            saved = null
            start(prev?.copy(finished = true))
        }
    }

    class Factory(private val app: Application, private val mode: Mode, private val day: LocalDate) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = GameViewModel(app, mode, day) as T
    }
}
