package com.github.demandss.dayz_workbench_tool.layout_editor.ui.canvas

import androidx.compose.ui.geometry.Rect
import com.github.demandss.dayz_workbench_tool.layout.WidgetNode
import com.github.demandss.dayz_workbench_tool.layout.WidgetType
import com.github.demandss.dayz_workbench_tool.layout_editor.EditorWidgetNode
import com.github.demandss.dayz_workbench_tool.layout_editor.findNode
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class NodeOpsTest {
    private fun panel(
        name: String,
        vararg props: Pair<String, String>,
        children: List<EditorWidgetNode> = emptyList(),
    ) = EditorWidgetNode(
        UUID.randomUUID(),
        WidgetNode(WidgetType.PANEL, WidgetType.PANEL.className, name, linkedMapOf(*props)),
        children.toMutableList(),
    )

    private val screen = Rect(0f, 0f, 1000f, 500f)

    @Test
    fun `a pixel position moves by pixels`() {
        val a =
            panel(
                "a",
                "position" to "10 10",
                "size" to "50 50",
                "hexactpos" to "1",
                "vexactpos" to "1",
                "hexactsize" to "1",
                "vexactsize" to "1",
            )
        val root = panel("root", children = listOf(a))
        val moved = root.movedBy(a.id, 5f, -3f, screen)
        assertEquals("15 7", moved.findNode(a.id)!!.node.properties["position"])
    }

    @Test
    fun `a relative position moves by a share of the parent`() {
        val a = panel("a", "position" to "0.1 0.1", "hexactpos" to "0", "vexactpos" to "0")
        val root = panel("root", children = listOf(a))
        val moved = root.movedBy(a.id, 100f, 50f, screen)
        assertEquals("0.2 0.2", moved.findNode(a.id)!!.node.properties["position"])
    }

    @Test
    fun `a right anchor counts the other way`() {
        val a = panel("a", "position" to "10 0", "hexactpos" to "1", "vexactpos" to "1", "halign" to "right_ref")
        val root = panel("root", children = listOf(a))
        assertEquals(
            "5 0",
            root
                .movedBy(a.id, 5f, 0f, screen)
                .findNode(a.id)!!
                .node.properties["position"],
        )
    }

    @Test
    fun `aligning to the left brings both widgets to the leftmost edge`() {
        val exact = arrayOf("hexactpos" to "1", "vexactpos" to "1", "hexactsize" to "1", "vexactsize" to "1")
        val a = panel("a", "position" to "20 0", "size" to "50 50", *exact)
        val b = panel("b", "position" to "100 40", "size" to "50 50", *exact)
        val root = panel("root", children = listOf(a, b))
        val layout = layoutTree(root, screen)
        val result = alignedRoot(root, listOf(a.id, b.id), layout, AlignMode.LEFT)
        assertEquals("20 40", result.findNode(b.id)!!.node.properties["position"])
        assertEquals("20 0", result.findNode(a.id)!!.node.properties["position"])
    }

    @Test
    fun `a copy goes after the original and has new ids`() {
        val a = panel("a", children = listOf(panel("child")))
        val b = panel("b")
        val root = panel("root", children = listOf(a, b))
        val (newRoot, copyId) = root.withCopyOf(a.id)!!
        assertEquals(listOf("a", "a_copy", "b"), newRoot.children.map { it.node.name })
        val copy = newRoot.findNode(copyId)!!
        assertNotEquals(a.children[0].id, copy.children[0].id)
        assertNotEquals(a.node.resourceId, copy.node.resourceId)
        assertNotEquals(a.children[0].node.resourceId, copy.children[0].node.resourceId)
        assertNotNull(copy.children.firstOrNull())
    }

    @Test
    fun `the root cannot be copied`() {
        val root = panel("root")
        assertNull(root.withCopyOf(root.id))
    }
}
