package com.github.demandss.dayz_workbench_tool.layout_editor.ui.panels

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.window.rememberDialogState
import com.github.demandss.dayz_workbench_tool.layout_editor.ui.canvas.ColorSpaceMode
import com.github.demandss.dayz_workbench_tool.layout_editor.ui.canvas.hsvToColor
import com.github.demandss.dayz_workbench_tool.layout_editor.ui.canvas.linearToSrgb
import com.github.demandss.dayz_workbench_tool.layout_editor.ui.canvas.srgbToLinear
import com.github.demandss.dayz_workbench_tool.layout_editor.ui.canvas.toHex
import com.github.demandss.dayz_workbench_tool.layout_editor.ui.canvas.toHsv
import com.github.demandss.dayz_workbench_tool.resources.*
import com.github.demandss.dayz_workbench_tool.ui.components.Tip
import com.github.demandss.dayz_workbench_tool.ui.icons.AppIcon
import com.github.demandss.dayz_workbench_tool.ui.icons.AppIconView
import com.github.demandss.dayz_workbench_tool.ui.theme.DarkenedBorder
import com.github.demandss.dayz_workbench_tool.ui.theme.ElementsBackground
import com.github.demandss.dayz_workbench_tool.ui.theme.FrameBackground
import com.github.demandss.dayz_workbench_tool.ui.theme.GhostlyText
import com.github.demandss.dayz_workbench_tool.ui.theme.SelectColor
import com.github.demandss.dayz_workbench_tool.ui.theme.SubSelectColor
import com.github.demandss.dayz_workbench_tool.ui.theme.TextForeground
import org.jetbrains.compose.resources.stringResource
import java.awt.Dimension
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

private val recentPickerColors = mutableStateListOf<Color>()
private val customPickerColors = mutableStateListOf<Color>()
private val defaultPickerColors =
    (0 until 8).flatMap { row ->
        (0 until 8).map { column ->
            if (row == 0) {
                Color(1f - column / 7f, 1f - column / 7f, 1f - column / 7f)
            } else {
                hsvToColor((row - 1) * 360f / 7f, (0.95f - column * 0.10f).coerceIn(0f, 1f), 1f)
            }
        }
    }

/** Resizable native dialog. Every edit is previewed by the caller; closing restores the caller's original value. */
@Composable
fun ColorPickerWindow(
    initial: Color,
    documentColors: List<Color> = emptyList(),
    onPreview: (Color) -> Unit,
    onConfirm: (Color) -> Unit,
    onCancel: () -> Unit,
) {
    val initialHsv = remember { initial.toHsv() }
    var hue by remember { mutableStateOf(initialHsv.h) }
    var saturation by remember { mutableStateOf(initialHsv.s) }
    var value by remember { mutableStateOf(initialHsv.v) }
    var alpha by remember { mutableStateOf(initial.alpha) }
    var integerRange by remember { mutableStateOf(true) }
    var paletteIndex by remember { mutableStateOf(0) }
    var paletteExpanded by remember { mutableStateOf(false) }
    var sampling by remember { mutableStateOf(false) }
    val originalColor = remember { initial }
    val current = hsvToColor(hue, saturation, value, alpha)
    val red = if (ColorSpaceMode.workbench130) srgbToLinear(current.red) else current.red
    val green = if (ColorSpaceMode.workbench130) srgbToLinear(current.green) else current.green
    val blue = if (ColorSpaceMode.workbench130) srgbToLinear(current.blue) else current.blue

    fun choose(color: Color) {
        val hsv = color.toHsv()
        if (hsv.s > 0f && hsv.v > 0f) hue = hsv.h
        saturation = hsv.s
        value = hsv.v
        alpha = color.alpha
        onPreview(color)
    }

    fun edit(
        h: Float = hue,
        s: Float = saturation,
        v: Float = value,
        a: Float = alpha,
    ) {
        hue = h.coerceIn(0f, 359.999f)
        saturation = s.coerceIn(0f, 1f)
        value = v.coerceIn(0f, 1f)
        alpha = a.coerceIn(0f, 1f)
        onPreview(hsvToColor(hue, saturation, value, alpha))
    }

    fun confirm() {
        recentPickerColors.remove(current)
        recentPickerColors.add(0, current)
        while (recentPickerColors.size > 14) recentPickerColors.removeAt(recentPickerColors.lastIndex)
        onConfirm(current)
    }

    val paletteNames =
        listOf(
            stringResource(Res.string.default_palette),
            stringResource(Res.string.layout_palette),
            stringResource(Res.string.custom_palette),
        )
    val palette =
        when (paletteIndex) {
            1 -> documentColors
            2 -> customPickerColors.toList()
            else -> defaultPickerColors
        }

    DialogWindow(
        onCloseRequest = { if (sampling) sampling = false else onCancel() },
        state = rememberDialogState(size = DpSize(880.dp, 520.dp)),
        title = stringResource(Res.string.pick_a_colour),
        resizable = true,
    ) {
        window.minimumSize = Dimension(740, 470)
        Column(
            Modifier
                .fillMaxSize()
                .background(FrameBackground)
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when {
                        event.key == Key.Escape -> {
                            if (sampling) sampling = false else onCancel()
                            true
                        }

                        else -> {
                            false
                        }
                    }
                },
        ) {
            Row(Modifier.weight(1f).fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1.2f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ColorWheel(hue, saturation, value, Modifier.weight(1f).fillMaxHeight()) { h, s, v -> edit(h = h, s = s, v = v) }
                        VerticalColorTrack(
                            value,
                            listOf(hsvToColor(hue, saturation, 1f), Color.Black),
                            Modifier.width(20.dp).fillMaxHeight(),
                        ) {
                            edit(v = it)
                        }
                        VerticalColorTrack(
                            alpha,
                            listOf(current.copy(alpha = 1f), current.copy(alpha = 0f)),
                            Modifier.width(20.dp).fillMaxHeight(),
                        ) {
                            edit(a = it)
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ColorSample(stringResource(Res.string.new_colour), current, Modifier.weight(1f))
                        ColorSample(stringResource(Res.string.old_colour), originalColor, Modifier.weight(1f))
                    }
                    Tip(stringResource(Res.string.pick_colour_from_screen)) {
                        Box(
                            Modifier
                                .size(26.dp)
                                .background(if (sampling) SubSelectColor else ElementsBackground)
                                .border(1.dp, DarkenedBorder)
                                .clickable(remember { MutableInteractionSource() }, null) { sampling = true },
                            contentAlignment = Alignment.Center,
                        ) {
                            AppIconView(AppIcon.UI_EYEDROPPER, size = 16.dp)
                        }
                    }
                    Text(stringResource(Res.string.recent), color = GhostlyText, fontSize = 10.sp)
                    Row(Modifier.fillMaxWidth().height(22.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        recentPickerColors.take(14).forEach { color ->
                            Swatch(color, Modifier.weight(1f).fillMaxHeight()) { choose(color) }
                        }
                        repeat((14 - recentPickerColors.size).coerceAtLeast(0)) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
                Column(
                    Modifier.weight(0.9f).fillMaxHeight().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(stringResource(Res.string.linear_colour), color = GhostlyText, fontSize = 11.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        PickerButton(
                            if (integerRange) stringResource(Res.string.integer_range) else stringResource(Res.string.float_range),
                        ) {
                            integerRange = !integerRange
                        }
                    }
                    PickerDivider()
                    NumericChannel("H", hue / 360f, 360f, true) { edit(h = it * 360f) }
                    NumericChannel("S", saturation, if (integerRange) 255f else 1f, integerRange) { edit(s = it) }
                    NumericChannel("V", value, if (integerRange) 255f else 1f, integerRange) { edit(v = it) }
                    PickerDivider()
                    NumericChannel("R", red, if (integerRange) 255f else 1f, integerRange) {
                        choose(current.copy(red = if (ColorSpaceMode.workbench130) linearToSrgb(it) else it))
                    }
                    NumericChannel("G", green, if (integerRange) 255f else 1f, integerRange) {
                        choose(current.copy(green = if (ColorSpaceMode.workbench130) linearToSrgb(it) else it))
                    }
                    NumericChannel("B", blue, if (integerRange) 255f else 1f, integerRange) {
                        choose(current.copy(blue = if (ColorSpaceMode.workbench130) linearToSrgb(it) else it))
                    }
                    NumericChannel("A", alpha, if (integerRange) 255f else 1f, integerRange) { edit(a = it) }
                    PickerDivider()
                    HexChannel(current.toHex()) { text ->
                        colorFromPickerText(text, alpha)?.let(::choose)
                    }
                }
                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Box {
                        PickerButton(paletteNames[paletteIndex] + " ▾") { paletteExpanded = true }
                        if (paletteExpanded) {
                            val menuOffset = with(LocalDensity.current) { 24.dp.roundToPx() }
                            Popup(
                                alignment = Alignment.TopStart,
                                offset = IntOffset(0, menuOffset),
                                onDismissRequest = { paletteExpanded = false },
                                properties = PopupProperties(focusable = true),
                            ) {
                                Column(Modifier.width(165.dp).background(ElementsBackground).border(1.dp, DarkenedBorder)) {
                                    paletteNames.forEachIndexed { index, name ->
                                        Box(
                                            Modifier
                                                .fillMaxWidth()
                                                .height(25.dp)
                                                .clickable(remember { MutableInteractionSource() }, null) {
                                                    paletteIndex = index
                                                    paletteExpanded = false
                                                }.padding(horizontal = 8.dp),
                                            contentAlignment = Alignment.CenterStart,
                                        ) {
                                            Text(name, color = TextForeground, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(8),
                        modifier = Modifier.weight(1f).fillMaxWidth().border(1.dp, DarkenedBorder),
                        horizontalArrangement = Arrangement.spacedBy(1.dp),
                        verticalArrangement = Arrangement.spacedBy(1.dp),
                    ) {
                        items(palette) { color -> Swatch(color, Modifier.fillMaxWidth().height(27.dp)) { choose(color) } }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        PickerButton("+") {
                            customPickerColors.add(current)
                            paletteIndex = 2
                        }
                        Spacer(Modifier.width(5.dp))
                        PickerButton("−") {
                            customPickerColors.remove(current)
                            paletteIndex = 2
                        }
                    }
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .background(ElementsBackground)
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PickerButton(stringResource(Res.string.confirm), ::confirm)
                Spacer(Modifier.width(8.dp))
                PickerButton(stringResource(Res.string.cancel), onCancel)
            }
        }
    }
    if (sampling) {
        ScreenColorSampler(
            onPick = {
                choose(it.copy(alpha = alpha))
                sampling = false
            },
            onCancel = { sampling = false },
        )
    }
}

@Composable
private fun ColorWheel(
    hue: Float,
    saturation: Float,
    value: Float,
    modifier: Modifier,
    onPick: (Float, Float, Float) -> Unit,
) {
    val latestValues by rememberUpdatedState(Triple(hue, saturation, value))
    val latestPick by rememberUpdatedState(onPick)

    fun pick(
        position: Offset,
        width: Float,
        height: Float,
    ) {
        val (currentHue, currentSaturation, currentValue) = latestValues
        val radius = min(width, height) / 2f - 4f
        val center = Offset(width / 2f, height / 2f)
        val dx = position.x - center.x
        val dy = position.y - center.y
        if (hypot(dx, dy) > radius - 24f) {
            val degrees = ((atan2(dy, dx) * 180f / PI.toFloat()) + 360f) % 360f
            latestPick(degrees, currentSaturation, currentValue)
        } else {
            val left = center.x - radius * 0.55f
            val top = center.y - radius * 0.60f
            val right = center.x + radius * 0.80f
            val bottom = center.y + radius * 0.60f
            val y = ((position.y - top) / (bottom - top)).coerceIn(0f, 1f)
            val maxHueWeight = 1f - abs(y * 2f - 1f)
            val hueWeight = ((position.x - left) / (right - left)).coerceIn(0f, maxHueWeight)
            val blackWeight = (y - hueWeight / 2f).coerceIn(0f, 1f - hueWeight)
            val v = 1f - blackWeight
            latestPick(currentHue, if (v == 0f) 0f else hueWeight / v, v)
        }
    }
    Canvas(
        modifier
            .pointerInput(Unit) {
                detectTapGestures { pick(it, size.width.toFloat(), size.height.toFloat()) }
            }.pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { pick(it, size.width.toFloat(), size.height.toFloat()) },
                    onDrag = { change, _ ->
                        pick(change.position, size.width.toFloat(), size.height.toFloat())
                        change.consume()
                    },
                )
            },
    ) {
        val radius = min(size.width, size.height) / 2f - 4f
        val center = Offset(size.width / 2f, size.height / 2f)
        val ringWidth = 18.dp.toPx()
        repeat(120) { i ->
            drawArc(
                color = hsvToColor(i * 3f, 1f, 1f),
                startAngle = i * 3f,
                sweepAngle = 3.1f,
                useCenter = false,
                topLeft = Offset(center.x - radius + ringWidth / 2f, center.y - radius + ringWidth / 2f),
                size = Size(2f * radius - ringWidth, 2f * radius - ringWidth),
                style = Stroke(ringWidth, cap = StrokeCap.Butt),
            )
        }
        val left = center.x - radius * 0.55f
        val right = center.x + radius * 0.80f
        val top = center.y - radius * 0.60f
        val bottom = center.y + radius * 0.60f
        var y = top
        while (y <= bottom) {
            val fraction = ((y - top) / (bottom - top)).coerceIn(0f, 1f)
            val hueWeight = 1f - abs(fraction * 2f - 1f)
            val edge = left + (right - left) * hueWeight
            val whiteWeight = 1f - fraction - hueWeight / 2f
            val hueColor = hsvToColor(hue, 1f, 1f)
            val edgeColor =
                Color(
                    hueColor.red * hueWeight + whiteWeight,
                    hueColor.green * hueWeight + whiteWeight,
                    hueColor.blue * hueWeight + whiteWeight,
                )
            drawLine(
                brush = Brush.horizontalGradient(listOf(Color(1f - fraction, 1f - fraction, 1f - fraction), edgeColor), left, edge),
                start = Offset(left, y),
                end = Offset(edge, y),
                strokeWidth = 1.5f,
            )
            y += 1f
        }
        val hueAngle = hue * PI.toFloat() / 180f
        val ringPoint = Offset(center.x + cos(hueAngle) * (radius - ringWidth / 2f), center.y + sin(hueAngle) * (radius - ringWidth / 2f))
        drawCircle(Color.White, 6.dp.toPx(), ringPoint, style = Stroke(2.dp.toPx()))
        drawCircle(Color.Black, 7.dp.toPx(), ringPoint, style = Stroke(1.dp.toPx()))
        val hueWeight = value * saturation
        val blackWeight = 1f - value
        val selected = Offset(left + hueWeight * (right - left), top + (blackWeight + hueWeight / 2f) * (bottom - top))
        drawCircle(Color.White, 5.dp.toPx(), selected, style = Stroke(2.dp.toPx()))
        drawCircle(Color.Black, 6.dp.toPx(), selected, style = Stroke(1.dp.toPx()))
    }
}

@Composable
private fun VerticalColorTrack(
    fraction: Float,
    colors: List<Color>,
    modifier: Modifier,
    onPick: (Float) -> Unit,
) {
    val latestPick by rememberUpdatedState(onPick)
    Canvas(
        modifier
            .pointerInput(Unit) { detectTapGestures { latestPick(1f - it.y / size.height) } }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { latestPick(1f - it.y / size.height) },
                    onDrag = { change, _ ->
                        latestPick(1f - change.position.y / size.height)
                        change.consume()
                    },
                )
            },
    ) {
        drawRect(Brush.verticalGradient(colors))
        val y = (1f - fraction.coerceIn(0f, 1f)) * size.height
        drawRect(Color.White, Offset(0f, y - 2f), Size(size.width, 4f), style = Stroke(2f))
        drawRect(Color.Black, Offset(0f, y - 3f), Size(size.width, 6f), style = Stroke(1f))
    }
}

@Composable
private fun ColorSample(
    label: String,
    color: Color,
    modifier: Modifier,
) {
    Column(modifier) {
        Text(label, color = GhostlyText, fontSize = 10.sp)
        Box(
            Modifier
                .fillMaxWidth()
                .height(38.dp)
                .background(ElementsBackground)
                .background(color)
                .border(1.dp, DarkenedBorder),
        )
    }
}

@Composable
private fun Swatch(
    color: Color,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .background(ElementsBackground)
            .background(color)
            .border(1.dp, DarkenedBorder)
            .clickable(remember { MutableInteractionSource() }, null, onClick = onClick),
    )
}

@Composable
private fun PickerButton(
    label: String,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .height(24.dp)
            .background(ElementsBackground)
            .border(1.dp, DarkenedBorder)
            .clickable(remember { MutableInteractionSource() }, null, onClick = onClick)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = TextForeground, fontSize = 11.sp)
    }
}

@Composable
private fun PickerDivider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(DarkenedBorder))
}

@Composable
private fun NumericChannel(
    label: String,
    normalized: Float,
    scale: Float,
    integer: Boolean,
    onCommit: (Float) -> Unit,
) {
    val display =
        if (integer) {
            (normalized * scale).roundToInt().toString()
        } else {
            String.format(Locale.US, "%.3f", normalized * scale)
        }
    val latestCommit by rememberUpdatedState(onCommit)
    val fieldBackground = ElementsBackground
    val fieldFill = SelectColor.copy(alpha = 0.35f)
    val fieldBorder = DarkenedBorder
    val focus = remember { FocusRequester() }
    var editing by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf(display) }
    var editStart by remember { mutableStateOf(display) }
    var hadFocus by remember { mutableStateOf(false) }
    LaunchedEffect(display) { if (!editing) text = display }
    LaunchedEffect(editing) { if (editing) focus.requestFocus() }

    fun finishEditing() {
        if (!editing) return
        if (text != editStart) text.toFloatOrNull()?.let { latestCommit((it / scale).coerceIn(0f, 1f)) }
        editing = false
        hadFocus = false
    }
    Row(Modifier.fillMaxWidth().height(20.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = GhostlyText, fontSize = 11.sp, modifier = Modifier.width(24.dp))
        Box(Modifier.weight(1f).fillMaxHeight()) {
            Canvas(
                Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { latestCommit((it.x / size.width).coerceIn(0f, 1f)) },
                            onDoubleTap = {
                                editStart = display
                                text = display
                                editing = true
                            },
                        )
                    }.pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { latestCommit((it.x / size.width).coerceIn(0f, 1f)) },
                            onDrag = { change, _ ->
                                latestCommit((change.position.x / size.width).coerceIn(0f, 1f))
                                change.consume()
                            },
                        )
                    },
            ) {
                drawRect(fieldBackground)
                drawRect(fieldFill, size = Size(size.width * normalized.coerceIn(0f, 1f), size.height))
                drawRect(fieldBorder, style = Stroke(1.dp.toPx()))
            }
            if (editing) {
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    textStyle = TextStyle(color = TextForeground, fontSize = 10.sp, fontFamily = FontFamily.Monospace),
                    cursorBrush = SolidColor(SelectColor),
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .focusRequester(focus)
                            .onFocusChanged {
                                if (hadFocus && !it.isFocused) finishEditing()
                                if (it.isFocused) hadFocus = true
                            }.onPreviewKeyEvent {
                                if (it.type == KeyEventType.KeyDown && (it.key == Key.Enter || it.key == Key.NumPadEnter)) {
                                    finishEditing()
                                    true
                                } else {
                                    false
                                }
                            }.padding(horizontal = 6.dp, vertical = 2.dp),
                )
            } else {
                Text(
                    display,
                    color = TextForeground,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.align(Alignment.CenterStart).padding(start = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun HexChannel(
    value: String,
    onCommit: (String) -> Unit,
) {
    var text by remember { mutableStateOf(value) }
    var focused by remember { mutableStateOf(false) }
    var editStart by remember { mutableStateOf(value) }
    LaunchedEffect(value) { if (!focused) text = value }

    fun commit() {
        if (text != editStart) onCommit(text)
        editStart = text
        focused = false
    }
    Row(Modifier.fillMaxWidth().height(20.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("Hex", color = GhostlyText, fontSize = 11.sp, modifier = Modifier.width(24.dp))
        BasicTextField(
            value = text,
            onValueChange = { text = it },
            singleLine = true,
            textStyle = TextStyle(color = TextForeground, fontSize = 11.sp, fontFamily = FontFamily.Monospace),
            cursorBrush = SolidColor(SelectColor),
            modifier =
                Modifier
                    .weight(1f)
                    .background(ElementsBackground)
                    .border(1.dp, DarkenedBorder)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
                    .onFocusChanged {
                        if (focused && !it.isFocused) commit()
                        if (!focused && it.isFocused) editStart = text
                        focused = it.isFocused
                    }.onPreviewKeyEvent {
                        if (it.type == KeyEventType.KeyDown && (it.key == Key.Enter || it.key == Key.NumPadEnter)) {
                            commit()
                            true
                        } else {
                            false
                        }
                    },
        )
    }
}
