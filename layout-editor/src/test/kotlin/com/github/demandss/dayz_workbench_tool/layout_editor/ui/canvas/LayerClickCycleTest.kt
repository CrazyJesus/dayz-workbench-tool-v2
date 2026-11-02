package com.github.demandss.dayz_workbench_tool.layout_editor.ui.canvas

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.github.demandss.dayz_workbench_tool.layout.LayoutParser
import com.github.demandss.dayz_workbench_tool.layout_editor.EditorWidgetNode
import com.github.demandss.dayz_workbench_tool.layout_editor.toEditorNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class LayerClickCycleTest {
    private val point = Offset(50f, 50f)

    private fun hits(): List<EditorWidgetNode> {
        val root =
            LayoutParser
                .parse(
                    """
                    FrameWidgetClass Root {
                     size 1 1
                     {
                      PanelWidgetClass Back {
                       size 1 1
                      }
                      PanelWidgetClass Front {
                       size 1 1
                       priority 5
                      }
                     }
                    }
                    """.trimIndent(),
                ).toEditorNode()
        val hits = mutableListOf<EditorWidgetNode>()
        hitsAt(root, point, layoutTree(root, Rect(0f, 0f, 100f, 100f)), emptySet(), hits)
        return hits
    }

    @Test
    fun `double clicks follow the context menu order and wrap after the root`() {
        val hits = hits()

        assertEquals(listOf("Front", "Back", "Root"), hits.map { it.node.name })
        assertEquals(hits[1], nextLayerHit(hits, hits[0].id))
        assertEquals(hits[2], nextLayerHit(hits, hits[1].id))
        assertEquals(hits[0], nextLayerHit(hits, hits[2].id))
        assertNull(nextLayerHit(hits, java.util.UUID.randomUUID()))
    }

    @Test
    fun `a pair advances only when a layer was selected before its first click`() {
        val hits = hits()
        val clicks = LayerClickCycle(doubleClickMillis = 500)

        clicks.rememberClick(10, point, "layout", selectedLayerAtStart = null)
        assertNull(assertNotNull(clicks.secondPress(100, point, "layout")).selectedAtStart)
        assertNull(clicks.secondPress(150, point, "layout"))

        clicks.rememberClick(200, point, "layout", selectedLayerAtStart = hits[0].id)
        assertEquals(hits[0].id, clicks.secondPress(300, point, "layout")?.selectedAtStart)
        assertNull(clicks.secondPress(350, point, "layout"))

        clicks.rememberClick(400, point, "layout", selectedLayerAtStart = hits[0].id)
        assertNull(clicks.secondPress(901, point, "layout"))
    }

    @Test
    fun `the next pair advances from the layer selected before its first click`() {
        val hits = hits()
        val clicks = LayerClickCycle(doubleClickMillis = 500)

        clicks.rememberClick(100, point, "layout", selectedLayerAtStart = hits[1].id)
        val previousLayer = assertNotNull(clicks.secondPress(200, point, "layout")?.selectedAtStart)
        assertEquals(hits[1].id, previousLayer)
        assertEquals(hits[2], nextLayerHit(hits, previousLayer))

        clicks.rememberClick(300, point, "layout", selectedLayerAtStart = hits[2].id)
        assertNull(clicks.secondPress(350, Offset(70f, 50f), "layout"))
    }

    @Test
    fun `a double click chooses a small image before a covering background and containers`() {
        val root =
            LayoutParser
                .parse(
                    """
                    FrameWidgetClass Root {
                     size 1 1
                     {
                      FrameWidgetClass Row {
                       size 0.5 0.5
                       {
                        ImageWidgetClass Icon {
                         position 10 10
                         size 20 20
                         hexactpos 1
                         vexactpos 1
                         hexactsize 1
                         vexactsize 1
                         image0 "set:icons image:mark"
                        }
                       }
                      }
                      ImageWidgetClass Background {
                       size 1 1
                       image0 "set:icons image:background"
                      }
                     }
                    }
                    """.trimIndent(),
                ).toEditorNode()
        val layout = layoutTree(root, Rect(0f, 0f, 100f, 100f))
        val hits = mutableListOf<EditorWidgetNode>()
        hitsAt(root, Offset(15f, 15f), layout, emptySet(), hits)

        assertEquals(listOf("Background", "Icon", "Row", "Root"), hits.map { it.node.name })
        val ordered = preferredLayerHits(hits, layout) { it.node.type == com.github.demandss.dayz_workbench_tool.layout.WidgetType.IMAGE }
        assertEquals(listOf("Icon", "Row", "Background", "Root"), ordered.map { it.node.name })

        val clicks = LayerClickCycle(doubleClickMillis = 500)
        assertEquals("Icon", clicks.nextTarget(ordered, hits.first().id, "layout")?.node?.name)
        clicks.reset() // A stray single click elsewhere clears the pending pair, not the layer stack.
        assertEquals("Row", clicks.nextTarget(ordered, null, "layout")?.node?.name)
        assertEquals("Background", clicks.nextTarget(ordered, null, "layout")?.node?.name)
        assertEquals("Root", clicks.nextTarget(ordered, null, "layout")?.node?.name)
        assertEquals("Icon", clicks.nextTarget(ordered, null, "layout")?.node?.name)
    }

    @Test
    fun `a different document starts a fresh layer cycle`() {
        val hits = hits()
        val clicks = LayerClickCycle(doubleClickMillis = 500)

        assertEquals(hits.first(), clicks.nextTarget(hits, null, "first"))
        assertEquals(hits[1], clicks.nextTarget(hits, null, "first"))
        assertEquals(hits.first(), clicks.nextTarget(hits, null, "second"))
    }
}
