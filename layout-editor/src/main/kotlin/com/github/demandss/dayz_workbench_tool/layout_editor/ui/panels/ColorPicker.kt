package com.github.demandss.dayz_workbench_tool.layout_editor.ui.panels

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.github.demandss.dayz_workbench_tool.layout_editor.ui.canvas.hsvToColor
import com.github.demandss.dayz_workbench_tool.layout_editor.ui.canvas.parseColorText
import com.github.demandss.dayz_workbench_tool.layout_editor.ui.canvas.toHex
import com.github.demandss.dayz_workbench_tool.layout_editor.ui.canvas.toHsv
import com.github.demandss.dayz_workbench_tool.resources.*
import com.github.demandss.dayz_workbench_tool.ui.components.Tip
import com.github.demandss.dayz_workbench_tool.ui.theme.DarkenedBorder
import com.github.demandss.dayz_workbench_tool.ui.theme.ElementsBackground
import com.github.demandss.dayz_workbench_tool.ui.theme.FrameBackground
import com.github.demandss.dayz_workbench_tool.ui.theme.GhostlyText
import com.github.demandss.dayz_workbench_tool.ui.theme.TextForeground
import org.jetbrains.compose.resources.stringResource

private val PRESETS =
    listOf(
        0xFFFFFFFF,
        0xFFC8C8C8,
        0xFF808080,
        0xFF404040,
        0xFF000000,
        0xFFE53935,
        0xFFFB8C00,
        0xFFFDD835,
        0xFF43A047,
        0xFF00ACC1,
        0xFF1E88E5,
        0xFF8E24AA,
    ).map { Color(it) }

internal fun colorFromPickerText(
    text: String,
    currentAlpha: Float,
): Color? {
    val parsed = parseColorText(text) ?: return null
    val digits = text.trim().removePrefix("#").length
    return if (text.contains('(') || digits == 4 || digits == 8) parsed else parsed.copy(alpha = currentAlpha)
}

/**
 * A colour picker as a popup under [anchorHeightPx]. [initial] is the colour as it is shown on the screen. The result goes
 * to [onCommit] when a drag ends or a hex value is entered, so one change is one step of the undo history.
 */
@Composable
fun ColorPickerPopup(
    initial: Color,
    anchorHeightPx: Int,
    onDismiss: () -> Unit,
    documentColors: List<Color> = emptyList(),
    onCommit: (Color) -> Unit,
) {
    val start = remember { initial.toHsv() }
    var hue by remember { mutableStateOf(start.h) }
    var sat by remember { mutableStateOf(start.s) }
    var value by remember { mutableStateOf(start.v) }
    var alpha by remember { mutableStateOf(initial.alpha) }
    val current = hsvToColor(hue, sat, value, alpha)
    var hexField by remember { mutableStateOf(TextFieldValue(current.toHex())) }
    val focus = remember { FocusRequester() }

    fun showHex(text: String) {
        hexField = TextFieldValue(text, selection = TextRange(text.length))
    }

    fun set(c: Color) {
        val h = c.toHsv()
        hue = if (h.s == 0f || h.v == 0f) hue else h.h
        sat = h.s
        value = h.v
        showHex(c.toHex())
    }

    fun finish() {
        showHex(hsvToColor(hue, sat, value, alpha).toHex())
        onCommit(hsvToColor(hue, sat, value, alpha))
    }

    fun applyText(text: String) {
        val parsed = colorFromPickerText(text, alpha) ?: return
        alpha = parsed.alpha
        set(parsed)
        finish()
    }

    Popup(
        alignment = Alignment.TopStart,
        offset = IntOffset(0, anchorHeightPx),
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        Column(
            Modifier
                .width(212.dp)
                .background(ElementsBackground)
                .border(1.dp, DarkenedBorder)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // The field is part of this popup, so it can take the focus only after the first frame
            LaunchedEffect(Unit) {
                withFrameNanos { }
                hexField = hexField.copy(selection = TextRange(0, hexField.text.length))
                runCatching { focus.requestFocus() }
            }
            // Saturation and value
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .border(1.dp, DarkenedBorder)
                    .dragPick({ p, size ->
                        sat = (p.x / size.width).coerceIn(0f, 1f)
                        value = 1f - (p.y / size.height).coerceIn(0f, 1f)
                        showHex(hsvToColor(hue, sat, value, alpha).toHex())
                    }, ::finish),
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    drawRect(Brush.horizontalGradient(listOf(Color.White, hsvToColor(hue, 1f, 1f))))
                    drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
                    val at = Offset(sat * size.width, (1f - value) * size.height)
                    drawCircle(Color.White, 6f, at, style = Stroke(2f))
                    drawCircle(Color.Black, 7.5f, at, style = Stroke(1f))
                }
            }

            // Hue
            Slider(
                brush =
                    Brush.horizontalGradient(
                        listOf(0f, 60f, 120f, 180f, 240f, 300f, 360f).map { hsvToColor(it.coerceAtMost(359.9f), 1f, 1f) },
                    ),
                tooltip = stringResource(Res.string.hue),
                fraction = hue / 360f,
                onPick = { f ->
                    hue = f * 360f
                    showHex(hsvToColor(hue, sat, value, alpha).toHex())
                },
                onDone = ::finish,
            )
            // Alpha
            Slider(
                brush = Brush.horizontalGradient(listOf(Color.Transparent, hsvToColor(hue, sat, value))),
                tooltip = stringResource(Res.string.opacity),
                fraction = alpha,
                backdrop = true,
                onPick = { alpha = it },
                onDone = ::finish,
            )

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    Modifier
                        .size(24.dp)
                        .background(Color(0xFF2B2B30))
                        .background(current)
                        .border(1.dp, DarkenedBorder),
                )
                BasicTextField(
                    value = hexField,
                    onValueChange = { typed ->
                        // Text that was pasted next to the old value is read on its own
                        val inserted =
                            when {
                                typed.text.length - hexField.text.length <= 1 && !typed.text.contains('(') -> null
                                typed.text.endsWith(hexField.text) -> typed.text.removeSuffix(hexField.text)
                                typed.text.startsWith(hexField.text) -> typed.text.removePrefix(hexField.text)
                                else -> typed.text
                            }
                        hexField = typed
                        inserted?.let(::applyText)
                    },
                    singleLine = true,
                    textStyle = TextStyle(color = TextForeground, fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                    cursorBrush = SolidColor(TextForeground),
                    decorationBox = { inner ->
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(
                                    24.dp,
                                ).background(FrameBackground)
                                .border(1.dp, DarkenedBorder)
                                .padding(horizontal = 6.dp),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            inner()
                        }
                    },
                    modifier =
                        Modifier.weight(1f).focusRequester(focus).onPreviewKeyEvent { ev ->
                            if ((ev.key == Key.Enter || ev.key == Key.NumPadEnter) && ev.type == KeyEventType.KeyDown) {
                                applyText(hexField.text)
                                true
                            } else {
                                false
                            }
                        },
                )
                Text("${(alpha * 100).toInt()}%", color = GhostlyText, fontSize = 11.sp)
            }
            Text(stringResource(Res.string.paste_a_colour_from_the_browser_rgb_hex), color = GhostlyText, fontSize = 10.sp)

            if (documentColors.isNotEmpty()) {
                Text(stringResource(Res.string.colours_of_this_layout), color = GhostlyText, fontSize = 10.sp)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    documentColors.chunked(6).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            row.forEach { used ->
                                Box(Modifier.weight(1f)) {
                                    Tip(used.toHex()) {
                                        Box(
                                            Modifier
                                                .fillMaxWidth()
                                                .height(20.dp)
                                                .background(Color(0xFF2B2B30))
                                                .background(used)
                                                .border(1.dp, DarkenedBorder)
                                                .clickable(remember { MutableInteractionSource() }, null) {
                                                    set(used)
                                                    alpha = used.alpha
                                                    finish()
                                                },
                                        )
                                    }
                                }
                            }
                            repeat(6 - row.size) { Box(Modifier.weight(1f)) }
                        }
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                PRESETS.chunked(6).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        row.forEach { preset ->
                            Box(Modifier.weight(1f)) {
                                Tip(preset.toHex()) {
                                    Box(
                                        Modifier
                                            .fillMaxWidth()
                                            .height(20.dp)
                                            .background(preset)
                                            .border(1.dp, DarkenedBorder)
                                            .clickable(remember { MutableInteractionSource() }, null) {
                                                set(preset.copy(alpha = alpha))
                                                finish()
                                            },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Slider(
    brush: Brush,
    fraction: Float,
    onPick: (Float) -> Unit,
    onDone: () -> Unit,
    backdrop: Boolean = false,
    tooltip: String? = null,
) {
    Tip(tooltip) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(14.dp)
                .border(1.dp, DarkenedBorder)
                .dragPick({ p, size -> onPick((p.x / size.width).coerceIn(0f, 1f)) }, onDone),
        ) {
            Canvas(Modifier.fillMaxSize()) {
                if (backdrop) {
                    val cell = 7f
                    var y = 0f
                    var row = 0
                    while (y < size.height) {
                        var x = 0f
                        var col = row % 2
                        while (x < size.width) {
                            if (col % 2 ==
                                0
                            ) {
                                drawRect(Color(0xFF3A3A3F), Offset(x, y), Size(cell, cell))
                            } else {
                                drawRect(Color(0xFF2B2B30), Offset(x, y), Size(cell, cell))
                            }
                            x += cell
                            col++
                        }
                        y += cell
                        row++
                    }
                }
                drawRect(brush)
                val x = fraction * size.width
                drawRect(Color.White, Offset(x - 2f, 0f), Size(4f, size.height))
                drawRect(Color.Black, Offset(x - 2f, 0f), Size(4f, size.height), style = Stroke(1f))
            }
        }
    }
}

/** Reports the position of a press and of the moves after it, and when the button is released. */
private fun Modifier.dragPick(
    onPick: (Offset, IntSize) -> Unit,
    onDone: () -> Unit,
): Modifier =
    pointerInput(Unit) {
        awaitPointerEventScope {
            var down = false
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull() ?: continue
                when (event.type) {
                    PointerEventType.Press -> {
                        down = true
                        onPick(change.position, size)
                        change.consume()
                    }

                    PointerEventType.Move -> {
                        if (down) {
                            onPick(change.position, size)
                            change.consume()
                        }
                    }

                    PointerEventType.Release -> {
                        if (down) {
                            down = false
                            onDone()
                            change.consume()
                        }
                    }

                    else -> {}
                }
            }
        }
    }
