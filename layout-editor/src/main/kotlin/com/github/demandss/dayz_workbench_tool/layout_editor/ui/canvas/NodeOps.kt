package com.github.demandss.dayz_workbench_tool.layout_editor.ui.canvas

import androidx.compose.ui.geometry.Rect
import com.github.demandss.dayz_workbench_tool.layout.newWidgetResourceId
import com.github.demandss.dayz_workbench_tool.layout_editor.EditorWidgetNode
import com.github.demandss.dayz_workbench_tool.layout_editor.findNode
import com.github.demandss.dayz_workbench_tool.layout_editor.updateNode
import java.util.UUID
import kotlin.math.round

enum class AlignMode { LEFT, CENTER_H, RIGHT, TOP, CENTER_V, BOTTOM }

/**
 * Moves a widget by a distance in the units of its parent's screen. A position in pixels takes the distance
 * as it is, a share of the parent takes it as a share, and a right or bottom anchor counts the other way.
 */
internal fun EditorWidgetNode.movedBy(
    id: UUID,
    dx: Float,
    dy: Float,
    parent: Rect,
): EditorWidgetNode =
    updateNode(id) { n ->
        val props = n.node.properties
        val pos = (props["position"] ?: "0 0").trim().split(Regex("\\s+"))
        val x = pos.getOrNull(0)?.toFloatOrNull() ?: 0f
        val y = pos.getOrNull(1)?.toFloatOrNull() ?: 0f
        val exactX = props["hexactpos"] == "1"
        val exactY = props["vexactpos"] == "1"
        val nx =
            x + anchorDirection(props["halign"]) *
                if (exactX) {
                    dx
                } else if (parent.width == 0f) {
                    0f
                } else {
                    dx / parent.width
                }
        val ny =
            y + anchorDirection(props["valign"]) *
                if (exactY) {
                    dy
                } else if (parent.height == 0f) {
                    0f
                } else {
                    dy / parent.height
                }
        val newProps = LinkedHashMap(props)
        newProps["position"] = "${formatNumber(if (exactX) round(nx) else nx)} ${formatNumber(if (exactY) round(ny) else ny)}"
        n.copy(node = n.node.copy(properties = newProps))
    }

/** Lines up the widgets [ids] on one edge, or on the middle of the group, and leaves everything else alone. */
internal fun alignedRoot(
    root: EditorWidgetNode,
    ids: List<UUID>,
    layout: TreeLayout,
    mode: AlignMode,
): EditorWidgetNode {
    val rects = ids.mapNotNull { id -> layout.rects[id]?.let { id to it } }
    if (rects.size < 2) return root
    val left = rects.minOf { it.second.left }
    val right = rects.maxOf { it.second.right }
    val top = rects.minOf { it.second.top }
    val bottom = rects.maxOf { it.second.bottom }
    var result = root
    for ((id, r) in rects) {
        val dx =
            when (mode) {
                AlignMode.LEFT -> left - r.left
                AlignMode.CENTER_H -> (left + right) / 2f - r.center.x
                AlignMode.RIGHT -> right - r.right
                else -> 0f
            }
        val dy =
            when (mode) {
                AlignMode.TOP -> top - r.top
                AlignMode.CENTER_V -> (top + bottom) / 2f - r.center.y
                AlignMode.BOTTOM -> bottom - r.bottom
                else -> 0f
            }
        val parent = layout.parentRects[id] ?: continue
        result = result.movedBy(id, dx, dy, parent)
    }
    return result
}

/** A copy of the widget and everything under it, with new ids. */
internal fun EditorWidgetNode.copiedWithNewIds(): EditorWidgetNode =
    EditorWidgetNode(
        UUID.randomUUID(),
        node.copy(resourceId = newWidgetResourceId()),
        children.map { it.copiedWithNewIds() }.toMutableList(),
    )

/** Puts a copy right after the widget [id] among its siblings. Returns the new root and the id of the copy, or null for the root. */
internal fun EditorWidgetNode.withCopyOf(id: UUID): Pair<EditorWidgetNode, UUID>? {
    if (this.id == id) return null
    val index = children.indexOfFirst { it.id == id }
    if (index >= 0) {
        val original = children[index]
        val copy = original.copiedWithNewIds().let { it.copy(node = it.node.copy(name = original.node.name + "_copy")) }
        val list = children.toMutableList().also { it.add(index + 1, copy) }
        return copy(children = list) to copy.id
    }
    for ((i, child) in children.withIndex()) {
        val (newChild, newId) = child.withCopyOf(id) ?: continue
        val list = children.toMutableList().also { it[i] = newChild }
        return copy(children = list) to newId
    }
    return null
}

internal fun EditorWidgetNode.hasNode(id: UUID): Boolean = findNode(id) != null

/** The parent of the widget [id], or null for the root and for an id that is not in the tree. */
internal fun EditorWidgetNode.parentOf(id: UUID): EditorWidgetNode? {
    if (children.any { it.id == id }) return this
    for (child in children) child.parentOf(id)?.let { return it }
    return null
}

/** The names of all widgets in the tree. */
internal fun EditorWidgetNode.allNames(): Set<String> = (listOf(node.name) + children.flatMap { it.allNames() }).toSet()

/** The id of this widget and of everything under it. */
internal fun EditorWidgetNode.allIds(): List<UUID> = listOf(id) + children.flatMap { it.allIds() }
