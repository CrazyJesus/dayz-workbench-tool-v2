package com.github.demandss.dayz_workbench_tool.layout_editor.ui.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.*
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import com.github.demandss.dayz_workbench_tool.i18n.str
import com.github.demandss.dayz_workbench_tool.layout.WidgetType
import com.github.demandss.dayz_workbench_tool.layout_editor.EditorWidgetNode
import com.github.demandss.dayz_workbench_tool.layout_editor.behavior.BehaviorEffect
import com.github.demandss.dayz_workbench_tool.layout_editor.behavior.Behaviors
import com.github.demandss.dayz_workbench_tool.layout_editor.behavior.PreviewDrag
import com.github.demandss.dayz_workbench_tool.layout_editor.behavior.VanillaPreview
import com.github.demandss.dayz_workbench_tool.layout_editor.behavior.VanillaScripts
import com.github.demandss.dayz_workbench_tool.layout_editor.behavior.expandSpawners
import com.github.demandss.dayz_workbench_tool.layout_editor.behavior.previewEffects
import com.github.demandss.dayz_workbench_tool.layout_editor.behavior.scriptClassName
import com.github.demandss.dayz_workbench_tool.layout_editor.behavior.spawnerClick
import com.github.demandss.dayz_workbench_tool.layout_editor.behavior.withEffectProperties
import com.github.demandss.dayz_workbench_tool.layout_editor.findNode
import com.github.demandss.dayz_workbench_tool.layout_editor.pathTo
import com.github.demandss.dayz_workbench_tool.layout_editor.state.CanvasMenu
import com.github.demandss.dayz_workbench_tool.layout_editor.state.LayoutEditorState
import com.github.demandss.dayz_workbench_tool.layout_editor.state.Resolution
import com.github.demandss.dayz_workbench_tool.layout_editor.state.WidgetSkin
import com.github.demandss.dayz_workbench_tool.layout_editor.updateNode
import com.github.demandss.dayz_workbench_tool.resources.*
import kotlinx.coroutines.delay
import java.awt.Cursor
import java.util.UUID
import kotlin.math.round
import kotlin.math.roundToInt

// ---------------------------------------------------------------------------
// Pure helpers
// ---------------------------------------------------------------------------

private fun worldToScreen(
    world: Offset,
    zoom: Float,
    pan: Offset,
) = world * zoom + pan

private fun screenToWorld(
    screen: Offset,
    zoom: Float,
    pan: Offset,
) = (screen - pan) / zoom

/** The ratio of the texture of a picture, once it is loaded. */
private fun textureAspect(
    state: LayoutEditorState,
    node: EditorWidgetNode,
): Float? {
    val texture = state.pictureFor(node, state.activeFile?.path) ?: return null
    return if (texture.height > 0) texture.width.toFloat() / texture.height else null
}

/** The rectangle of the screen the layout is drawn on: the chosen resolution, or the real screen for FitToWindow. */
internal fun screenRect(state: LayoutEditorState): Rect {
    val r = state.selectedResolution
    return if (r == Resolution.FIT) {
        Rect(0f, 0f, state.screenSize.width, state.screenSize.height)
    } else {
        Rect(0f, 0f, r.width.toFloat(), r.height.toFloat())
    }
}

private fun hitTest(
    node: EditorWidgetNode,
    worldPos: Offset,
    layout: TreeLayout,
    hidden: Set<UUID>,
    locked: Set<UUID>,
): EditorWidgetNode? {
    if (!node.isVisible() || node.id in hidden) return null
    val rect = layout.rects[node.id] ?: return null
    val inside = rect.contains(worldPos)
    // Children that stick out of a parent which does not clip them can still be hit
    if (!inside && node.clipsChildren()) return null
    for (child in node.drawOrder().asReversed()) {
        hitTest(child, worldPos, layout, hidden, locked)?.let { return it }
    }
    return if (inside && node.id !in locked) node else null
}

/** Every widget under [worldPos], the topmost first, for the context menu and double-click layer selection. */
internal fun hitsAt(
    node: EditorWidgetNode,
    point: Offset,
    layout: TreeLayout,
    hidden: Set<UUID>,
    out: MutableList<EditorWidgetNode>,
    // How far a script class has moved a widget (and with it everything in it) away from where the layout puts it
    shift: (EditorWidgetNode) -> Offset = { Offset.Zero },
) {
    if (!node.isVisible() || node.id in hidden) return
    val rect = layout.rects[node.id] ?: return
    val worldPos = point - shift(node)
    val inside = rect.contains(worldPos)
    if (!inside && node.clipsChildren()) return
    for (child in node.drawOrder().asReversed()) hitsAt(child, worldPos, layout, hidden, out, shift)
    if (inside) out += node
}

/** The button under [worldPos] that takes the pointer, the topmost one; null when there is none. */
private fun buttonAt(
    node: EditorWidgetNode,
    point: Offset,
    layout: TreeLayout,
    shift: (EditorWidgetNode) -> Offset = { Offset.Zero },
): EditorWidgetNode? {
    if (!node.isVisible()) return null
    val rect = layout.rects[node.id] ?: return null
    val worldPos = point - shift(node)
    val inside = rect.contains(worldPos)
    if (!inside && node.clipsChildren()) return null
    for (child in node.drawOrder().asReversed()) buttonAt(child, worldPos, layout, shift)?.let { return it }
    val takes = node.node.type == WidgetType.BUTTON && node.node.properties["ignorepointer"]?.trim() != "1"
    return if (inside && takes) node else null
}

/** Pointer events of Live Preview: hover gives Highlight, a held button Pushed, a click leaves the button focused. */
private fun previewPointer(
    state: LayoutEditorState,
    type: PointerEventType,
    primaryDown: Boolean,
    world: Offset,
) {
    val doc = state.activeFile ?: return
    // The layout with the widgets that the script classes build in it
    val tree = expandSpawners(doc.root, state.previewBehaviorState)
    val layout = layoutOf(state, tree, screenRect(state))
    // Widgets that a script class has moved are hit where they are shown
    val moved = previewEffects(tree, emptySet(), emptySet(), states = state.previewBehaviorState)
    val shift = { n: EditorWidgetNode -> moved[n.id]?.offset ?: Offset.Zero }
    val target = buttonAt(tree, world, layout, shift)?.id
    // The topmost widget that takes the pointer and the widgets around it
    val hits = mutableListOf<EditorWidgetNode>()
    hitsAt(tree, world, layout, emptySet(), hits, shift)
    val top = hits.firstOrNull { it.prop("ignorepointer") != "1" }
    val path = top?.let { tree.pathTo(it.id) }.orEmpty()
    val chain = path.map { it.id }.toSet()
    when (type) {
        PointerEventType.Press -> {
            if (primaryDown) {
                state.previewHover = target
                state.previewPressed = target
                // A click into an edit box with EditFilter puts the keyboard there
                val typing = path.lastOrNull { owner -> owner.scriptClassName()?.let { Behaviors.find(it)?.name } == "EditFilter" }
                state.previewFocus = typing?.id ?: target
                state.previewPressChain = chain
                state.previewPressTimes = state.previewPressTimes + chain.associateWith { state.previewClock }
                // A drag starts on the widget that carries DragMove
                path.lastOrNull { owner -> owner.scriptClassName()?.let { Behaviors.find(it)?.name } == "DragMove" }?.let { owner ->
                    val moved = state.previewBehaviorState[owner.id].orEmpty()
                    state.previewDrag =
                        PreviewDrag(owner.id, world, moved["dx"]?.toFloatOrNull() ?: 0f, moved["dy"]?.toFloatOrNull() ?: 0f)
                }
                // A click is seen by every script class that builds widgets and has the clicked widget inside it
                if (top != null) {
                    for (owner in path) {
                        val before = state.previewBehaviorState[owner.id].orEmpty()
                        spawnerClick(owner, top.node.name, before)?.let { state.previewBehaviorState += (owner.id to it) }
                    }
                    // The same for the classes of the game that answer to clicks (tabs): they see the widgets from the owner down to the click
                    path.forEachIndexed { index, owner ->
                        val before = state.previewBehaviorState[owner.id].orEmpty()
                        VanillaPreview.click(owner, path.drop(index + 1), before)?.let { state.previewBehaviorState += (owner.id to it) }
                    }
                }
            }
        }

        PointerEventType.Release -> {
            state.previewPressed = null
            state.previewHover = target
            state.previewPressChain = emptySet()
        }

        PointerEventType.Exit -> {
            state.previewHover = null
            state.previewPressed = null
            state.previewPressChain = emptySet()
        }

        else -> {
            state.previewHover = target
        }
    }
    val drag = state.previewDrag
    if (drag != null) {
        if (type == PointerEventType.Release || type == PointerEventType.Exit || !primaryDown) {
            state.previewDrag = null
        } else if (type != PointerEventType.Press) {
            val moved = world - drag.start
            state.previewBehaviorState +=
                (drag.owner to mapOf("dx" to (drag.startX + moved.x).toString(), "dy" to (drag.startY + moved.y).toString()))
        }
    }
    val hovered = if (type == PointerEventType.Exit) emptySet() else chain
    if (state.previewHoverChain != hovered) state.previewHoverChain = hovered
}

// ---------------------------------------------------------------------------
// Drag state
// ---------------------------------------------------------------------------

// How far the pointer has to move before a press on a widget turns into a drag
private const val DRAG_THRESHOLD = 3f

private data class WidgetDragInfo(
    val id: UUID,
    val origPosX: Float,
    val origPosY: Float,
    val hexactpos: Boolean,
    val vexactpos: Boolean,
    val signX: Float,
    val signY: Float,
    val parentRect: Rect,
)

// ---------------------------------------------------------------------------
// Draw helpers
// ---------------------------------------------------------------------------

/**
 * The grid over the work area: a line every [gridSize] pixels of the screen it is made for and a brighter one every fifth.
 * Lines that would lie closer than 6 screen pixels are left out (every second, fourth... one is kept), so a small step does
 * not turn the area into a solid fill when the view is far away.
 */
private fun DrawScope.drawGrid(
    gridSize: Int,
    refW: Float,
    refH: Float,
    zoom: Float,
    pan: Offset,
) {
    var every = 1
    while (gridSize * every * zoom < 6f && every < 4096) every *= 2
    val step = gridSize.coerceAtLeast(1) * every
    // Difference with white darkens a light widget and lightens a dark one, so the grid shows on both
    val minor = Color(0x38FFFFFF)
    val major = Color(0x66FFFFFF)
    val top = pan.y
    val bottom = refH * zoom + pan.y
    val left = pan.x
    val right = refW * zoom + pan.x
    var i = 0
    var x = 0f
    while (x <= refW) {
        val sx = (x * zoom + pan.x).roundToInt() + 0.5f
        drawLine(if (i % 5 == 0) major else minor, Offset(sx, top), Offset(sx, bottom), 1f, blendMode = BlendMode.Difference)
        x += step
        i++
    }
    i = 0
    var y = 0f
    while (y <= refH) {
        val sy = (y * zoom + pan.y).roundToInt() + 0.5f
        drawLine(if (i % 5 == 0) major else minor, Offset(left, sy), Offset(right, sy), 1f, blendMode = BlendMode.Difference)
        y += step
        i++
    }
}

private val MeasureColor = Color(0xFFF2994A)

/** Draws each gap with a tick at both ends and its size in a chip on the line. */
private fun DrawScope.drawEdgeDistances(
    distances: List<EdgeDistance>,
    textMeasurer: TextMeasurer,
    zoom: Float,
    pan: Offset,
) {
    val tick = 4f
    val pad = 3f
    for (d in distances) {
        val a = worldToScreen(d.from, zoom, pan)
        val b = worldToScreen(d.to, zoom, pan)
        drawLine(MeasureColor, a, b, 1f)
        val horizontal = d.from.y == d.to.y
        for (end in listOf(a, b)) {
            if (horizontal) {
                drawLine(MeasureColor, Offset(end.x, end.y - tick), Offset(end.x, end.y + tick), 1f)
            } else {
                drawLine(MeasureColor, Offset(end.x - tick, end.y), Offset(end.x + tick, end.y), 1f)
            }
        }
        val label = textMeasurer.measure(formatNumber(round(d.value * 10f) / 10f), TextStyle(color = Color(0xFF1E1F22), fontSize = 10.sp))
        val chip = Size(label.size.width + pad * 2, label.size.height + pad * 2)
        val topLeft = Offset((a.x + b.x) / 2f - chip.width / 2f, (a.y + b.y) / 2f - chip.height / 2f)
        drawRect(MeasureColor, topLeft, chip)
        drawText(label, topLeft = Offset(topLeft.x + pad, topLeft.y + pad))
    }
}

internal fun DrawScope.drawWidgetTree(
    node: EditorWidgetNode,
    layout: TreeLayout,
    isRoot: Boolean,
    selectedIds: Set<UUID>,
    hidden: Set<UUID>,
    showBorders: Boolean,
    showLabels: Boolean,
    textMeasurer: TextMeasurer,
    zoom: Float,
    pan: Offset,
    textureFor: (String) -> ImageBitmap?,
    skinFor: (EditorWidgetNode) -> WidgetSkin? = { null },
    styleKnown: (EditorWidgetNode) -> Boolean = { false },
    pictureFor: (EditorWidgetNode) -> ImageBitmap? = { null },
    textColorFor: (EditorWidgetNode) -> Color = { Color.White },
    // The alpha this widget ends up with: its own colour alpha times that of its parent when it has `inheritalpha 1`
    parentAlpha: Float = 1f,
    // The SDF font a text widget uses, if it has one, and the name of the font it uses
    fontFor: (EditorWidgetNode, Float) -> GameFont? = { _, _ -> null },
    fontNameFor: (EditorWidgetNode) -> String = { "" },
    // What a script class does to a widget in Live Preview: scale and shift around its centre, drawn together with its children
    effectFor: (EditorWidgetNode) -> BehaviorEffect? = { null },
) {
    if (!node.isVisible() || node.id in hidden) return
    val wRect = layout.rects[node.id] ?: return
    val inherited = if (node.prop("inheritalpha") == "1") parentAlpha else 1f
    val ownAlpha = (node.prop("color")?.let { parseWidgetColor(it).alpha } ?: 1f) * inherited
    val tl = worldToScreen(Offset(wRect.left, wRect.top), zoom, pan)
    val br = worldToScreen(Offset(wRect.right, wRect.bottom), zoom, pan)
    val effect = effectFor(node)
    if (effect != null && effect.moves) {
        val pivot = Offset((tl.x + br.x) / 2f, (tl.y + br.y) / 2f)
        // A faded widget is drawn into a layer together with its children, so that they fade as one
        val layerPaint = if (effect.alpha < 1f) Paint().apply { alpha = effect.alpha.coerceAtLeast(0f) } else null
        if (layerPaint != null) drawIntoCanvas { it.saveLayer(Rect(Offset.Zero, size), layerPaint) }
        withTransform({
            translate(effect.offset.x * zoom, effect.offset.y * zoom)
            scale(effect.scale, effect.scale, pivot)
        }) {
            drawWidgetTree(
                node,
                layout,
                isRoot,
                selectedIds,
                hidden,
                showBorders,
                showLabels,
                textMeasurer,
                zoom,
                pan,
                textureFor,
                skinFor,
                styleKnown,
                pictureFor,
                textColorFor,
                parentAlpha,
                fontFor,
                fontNameFor,
            ) { if (it.id == node.id) null else effectFor(it) }
        }
        if (layerPaint != null) drawIntoCanvas { it.restore() }
        return
    }
    // Far outside of the view nothing can be seen (a script class moves a widget by far less than this), and a parent that cuts its
    // children off hides the whole subtree: this is what makes a zoomed view of a big layout cheap to draw and to drag
    val margin = 1000f
    if ((br.x < -margin || br.y < -margin || tl.x > size.width + margin || tl.y > size.height + margin) && node.clipsChildren()) return
    val ss = Size((br.x - tl.x).coerceAtLeast(0f), (br.y - tl.y).coerceAtLeast(0f))
    val isSelected = selectedIds.contains(node.id)

    // A vanilla style with images is drawn from them; everything else falls back to the flat approximation below
    val skinned = node.node.type in SKINNED_TYPES || node.node.originalClassName == "SimpleProgressBarWidgetClass"
    val skin = if (skinned) skinFor(node) else null
    // A vanilla style that paints nothing is not a reason to guess a fill
    val paintsNothing = skin == null && skinned && styleKnown(node)
    if (skin != null) drawSkin(skin, node, tl, ss, zoom, inherited)

    // Fill by type
    when (if (skin != null || paintsNothing) null else node.node.type) {
        null -> {}

        WidgetType.PANEL, WidgetType.SMART_PANEL, WidgetType.WRAP_SPACER, WidgetType.GRID_SPACER, WidgetType.SCROLL, WidgetType.WINDOW -> {
            // Only what the style paints: a widget that paints nothing is found by its border, not by a tint that would add up
            // with the tints of the widgets above and below it
            val fill = node.fillColor()?.let { it.copy(alpha = it.alpha * inherited) }
            if (fill != null) drawRect(fill, tl, ss)
        }

        WidgetType.FRAME -> {}

        WidgetType.IMAGE -> {
            val texture = pictureFor(node)
            val blank = node.prop("image0")?.trim('"').isNullOrEmpty() && node.prop("imageTexture")?.trim('"').isNullOrEmpty()
            if (texture != null && ss.width >= 1f && ss.height >= 1f) {
                drawImageWidget(node, texture, tl, ss, zoom, inherited)
            } else if (blank) {
                // A picture with no image is a white rectangle in the Workbench preview, tinted by its colour; the editor shows
                // it the same way, so that the colours of the layout can be seen while it is edited
                val tint = node.prop("color")?.let { parseWidgetColor(it) } ?: Color.White
                drawRect(tint.copy(alpha = tint.alpha * inherited), tl, ss)
            }
            // A picture whose image is named but not found draws nothing: no tint and no crossed lines over what lies below
        }

        WidgetType.TEXT, WidgetType.MULTILINE_TEXT, WidgetType.RICH_TEXT -> {
            val label = node.node.properties["text"] ?: node.node.name
            if (node.shownText().isNotEmpty()) {
                drawWidgetText(
                    node,
                    tl,
                    ss,
                    zoom,
                    textMeasurer,
                    textColorFor(node).let { it.copy(alpha = it.alpha * inherited) },
                    fontAt = { px -> fontFor(node, px) },
                    fontName = fontNameFor(node),
                )
            } else if (showLabels && WidgetNames.shown && label.isNotEmpty() && ss.width > 4f && ss.height > 8f) {
                val measured =
                    textMeasurer.measure(
                        label,
                        TextStyle(color = Color(0xDDFFFFFF), fontSize = 11.sp, shadow = LabelShadow),
                    )
                drawText(measured, topLeft = Offset(tl.x + 2f, tl.y + 1f))
            }
        }

        WidgetType.BUTTON -> {
            val label = node.node.properties["text"] ?: node.node.name
            if (node.shownText().isNotEmpty()) {
                drawWidgetText(
                    node,
                    tl,
                    ss,
                    zoom,
                    textMeasurer,
                    textColorFor(node).let { it.copy(alpha = it.alpha * inherited) },
                    centered = true,
                    fontAt = { px -> fontFor(node, px) },
                    fontName = fontNameFor(node),
                )
            } else if (showLabels && WidgetNames.shown && label.isNotEmpty() && ss.width > 4f && ss.height > 8f) {
                val measured =
                    textMeasurer.measure(
                        label,
                        TextStyle(color = Color(0xDDCCCCFF), fontSize = 11.sp, shadow = LabelShadow),
                    )
                drawText(measured, topLeft = Offset(tl.x + 2f, tl.y + 1f))
            }
        }

        else -> {}
    }

    // A button with a skin (or a style that paints nothing) still shows its text
    if (node.node.type == WidgetType.BUTTON && (skin != null || paintsNothing) && node.shownText().isNotEmpty()) {
        drawWidgetText(
            node,
            tl,
            ss,
            zoom,
            textMeasurer,
            textColorFor(node).let { it.copy(alpha = it.alpha * inherited) },
            centered = true,
            fontAt = { px -> fontFor(node, px) },
            fontName = fontNameFor(node),
        )
    }

    // An edit box shows the text that is in it, after a small margin
    // A password box with `hide text` shows nothing at all in the Workbench preview (measured)
    val textHidden = node.node.type == WidgetType.PASSWORD_EDIT_BOX && node.prop("hide text")?.trim() == "1"
    if ((node.node.type == WidgetType.EDIT_BOX || node.node.type == WidgetType.PASSWORD_EDIT_BOX) && node.shownText().isNotEmpty() &&
        !textHidden
    ) {
        val margin = 6f * zoom
        drawWidgetText(
            node,
            Offset(tl.x + margin, tl.y),
            Size((ss.width - margin * 2).coerceAtLeast(0f), ss.height),
            zoom,
            textMeasurer,
            textColorFor(node).let { it.copy(alpha = it.alpha * inherited) },
            centered = false,
            fontAt = { px -> fontFor(node, px) },
            fontName = fontNameFor(node),
            verticalCenter = true,
        )
    }

    // A combo box shows its first item between the arrows, whatever `current item` says (measured in Workbench 1.30)
    if (node.node.type == WidgetType.XCOMBO_BOX) {
        val first =
            node
                .prop("items")
                ?.trim()
                ?.trim('"')
                ?.split(';')
                ?.firstOrNull()
                ?.trim()
                .orEmpty()
        if (first.isNotEmpty()) {
            val shown =
                node.copy(
                    node = node.node.copy(properties = LinkedHashMap(node.node.properties).also { it["text"] = "\"$first\"" }),
                )
            drawWidgetText(
                shown,
                tl,
                ss,
                zoom,
                textMeasurer,
                textColorFor(node).let { it.copy(alpha = it.alpha * inherited) },
                centered = true,
                fontAt = { px -> fontFor(node, px) },
                fontName = fontNameFor(node),
            )
        }
    }

    // The label of a check box starts after its box (measured: the box is 22 px wide, the text begins 6 px after it)
    if ((node.node.type == WidgetType.CHECKBOX || node.node.type == WidgetType.THREE_STATE_CHECKBOX) && node.shownText().isNotEmpty()) {
        val boxWidth = ((skin?.native?.get("CheckBox")?.width ?: 22f) + 6f) * zoom
        drawWidgetText(
            node,
            Offset(tl.x + boxWidth, tl.y),
            Size((ss.width - boxWidth).coerceAtLeast(0f), ss.height),
            zoom,
            textMeasurer,
            textColorFor(node).let { it.copy(alpha = it.alpha * inherited) },
            centered = false,
            fontAt = { px -> fontFor(node, px) },
            fontName = fontNameFor(node),
            verticalCenter = true,
        )
    }

    // Border
    val showBorder = showBorders || isSelected
    if (showBorder && ss.width > 0f && ss.height > 0f) {
        if (isSelected) {
            drawRect(Color(0xFF4A9EFF), tl, ss, style = Stroke(2f))
        } else {
            // A dark line with a light one inside it: seen on the grey of the work area, on a dark widget and on a light one
            drawRect(Color(0x99000000), tl, ss, style = Stroke(1f))
            if (ss.width > 3f && ss.height > 3f) {
                drawRect(Color(0x66FFFFFF), Offset(tl.x + 1f, tl.y + 1f), Size(ss.width - 2f, ss.height - 2f), style = Stroke(1f))
            }
        }
    }

    // Widget type label (screen-space, fixed 10sp, only when big enough)
    if (showLabels && WidgetNames.shown && !isRoot && ss.width > 40f && ss.height > 14f) {
        val className =
            node.node.type.className
                .removeSuffix("WidgetClass")
        val label = if (node.node.name.isNotEmpty()) "$className · ${node.node.name}" else className
        val measured =
            textMeasurer.measure(
                label,
                TextStyle(color = Color(0xDDFFFFFF), fontSize = 9.sp, shadow = LabelShadow),
            )
        drawText(measured, topLeft = Offset(tl.x + 2f, tl.y + 2f))
    }

    // Recurse children: by priority, and cut at the parent's edge unless clipchildren is 0
    val children = node.drawOrder()
    if (node.clipsChildren()) {
        clipRect(tl.x, tl.y, br.x, br.y) {
            for (child in children) {
                drawWidgetTree(
                    child,
                    layout,
                    false,
                    selectedIds,
                    hidden,
                    showBorders,
                    showLabels,
                    textMeasurer,
                    zoom,
                    pan,
                    textureFor,
                    skinFor,
                    styleKnown,
                    pictureFor,
                    textColorFor,
                    ownAlpha,
                    fontFor,
                    fontNameFor,
                    effectFor,
                )
            }
        }
    } else {
        for (child in children) {
            drawWidgetTree(
                child,
                layout,
                false,
                selectedIds,
                hidden,
                showBorders,
                showLabels,
                textMeasurer,
                zoom,
                pan,
                textureFor,
                skinFor,
                styleKnown,
                pictureFor,
                textColorFor,
                ownAlpha,
                fontFor,
                fontNameFor,
                effectFor,
            )
        }
    }

    // Selection resize handles
    if (isSelected && !isRoot && ss.width > 8f && ss.height > 8f) {
        val h = 6f
        for (corner in listOf(tl, Offset(br.x - h, tl.y), Offset(tl.x, br.y - h), Offset(br.x - h, br.y - h))) {
            drawRect(Color(0xFF4A9EFF), corner, Size(h, h))
            drawRect(Color(0xFF1A3A6E), corner, Size(h, h), style = Stroke(1f))
        }
    }
}

// ---------------------------------------------------------------------------
// Composable
// ---------------------------------------------------------------------------

@Composable
fun LayoutCanvas(
    state: LayoutEditorState,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer(cacheSize = 256)
    var spacePressed by remember { mutableStateOf(false) }
    var selectionBox by remember { mutableStateOf<Rect?>(null) }
    val focusRequester = remember { FocusRequester() }

    val resolution = state.selectedResolution
    val refW = if (resolution == Resolution.FIT) state.screenSize.width else resolution.width.toFloat()
    val refH = if (resolution == Resolution.FIT) state.screenSize.height else resolution.height.toFloat()

    // A layout that is opened (or switched to) is shown at 100% or fitted to the window, as the settings say; a change of the resolution fits the screen into the window again
    var shownIndex by remember { mutableStateOf(-2) }
    LaunchedEffect(state.activeFileIndex, resolution) {
        if (state.lastCanvasSize == Size.Zero) return@LaunchedEffect
        if (state.activeFileIndex != shownIndex) {
            shownIndex = state.activeFileIndex
            state.viewOnOpen()
        } else {
            state.centerView()
        }
    }
    // The clock of the script classes that play over time. It only ticks while the open layout has one, so that others cost nothing
    LaunchedEffect(state.livePreview) {
        if (!state.livePreview) return@LaunchedEffect
        state.previewClock = 0f
        state.previewPressTimes = emptyMap()
        state.previewBehaviorState = emptyMap()
        val start = System.nanoTime()
        var seen: Any? = null
        var timed = false
        while (true) {
            val root = state.activeFile?.root
            if (root !== seen) {
                seen = root
                timed = root?.let { Behaviors.hasTimed(it) } ?: false
            }
            if (timed) {
                withFrameNanos { state.previewClock = (System.nanoTime() - start) / 1_000_000_000f }
            } else {
                delay(250)
            }
        }
    }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    LaunchedEffect(state.canvasFocusTick) { if (state.canvasFocusTick > 0) runCatching { focusRequester.requestFocus() } }

    Canvas(
        modifier =
            modifier
                .clipToBounds()
                .focusRequester(focusRequester)
                .focusable()
                .onKeyEvent { ev ->
                    if (ev.key == Key.Spacebar) {
                        spacePressed = ev.type == KeyEventType.KeyDown
                        true
                    } else if (handleCanvasKey(state, ev)) {
                        true
                    } else {
                        false
                    }
                }.pointerHoverIcon(resizeCursor(state.hoverHandle))
                .onSizeChanged {
                    val newSize = Size(it.width.toFloat(), it.height.toFloat())
                    val first = state.lastCanvasSize == Size.Zero
                    state.lastCanvasSize = newSize
                    if (first && state.activeFile != null) {
                        shownIndex = state.activeFileIndex
                        state.viewOnOpen()
                    }
                }.pointerInput(Unit) {
                    awaitPointerEventScope {
                        var isPanning = false
                        var panStartScreen = Offset.Zero
                        var panStartOffset = Offset.Zero

                        var isDragging = false
                        var dragStartScreen = Offset.Zero
                        var dragInfos: List<WidgetDragInfo> = emptyList()
                        var dragStartRoot: EditorWidgetNode? = null
                        var dragUnion: Rect? = null
                        var dragCandidates: List<Rect> = emptyList()
                        var undoPushed = false

                        // Resizing: which handle, the rectangle at the start and what the widget sits in
                        var resizeHandle: ResizeHandle? = null
                        var resizeId: UUID? = null
                        var resizeStart: Rect? = null
                        var resizeParent: Rect? = null
                        var resizeRoot: EditorWidgetNode? = null
                        var resizeCandidates: List<Rect> = emptyList()
                        var resizePushed = false
                        var resizeStartScreen = Offset.Zero

                        var isBoxSelecting = false

                        // A press on a background waits: a drag makes a frame of selection, a release picks the widget
                        var pendingSelect: UUID? = null
                        var boxStartWorld = Offset.Zero
                        var currentBox: Rect? = null

                        val layerClicks = LayerClickCycle(viewConfiguration.doubleTapTimeoutMillis)
                        var primaryClickEligible = false
                        var primaryClickStart = Offset.Zero
                        var primaryClickMoved = false
                        var primaryClickWasPair = false
                        var primaryClickStartedOnLayer: UUID? = null

                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Main)
                            val pos = event.changes.firstOrNull()?.position ?: continue
                            val zoom = state.zoom
                            val pan = state.panOffset

                            // Live Preview: the pointer only drives the widgets, the wheel and the middle button still move the view
                            if (state.livePreview && event.type != PointerEventType.Scroll && !isPanning &&
                                !event.buttons.isTertiaryPressed && !spacePressed
                            ) {
                                if (event.type == PointerEventType.Press) layerClicks.reset()
                                // A click gives the canvas the keyboard, so that typing goes into the field that was clicked
                                if (event.type == PointerEventType.Press) runCatching { focusRequester.requestFocus() }
                                previewPointer(state, event.type, event.buttons.isPrimaryPressed, screenToWorld(pos, zoom, pan))
                                continue
                            }

                            when (event.type) {
                                PointerEventType.Scroll -> {
                                    if (!event.keyboardModifiers.isCtrlPressed) {
                                        // The wheel moves the view, with Shift sideways, as in Figma
                                        val scroll = event.changes.firstOrNull()?.scrollDelta ?: Offset.Zero
                                        val sideways = event.keyboardModifiers.isShiftPressed
                                        val shift =
                                            if (sideways) {
                                                Offset(-(scroll.y + scroll.x) * 40f, 0f)
                                            } else {
                                                Offset(
                                                    -scroll.x * 40f,
                                                    -scroll.y * 40f,
                                                )
                                            }
                                        state.panOffset += shift
                                        event.changes.forEach { it.consume() }
                                        continue
                                    }
                                    val dy =
                                        event.changes
                                            .firstOrNull()
                                            ?.scrollDelta
                                            ?.y ?: 0f
                                    if (dy == 0f) continue
                                    val factor = if (dy > 0) 0.9f else 1.1f
                                    val newZoom = (zoom * factor).coerceIn(0.1f, 8f)
                                    val worldUnder = screenToWorld(pos, zoom, pan)
                                    state.zoom = newZoom
                                    state.panOffset = pos - worldUnder * newZoom
                                    event.changes.forEach { it.consume() }
                                }

                                PointerEventType.Press -> {
                                    val isMiddle = event.buttons.isTertiaryPressed
                                    val isPrimary = event.buttons.isPrimaryPressed
                                    val plainPrimary =
                                        isPrimary && !isMiddle && !event.buttons.isSecondaryPressed && !spacePressed &&
                                            !event.keyboardModifiers.isShiftPressed && !event.keyboardModifiers.isCtrlPressed &&
                                            !event.keyboardModifiers.isAltPressed
                                    primaryClickEligible = plainPrimary
                                    primaryClickStart = pos
                                    primaryClickMoved = false
                                    primaryClickWasPair = false
                                    primaryClickStartedOnLayer = null
                                    if (!plainPrimary) layerClicks.reset()

                                    val pressDoc = state.activeFile?.takeIf { plainPrimary }
                                    val pressLayout = pressDoc?.let { layoutOf(state, it.root, screenRect(state)) }
                                    val pressHits = mutableListOf<EditorWidgetNode>()
                                    if (pressDoc != null && pressLayout != null) {
                                        hitsAt(
                                            pressDoc.root,
                                            screenToWorld(pos, zoom, pan),
                                            pressLayout,
                                            state.hiddenIds.toSet(),
                                            pressHits,
                                        )
                                    }
                                    primaryClickStartedOnLayer =
                                        state.selectedIds.singleOrNull()?.takeIf { selected -> pressHits.any { it.id == selected } }
                                    val clickPair =
                                        if (plainPrimary) {
                                            layerClicks.secondPress(event.changes.first().uptimeMillis, pos, pressDoc?.path)
                                        } else {
                                            null
                                        }
                                    primaryClickWasPair = clickPair != null
                                    val cycleTarget =
                                        if (clickPair != null && pressDoc != null && pressLayout != null) {
                                            layerClicks.nextTarget(
                                                preferredLayerHits(pressHits, pressLayout) { state.pictureFor(it, pressDoc.path) != null },
                                                clickPair.selectedAtStart,
                                                pressDoc.path,
                                            )
                                        } else {
                                            null
                                        }
                                    when {
                                        event.buttons.isSecondaryPressed -> {
                                            runCatching { focusRequester.requestFocus() }
                                            val doc = state.activeFile
                                            if (doc != null) {
                                                val layout = layoutOf(state, doc.root, screenRect(state))
                                                val hits = mutableListOf<EditorWidgetNode>()
                                                hitsAt(doc.root, screenToWorld(pos, zoom, pan), layout, state.hiddenIds.toSet(), hits)
                                                state.canvasMenu = if (hits.isEmpty()) null else CanvasMenu(pos, hits)
                                            }
                                            event.changes.forEach { it.consume() }
                                        }

                                        isMiddle || (isPrimary && spacePressed) -> {
                                            isPanning = true
                                            panStartScreen = pos
                                            panStartOffset = pan
                                            event.changes.forEach { it.consume() }
                                        }

                                        cycleTarget != null && pressDoc != null -> {
                                            runCatching { focusRequester.requestFocus() }
                                            state.selectBy(cycleTarget.id, emptyList(), SelectMode.REPLACE)
                                            event.changes.forEach { it.consume() }
                                        }

                                        isPrimary && state.hoverHandle != null && state.selectedIds.size == 1 &&
                                            resizeTargetFor(state) != null -> {
                                            runCatching { focusRequester.requestFocus() }
                                            val target = resizeTargetFor(state)!!
                                            resizeHandle = state.hoverHandle
                                            resizeId = target.id
                                            resizeStart = target.rect
                                            resizeParent = target.parent
                                            resizeRoot = target.root
                                            resizeCandidates = target.candidates
                                            resizePushed = false
                                            resizeStartScreen = pos
                                            event.changes.forEach { it.consume() }
                                        }

                                        isPrimary -> {
                                            runCatching { focusRequester.requestFocus() }
                                            val worldPos = screenToWorld(pos, zoom, pan)
                                            val doc = state.activeFile
                                            val refRect = screenRect(state)
                                            val layout = pressLayout ?: doc?.let { layoutOf(state, it.root, refRect) }
                                            val hit =
                                                if (plainPrimary) {
                                                    pressHits.firstOrNull { it.id !in state.lockedIds }
                                                } else if (doc != null &&
                                                    layout != null
                                                ) {
                                                    hitTest(doc.root, worldPos, layout, state.hiddenIds.toSet(), state.lockedIds.toSet())
                                                } else {
                                                    null
                                                }
                                            val hitIsRoot = hit?.id == doc?.root?.id

                                            val plainPress =
                                                !event.keyboardModifiers.isShiftPressed && !event.keyboardModifiers.isCtrlPressed &&
                                                    !event.keyboardModifiers.isAltPressed
                                            if (doc != null && layout != null && hit != null && !hitIsRoot && plainPress &&
                                                hit.id !in state.selectedIds &&
                                                coversOthers(doc.root, hit.id, layout, state.hiddenIds.toSet(), state.lockedIds.toSet())
                                            ) {
                                                pendingSelect = hit.id
                                                isBoxSelecting = true
                                                boxStartWorld = worldPos
                                                currentBox = null
                                                selectionBox = null
                                                event.changes.forEach { it.consume() }
                                            } else if (doc != null && hit != null && !hitIsRoot) {
                                                // Ctrl adds or removes one widget, Shift adds too; a plain click on a widget outside the selection replaces it
                                                if (event.keyboardModifiers.isShiftPressed || event.keyboardModifiers.isCtrlPressed) {
                                                    state.selectBy(hit.id, emptyList(), SelectMode.TOGGLE)
                                                } else if (!state.selectedIds.contains(hit.id)) {
                                                    state.selectBy(hit.id, emptyList(), SelectMode.REPLACE)
                                                }
                                                // Alt drags a copy and leaves the original where it is
                                                var dragDoc = doc
                                                var dragLayout = layout
                                                if (event.keyboardModifiers.isAltPressed) {
                                                    state.duplicateSelected(0f)
                                                    dragDoc = state.activeFile ?: doc
                                                    dragLayout = layoutOf(state, dragDoc.root, refRect)
                                                }
                                                val parentRects = dragLayout?.parentRects.orEmpty()
                                                dragInfos =
                                                    state.selectedIds.mapNotNull { id ->
                                                        val n = dragDoc.root.findNode(id) ?: return@mapNotNull null
                                                        val pr = parentRects[id] ?: refRect
                                                        val pp = (n.node.properties["position"] ?: "0 0").trim().split(Regex("\\s+"))
                                                        WidgetDragInfo(
                                                            id = id,
                                                            origPosX = pp.getOrNull(0)?.toFloatOrNull() ?: 0f,
                                                            origPosY = pp.getOrNull(1)?.toFloatOrNull() ?: 0f,
                                                            hexactpos = n.node.properties["hexactpos"] == "1",
                                                            vexactpos = n.node.properties["vexactpos"] == "1",
                                                            signX = anchorDirection(n.node.properties["halign"]),
                                                            signY = anchorDirection(n.node.properties["valign"]),
                                                            parentRect = pr,
                                                        )
                                                    }
                                                // What the dragged widgets can snap to: everything that is not part of the selection
                                                val moving =
                                                    state.selectedIds
                                                        .flatMap { id ->
                                                            dragDoc.root
                                                                .findNode(id)
                                                                ?.allIds()
                                                                .orEmpty()
                                                        }.toSet()
                                                val selectedRects = state.selectedIds.mapNotNull { dragLayout?.rects?.get(it) }
                                                dragUnion =
                                                    selectedRects.takeIf { it.isNotEmpty() }?.let { rs ->
                                                        Rect(
                                                            rs.minOf { it.left },
                                                            rs.minOf { it.top },
                                                            rs.maxOf { it.right },
                                                            rs.maxOf { it.bottom },
                                                        )
                                                    }
                                                dragCandidates =
                                                    dragLayout
                                                        ?.rects
                                                        ?.filter { (id, _) -> id !in moving && id !in state.hiddenIds }
                                                        ?.values
                                                        ?.toList()
                                                        .orEmpty()
                                                isDragging = true
                                                dragStartScreen = pos
                                                dragStartRoot = dragDoc.root
                                                undoPushed = false
                                                event.changes.forEach { it.consume() }
                                            } else {
                                                if (!event.keyboardModifiers.isShiftPressed &&
                                                    !event.keyboardModifiers.isCtrlPressed
                                                ) {
                                                    state.selectedIds.clear()
                                                }
                                                isBoxSelecting = true
                                                boxStartWorld = worldPos
                                                currentBox = null
                                                selectionBox = null
                                            }
                                        }
                                    }
                                }

                                PointerEventType.Move -> {
                                    if (primaryClickEligible && (pos - primaryClickStart).getDistance() >= DRAG_THRESHOLD) {
                                        primaryClickMoved = true
                                        layerClicks.reset()
                                    }
                                    when {
                                        isPanning -> {
                                            state.panOffset = panStartOffset + (pos - panStartScreen)
                                            event.changes.forEach { it.consume() }
                                        }

                                        resizeHandle != null -> {
                                            val handle = resizeHandle
                                            val start = resizeStart!!
                                            if (!resizePushed && (pos - resizeStartScreen).getDistance() >= DRAG_THRESHOLD) {
                                                resizeRoot?.let { state.pushUndo(state.activeFileIndex, it, str(Res.string.resize)) }
                                                resizePushed = true
                                            }
                                            if (resizePushed) {
                                                val delta = (pos - resizeStartScreen) / zoom
                                                var rect =
                                                    resizedRect(
                                                        start,
                                                        handle,
                                                        delta,
                                                        keepAspect = event.keyboardModifiers.isShiftPressed,
                                                        fromCentre = event.keyboardModifiers.isAltPressed,
                                                    )
                                                if (!event.keyboardModifiers.isCtrlPressed) {
                                                    val (snapped, guides) = snapResized(rect, handle, resizeCandidates, 6f / zoom)
                                                    rect = snapped
                                                    state.snapGuides = guides
                                                } else {
                                                    state.snapGuides = emptyList()
                                                }
                                                val doc = state.activeFile
                                                val parent = resizeParent
                                                val id = resizeId
                                                if (doc != null && parent != null && id != null) {
                                                    state.updateActiveRoot(withRect(doc.root, id, rect, parent))
                                                    state.resizeLabel =
                                                        rect to "${formatNumber(round(rect.width))} × ${formatNumber(round(rect.height))}"
                                                }
                                            }
                                            event.changes.forEach { it.consume() }
                                        }

                                        isDragging && dragInfos.isNotEmpty() -> {
                                            // A plain click only selects. The first real step of a drag is what counts as an edit
                                            if (!undoPushed && (pos - dragStartScreen).getDistance() >= DRAG_THRESHOLD) {
                                                dragStartRoot?.let {
                                                    state.pushUndo(
                                                        state.activeFileIndex,
                                                        it,
                                                        if (dragInfos.size >
                                                            1
                                                        ) {
                                                            str(Res.string.move_x_widgets, dragInfos.size)
                                                        } else {
                                                            str(Res.string.move)
                                                        },
                                                    )
                                                }
                                                undoPushed = true
                                            }
                                            var worldDelta = (pos - dragStartScreen) / zoom
                                            // Positions in pixels are whole, so the measured gaps are as well
                                            if (dragInfos.all { it.hexactpos }) worldDelta = Offset(round(worldDelta.x), worldDelta.y)
                                            if (dragInfos.all { it.vexactpos }) worldDelta = Offset(worldDelta.x, round(worldDelta.y))
                                            // Snap to the neighbours. Ctrl switches it off
                                            val union = dragUnion
                                            if (union != null && undoPushed && !event.keyboardModifiers.isCtrlPressed) {
                                                val screenRect = screenRect(state)
                                                // The edges of the screen are snapped to, but no gap is measured to them
                                                val snap = snapMove(union, worldDelta, dragCandidates, 6f / zoom) { it != screenRect }
                                                worldDelta += Offset(snap.dx, snap.dy)
                                                // The top left corner goes to the grid where no neighbour was snapped to
                                                if (state.snapToGrid && snap.guides.isEmpty()) {
                                                    val g = state.gridSize.coerceAtLeast(1).toFloat()
                                                    worldDelta =
                                                        Offset(
                                                            round((union.left + worldDelta.x) / g) * g - union.left,
                                                            round((union.top + worldDelta.y) / g) * g - union.top,
                                                        )
                                                }
                                                state.snapGuides = snap.guides
                                                state.snapDistances = snap.distances
                                            } else {
                                                state.snapGuides = emptyList()
                                                state.snapDistances = emptyList()
                                            }
                                            val doc = state.activeFile
                                            if (doc != null && undoPushed) {
                                                var newRoot = doc.root
                                                for (info in dragInfos) {
                                                    val nx =
                                                        info.origPosX +
                                                            info.signX *
                                                            if (info.hexactpos) worldDelta.x else worldDelta.x / info.parentRect.width
                                                    val ny =
                                                        info.origPosY +
                                                            info.signY *
                                                            if (info.vexactpos) worldDelta.y else worldDelta.y / info.parentRect.height
                                                    newRoot =
                                                        newRoot.updateNode(info.id) { n ->
                                                            n.copy(
                                                                node =
                                                                    n.node.copy(
                                                                        properties =
                                                                            LinkedHashMap(n.node.properties).also {
                                                                                it["position"] =
                                                                                    "${formatNumber(
                                                                                        if (info.hexactpos) round(nx) else nx,
                                                                                    )} ${formatNumber(
                                                                                        if (info.vexactpos) round(ny) else ny,
                                                                                    )}"
                                                                            },
                                                                    ),
                                                            )
                                                        }
                                                }
                                                state.updateActiveRoot(newRoot)
                                            }
                                            event.changes.forEach { it.consume() }
                                        }

                                        isBoxSelecting -> {
                                            val wp = screenToWorld(pos, zoom, pan)
                                            val box =
                                                Rect(
                                                    left = minOf(boxStartWorld.x, wp.x),
                                                    top = minOf(boxStartWorld.y, wp.y),
                                                    right = maxOf(boxStartWorld.x, wp.x),
                                                    bottom = maxOf(boxStartWorld.y, wp.y),
                                                )
                                            currentBox = box
                                            selectionBox = box
                                        }

                                        else -> {
                                            // No button is down: remember where the pointer is for the Alt measurement
                                            state.hoverWorld = screenToWorld(pos, zoom, pan)
                                            state.altHeld = event.keyboardModifiers.isAltPressed
                                            state.hoverHandle =
                                                if (state.selectedIds.size == 1 && !spacePressed) {
                                                    resizeTargetFor(state)?.let { handleAt(it.rect, state.hoverWorld!!, 6f / zoom) }
                                                } else {
                                                    null
                                                }
                                        }
                                    }
                                }

                                PointerEventType.Release -> {
                                    if (isBoxSelecting) {
                                        val box = currentBox
                                        val doc = state.activeFile
                                        // A frame smaller than a drag is a click
                                        val dragged = box != null && maxOf(box.width, box.height) * state.zoom >= DRAG_THRESHOLD
                                        val pending = pendingSelect
                                        pendingSelect = null
                                        if (pending != null && !dragged) {
                                            state.selectBy(pending, emptyList(), SelectMode.REPLACE)
                                        } else if (box != null && dragged && doc != null) {
                                            val hits = mutableListOf<UUID>()
                                            selectInBox(
                                                doc.root,
                                                box,
                                                layoutOf(state, doc.root, screenRect(state)),
                                                hits,
                                                state.hiddenIds.toSet(),
                                                state.lockedIds.toSet(),
                                                isRoot = true,
                                            )
                                            hits.remove(doc.root.id)
                                            if (!event.keyboardModifiers.isShiftPressed &&
                                                !event.keyboardModifiers.isCtrlPressed
                                            ) {
                                                state.selectedIds.clear()
                                            }
                                            for (id in hits) if (!state.selectedIds.contains(id)) state.selectedIds.add(id)
                                        }
                                        selectionBox = null
                                        currentBox = null
                                    }
                                    if (primaryClickEligible) {
                                        when {
                                            primaryClickWasPair -> {}

                                            !primaryClickMoved && !undoPushed && !resizePushed && !isPanning -> {
                                                layerClicks.rememberClick(
                                                    event.changes.first().uptimeMillis,
                                                    pos,
                                                    state.activeFile?.path,
                                                    primaryClickStartedOnLayer,
                                                )
                                            }

                                            else -> {
                                                layerClicks.reset()
                                            }
                                        }
                                    }
                                    primaryClickEligible = false
                                    isPanning = false
                                    resizeHandle = null
                                    state.resizeLabel = null
                                    isDragging = false
                                    state.snapGuides = emptyList()
                                    state.snapDistances = emptyList()
                                    isBoxSelecting = false
                                    dragInfos = emptyList()
                                }

                                else -> {}
                            }
                        }
                    }
                },
    ) {
        val zoom = state.zoom
        val pan = state.panOffset
        val doc = state.activeFile
        val selectedSet = state.selectedIds.toHashSet()
        WidgetNames.shown = state.showWidgetNames

        // 1. Infinite dark background
        drawRect(Color(0xFF1A1B1E))

        // 2. Grid (screen-space lines aligned to world grid)

        // 3. Shadow behind work area
        val wtl = worldToScreen(Offset(-6f, -6f), zoom, pan)
        val wbr = worldToScreen(Offset(refW + 6f, refH + 6f), zoom, pan)
        drawRect(Color(0x66000000), wtl, Size(wbr.x - wtl.x, wbr.y - wtl.y))

        // 4. Work area
        val atl = worldToScreen(Offset.Zero, zoom, pan)
        val abr = worldToScreen(Offset(refW, refH), zoom, pan)
        val aSize = Size(abr.x - atl.x, abr.y - atl.y)
        // Live Preview shows the widgets that script classes build; the file itself does not have them
        val tree = doc?.root?.let { if (state.livePreview) expandSpawners(it, state.previewBehaviorState) else it }
        val layout = doc?.let { d -> tree?.let { layoutOf(state, it, Rect(0f, 0f, refW, refH)) } }

        fun DrawScope.widgets(pan: Offset) {
            if (doc == null || layout == null || tree == null) return
            val effects =
                if (state.livePreview) {
                    previewEffects(
                        tree,
                        state.previewHoverChain,
                        state.previewPressChain,
                        state.previewClock,
                        state.previewPressTimes,
                        state.previewBehaviorState,
                        state.previewFocus,
                    )
                } else {
                    emptyMap()
                }
            drawWidgetTree(
                tree.withEffectProperties(effects),
                layout,
                isRoot = true,
                if (state.livePreview) emptySet() else selectedSet,
                state.hiddenIds.toHashSet(),
                state.showBorders && !state.livePreview,
                !state.livePreview,
                textMeasurer,
                zoom,
                pan,
                state::textureFor,
                { n -> state.skinFor(n, doc.path, state.previewStateOf(n)) },
                { n -> state.styleKnown(n, doc.path) },
                { n -> state.pictureFor(n, doc.path) },
                { n -> state.textColorFor(n, doc.path) },
                fontFor = { n, px -> state.gameFontFor(state.fontNameFor(n, doc.path), doc.path, px) },
                fontNameFor = { n -> state.fontNameFor(n, doc.path) },
                effectFor = { n -> effects[n.id] },
            )
        }
        if (state.livePreview && !state.workbenchColors) {
            // The game: colours are sRGB values and blend as they are
            drawRect(PREVIEW_GREY, atl, aSize)
            drawRect(Color(0xFF555860), atl, aSize, style = Stroke(1f))
            widgets(pan)
        } else if (state.livePreview) {
            // Workbench blends in linear light, so Live Preview is painted into a linear buffer and shown from it: a black
            // image with the alpha of a vignette comes out as dark as in the Workbench preview
            drawLinear(atl, aSize) {
                drawRect(PREVIEW_GREY)
                widgets(Offset.Zero)
            }
            drawRect(Color(0xFF555860), atl, aSize, style = Stroke(1f))
        } else {
            // The same grey as Live Preview, so that the colours of the layout look the way they do there: dark and translucent ones
            // disappear on a dark background
            drawRect(PREVIEW_GREY, atl, aSize)
            drawRect(Color(0xFF555860), atl, aSize, style = Stroke(1f))
            // 5. Widgets, with the grid over them so that no widget hides it
            widgets(pan)
            if (state.showGrid) drawGrid(state.gridSize, refW, refH, zoom, pan)
        }

        // 6. Safe frame overlay
        if (state.showSafeFrame) {
            val sx = refW * 0.05f
            val sy = refH * 0.05f
            val stl = worldToScreen(Offset(sx, sy), zoom, pan)
            val sbr = worldToScreen(Offset(refW - sx, refH - sy), zoom, pan)
            drawRect(Color(0x22FFFF44), stl, Size(sbr.x - stl.x, sbr.y - stl.y))
            drawRect(Color(0x88FFFF44), stl, Size(sbr.x - stl.x, sbr.y - stl.y), style = Stroke(1f))
        }

        // The Workbench screenshot over the work area, under the measurements
        val reference = state.referenceImage
        if (state.showReference && reference != null) {
            drawImage(
                reference,
                srcOffset = IntOffset.Zero,
                srcSize = IntSize(reference.width, reference.height),
                dstOffset = IntOffset(atl.x.roundToInt(), atl.y.roundToInt()),
                dstSize = IntSize(aSize.width.roundToInt().coerceAtLeast(1), aSize.height.roundToInt().coerceAtLeast(1)),
                alpha = state.referenceOpacity,
                filterQuality = FilterQuality.Medium,
            )
        }

        // Gaps between the selected widgets and the edges of their parents
        if (doc != null && layout != null && state.showMeasure && !state.livePreview && selectedSet.isNotEmpty()) {
            for (id in selectedSet) {
                val node = doc.root.findNode(id) ?: continue
                val rect = layout.rects[id]
                val parent = layout.parentRects[id]
                if (node.id == doc.root.id || rect == null || parent == null) continue
                val props = node.node.properties
                drawEdgeDistances(
                    edgeDistances(rect, parent, props["halign"], props["valign"]),
                    textMeasurer,
                    zoom,
                    pan,
                )
            }
        }

        // Snap guides while dragging
        for (g in state.snapGuides) {
            val a = if (g.vertical) worldToScreen(Offset(g.at, g.from), zoom, pan) else worldToScreen(Offset(g.from, g.at), zoom, pan)
            val b = if (g.vertical) worldToScreen(Offset(g.at, g.to), zoom, pan) else worldToScreen(Offset(g.to, g.at), zoom, pan)
            drawLine(Color(0xFFFF4FA3), a, b, 1f)
        }
        // The gaps to the widgets that the dragged one snapped to, in layout pixels
        if (state.snapDistances.isNotEmpty()) drawEdgeDistances(state.snapDistances, textMeasurer, zoom, pan)

        // While a widget is resized its new size shows under it
        state.resizeLabel?.let { (rect, text) ->
            val label = textMeasurer.measure(text, TextStyle(color = Color.White, fontSize = 10.sp))
            val bl = worldToScreen(Offset(rect.center.x, rect.bottom), zoom, pan)
            val chip = Size(label.size.width + 10f, label.size.height + 6f)
            val topLeft = Offset(bl.x - chip.width / 2f, bl.y + 8f)
            drawRect(Color(0xFF346BD8), topLeft, chip)
            drawText(label, topLeft = Offset(topLeft.x + 5f, topLeft.y + 3f))
        }

        // Alt over another widget: the distances between it and the selected one
        val hover = state.hoverWorld
        if (doc != null && layout != null && state.altHeld && hover != null && selectedSet.size == 1 && !state.livePreview) {
            val selectedId = selectedSet.first()
            val selectedRect = layout.rects[selectedId]
            val target = hitTest(doc.root, hover, layout, state.hiddenIds.toSet() + selectedId, emptySet())
            val targetRect = target?.let { layout.rects[it.id] }
            if (selectedRect != null && targetRect != null) {
                val tl = worldToScreen(Offset(targetRect.left, targetRect.top), zoom, pan)
                val br = worldToScreen(Offset(targetRect.right, targetRect.bottom), zoom, pan)
                drawRect(MeasureColor, tl, Size(br.x - tl.x, br.y - tl.y), style = Stroke(1f))
                drawEdgeDistances(distancesBetween(selectedRect, targetRect), textMeasurer, zoom, pan)
            }
        }

        // 7. Selection box
        selectionBox?.let { box ->
            val stl = worldToScreen(Offset(box.left, box.top), zoom, pan)
            val sbr = worldToScreen(Offset(box.right, box.bottom), zoom, pan)
            val ss = Size(sbr.x - stl.x, sbr.y - stl.y)
            drawRect(Color(0x334A9EFF), stl, ss)
            drawRect(Color(0xFF4A9EFF), stl, ss, style = Stroke(1f))
        }
    }
}

/** What a resize needs to know about the one selected widget. */
private class ResizeTarget(
    val id: UUID,
    val rect: Rect,
    val parent: Rect,
    val root: EditorWidgetNode,
    val candidates: List<Rect>,
)

/** The selected widget with its rectangle, or null when it cannot be resized by hand (root, locked, hidden, nothing). */
private fun resizeTargetFor(state: LayoutEditorState): ResizeTarget? {
    val doc = state.activeFile ?: return null
    val id = state.selectedIds.singleOrNull() ?: return null
    if (id == doc.root.id || id in state.lockedIds || id in state.hiddenIds || state.livePreview) return null
    val layout = layoutOf(state, doc.root, screenRect(state))
    val rect = layout.rects[id] ?: return null
    val parent = layout.parentRects[id] ?: return null
    val own =
        doc.root
            .findNode(id)
            ?.allIds()
            .orEmpty()
            .toSet()
    val others =
        layout.rects
            .filter { (other, _) -> other !in own && other !in state.hiddenIds }
            .values
            .toList()
    return ResizeTarget(id, rect, parent, doc.root, others)
}

/** The mouse cursor for a handle: arrows along the edge or the diagonal. */
internal fun resizeCursor(handle: ResizeHandle?): PointerIcon =
    when (handle) {
        null -> PointerIcon.Default
        ResizeHandle.E -> PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR))
        ResizeHandle.W -> PointerIcon(Cursor(Cursor.W_RESIZE_CURSOR))
        ResizeHandle.N -> PointerIcon(Cursor(Cursor.N_RESIZE_CURSOR))
        ResizeHandle.S -> PointerIcon(Cursor(Cursor.S_RESIZE_CURSOR))
        ResizeHandle.NE -> PointerIcon(Cursor(Cursor.NE_RESIZE_CURSOR))
        ResizeHandle.NW -> PointerIcon(Cursor(Cursor.NW_RESIZE_CURSOR))
        ResizeHandle.SE -> PointerIcon(Cursor(Cursor.SE_RESIZE_CURSOR))
        ResizeHandle.SW -> PointerIcon(Cursor(Cursor.SW_RESIZE_CURSOR))
    }

private val SKINNED_TYPES =
    setOf(
        WidgetType.PANEL,
        WidgetType.SMART_PANEL,
        WidgetType.WRAP_SPACER,
        WidgetType.GRID_SPACER,
        WidgetType.BUTTON,
        WidgetType.WINDOW,
        WidgetType.EDIT_BOX,
        WidgetType.PASSWORD_EDIT_BOX,
        WidgetType.MULTILINE_EDIT_BOX,
        WidgetType.SLIDER,
        WidgetType.PROGRESS_BAR,
        WidgetType.CHECKBOX,
        WidgetType.THREE_STATE_CHECKBOX,
        WidgetType.XCOMBO_BOX,
        WidgetType.TEXT_LISTBOX,
        WidgetType.SERVER_BROWSER,
        WidgetType.SCROLL,
    )

/**
 * Draws the pieces of a style as a 9-slice: corners at their size, edges and centre stretched. The widget's own `color`
 * replaces the colour of the style when it is set.
 */
private fun DrawScope.drawSkin(
    skin: WidgetSkin,
    node: EditorWidgetNode,
    tl: Offset,
    ss: Size,
    zoom: Float,
    alphaFactor: Float = 1f,
) {
    if (ss.width <= 0f || ss.height <= 0f) return
    // A widget thinner than a device pixel (1 px thick at a zoom below 1) still shows as a line of one pixel
    val ss = Size(ss.width.coerceAtLeast(1f), ss.height.coerceAtLeast(1f))
    val baseTint = node.node.properties["color"]?.let { parseWidgetColor(it) } ?: skin.styleColor
    val tint = baseTint.copy(alpha = baseTint.alpha * alphaFactor)
    drawSkinWidget(
        skin,
        tint,
        androidx.compose.ui.geometry
            .Rect(tl.x, tl.y, tl.x + ss.width, tl.y + ss.height),
        zoom,
        node.node.type,
        progress = node.node.type == WidgetType.PROGRESS_BAR || node.node.originalClassName == "SimpleProgressBarWidgetClass",
        share = (node.prop("current")?.toFloatOrNull()?.div(100f) ?: 0.5f).coerceIn(0f, 1f),
        vertical = node.prop("vertical") == "1",
        flipped = node.prop("flipped") == "1",
        sliderShare = sliderShare(node),
        sliderFill = node.prop("fill in") == "1",
        sliderMarker = node.prop("draw marker") == "1",
        markerThickness = node.prop("marker thickness")?.toFloatOrNull() ?: 0f,
    )
}

/** [drawSkin] with the colour given: the style editor draws a style without a widget. */
internal fun DrawScope.drawSkinTinted(
    skin: WidgetSkin,
    tint: Color,
    tl: Offset,
    ss: Size,
    zoom: Float,
) {
    if (ss.width <= 0f || ss.height <= 0f) return
    val ss = Size(ss.width.coerceAtLeast(1f), ss.height.coerceAtLeast(1f))
    val dst =
        androidx.compose.ui.geometry
            .Rect(tl.x, tl.y, tl.x + ss.width, tl.y + ss.height)
    // Every edge is put on a whole pixel: the pieces share their borders, so a zoom that gives them fractions leaves no seams
    val rects =
        sliceRects(dst, skin.native.mapValues { (_, v) -> Size(v.width * zoom, v.height * zoom) }).mapValues { (_, r) ->
            androidx.compose.ui.geometry.Rect(
                r.left.roundToInt().toFloat(),
                r.top.roundToInt().toFloat(),
                r.right.roundToInt().toFloat(),
                r.bottom.roundToInt().toFloat(),
            )
        }
    // The Center first: the edges are drawn over it, and where they have holes it shows through
    rects["Center"]?.let { drawSkinSlot(skin, "Center", it, tint, zoom) }
    for ((slot, r) in rects) if (slot != "Center") drawSkinSlot(skin, slot, r, tint, zoom)
}

/** The tint of a skin as a colour filter: a multiplication by [tint], plus white when the skin is lifted. */
private fun skinFilter(
    skin: WidgetSkin,
    tint: Color,
): ColorFilter = skinFilterFor(skin.lift, tint) { makeSkinFilter(skin, tint) }

private fun makeSkinFilter(
    skin: WidgetSkin,
    tint: Color,
): ColorFilter {
    if (skin.lift <= 0f) return tintFilter(tint)
    val add = skin.lift * 255f
    return ColorFilter.colorMatrix(
        androidx.compose.ui.graphics.ColorMatrix(
            floatArrayOf(
                tint.red,
                0f,
                0f,
                0f,
                add,
                0f,
                tint.green,
                0f,
                0f,
                add,
                0f,
                0f,
                tint.blue,
                0f,
                add,
                0f,
                0f,
                0f,
                tint.alpha,
                0f,
            ),
        ),
    )
}

/** One piece of a skin into [r]: stretched, or repeated at its own size along the axes its flags tile. */
internal fun DrawScope.drawSkinSlot(
    skin: WidgetSkin,
    slot: String,
    r: androidx.compose.ui.geometry.Rect,
    tint: Color,
    zoom: Float,
) {
    run {
        val src = skin.source[slot] ?: return
        if (r.width < 0.5f || r.height < 0.5f) return
        val bits = skin.flags[slot] ?: 0
        val nat = skin.native[slot] ?: return
        val tile = skin.tiles[slot]
        if (bits != 0 && tile != null) {
            // One draw with a repeating shader; a loop of drawImage calls is far too slow for a 2 px dash
            val sx = if (bits and 1 != 0) nat.width * zoom / tile.width else r.width / tile.width
            val sy = if (bits and 2 != 0) nat.height * zoom / tile.height else r.height / tile.height
            if (sx > 0f && sy > 0f) {
                val brush = tileBrush(tile)
                withTransform({
                    translate(r.left, r.top)
                    scale(sx, sy, Offset.Zero)
                }) {
                    drawRect(
                        brush,
                        Offset.Zero,
                        Size(r.width / sx, r.height / sy),
                        colorFilter = skinFilter(skin, tint),
                    )
                }
            }
        } else {
            // The piece cut out of the atlas when there is one; a sub-rectangle of the whole atlas is a slow draw
            drawImage(
                image = tile ?: skin.atlas,
                srcOffset = if (tile != null) IntOffset.Zero else IntOffset(src.left, src.top),
                srcSize = if (tile != null) IntSize(tile.width, tile.height) else IntSize(src.width, src.height),
                dstOffset = IntOffset(r.left.toInt(), r.top.toInt()),
                dstSize = IntSize(r.width.toInt().coerceAtLeast(1), r.height.toInt().coerceAtLeast(1)),
                colorFilter = skinFilter(skin, tint),
            )
        }
    }
}

/** Whether the editor view writes "Type · name" in the corner of every widget; set from the state before a frame is drawn. */
internal object WidgetNames {
    @Volatile
    var shown = true
}

/** The grey the Workbench preview clears its window with. */
private val PREVIEW_GREY = Color(0xFF828282)

// The names of the widgets are read on any background: grey, dark panels and light ones
private val LabelShadow = Shadow(Color(0xFF000000), Offset(1f, 1f), 2f)

private object LinearSurface {
    var bitmap: ImageBitmap? = null
}

/**
 * Draws [content] into a buffer in linear light and puts it at [topLeft]: Skia blends in the colour space of the buffer, so
 * translucent widgets mix the way Workbench (which has an sRGB back buffer) mixes them. The coordinates of [content] start at
 * the top left of the buffer.
 */
private fun DrawScope.drawLinear(
    topLeft: Offset,
    size: Size,
    content: DrawScope.() -> Unit,
) {
    val w = size.width.roundToInt().coerceAtLeast(1)
    val h = size.height.roundToInt().coerceAtLeast(1)
    val current = LinearSurface.bitmap
    val bitmap =
        if (current != null && current.width == w && current.height == h) {
            current
        } else {
            ImageBitmap(w, h, ImageBitmapConfig.F16, true, ColorSpaces.LinearSrgb).also { LinearSurface.bitmap = it }
        }
    val canvas =
        androidx.compose.ui.graphics
            .Canvas(bitmap)
    CanvasDrawScope().draw(this, layoutDirection, canvas, Size(w.toFloat(), h.toFloat())) {
        drawRect(Color.Transparent, blendMode = BlendMode.Clear)
        content()
    }
    drawImage(bitmap, topLeft = Offset(topLeft.x.roundToInt().toFloat(), topLeft.y.roundToInt().toFloat()))
}

/** The layout of a tree on the screen [rect], with the sizes of the pictures and of the texts that follow their text. */
private fun layoutOf(
    state: LayoutEditorState,
    root: EditorWidgetNode,
    rect: Rect,
): TreeLayout {
    // The same tree on the same screen has the same layout: dragging the view asks for it on every frame, so the last one is kept
    val stamp = state.layoutStamp()
    lastLayout?.let { if (it.root === root && it.rect == rect && it.live == state.livePreview && it.stamp == stamp) return it.layout }

    fun place(tree: EditorWidgetNode) =
        layoutTree(
            tree,
            rect,
            { n -> textureAspect(state, n) },
            { n, w, h -> state.textSizeFor(n, state.activeFile?.path, w, h) },
        )
    // Live Preview runs the script classes of the game that size and place widgets, as Workbench does
    val layout = if (state.livePreview && VanillaScripts.present(root)) VanillaScripts.layout(root, ::place) else place(root)
    lastLayout = LayoutMemo(root, rect, state.livePreview, stamp, layout)
    return layout
}

private class LayoutMemo(
    val root: EditorWidgetNode,
    val rect: Rect,
    val live: Boolean,
    val stamp: Int,
    val layout: TreeLayout,
)

private var lastLayout: LayoutMemo? = null

/** How far the value of a slider is between its `minimum` and `maximum`, from 0 to 1. */
private fun sliderShare(node: EditorWidgetNode): Float {
    val minimum = node.prop("minimum")?.toFloatOrNull() ?: 0f
    val maximum = node.prop("maximum")?.toFloatOrNull() ?: 1f
    val current = node.prop("current")?.toFloatOrNull() ?: minimum
    return if (maximum > minimum) ((current - minimum) / (maximum - minimum)).coerceIn(0f, 1f) else 0f
}
