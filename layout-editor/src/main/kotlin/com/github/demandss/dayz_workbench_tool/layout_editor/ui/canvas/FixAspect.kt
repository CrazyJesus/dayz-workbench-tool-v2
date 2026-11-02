package com.github.demandss.dayz_workbench_tool.layout_editor.ui.canvas

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import com.github.demandss.dayz_workbench_tool.layout_editor.EditorWidgetNode

/*
 * What `fixaspect` does, measured in Workbench 1.30 on panels in containers of three shapes (probe layouts of the
 * test stand, 20 cells):
 *
 * - It acts on a child whose size is relative on both axes. A pixel size is left alone whatever the value is.
 * - The ratio comes from the numbers of `size` (`1 1` is 1:1, `0.5 0.25` is 2:1). For a picture it is the ratio of
 *   the texture, which is an assumption, since the probes had no texture.
 * - `inside` is the largest box of that ratio that fits the parent, `outside` the smallest that covers it,
 *   `fixwidth` keeps the height the size gave and takes the width from the ratio (the name says the opposite).
 * - The resulting box keeps the widget's alignment in the parent, and it may stick out of its parent.
 */

/** The rectangle of [node] after `fixaspect`, given the [rect] its size and position gave and its [parent]. */
internal fun fixAspect(
    node: EditorWidgetNode,
    rect: Rect,
    parent: Rect,
    textureAspect: Float? = null,
): Rect {
    val mode =
        when (node.prop("fixaspect")?.lowercase()) {
            "inside", "true" -> Mode.INSIDE
            "outside" -> Mode.OUTSIDE
            "fixwidth" -> Mode.FIX_WIDTH
            else -> return rect
        }
    if (node.prop("hexactsize") == "1" || node.prop("vexactsize") == "1") return rect

    val size = (node.prop("size") ?: "0.1 0.1").split(Regex("\\s+"))
    val sw = size.getOrNull(0)?.toFloatOrNull() ?: return rect
    val sh = size.getOrNull(1)?.toFloatOrNull() ?: return rect
    val ratio = textureAspect?.takeIf { it > 0f } ?: (if (sh > 0f && sw > 0f) sw / sh else return rect)

    val pw = parent.width
    val ph = parent.height
    // Is the parent wider than the ratio? Then its height limits the box, otherwise its width does
    val parentIsWider = ph > 0f && pw / ph > ratio
    val (w, h) =
        when (mode) {
            Mode.INSIDE -> if (parentIsWider) ph * ratio to ph else pw to pw / ratio
            Mode.OUTSIDE -> if (parentIsWider) pw to pw / ratio else ph * ratio to ph
            Mode.FIX_WIDTH -> rect.height * ratio to rect.height
        }
    return computeRect(node, parent, Size(w, h))
}

private enum class Mode { INSIDE, OUTSIDE, FIX_WIDTH }
