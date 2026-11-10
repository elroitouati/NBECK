package com.nbeck.app.ui.screens

import android.app.Application
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.nbeck.app.NbeckApp
import com.nbeck.app.audio.Sfx
import com.nbeck.app.data.Player
import com.nbeck.app.game.Achievements
import com.nbeck.app.game.CHAIN_ATTEMPTS
import com.nbeck.app.game.ChainMove
import com.nbeck.app.game.ChainRound
import com.nbeck.app.game.PlayerSearch
import com.nbeck.app.game.TeammateGraph
import com.nbeck.app.game.evaluateChain
import com.nbeck.app.ui.LocalStrings
import com.nbeck.app.ui.components.ArenaBackground
import com.nbeck.app.ui.components.ButtonKind
import com.nbeck.app.ui.components.Confetti
import com.nbeck.app.ui.components.NbButton
import com.nbeck.app.ui.components.PlayerPhoto
import com.nbeck.app.ui.components.TopBar
import com.nbeck.app.ui.theme.Motion
import com.nbeck.app.ui.theme.Space
import com.nbeck.app.ui.theme.Type
import com.nbeck.app.ui.theme.nb
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class ChainUi(
    val loading: Boolean = true,
    val round: ChainRound? = null,
    val query: String = "",
    val suggestions: List<Player> = emptyList(),
    /** "X never played with Y" after a miss; cleared on the next guess. */
    val missText: String? = null,
    /** Fewest-players solution, shown after a loss. */
    val solution: List<String> = emptyList(),
    val roundKey: Int = 0,
    val endedLive: Boolean = false,
)

class ChainViewModel(app: Application) : AndroidViewModel(app) {
    private val nb = app as NbeckApp
    private val _ui = MutableStateFlow(ChainUi())
    val ui: StateFlow<ChainUi> = _ui
    /** One-shot sound/haptic cues for the screen. */
    val cues = MutableSharedFlow<ChainMove>(extraBufferCapacity = 4)
    lateinit var graph: TeammateGraph
        private set
    private var search = PlayerSearch(emptyList())

    init {
        viewModelScope.launch {
            val players = nb.players.load()
            graph = TeammateGraph(players)
            search = PlayerSearch(players)
            val saved = nb.prefs.chainRound.first()
            val valid = saved?.takeIf { r -> !r.finished && (r.chain + r.puzzle.goalId).all { it in graph.byId } }
            start(valid ?: ChainRound(graph.newPuzzle()))
        }
    }

    private suspend fun start(r: ChainRound) {
        nb.prefs.saveChainRound(r)
        _ui.update { ChainUi(loading = false, round = r, roundKey = it.roundKey + 1) }
    }

    fun player(id: String) = graph.byId[id]

    fun onQuery(q: String) {
        val r = _ui.value.round ?: return
        if (r.finished) return
        _ui.update { it.copy(query = q, suggestions = search.search(q, exclude = r.chain.toSet() + r.misses, limit = 40)) }
    }

    fun guess(p: Player) {
        val r = _ui.value.round ?: return
        val (next, move) = r.guess(p.id, graph)
        if (move == ChainMove.ALREADY_USED) return
        val miss = if (move == ChainMove.NOT_TEAMMATES) LocalStringsHolder.miss(p, player(r.chain.last())) else null
        _ui.update {
            it.copy(
                round = next, query = "", suggestions = emptyList(), missText = miss,
                solution = if (next.lost) graph.shortestPath(next.puzzle.startId, next.puzzle.goalId).orEmpty() else emptyList(),
                endedLive = next.finished,
            )
        }
        cues.tryEmit(move)
        persist(next)
    }

    private fun finish(next: ChainRound) {
        _ui.update {
            it.copy(round = next, query = "", suggestions = emptyList(), missText = null, endedLive = true,
                solution = graph.shortestPath(next.puzzle.startId, next.puzzle.goalId).orEmpty())
        }
        cues.tryEmit(ChainMove.NOT_TEAMMATES)
        persist(next)
    }

    private fun persist(next: ChainRound) {
        viewModelScope.launch {
            nb.prefs.saveChainRound(next)
            if (next.finished) {
                val stats = nb.prefs.chainStats.first().record(next)
                nb.prefs.saveChainStats(stats)
                val (ach, earned) = Achievements.evaluateChain(nb.prefs.achievements.first(), stats, next, LocalDate.now())
                nb.prefs.saveAchievements(ach)
                if (earned.isNotEmpty()) nb.achievementEvents.emit(earned)
            }
        }
    }

    fun giveUp() {
        val r = _ui.value.round ?: return
        if (r.finished) return
        finish(r.giveUp())
    }

    fun newRound() {
        val r = _ui.value.round
        viewModelScope.launch { start(ChainRound(graph.newPuzzle(avoid = setOfNotNull(r?.puzzle?.startId, r?.puzzle?.goalId)))) }
    }

    class Factory(private val app: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = ChainViewModel(app) as T
    }
}

/** The ViewModel builds the miss line before the screen recomposes; it reads the current language here. */
object LocalStringsHolder {
    @Volatile var strings: com.nbeck.app.ui.Strings = com.nbeck.app.ui.He
    fun miss(guess: Player, last: Player?) = strings.chainMiss(guess.name, last?.name ?: "")
}

@Composable
fun ChainScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as NbeckApp
    val vm: ChainViewModel = viewModel(factory = ChainViewModel.Factory(ctx.applicationContext as Application))
    val ui by vm.ui.collectAsState()
    val s = LocalStrings.current
    val c = nb
    LocalStringsHolder.strings = s
    val shake = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        vm.cues.collect { move ->
            when (move) {
                ChainMove.LINKED -> { app.feedback.play(Sfx.FLIP); app.feedback.tick() }
                ChainMove.LINKED_AND_DONE -> { app.feedback.play(Sfx.WIN); app.feedback.heavy() }
                ChainMove.NOT_TEAMMATES -> {
                    app.feedback.heavy()
                    if (vm.ui.value.round?.lost == true) app.feedback.play(Sfx.LOSE)
                    for (x in listOf(14f, -12f, 8f, -5f, 0f)) shake.animateTo(x, tween(55))
                }
                ChainMove.ALREADY_USED -> {}
            }
        }
    }

    ArenaBackground(parquet = false) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
            var confirmGiveUp by remember { mutableStateOf(false) }
            if (confirmGiveUp) GiveUpDialog(onConfirm = { confirmGiveUp = false; vm.giveUp() }, onDismiss = { confirmGiveUp = false })
            TopBar(s.chain, onBack) {
                ui.round?.let { r ->
                    AttemptBalls(r.attemptsLeft)
                    Spacer(Modifier.width(Space.m))
                    if (!r.finished) GiveUpButton { confirmGiveUp = true }
                }
            }
            val r = ui.round
            if (r == null || ui.loading) {
                Text(s.loading, style = Type.body, color = c.text2, modifier = Modifier.padding(Space.gutter))
                return@Column
            }
            val scroll = rememberScrollState()
            LaunchedEffect(r.chain.size, r.finished) { scroll.animateScrollTo(scroll.maxValue) }
            Column(
                Modifier.weight(1f).verticalScroll(scroll).padding(horizontal = Space.gutter),
            ) {
                Text(s.chainRules, style = Type.label.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Normal), color = c.text2)
                Spacer(Modifier.height(4.dp))
                Text(s.chainMin(r.puzzle.minBetween), style = Type.label, color = c.accent)
                Spacer(Modifier.height(Space.m))
                // start → links → (gap) → goal
                r.chain.forEachIndexed { i, id ->
                    if (i > 0) Connector(vm.graph.link(r.chain[i - 1], id))
                    ChainCard(vm.player(id), label = if (i == 0) s.chainStart else null, highlight = i == 0)
                }
                if (r.won) Connector(vm.graph.link(r.chain.last(), r.puzzle.goalId)) else GapConnector()
                ChainCard(vm.player(r.puzzle.goalId), label = s.chainGoal, highlight = true, goal = true)
                if (r.misses.isNotEmpty()) {
                    Spacer(Modifier.height(Space.m))
                    Text(s.chainMisses, style = Type.label, color = c.text2)
                    Spacer(Modifier.height(4.dp))
                    r.misses.forEach { id -> vm.player(id)?.let { MissChip(it) } }
                }
                if (r.lost && ui.solution.isNotEmpty()) {
                    Spacer(Modifier.height(Space.l))
                    Text(s.chainSolution, style = Type.bodyStrong, color = c.text1)
                    Spacer(Modifier.height(Space.s))
                    ui.solution.forEachIndexed { i, id ->
                        if (i > 0) Connector(vm.graph.link(ui.solution[i - 1], id), compact = true)
                        ChainCard(vm.player(id), compact = true)
                    }
                }
                Spacer(Modifier.height(Space.l))
            }
            AnimatedVisibility(ui.missText != null && !r.finished, enter = fadeIn(tween(Motion.small)), exit = fadeOut(tween(Motion.press))) {
                Text(
                    ui.missText.orEmpty(), style = Type.bodyStrong.copy(textDirection = TextDirection.Ltr), color = Color(0xFFE5484D),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Space.gutter, vertical = 4.dp).graphicsLayer { translationX = shake.value * density },
                )
            }
            if (r.finished) {
                EndPanel(r, onNew = vm::newRound, onHome = onBack)
            } else {
                SearchBar(query = ui.query, suggestions = ui.suggestions, onQuery = vm::onQuery, onPick = vm::guess)
            }
        }
        if (ui.round?.won == true && ui.endedLive) {
            val t = vm.player(ui.round!!.puzzle.goalId)?.team
            Confetti(listOfNotNull(t?.color, t?.accent, c.accent, Color(0xFFF7F7F7)), key = ui.roundKey)
        }
    }
}

/** Remaining attempts as small basketballs. */
@Composable
private fun AttemptBalls(left: Int) {
    val c = nb
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(CHAIN_ATTEMPTS) { i ->
            Box(Modifier.size(12.dp).clip(CircleShape).background(if (i < left) c.accent else c.surface2).border(1.dp, c.borderStrong, CircleShape))
        }
    }
}

@Composable
private fun ChainCard(p: Player?, label: String? = null, highlight: Boolean = false, goal: Boolean = false, compact: Boolean = false) {
    val c = nb
    p ?: return
    val t = p.team
    val shape = RoundedCornerShape(Space.rCard)
    Row(
        Modifier.fillMaxWidth().heightIn(min = if (compact) 48.dp else 64.dp).clip(shape)
            .background(if (highlight) t.color.copy(alpha = 0.22f) else c.surface1)
            .border(if (goal) 2.dp else 1.dp, if (goal) c.accent else if (highlight) t.color.copy(alpha = 0.7f) else c.border, shape)
            .padding(horizontal = Space.m, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(if (compact) 36.dp else 48.dp).clip(CircleShape).background(c.surface2)) {
            PlayerPhoto(p, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
        Spacer(Modifier.width(Space.m))
        Column(Modifier.weight(1f)) {
            Text(p.name, style = (if (compact) Type.body else Type.bodyStrong).copy(textDirection = TextDirection.Ltr), color = c.text1, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(t.fullName, style = Type.label.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Normal, textDirection = TextDirection.Ltr), color = c.text2, maxLines = 1)
        }
        if (label != null) {
            Box(Modifier.clip(RoundedCornerShape(8.dp)).background(if (goal) c.accent else c.surface2).padding(horizontal = Space.s, vertical = 3.dp)) {
                Text(label, style = Type.label, color = if (goal) c.onAccent else c.text1)
            }
        }
    }
}

/** Vertical link between two cards, labelled with the team and season they shared. */
@Composable
private fun Connector(link: com.nbeck.app.game.Link?, compact: Boolean = false) {
    val c = nb
    Row(Modifier.fillMaxWidth().height(if (compact) 30.dp else 40.dp).padding(start = 30.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(4.dp).fillMaxSize().clip(RoundedCornerShape(2.dp)).background(c.accent))
        Spacer(Modifier.width(Space.m))
        val t = link?.team
        if (link != null && t != null) {
            AsyncImage(t.logoUrl, null, Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("${t.abbr} · ${link.seasonLabel}", style = Type.label.copy(textDirection = TextDirection.Ltr), color = c.text1)
        }
    }
}

@Composable
private fun GapConnector() {
    val c = nb
    Row(Modifier.fillMaxWidth().height(48.dp).padding(start = 30.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.width(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(4) { Box(Modifier.size(4.dp, 6.dp).clip(RoundedCornerShape(2.dp)).background(c.borderStrong)) }
        }
        Spacer(Modifier.width(Space.m))
        Text("?", style = Type.display.copy(fontSize = 26.sp, lineHeight = 26.sp), color = c.led)
    }
}

@Composable
private fun MissChip(p: Player) {
    val c = nb
    Row(
        Modifier.padding(vertical = 2.dp).clip(RoundedCornerShape(10.dp)).background(c.surface2).padding(horizontal = Space.s, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("✕", style = Type.label, color = Color(0xFFE5484D))
        Spacer(Modifier.width(6.dp))
        Text(p.name, style = Type.label.copy(textDirection = TextDirection.Ltr), color = c.text2)
    }
}

@Composable
private fun EndPanel(r: ChainRound, onNew: () -> Unit, onHome: () -> Unit) {
    val c = nb
    val s = LocalStrings.current
    val shape = RoundedCornerShape(topStart = Space.rPanel, topEnd = Space.rPanel)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(c.surface1).border(1.dp, c.border, shape).padding(Space.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(if (r.won) s.chainWin else if (r.gaveUp) s.gaveUp else s.chainLoss, style = Type.h2, color = c.text1)
        if (r.won) Text(s.chainWinSub(r.between, r.attempts), style = Type.body, color = c.text2)
        Spacer(Modifier.height(Space.l))
        NbButton(s.newRound, Modifier.fillMaxWidth(), onClick = onNew)
        Spacer(Modifier.height(Space.s))
        NbButton(s.backHome, Modifier.fillMaxWidth(), kind = ButtonKind.GHOST, onClick = onHome)
    }
}

