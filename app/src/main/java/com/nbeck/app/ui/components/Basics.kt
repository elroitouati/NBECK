package com.nbeck.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nbeck.app.ui.LocalStrings
import com.nbeck.app.ui.theme.Led
import com.nbeck.app.ui.theme.Motion
import com.nbeck.app.ui.theme.Space
import com.nbeck.app.ui.theme.Type
import com.nbeck.app.ui.theme.nb

/** Clickable with press feedback: scale 0.97 on press, 140ms ease-out. */
fun Modifier.pressable(enabled: Boolean = true, role: Role = Role.Button, onClick: () -> Unit): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, tween(Motion.press, easing = Motion.easeOut), label = "press")
    this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(interactionSource = source, indication = null, enabled = enabled, role = role, onClick = onClick)
}

/** Night arena backdrop: dark court, parquet planks fading in from the bottom, a spotlight cone from the top. */
@Composable
fun ArenaBackground(modifier: Modifier = Modifier, parquet: Boolean = true, content: @Composable BoxScope.() -> Unit) {
    val c = nb
    Box(modifier.fillMaxSize().background(c.bg)) {
        Canvas(Modifier.fillMaxSize()) {
            if (parquet) {
                val plank = 22.dp.toPx()
                var y = size.height * 0.45f
                var i = 0
                while (y < size.height) {
                    val fade = ((y - size.height * 0.45f) / (size.height * 0.55f)).coerceIn(0f, 1f)
                    val col = if (i % 2 == 0) c.woodA else c.woodB
                    drawRect(col.copy(alpha = col.alpha * fade * if (c.isDark) 0.55f else 0.35f), Offset(0f, y), androidx.compose.ui.geometry.Size(size.width, plank))
                    // plank joints, staggered
                    val step = size.width / 3.2f
                    var x = if (i % 2 == 0) step * 0.4f else step * 0.9f
                    while (x < size.width) {
                        drawRect(Color.Black.copy(alpha = 0.10f * fade), Offset(x, y), androidx.compose.ui.geometry.Size(1.dp.toPx(), plank))
                        x += step
                    }
                    y += plank
                    i++
                }
            }
            drawRect(
                Brush.radialGradient(
                    listOf(c.spotlight, Color.Transparent),
                    center = Offset(size.width / 2f, -size.height * 0.05f),
                    radius = size.height * 0.7f,
                )
            )
        }
        content()
    }
}

/** The physical scoreboard: always a dark panel with an amber dot-matrix readout. */
@Composable
fun Scoreboard(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    val c = nb
    Row(
        modifier
            .clip(RoundedCornerShape(Space.rControl))
            .background(Brush.verticalGradient(listOf(Color(0xFF10131A), c.board)))
            .border(1.dp, c.boardEdge, RoundedCornerShape(Space.rControl))
            .padding(horizontal = Space.m, vertical = Space.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.s),
        content = content,
    )
}

@Composable
fun LedText(text: String, modifier: Modifier = Modifier, style: TextStyle = Type.led, color: Color = nb.led) {
    // Numbers never mirror in RTL.
    Text(text, modifier, style = style.copy(fontFamily = Led, textDirection = androidx.compose.ui.text.style.TextDirection.Ltr), color = color)
}

enum class ButtonKind { PRIMARY, SECONDARY, GHOST }

@Composable
fun NbButton(
    text: String,
    modifier: Modifier = Modifier,
    kind: ButtonKind = ButtonKind.PRIMARY,
    enabled: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    val c = nb
    val shape = RoundedCornerShape(Space.rControl)
    val (bg, fg) = when (kind) {
        ButtonKind.PRIMARY -> c.accent to c.onAccent
        ButtonKind.SECONDARY -> c.surface2 to c.text1
        ButtonKind.GHOST -> Color.Transparent to c.text1
    }
    Row(
        modifier
            .heightIn(min = 56.dp)
            .clip(shape)
            .background(if (enabled) bg else c.surface2)
            .then(if (kind == ButtonKind.GHOST) Modifier.border(1.dp, c.borderStrong, shape) else Modifier)
            .pressable(enabled, onClick = onClick)
            .padding(horizontal = Space.xl),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            leading(); Box(Modifier.size(Space.s))
        }
        Text(text, style = Type.title.copy(fontSize = Type.bodyStrong.fontSize * 1.06f), color = if (enabled) fg else c.text3, textAlign = TextAlign.Center)
    }
}

@Composable
fun TopBar(title: String, onBack: () -> Unit, modifier: Modifier = Modifier, trailing: @Composable RowScope.() -> Unit = {}) {
    val s = LocalStrings.current
    Row(
        modifier.fillMaxWidth().padding(horizontal = Space.s, vertical = Space.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBox(onClick = onBack, label = s.back) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = s.back, tint = nb.text1)
        }
        Text(title, style = Type.title, color = nb.text1, modifier = Modifier.weight(1f).padding(horizontal = Space.xs))
        trailing()
    }
}

/** 48dp minimum tap target around an icon. */
@Composable
fun IconBox(onClick: () -> Unit, label: String, size: Dp = 48.dp, content: @Composable () -> Unit) {
    Box(
        Modifier.size(size).clip(RoundedCornerShape(Space.rControl)).pressable(onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
fun Card(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val c = nb
    Column(
        modifier
            .clip(RoundedCornerShape(Space.rCard))
            .background(c.surface1)
            .border(1.dp, c.border, RoundedCornerShape(Space.rCard))
            .padding(Space.l)
    ) { content() }
}
