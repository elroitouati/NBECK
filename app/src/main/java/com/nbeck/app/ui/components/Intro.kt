package com.nbeck.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nbeck.app.R
import com.nbeck.app.ui.theme.Type
import com.nbeck.app.ui.theme.nb
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val Gravity = CubicBezierEasing(0.55f, 0f, 1f, 0.45f)   // accelerating fall
private val Rebound = CubicBezierEasing(0.2f, 0.7f, 0.4f, 1f)    // decelerating rise

/**
 * Cold-start intro (~1.1s): the D-ball drops onto the court and bounces with a squash, its glow
 * lights up, "NBA" slides in letter by letter, then the whole thing fades into the home screen.
 * [onLand] fires on the first impact (light haptic). [onDone] when it has fully faded out.
 */
@Composable
fun IntroOverlay(onLand: () -> Unit, onDone: () -> Unit) {
    val c = nb
    val drop = remember { Animatable(-1f) }       // ball y: -1 = above the screen, 0 = resting
    val squash = remember { Animatable(0f) }      // 0..1 impact squash
    val glow = remember { Animatable(0f) }
    val letters = List(3) { remember { Animatable(0f) } }
    val fade = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        coroutineScope {
            launch {
                drop.animateTo(0f, tween(380, easing = Gravity))
                onLand()
                launch { squash.animateTo(1f, tween(70)); squash.animateTo(0f, tween(160, easing = Rebound)) }
                drop.animateTo(-0.12f, tween(170, easing = Rebound))
                drop.animateTo(0f, tween(150, easing = Gravity))
                squash.animateTo(0.35f, tween(50)); squash.animateTo(0f, tween(110))
            }
            launch { delay(300); glow.animateTo(1f, tween(450, easing = LinearEasing)) }
            letters.forEachIndexed { i, a ->
                launch { delay(520L + i * 70L); a.animateTo(1f, tween(220, easing = com.nbeck.app.ui.theme.Motion.easeOut)) }
            }
        }
        delay(120)
        fade.animateTo(0f, tween(240, easing = com.nbeck.app.ui.theme.Motion.easeOut))
        onDone()
    }

    val ball = 104.dp
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = fade.value; val s = 1f + (1f - fade.value) * 0.04f; scaleX = s; scaleY = s }
            .background(c.bg),
        contentAlignment = Alignment.Center,
    ) {
        ArenaBackground(Modifier.fillMaxSize()) {}
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(verticalAlignment = Alignment.Bottom) {
                Box(Modifier.size(ball), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.requiredSize(ball * 1.9f)) {
                        drawCircle(
                            Brush.radialGradient(listOf(c.accent.copy(alpha = 0.5f * glow.value), Color.Transparent), center, size.minDimension / 2f),
                        )
                    }
                    Image(
                        painterResource(R.drawable.logo_d), contentDescription = null,
                        modifier = Modifier
                            .size(ball)
                            .graphicsLayer {
                                transformOrigin = TransformOrigin(0.5f, 1f)
                                translationY = drop.value * 900.dp.toPx()
                                scaleY = 1f - 0.16f * squash.value
                                scaleX = 1f + 0.12f * squash.value
                            },
                    )
                }
                val metal = if (c.isDark) Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFE3E6EA), Color(0xFFA9B0BA)))
                else Brush.verticalGradient(listOf(Color(0xFF3A404B), Color(0xFF14181D)))
                Row(Modifier.padding(start = ball * 0.04f).offset(y = ball * 0.26f)) {
                    "NBA".forEachIndexed { i, ch ->
                        val t = letters[i].value
                        Text(
                            ch.toString(),
                            modifier = Modifier.graphicsLayer { alpha = t; translationX = (1f - t) * 28.dp.toPx() },
                            style = Type.display.copy(
                                fontSize = (ball.value * 0.98f).sp, lineHeight = (ball.value * 0.98f).sp, letterSpacing = 1.sp,
                                brush = metal, textDirection = TextDirection.Ltr,
                                shadow = Shadow(Color.Black.copy(alpha = if (c.isDark) 0.7f else 0.18f), Offset(0f, 6f), 14f),
                            ),
                        )
                    }
                }
            }
        }
    }
}
