package com.nbeck.app.ui.screens

import android.app.Application
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nbeck.app.NbeckApp
import com.nbeck.app.audio.Sfx
import com.nbeck.app.data.Player
import com.nbeck.app.game.HL_ROUNDS
import com.nbeck.app.game.HigherLowerGame
import com.nbeck.app.game.HlChoice
import com.nbeck.app.game.HlRound
import com.nbeck.app.game.Mark
import com.nbeck.app.ui.LocalStrings
import com.nbeck.app.ui.components.ArenaBackground
import com.nbeck.app.ui.components.ButtonKind
import com.nbeck.app.ui.components.LedText
import com.nbeck.app.ui.components.NbButton
import com.nbeck.app.ui.components.PlayerPhoto
import com.nbeck.app.ui.components.Scoreboard
import com.nbeck.app.ui.components.pressable
import com.nbeck.app.ui.theme.Bebas
import com.nbeck.app.ui.theme.Motion
import com.nbeck.app.ui.theme.Space
import com.nbeck.app.ui.theme.Type
import com.nbeck.app.ui.theme.nb
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** How long the numbers and the right/wrong verdict stay on screen before the next comparison. */
const val HL_REVEAL_MS = 1000L

enum class HlPhase { LOADING, PLAYING, REVEAL, SUMMARY }

data class HlUi(
    val phase: HlPhase = HlPhase.LOADING,
    val game: HigherLowerGame? = null,
    /** The comparison just answered (shown during REVEAL). */
    val last: HlRound? = null,
    /** Bumped on every state change so Compose re-reads the (mutable) game. */
    val tick: Int = 0,
)

/** Nothing here is saved: quitting or leaving the screen drops the game. */
class HigherLowerViewModel(app: Application) : AndroidViewModel(app) {
    private val nb = app as NbeckApp
    private val _ui = MutableStateFlow(HlUi())
    val ui: StateFlow<HlUi> = _ui
    private var players: List<Player> = emptyList()

    init {
        viewModelScope.launch {
            players = nb.players.load()
            newGame()
        }
    }

    fun newGame() {
        val prev = _ui.value.game
        val game = HigherLowerGame.create(
            players,
            previousCategory = prev?.category,
            avoid = prev?.players?.map { it.id }?.toSet().orEmpty(),
        )
        _ui.value = HlUi(HlPhase.PLAYING, game, null, _ui.value.tick + 1)
    }

    /** One answer per comparison: taps during the reveal (or after the end) are ignored. */
    fun answer(choice: HlChoice) {
        val s = _ui.value
        val game = s.game ?: return
        if (s.phase != HlPhase.PLAYING) return
        val round = game.answer(choice) ?: return
        _ui.value = s.copy(phase = HlPhase.REVEAL, last = round, tick = s.tick + 1)
        if (round.correct) { nb.feedback.play(Sfx.FLIP); nb.feedback.tick() } else nb.feedback.heavy()
        viewModelScope.launch {
            delay(HL_REVEAL_MS)
            val now = _ui.value
            if (now.game !== game) return@launch
            if (game.finished) {
                nb.feedback.play(if (game.score >= HL_ROUNDS / 2) Sfx.WIN else Sfx.LOSE)
                _ui.value = now.copy(phase = HlPhase.SUMMARY, tick = now.tick + 1)
            } else {
                _ui.value = now.copy(phase = HlPhase.PLAYING, last = null, tick = now.tick + 1)
            }
        }
    }

    class Factory(private val app: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = HigherLowerViewModel(app) as T
    }
}

@Composable
fun HigherLowerScreen(onQuit: () -> Unit) {
    val ctx = LocalContext.current
    val vm: HigherLowerViewModel = viewModel(factory = HigherLowerViewModel.Factory(ctx.applicationContext as Application))
    val ui by vm.ui.collectAsState()
    val s = LocalStrings.current
    val c = nb
    // System back = QUIT: leave, nothing kept.
    BackHandler(onBack = onQuit)

    ArenaBackground(parquet = false) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Header(score = ui.game?.score ?: 0, showScore = ui.phase != HlPhase.SUMMARY, onQuit = onQuit)
            val game = ui.game
            when {
                game == null || ui.phase == HlPhase.LOADING ->
                    Text(s.loading, style = Type.body, color = c.text2, modifier = Modifier.padding(Space.gutter))
                ui.phase == HlPhase.SUMMARY -> Summary(game, onNew = vm::newGame, onQuit = onQuit)
                else -> Play(game, ui, vm::answer)
            }
        }
    }
}

/** Score at the left, title, QUIT in the top-right corner — fixed positions in both languages. */
@Composable
private fun Header(score: Int, showScore: Boolean, onQuit: () -> Unit) {
    val c = nb
    val s = LocalStrings.current
    Box(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = Space.m)) {
        if (showScore) {
            Scoreboard(Modifier.align(AbsoluteAlignment.CenterLeft)) {
                Text(s.hlScore, style = Type.label.copy(fontSize = 11.sp), color = Color(0xFF8C93A1))
                LedText("$score", style = Type.led.copy(fontSize = 24.sp, lineHeight = 26.sp))
            }
        }
        Text(s.hl, style = Type.title, color = c.text1, modifier = Modifier.align(Alignment.Center))
        Row(
            Modifier.align(AbsoluteAlignment.CenterRight).heightIn(min = 40.dp).clip(RoundedCornerShape(Space.rControl))
                .border(1.dp, c.borderStrong, RoundedCornerShape(Space.rControl)).pressable(onClick = onQuit).padding(horizontal = Space.m),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(s.quit, style = Type.label.copy(fontSize = 14.sp, fontFamily = Bebas, letterSpacing = 1.sp), color = c.text1)
        }
    }
}

@Composable
private fun Play(game: HigherLowerGame, ui: HlUi, onAnswer: (HlChoice) -> Unit) {
    val c = nb
    val s = LocalStrings.current
    val revealing = ui.phase == HlPhase.REVEAL
    // During the reveal the cards still show the pair that was just answered.
    val top = ui.last?.top ?: game.top
    val bottom = ui.last?.bottom ?: game.bottom
    Column(Modifier.fillMaxSize().padding(horizontal = Space.gutter)) {
        // The category, fixed for the whole game.
        Scoreboard(Modifier.fillMaxWidth()) {
            // The abbreviation big (RPG), what it means small underneath.
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                LedText(game.category.abbr, style = Type.led.copy(fontSize = 30.sp, lineHeight = 32.sp))
                Text(s.hlCategory(game.category), style = Type.label, color = Color(0xFFB8BEC9))
            }
        }
        Spacer(Modifier.height(Space.m))
        // Bottom card slides up into the top slot for the next comparison.
        AnimatedContent(
            targetState = top to bottom,
            transitionSpec = {
                (slideInVertically(tween(Motion.page, easing = Motion.easeOut)) { it / 3 } + fadeIn(tween(Motion.small)))
                    .togetherWith(slideOutVertically(tween(Motion.page, easing = Motion.easeOut)) { -it / 3 } + fadeOut(tween(Motion.press)))
            },
            modifier = Modifier.weight(1f),
            label = "pair",
        ) { (t, b) ->
            Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
                // The top player's number is always visible (it was revealed when they were the bottom).
                PlayerSlot(t, game.category.format(game.category.value(t)!!), Modifier.weight(1f), highlight = false)
                Text(s.hlQuestion, style = Type.bodyStrong, color = c.text2, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                PlayerSlot(b, if (revealing) ui.last?.bottomValue?.let(game.category::format) else null, Modifier.weight(1f), highlight = true)
            }
        }
        // Verdict for one second, then the next pair.
        Box(Modifier.fillMaxWidth().height(40.dp), contentAlignment = Alignment.Center) {
            androidx.compose.animation.AnimatedVisibility(revealing, enter = fadeIn(tween(Motion.press)) + scaleIn(tween(Motion.small), initialScale = 0.9f), exit = fadeOut(tween(Motion.press))) {
                val ok = ui.last?.correct == true
                Text(if (ok) "✓ ${s.hlCorrect}" else "✕ ${s.hlWrong}", style = Type.title, color = if (ok) c.green else Color(0xFFE5484D))
            }
        }
        // HIGHER / LOWER — fixed order in both languages. Against a 100% top nothing is higher,
        // so the choice becomes EQUAL / LOWER.
        val capped = game.category.isCapped(game.category.value(top)!!)
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(Modifier.fillMaxWidth().padding(bottom = Space.m), horizontalArrangement = Arrangement.spacedBy(Space.m)) {
                if (capped) ChoiceButton("EQUAL", Icons.Rounded.DragHandle, enabled = !revealing, modifier = Modifier.weight(1f)) { onAnswer(HlChoice.EQUAL) }
                else ChoiceButton("HIGHER", Icons.Rounded.ArrowUpward, enabled = !revealing, modifier = Modifier.weight(1f)) { onAnswer(HlChoice.HIGHER) }
                ChoiceButton("LOWER", Icons.Rounded.ArrowDownward, enabled = !revealing, modifier = Modifier.weight(1f)) { onAnswer(HlChoice.LOWER) }
            }
        }
    }
}

@Composable
private fun PlayerSlot(p: Player, value: String?, modifier: Modifier, highlight: Boolean) {
    val c = nb
    val t = p.team
    val shape = RoundedCornerShape(Space.rPanel)
    Row(
        modifier.fillMaxWidth().clip(shape).background(t.color.copy(alpha = 0.22f))
            .border(if (highlight) 2.dp else 1.dp, if (highlight) c.accent else t.color.copy(alpha = 0.6f), shape)
            .padding(Space.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(96.dp).clip(RoundedCornerShape(Space.rCard)).background(c.surface2)) {
            PlayerPhoto(p, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
        Spacer(Modifier.width(Space.m))
        Column(Modifier.weight(1f)) {
            Text(p.name, style = Type.title.copy(textDirection = TextDirection.Ltr), color = c.text1, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(t.fullName, style = Type.body.copy(textDirection = TextDirection.Ltr), color = c.text2, maxLines = 1)
        }
        Box(
            Modifier.clip(RoundedCornerShape(Space.rControl)).background(c.board).padding(horizontal = Space.m, vertical = Space.s),
            contentAlignment = Alignment.Center,
        ) {
            LedText(value ?: "?", style = Type.led.copy(fontSize = 26.sp, lineHeight = 28.sp))
        }
    }
}

@Composable
private fun ChoiceButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val c = nb
    val shape = RoundedCornerShape(Space.rControl)
    Row(
        modifier.heightIn(min = 64.dp).clip(shape).background(if (enabled) c.accent else c.surface2)
            .pressable(enabled = enabled, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = if (enabled) c.onAccent else c.text3)
        Spacer(Modifier.width(Space.s))
        Text(label, style = Type.display.copy(fontSize = 28.sp, lineHeight = 28.sp, letterSpacing = 1.sp), color = if (enabled) c.onAccent else c.text3)
    }
}

@Composable
private fun Summary(game: HigherLowerGame, onNew: () -> Unit, onQuit: () -> Unit) {
    val c = nb
    val s = LocalStrings.current
    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.weight(1f).padding(horizontal = Space.gutter),
            verticalArrangement = Arrangement.spacedBy(Space.s),
            contentPadding = PaddingValues(bottom = Space.l),
        ) {
            item {
                Scoreboard(Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        LedText("${game.score}/$HL_ROUNDS", style = Type.led.copy(fontSize = 48.sp, lineHeight = 50.sp))
                        Text(s.hlCategory(game.category), style = Type.label, color = Color(0xFFB8BEC9))
                    }
                }
                Spacer(Modifier.height(Space.s))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                    CountChip(s.hlCorrectCount, game.score, c.tile(Mark.GREEN), c.onTile(Mark.GREEN), Modifier.weight(1f))
                    CountChip(s.hlWrongCount, HL_ROUNDS - game.score, Color(0xFFE5484D), Color(0xFFFFFFFF), Modifier.weight(1f))
                }
                Spacer(Modifier.height(Space.s))
            }
            items(game.rounds, key = { it.number }) { r -> RoundRow(r) }
        }
        Column(Modifier.padding(horizontal = Space.gutter, vertical = Space.s)) {
            NbButton(s.newGame, Modifier.fillMaxWidth(), onClick = onNew)
            Spacer(Modifier.height(Space.s))
            NbButton(s.quit, Modifier.fillMaxWidth(), kind = ButtonKind.GHOST, onClick = onQuit)
        }
    }
}

@Composable
private fun CountChip(label: String, n: Int, bg: Color, fg: Color, modifier: Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(Space.rCard)).background(bg).padding(vertical = Space.s),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("$n", style = Type.h2, color = fg)
        Text(label, style = Type.label, color = fg)
    }
}

/** One comparison: round, category, both players with their numbers, the pick, ✓ / ✕. */
@Composable
private fun RoundRow(r: HlRound) {
    val c = nb
    val s = LocalStrings.current
    val shape = RoundedCornerShape(Space.rCard)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(c.surface1)
            .border(1.dp, if (r.correct) c.green.copy(alpha = 0.6f) else Color(0x99E5484D), shape).padding(Space.m),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(28.dp).clip(CircleShape).background(if (r.correct) c.green else Color(0xFFE5484D)),
                contentAlignment = Alignment.Center,
            ) { Text(if (r.correct) "✓" else "✕", style = Type.bodyStrong, color = if (r.correct) c.onGreen else Color.White) }
            Spacer(Modifier.width(Space.s))
            Text("${s.hlRound} ${r.number} · ${r.category.abbr}", style = Type.bodyStrong, color = c.text1, modifier = Modifier.weight(1f))
            Text("${s.hlYourPick}: ${r.choice.name}", style = Type.label, color = c.text2)
        }
        Spacer(Modifier.height(Space.s))
        ValueLine(r.top.name, r.category.format(r.topValue))
        ValueLine(r.bottom.name, r.category.format(r.bottomValue))
    }
}

@Composable
private fun ValueLine(name: String, value: String) {
    val c = nb
    // Name at the start, number at the far end — never glued together.
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(name, style = Type.body.copy(textDirection = TextDirection.Ltr), color = c.text1, modifier = Modifier.weight(1f, fill = false).padding(end = Space.m), maxLines = 1, overflow = TextOverflow.Ellipsis)
        LedText(value, style = Type.led.copy(fontSize = 18.sp, lineHeight = 20.sp))
    }
}
