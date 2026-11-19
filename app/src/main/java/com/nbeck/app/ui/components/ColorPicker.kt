package com.nbeck.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.nbeck.app.ui.theme.Space
import com.nbeck.app.ui.theme.Type
import com.nbeck.app.ui.theme.lum
import com.nbeck.app.ui.theme.nb
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

/** A broad palette: team colors, neutrals and brights. */
val Palette: List<Long> = listOf(
    0xFF0B0D12, 0xFF121212, 0xFF1E1E1E, 0xFF2A303B, 0xFF5A6170, 0xFF8C93A1, 0xFFC4CED4, 0xFFF5F3EE, 0xFFFFFFFF,
    0xFF0F1626, 0xFF1D428A, 0xFF006BB6, 0xFF1D8CF2, 0xFF38BDF8, 0xFF00A6A6, 0xFF0E2240, 0xFF002B5C, 0xFF5D76A9,
    0xFF0D1A14, 0xFF007A33, 0xFF00471B, 0xFF2BB35A, 0xFF4ADE80, 0xFF78BE20, 0xFF34C759, 0xFFBA9653, 0xFFEEE1C6,
    0xFF161022, 0xFF552583, 0xFF5A2D81, 0xFF7B4DDB, 0xFF1D1160, 0xFFE879F9, 0xFFEC4899, 0xFFED174C, 0xFF98002E,
    0xFF1A120D, 0xFFCE1141, 0xFFE03A3E, 0xFFC8102E, 0xFFFF4D4D, 0xFFE56020, 0xFFFF7A1A, 0xFFF58426, 0xFFF9A01B,
    0xFFFDB927, 0xFFFFC72C, 0xFFF7C631, 0xFFFFB021, 0xFFFEC524, 0xFFFFF4E8, 0xFFEEF2F8, 0xFFF1F5F0, 0xFF3A2414,
)

private fun hsv(c: Color): FloatArray {
    val out = FloatArray(3)
    android.graphics.Color.colorToHSV(c.toArgb(), out)
    return out
}

/**
 * Picker dialog: palette swatches, a color wheel (hue around, saturation outward), a brightness
 * slider and a hex field. [onAuto] (optional) resets the setting to its automatic value.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorPickerDialog(
    title: String,
    initial: Color,
    useLabel: String,
    autoLabel: String?,
    onPick: (Color) -> Unit,
    onAuto: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) { ColorPickerPanel(title, initial, useLabel, autoLabel, onPick, onAuto) }
}

/** The picker's content, without the dialog window around it. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorPickerPanel(
    title: String,
    initial: Color,
    useLabel: String,
    autoLabel: String?,
    onPick: (Color) -> Unit,
    onAuto: (() -> Unit)?,
) {
    val c = nb
    val start = remember { hsv(initial) }
    var hue by remember { mutableFloatStateOf(start[0]) }
    var sat by remember { mutableFloatStateOf(start[1]) }
    var value by remember { mutableFloatStateOf(start[2]) }
    val color = Color.hsv(hue, sat, value)
    var hexText by remember { mutableStateOf<String?>(null) }
    fun set(col: Color) { val h = hsv(col); hue = h[0]; sat = h[1]; value = h[2]; hexText = null }
    val hex = "#%06X".format(color.toArgb() and 0xFFFFFF)

    run {
        val shape = RoundedCornerShape(Space.rPanel)
        Column(
            Modifier.fillMaxWidth().clip(shape).background(c.surface1).border(1.dp, c.border, shape)
                .verticalScroll(rememberScrollState()).padding(Space.l),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).clip(CircleShape).background(color).border(1.dp, c.borderStrong, CircleShape))
                Spacer(Modifier.width(Space.m))
                Text(title, style = Type.title, color = c.text1, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(Space.m))
            ColorWheel(hue, sat, value, Modifier.fillMaxWidth(0.8f).align(Alignment.CenterHorizontally)) { h, s -> hue = h; sat = s; hexText = null }
            Spacer(Modifier.height(Space.s))
            // Brightness, drawn as a gradient from black to the full color.
            Box(Modifier.fillMaxWidth().height(28.dp).clip(RoundedCornerShape(14.dp))
                .background(Brush.horizontalGradient(listOf(Color.Black, Color.hsv(hue, sat, 1f))))) {
                Slider(
                    value = value, onValueChange = { value = it; hexText = null },
                    colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.Transparent, inactiveTrackColor = Color.Transparent),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(Space.m))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Palette.forEach { argb ->
                    val sw = Color(argb)
                    Box(
                        Modifier.size(30.dp).clip(CircleShape).background(sw).border(1.dp, c.borderStrong, CircleShape).pressable { set(sw) },
                    )
                }
            }
            Spacer(Modifier.height(Space.m))
            // Hex: shows the color, and accepts a typed one (#RRGGBB).
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(Space.rControl)).background(c.surface2).padding(horizontal = Space.m, vertical = Space.s),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicTextField(
                    value = hexText ?: hex,
                    onValueChange = { t ->
                        hexText = t.take(7)
                        val clean = t.removePrefix("#")
                        if (clean.length == 6) clean.toLongOrNull(16)?.let { set(Color(0xFF000000 or it)); hexText = "#" + clean.uppercase() }
                    },
                    singleLine = true,
                    textStyle = Type.bodyStrong.copy(color = c.text1, textDirection = TextDirection.Ltr),
                    cursorBrush = SolidColor(c.accent),
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(Space.l))
            NbButton(useLabel, Modifier.fillMaxWidth()) { onPick(color) }
            if (onAuto != null && autoLabel != null) {
                Spacer(Modifier.height(Space.s))
                NbButton(autoLabel, Modifier.fillMaxWidth(), kind = ButtonKind.GHOST, onClick = onAuto)
            }
        }
    }
}

/** Hue around the circle, saturation from the center out; tap or drag to pick. */
@Composable
fun ColorWheel(hue: Float, sat: Float, value: Float, modifier: Modifier = Modifier, onChange: (Float, Float) -> Unit) {
    fun pick(o: Offset, w: Float, h: Float) {
        val r = min(w, h) / 2f
        val dx = o.x - w / 2f; val dy = o.y - h / 2f
        val ang = ((Math.toDegrees(atan2(dy, dx).toDouble()) + 360) % 360).toFloat()
        onChange(ang, (hypot(dx, dy) / r).coerceIn(0f, 1f))
    }
    Canvas(
        modifier.aspectRatio(1f)
            .pointerInput(Unit) { detectTapGestures { pick(it, size.width.toFloat(), size.height.toFloat()) } }
            .pointerInput(Unit) { detectDragGestures { ch, _ -> pick(ch.position, size.width.toFloat(), size.height.toFloat()) } },
    ) {
        val r = size.minDimension / 2f
        // Screen angles run clockwise from 3 o'clock, matching the sweep gradient.
        drawCircle(Brush.sweepGradient(listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)), r)
        drawCircle(Brush.radialGradient(listOf(Color.White, Color.White.copy(alpha = 0f)), radius = r), r)
        if (value < 1f) drawCircle(Color.Black.copy(alpha = 1f - value), r)
        val a = Math.toRadians(hue.toDouble())
        val p = Offset(center.x + (cos(a) * sat * r).toFloat(), center.y + (sin(a) * sat * r).toFloat())
        val sel = Color.hsv(hue, sat, value)
        drawCircle(sel, 14.dp.toPx(), p)
        drawCircle(if (sel.lum() > 0.5f) Color.Black else Color.White, 14.dp.toPx(), p, style = Stroke(3.dp.toPx()))
    }
}
