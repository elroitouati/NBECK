package com.nbeck.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil.compose.SubcomposeAsyncImage
import com.nbeck.app.R
import com.nbeck.app.data.Player
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Player headshot. [reveal] 0 = flat silhouette in [silhouette] color, 1 = full photo. Values in
 * between blend, which is how the end-of-game "silhouette brightens" reveal is animated.
 */
@Composable
fun PlayerPhoto(
    player: Player?,
    modifier: Modifier = Modifier,
    reveal: Float = 1f,
    silhouette: Color = Color(0xFF0E1117),
    contentScale: ContentScale = ContentScale.Fit,
) {
    val filter = if (reveal >= 1f) null else ColorFilter.colorMatrix(blendMatrix(reveal, silhouette))
    val fallback = @Composable {
        Image(painterResource(R.drawable.ic_silhouette), null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit,
            colorFilter = if (reveal >= 1f) null else ColorFilter.colorMatrix(blendMatrix(reveal, silhouette)))
    }
    if (player?.headshot == null) {
        Box(modifier, contentAlignment = Alignment.BottomCenter) { fallback() }
        return
    }
    SubcomposeAsyncImage(
        model = player.headshot,
        contentDescription = if (reveal >= 1f) player.name else null,
        modifier = modifier,
        contentScale = contentScale,
        colorFilter = filter,
        error = { fallback() },
        loading = { fallback() },
    )
}

private fun blendMatrix(t: Float, c: Color): ColorMatrix {
    val k = t.coerceIn(0f, 1f)
    val inv = 1f - k
    return ColorMatrix(
        floatArrayOf(
            k, 0f, 0f, 0f, inv * c.red * 255f,
            0f, k, 0f, 0f, inv * c.green * 255f,
            0f, 0f, k, 0f, inv * c.blue * 255f,
            0f, 0f, 0f, 1f, 0f,
        )
    )
}

private class Piece(val x: Float, val vx: Float, val vy: Float, val spin: Float, val w: Float, val h: Float, val color: Color, val phase: Float)

/** One-shot confetti burst in the given (team) colors. */
@Composable
fun Confetti(colors: List<Color>, modifier: Modifier = Modifier, count: Int = 120, key: Any = Unit) {
    val progress = remember(key) { Animatable(0f) }
    var pieces by remember(key) { mutableStateOf(emptyList<Piece>()) }
    LaunchedEffect(key) {
        val r = Random(System.nanoTime())
        pieces = List(count) {
            val angle = (-PI / 2 + (r.nextFloat() - 0.5f) * PI * 0.9).toFloat()
            val speed = 0.9f + r.nextFloat() * 0.9f
            Piece(
                x = 0.5f + (r.nextFloat() - 0.5f) * 0.3f,
                vx = cos(angle) * speed * 0.55f,
                vy = sin(angle) * speed,
                spin = (r.nextFloat() - 0.5f) * 1080f,
                w = 6f + r.nextFloat() * 6f,
                h = 10f + r.nextFloat() * 8f,
                color = colors[it % colors.size],
                phase = r.nextFloat() * 6.28f,
            )
        }
        progress.animateTo(1f, tween(2600, easing = LinearEasing))
    }
    val t = progress.value
    if (t >= 1f || pieces.isEmpty()) return
    Canvas(modifier.fillMaxSize()) {
        val g = 2.2f
        for (p in pieces) {
            val x = (p.x + p.vx * t + sin(p.phase + t * 9f) * 0.015f) * size.width
            val y = size.height * 0.62f + (p.vy * t + 0.5f * g * t * t) * size.height * 0.75f
            val alpha = (1f - ((t - 0.75f) / 0.25f).coerceIn(0f, 1f))
            rotate(p.spin * t, Offset(x, y)) {
                drawRect(p.color.copy(alpha = alpha), Offset(x - p.w / 2, y - p.h / 2), Size(p.w * density / 2.5f, p.h * density / 2.5f))
            }
        }
    }
}
