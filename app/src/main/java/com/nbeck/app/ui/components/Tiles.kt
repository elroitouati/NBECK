package com.nbeck.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Adjust
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.nbeck.app.data.Countries
import com.nbeck.app.data.Player
import com.nbeck.app.game.Arrow
import com.nbeck.app.game.Category
import com.nbeck.app.game.Cell
import com.nbeck.app.game.Mark
import com.nbeck.app.ui.theme.Motion
import com.nbeck.app.ui.theme.Space
import com.nbeck.app.ui.theme.Type
import com.nbeck.app.ui.theme.nb
import java.time.LocalDate

/** Basketball tank top: straps, scooped neck, cut armholes. */
val JerseyShape: Shape = GenericShape { size, _ ->
    val w = size.width; val h = size.height
    moveTo(w * 0.24f, 0f)
    lineTo(w * 0.38f, 0f)
    quadraticTo(w * 0.50f, h * 0.30f, w * 0.62f, 0f)
    lineTo(w * 0.76f, 0f)
    quadraticTo(w * 0.80f, h * 0.26f, w, h * 0.30f)
    lineTo(w, h)
    lineTo(0f, h)
    lineTo(0f, h * 0.30f)
    quadraticTo(w * 0.20f, h * 0.26f, w * 0.24f, 0f)
    close()
}

private val TileShape = RoundedCornerShape(Space.rTile)

fun shapeFor(cat: Category): Shape = if (cat == Category.NUMBER) JerseyShape else TileShape

/**
 * A grading tile that flips into place. [animate] = true flips it after [delayMs]; restored games
 * render already-flipped. [onLanded] fires as the face turns (used for the tick sound + haptic).
 */
@Composable
fun FlipTile(
    category: Category,
    cell: Cell,
    guess: Player,
    date: LocalDate,
    animate: Boolean,
    delayMs: Int,
    modifier: Modifier = Modifier,
    onLanded: (Mark) -> Unit = {},
) {
    val rot = remember { Animatable(if (animate) 0f else 180f) }
    LaunchedEffect(animate) {
        if (animate && rot.value < 180f) {
            kotlinx.coroutines.delay(delayMs.toLong())
            rot.animateTo(90f, tween(Motion.flip / 2, easing = androidx.compose.animation.core.FastOutLinearInEasing))
            onLanded(cell.mark)
            rot.animateTo(180f, tween(Motion.flip / 2, easing = Motion.easeOut))
        }
    }
    val density = LocalDensity.current.density
    Box(
        modifier.graphicsLayer {
            rotationX = 0f
            rotationY = if (rot.value <= 90f) rot.value else rot.value - 180f
            cameraDistance = 14f * density
        }
    ) {
        if (rot.value <= 90f) BlankTile(category, Modifier.fillMaxSize())
        else TileFace(category, cell, guess, date, Modifier.fillMaxSize())
    }
}

@Composable
fun BlankTile(category: Category, modifier: Modifier = Modifier) {
    val c = nb
    val shape = shapeFor(category)
    Box(modifier.clip(shape).background(c.surface2).border(1.dp, c.borderStrong, shape))
}

@Composable
fun TileFace(category: Category, cell: Cell, guess: Player, date: LocalDate, modifier: Modifier = Modifier) {
    val c = nb
    val shape = shapeFor(category)
    val team = guess.team
    val teamGreen = category == Category.TEAM && cell.mark == Mark.GREEN
    val bg = if (teamGreen) team.color else c.tile(cell.mark)
    val fg = if (teamGreen) Color(0xFFF7F7F7) else c.onTile(cell.mark)
    Box(
        modifier
            .clip(shape)
            .background(bg)
            .then(
                when {
                    teamGreen -> Modifier.border(3.dp, c.green, shape)
                    cell.mark == Mark.GRAY -> Modifier.border(1.dp, c.border, shape)
                    else -> Modifier
                }
            ),
        contentAlignment = Alignment.Center,
    ) {
        // Content starts below the corner mark so the two never overlap on narrow tiles.
        val badgeTop = category != Category.NUMBER && category != Category.RING
        BoxWithConstraints(
            Modifier.fillMaxSize().padding(start = 2.dp, end = 2.dp, top = if (badgeTop) 9.dp else 2.dp, bottom = 2.dp),
            contentAlignment = Alignment.Center,
        ) {
            val small = maxWidth < 46.dp
            val main = Type.tile.copy(fontSize = if (small) 13.sp else 15.sp, textDirection = TextDirection.Ltr)
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                when (category) {
                    Category.TEAM -> {
                        AsyncImage(team.logoUrl, contentDescription = null, modifier = Modifier.size(if (small) 17.dp else 22.dp))
                        Text(team.abbr, style = main.copy(fontSize = 11.sp), color = fg, maxLines = 1)
                    }
                    Category.DIVISION -> {
                        Text(team.division.short, style = main.copy(fontSize = if (small) 12.sp else 15.sp), color = fg, maxLines = 1)
                        Text(if (team.conference.name == "EAST") "EAST" else "WEST", style = Type.label.copy(fontSize = 9.sp, lineHeight = 10.sp), color = fg.copy(alpha = 0.75f))
                    }
                    Category.POSITION -> Text(guess.position, style = main, color = fg, maxLines = 1)
                    Category.AGE -> ValueWithArrow(guess.ageOn(date).toString(), cell.arrow, fg, main)
                    Category.NUMBER -> Box(Modifier.offset(y = 4.dp)) { ValueWithArrow(guess.jersey ?: "—", cell.arrow, fg, main) }
                    Category.DRAFT -> {
                        // Year on its own line so "2020" fits a narrow tile; arrow underneath.
                        Text(guess.draftYear?.toString() ?: "UND", style = main.copy(fontSize = if (small) 12.sp else 14.sp), color = fg, maxLines = 1)
                        if (cell.arrow != Arrow.NONE) {
                            Icon(
                                if (cell.arrow == Arrow.UP) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward,
                                contentDescription = if (cell.arrow == Arrow.UP) "later" else "earlier",
                                tint = fg, modifier = Modifier.size(12.dp),
                            )
                        }
                    }
                    Category.RING -> Icon(
                        if (guess.ring) Icons.Rounded.Check else Icons.Rounded.Close,
                        contentDescription = if (guess.ring) "champion" else "no title",
                        tint = fg, modifier = Modifier.size(if (small) 24.dp else 28.dp),
                    )
                    Category.COUNTRY -> {
                        val flag = Countries.flag(guess.country)
                        if (flag != null) Text(flag, fontSize = if (small) 14.sp else 18.sp, lineHeight = if (small) 17.sp else 20.sp)
                        Text(Countries.short(guess.country), style = main.copy(fontSize = 11.sp), color = fg, maxLines = 1)
                    }
                }
            }
        }
        // The ring tile's content already is a ✓/✗, so a second mark badge would only confuse.
        if (category != Category.RING) {
            val badgeAt = if (category == Category.NUMBER) Alignment.BottomEnd else Alignment.TopEnd
            MarkBadge(cell.mark, Modifier.align(badgeAt).padding(2.dp), size = 10.dp, solid = teamGreen)
        }
    }
}

@Composable
private fun ValueWithArrow(value: String, arrow: Arrow, fg: Color, style: androidx.compose.ui.text.TextStyle) {
    // Number then arrow, in both languages.
    androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(value, style = style, color = fg, maxLines = 1, textAlign = TextAlign.Center)
        if (arrow != Arrow.NONE) {
            Icon(
                if (arrow == Arrow.UP) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward,
                contentDescription = if (arrow == Arrow.UP) "higher" else "lower",
                tint = fg,
                modifier = Modifier.size(14.dp),
            )
        }
    }
    }
}

/** Shape cue on top of color, for color-blind players: ✓ exact, ◎ close, ✕ far. */
@Composable
fun MarkBadge(mark: Mark, modifier: Modifier = Modifier, size: androidx.compose.ui.unit.Dp = 12.dp, solid: Boolean = false) {
    val c = nb
    val icon = when (mark) { Mark.GREEN -> Icons.Rounded.Check; Mark.YELLOW -> Icons.Rounded.Adjust; Mark.GRAY -> Icons.Rounded.Close }
    val bg = if (solid) c.tile(mark) else c.onTile(mark).copy(alpha = 0.16f)
    Box(modifier.size(size).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = mark.name.lowercase(), tint = c.onTile(mark), modifier = Modifier.size(size * 0.8f))
    }
}

/** A mini colored square for the result grid (share preview / end screen). */
@Composable
fun MiniSquare(mark: Mark, modifier: Modifier = Modifier) {
    Box(modifier.size(16.dp).clip(RoundedCornerShape(4.dp)).background(nb.tile(mark)))
}
