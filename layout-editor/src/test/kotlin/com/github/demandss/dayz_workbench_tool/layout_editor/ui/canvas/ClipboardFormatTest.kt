package com.github.demandss.dayz_workbench_tool.layout_editor.ui.canvas

import com.github.demandss.dayz_workbench_tool.layout.LayoutSerializer
import com.github.demandss.dayz_workbench_tool.layout.WidgetNode
import com.github.demandss.dayz_workbench_tool.layout.WidgetType
import com.github.demandss.dayz_workbench_tool.layout_editor.EditorWidgetNode
import com.github.demandss.dayz_workbench_tool.layout_editor.toWidgetNode
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ClipboardFormatTest {
    private fun panel(name: String) =
        EditorWidgetNode(
            UUID.randomUUID(),
            WidgetNode(WidgetType.PANEL, WidgetType.PANEL.className, name, linkedMapOf("size" to "10 10", "style" to "x")),
            mutableListOf(),
        )

    @Test
    fun `copied widgets come back with new ids`() {
        val a = panel("a")
        val b = panel("b")
        val back = parseClipboard(clipboardText(listOf(a, b)))
        assertNotNull(back)
        assertEquals(listOf("a", "b"), back.map { it.node.name })
        assertNotEquals(a.id, back[0].id)
        assertNotEquals(a.node.resourceId, back[0].node.resourceId)
        assertEquals("10 10", back[0].node.properties["size"])
    }

    @Test
    fun `one widget in the layout format is taken as it is`() {
        val text = LayoutSerializer.serialize(panel("solo").toWidgetNode())
        assertEquals(1, parseClipboard(text)?.size)
    }

    @Test
    fun `text that is not a layout is refused`() {
        assertNull(parseClipboard("hello world"))
        assertNull(parseClipboard(""))
    }

    @Test
    fun `a name that is taken gets a copy suffix`() {
        assertEquals("a", uniqueName("a", setOf("b")))
        assertEquals("a_copy", uniqueName("a", setOf("a")))
        assertEquals("a_copy2", uniqueName("a", setOf("a", "a_copy")))
        assertEquals("a_copy2", uniqueName("a_copy", setOf("a", "a_copy")))
    }
}
