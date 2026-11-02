package com.github.demandss.dayz_workbench_tool.layout_editor.ui.canvas

import androidx.compose.ui.geometry.Offset
import com.github.demandss.dayz_workbench_tool.layout.WidgetType
import com.github.demandss.dayz_workbench_tool.layout_editor.EditorWidgetNode
import java.util.UUID

/** Put the smallest drawn widget first, then offer the other hit boxes from small to large. */
internal fun preferredLayerHits(
    hits: List<EditorWidgetNode>,
    layout: TreeLayout,
    hasPicture: (EditorWidgetNode) -> Boolean,
): List<EditorWidgetNode> {
    if (hits.size < 2) return hits

    fun area(node: EditorWidgetNode): Float = layout.rects[node.id]?.let { it.width * it.height } ?: Float.POSITIVE_INFINITY

    fun drawsContent(node: EditorWidgetNode): Boolean {
        if (node.prop("color")?.let { parseWidgetColor(it).alpha <= 0f } == true) return false
        return when (node.node.type) {
            WidgetType.IMAGE -> hasPicture(node)
            WidgetType.TEXT, WidgetType.MULTILINE_TEXT, WidgetType.RICH_TEXT -> node.shownText().isNotEmpty()
            else -> false
        }
    }

    val preferred = hits.filter(::drawsContent).minByOrNull(::area) ?: hits.minBy(::area)
    return listOf(preferred) + hits.filterNot { it.id == preferred.id }.sortedBy(::area)
}

/** The next widget in the ordered stack, wrapping back to the first one. */
internal fun nextLayerHit(
    hits: List<EditorWidgetNode>,
    selected: UUID,
): EditorWidgetNode? {
    if (hits.size < 2) return null
    val index = hits.indexOfFirst { it.id == selected }
    return if (index < 0) null else hits[(index + 1) % hits.size]
}

internal data class LayerClickPair(
    val selectedAtStart: UUID?,
)

/** Groups plain primary clicks into pairs and remembers which layer was selected before the first click. */
internal class LayerClickCycle(
    private val doubleClickMillis: Long,
    private val maxDistance: Float = 6f,
) {
    private data class Spot(
        val position: Offset,
        val document: String,
    )

    private data class Click(
        val spot: Spot,
        val timeMillis: Long,
        val selectedLayerAtStart: UUID?,
    )

    private data class Cycle(
        val document: String,
        val hitIds: List<UUID>,
        val selected: UUID,
    )

    private var lastClick: Click? = null
    private var cycle: Cycle? = null

    /** A missed single click does not discard the place reached in this stack. */
    fun nextTarget(
        hits: List<EditorWidgetNode>,
        selectedAtStart: UUID?,
        document: String?,
    ): EditorWidgetNode? {
        if (document == null || hits.size < 2) return null
        val hitIds = hits.map { it.id }
        val previous = cycle?.takeIf { it.document == document && it.hitIds == hitIds }
        val target =
            when {
                previous != null -> nextLayerHit(hits, previous.selected)
                selectedAtStart == hits.first().id -> nextLayerHit(hits, selectedAtStart)
                else -> hits.first()
            } ?: return null
        cycle = Cycle(document, hitIds, target.id)
        return target
    }

    fun secondPress(
        timeMillis: Long,
        position: Offset,
        document: String?,
    ): LayerClickPair? {
        val previous = lastClick
        lastClick = null
        if (previous == null || document == null) return null
        if (!previous.spot.matches(position, document) || timeMillis - previous.timeMillis !in 0..doubleClickMillis) return null
        return LayerClickPair(previous.selectedLayerAtStart)
    }

    fun rememberClick(
        timeMillis: Long,
        position: Offset,
        document: String?,
        selectedLayerAtStart: UUID?,
    ) {
        lastClick = document?.let { Click(Spot(position, it), timeMillis, selectedLayerAtStart) }
    }

    /** Cancel a pending double click without forgetting the last layer in its stack. */
    fun reset() {
        lastClick = null
    }

    private fun Spot.matches(
        position: Offset,
        document: String,
    ): Boolean = this.document == document && (this.position - position).getDistance() <= maxDistance
}
