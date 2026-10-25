package com.nbeck.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
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

/**
 * The letter A drawn as a basketball: the two black seams that meet at the top are the legs of the A,
 * and the ball's horizontal seam doubles as the crossbar. Same geometry as the launcher icon.
 */
@Composable
fun BallA(size: Dp, modifier: Modifier = Modifier, ball: Color = nb.accent, seam: Color = Color(0xFF1A0E06)) {
    Canvas(modifier.size(size)) {
        val r = this.size.minDimension / 2f
        val c = Offset(r, r)
        drawCircle(ball, r, c)
        drawCircle(Color.White.copy(alpha = 0.10f), r * 0.5f, Offset(c.x - r * 0.38f, c.y - r * 0.4f))
        val thin = Stroke(width = r * 0.06f)
        // Side seams, light, so the A reads first.
        drawPath(Path().apply {
            moveTo(c.x - r * 0.92f, c.y - r * 0.38f); quadraticTo(c.x - r * 0.55f, c.y + r * 0.1f, c.x - r * 0.9f, c.y + r * 0.45f)
        }, seam.copy(alpha = 0.55f), style = thin)
        drawPath(Path().apply {
            moveTo(c.x + r * 0.92f, c.y - r * 0.38f); quadraticTo(c.x + r * 0.55f, c.y + r * 0.1f, c.x + r * 0.9f, c.y + r * 0.45f)
        }, seam.copy(alpha = 0.55f), style = thin)
        // The A.
        val leg = Stroke(width = r * 0.17f, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)
        drawPath(Path().apply {
            moveTo(c.x - r * 0.56f, c.y + r * 0.83f)
            lineTo(c.x, c.y - r * 0.80f)
            lineTo(c.x + r * 0.56f, c.y + r * 0.83f)
        }, seam, style = leg)
        val barY = c.y + r * 0.22f
        val half = kotlin.math.sqrt(r * r - (barY - c.y) * (barY - c.y))
        drawLine(seam, Offset(c.x - half, barY), Offset(c.x + half, barY), r * 0.12f)
    }
}

/**
 * DNBA wordmark: the D is the basketball "D" from the launcher icon, lit by an orange glow, followed
 * by "NBA" in the display face with a brushed-metal fill. Always LTR.
 */
@Composable
fun Wordmark(modifier: Modifier = Modifier, height: Dp = 96.dp) {
    val c = nb
    val fs = (height.value * 0.98f).sp
    val metal = if (c.isDark) {
        androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFE3E6EA), Color(0xFFA9B0BA)))
    } else {
        androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0xFF3A404B), Color(0xFF14181D)))
    }
    androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr) {
        Row(modifier, verticalAlignment = Alignment.Bottom) {
            androidx.compose.foundation.layout.Box(Modifier.size(height), contentAlignment = Alignment.Center) {
                // Glow behind the ball; requiredSize lets it spill past the layout bounds.
                Canvas(Modifier.requiredSize(height * 1.8f)) {
                    drawCircle(
                        androidx.compose.ui.graphics.Brush.radialGradient(
                            listOf(c.accent.copy(alpha = if (c.isDark) 0.42f else 0.25f), Color.Transparent),
                            center = center, radius = this.size.minDimension / 2f,
                        )
                    )
                }
                androidx.compose.foundation.Image(
                    androidx.compose.ui.res.painterResource(com.nbeck.app.R.drawable.logo_d),
                    contentDescription = null,
                    modifier = Modifier.size(height),
                )
            }
            Text(
                "NBA",
                modifier = Modifier.padding(start = height * 0.04f).offset(y = height * 0.26f),
                style = Type.display.copy(
                    fontSize = fs, lineHeight = fs, letterSpacing = 1.sp, brush = metal,
                    textDirection = TextDirection.Ltr,
                    shadow = androidx.compose.ui.graphics.Shadow(Color.Black.copy(alpha = if (c.isDark) 0.7f else 0.18f), Offset(0f, 6f), 14f),
                ),
            )
        }
    }
}
