package com.nbeck.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nbeck.app.ui.theme.Type
import com.nbeck.app.ui.theme.nb

/** The ball mark — same geometry as the launcher icon. */
@Composable
fun BallMark(size: Dp, modifier: Modifier = Modifier, ball: Color = nb.accent, seam: Color = Color(0xFF1A0E06)) {
    Canvas(modifier.size(size)) {
        val r = this.size.minDimension / 2f
        val c = Offset(r, r)
        drawCircle(ball, r, c)
        val sw = r * 0.085f
        val st = Stroke(width = sw)
        drawLine(seam, Offset(c.x, c.y - r), Offset(c.x, c.y + r), sw)
        drawLine(seam, Offset(c.x - r, c.y), Offset(c.x + r, c.y), sw)
        val left = Path().apply {
            moveTo(c.x - r * 0.62f, c.y - r * 0.78f)
            quadraticTo(c.x - r * 0.12f, c.y, c.x - r * 0.62f, c.y + r * 0.78f)
        }
        val right = Path().apply {
            moveTo(c.x + r * 0.62f, c.y - r * 0.78f)
            quadraticTo(c.x + r * 0.12f, c.y, c.x + r * 0.62f, c.y + r * 0.78f)
        }
        drawPath(left, seam, style = st)
        drawPath(right, seam, style = st)
        drawCircle(Color.White.copy(alpha = 0.10f), r * 0.55f, Offset(c.x - r * 0.3f, c.y - r * 0.35f))
    }
}

/** NBECK wordmark: the ball sits in for the space before the E — always LTR. */
@Composable
fun Wordmark(modifier: Modifier = Modifier, height: Dp = 64.dp) {
    val c = nb
    val fs = (height.value * 1.0f).sp
    androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(height * 0.08f)) {
        Text("NB", style = Type.display.copy(fontSize = fs, lineHeight = fs, textDirection = TextDirection.Ltr), color = c.text1)
        BallMark(height * 0.72f)
        Text("ECK", style = Type.display.copy(fontSize = fs, lineHeight = fs, textDirection = TextDirection.Ltr), color = c.text1)
    }
    }
}
