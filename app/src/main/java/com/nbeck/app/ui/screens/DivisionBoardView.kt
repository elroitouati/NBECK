package com.nbeck.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.nbeck.app.data.Conference
import com.nbeck.app.data.Division
import com.nbeck.app.data.Team
import com.nbeck.app.data.Teams
import com.nbeck.app.game.TeamState
import com.nbeck.app.ui.LocalStrings
import com.nbeck.app.ui.components.LedText
import com.nbeck.app.ui.components.pressable
import com.nbeck.app.ui.theme.Motion
import com.nbeck.app.ui.theme.Space
import com.nbeck.app.ui.theme.Type
import com.nbeck.app.ui.theme.nb

/** Smart division table: all 30 teams by conference and division, dimmed as guesses rule them out. */
@Composable
fun DivisionBoardView(
    states: Map<Int, TeamState>,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = nb
    val s = LocalStrings.current
    val remaining = states.count { it.value != TeamState.DIM }
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = Space.rCard, topEnd = Space.rCard))
            .background(c.surface1)
            .border(1.dp, c.border, RoundedCornerShape(topStart = Space.rCard, topEnd = Space.rCard))
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 44.dp).pressable(role = Role.Button, onClick = onToggle).padding(horizontal = Space.l),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(s.divisionsBoard, style = Type.label.copy(fontSize = 14.sp), color = c.text1, modifier = Modifier.weight(1f))
            LedText("$remaining/30", style = Type.led.copy(fontSize = 16.sp, lineHeight = 18.sp), color = c.led)
            Spacer(Modifier.width(Space.s))
            Icon(
                if (expanded) Icons.Rounded.KeyboardArrowDown else Icons.Rounded.KeyboardArrowUp,
                contentDescription = if (expanded) s.collapse else s.expand,
                tint = c.text2,
            )
        }
        AnimatedVisibility(
            expanded,
            enter = expandVertically(tween(Motion.small, easing = Motion.easeOut)) + fadeIn(tween(Motion.small)),
            exit = shrinkVertically(tween(Motion.small, easing = Motion.easeOut)) + fadeOut(tween(Motion.press)),
        ) {
            Row(Modifier.fillMaxWidth().padding(start = Space.s, end = Space.s, bottom = Space.s), horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                ConferenceColumn(Conference.EAST, s.east, states, Modifier.weight(1f))
                ConferenceColumn(Conference.WEST, s.west, states, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ConferenceColumn(conf: Conference, label: String, states: Map<Int, TeamState>, modifier: Modifier) {
    val c = nb
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = Type.label, color = c.text2, modifier = Modifier.padding(horizontal = 4.dp))
        Division.entries.filter { it.conference == conf }.forEach { d ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    d.short, style = Type.label.copy(fontSize = 10.sp, textDirection = TextDirection.Ltr), color = c.text3,
                    modifier = Modifier.width(30.dp),
                )
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Teams.inDivision(d).forEach { t -> TeamChip(t, states[t.espnId] ?: TeamState.NORMAL, Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun TeamChip(t: Team, state: TeamState, modifier: Modifier) {
    val c = nb
    val alpha by animateFloatAsState(if (state == TeamState.DIM) 0.22f else 1f, tween(Motion.page, easing = Motion.easeOut), label = "dim")
    val bg by animateColorAsState(if (state == TeamState.HIGHLIGHT) t.color else c.surface2, tween(Motion.page), label = "bg")
    val shape = RoundedCornerShape(8.dp)
    Column(
        modifier
            .alpha(alpha)
            .clip(shape)
            .background(bg)
            .then(if (state == TeamState.HIGHLIGHT) Modifier.border(2.dp, c.green, shape) else Modifier)
            .padding(vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AsyncImage(t.logoUrl, contentDescription = t.fullName, modifier = Modifier.size(18.dp))
        Text(
            t.abbr,
            style = Type.label.copy(fontSize = 9.sp, lineHeight = 10.sp, textDirection = TextDirection.Ltr),
            color = if (state == TeamState.HIGHLIGHT) Color(0xFFF7F7F7) else c.text2,
            maxLines = 1,
        )
    }
}

