package com.github.demandss.dayz_workbench_tool.layout_editor.ui.panels

import androidx.compose.ui.graphics.Color
import com.github.demandss.dayz_workbench_tool.layout.WidgetNode
import com.github.demandss.dayz_workbench_tool.layout.WidgetType
import com.github.demandss.dayz_workbench_tool.layout_editor.EditorWidgetNode
import com.github.demandss.dayz_workbench_tool.layout_editor.findNode
import com.github.demandss.dayz_workbench_tool.layout_editor.state.LayoutDocument
import com.github.demandss.dayz_workbench_tool.layout_editor.state.LayoutEditorState
import com.github.demandss.dayz_workbench_tool.layout_editor.updateNode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ColorEditSessionTest {
    private fun LayoutEditorState.property(
        id: UUID,
        key: String,
    ): String? =
        activeFile!!
            .root
            .findNode(id)
            ?.node
            ?.properties
            ?.get(key)

    private fun fixture(): Pair<LayoutEditorState, EditorWidgetNode> {
        val child =
            EditorWidgetNode(
                node = WidgetNode(WidgetType.PANEL, WidgetType.PANEL.className, "Child", linkedMapOf("color" to "1 0 0 1")),
            )
        val root =
            EditorWidgetNode(
                node = WidgetNode(WidgetType.FRAME, WidgetType.FRAME.className, "Root"),
                children = mutableListOf(child),
            )
        val state = LayoutEditorState("", CoroutineScope(SupervisorJob() + Dispatchers.Default))
        state.openFiles.add(LayoutDocument("test.layout", root))
        state.activeFileIndex = 0
        state.selectedIds.add(child.id)
        return state to child
    }

    @Test
    fun cancelRestoresTheOriginalTreeAndModifiedFlag() {
        val (state, child) = fixture()
        val before = state.activeFile!!.root
        val session = ColorEditSession(state)

        session.preview(Color.Blue)
        assertNotEquals(before, state.activeFile!!.root)
        assertTrue(state.activeFile!!.isModified)
        assertTrue(state.activeFile!!.undoStack.isEmpty())

        session.cancel()
        assertEquals(before, state.activeFile!!.root)
        assertEquals(
            "1 0 0 1",
            state.activeFile!!
                .root
                .findNode(child.id)!!
                .node.properties["color"],
        )
        assertFalse(state.activeFile!!.isModified)
        assertTrue(state.activeFile!!.undoStack.isEmpty())
    }

    @Test
    fun severalPreviewChangesBecomeOneUndoStep() {
        val (state, child) = fixture()
        val before = state.activeFile!!.root
        val session = ColorEditSession(state)

        session.preview(Color.Blue)
        session.preview(Color.Green)
        session.confirm(Color.Green)
        assertEquals(1, state.activeFile!!.undoStack.size)
        assertNotEquals(before, state.activeFile!!.root)

        state.undo(0)
        assertEquals(before, state.activeFile!!.root)
        assertEquals(
            "1 0 0 1",
            state.activeFile!!
                .root
                .findNode(child.id)!!
                .node.properties["color"],
        )
    }

    @Test
    fun confirmWithoutPickingDoesNotModifyTheDocument() {
        val (state, _) = fixture()
        val before = state.activeFile!!.root

        ColorEditSession(state).confirm(Color.Red)

        assertEquals(before, state.activeFile!!.root)
        assertFalse(state.activeFile!!.isModified)
        assertTrue(state.activeFile!!.undoStack.isEmpty())
    }

    @Test
    fun cancelPreservesConcurrentChangeAndItsUndo() {
        val (state, child) = fixture()
        val other =
            EditorWidgetNode(
                node = WidgetNode(WidgetType.PANEL, WidgetType.PANEL.className, "Other", linkedMapOf("priority" to "0")),
            )
        state.openFiles[0] = state.activeFile!!.copy(root = state.activeFile!!.root.copy(children = mutableListOf(child, other)))
        val session = ColorEditSession(state)

        session.preview(Color.Blue)
        state.pushUndo(0, state.activeFile!!.root, "MCP priority")
        state.updateActiveRoot(
            state.activeFile!!.root.updateNode(other.id) { node ->
                node.copy(node = node.node.copy(properties = linkedMapOf("priority" to "1")))
            },
        )
        session.cancel()
        assertEquals("1 0 0 1", state.property(child.id, "color"))
        assertEquals("1", state.property(other.id, "priority"))

        state.undo(0)
        assertEquals("1 0 0 1", state.property(child.id, "color"))
        assertEquals("0", state.property(other.id, "priority"))
    }

    @Test
    fun confirmingAfterConcurrentChangeKeepsBothUndoStepsSeparate() {
        val (state, child) = fixture()
        val other =
            EditorWidgetNode(
                node = WidgetNode(WidgetType.PANEL, WidgetType.PANEL.className, "Other", linkedMapOf("priority" to "0")),
            )
        state.openFiles[0] = state.activeFile!!.copy(root = state.activeFile!!.root.copy(children = mutableListOf(child, other)))
        val session = ColorEditSession(state)

        session.preview(Color.Blue)
        state.pushUndo(0, state.activeFile!!.root, "MCP priority")
        state.updateActiveRoot(
            state.activeFile!!.root.updateNode(other.id) { node ->
                node.copy(node = node.node.copy(properties = linkedMapOf("priority" to "1")))
            },
        )
        session.confirm(Color.Blue)
        assertEquals(2, state.activeFile!!.undoStack.size)

        state.undo(0)
        assertEquals("1 0 0 1", state.property(child.id, "color"))
        assertEquals("1", state.property(other.id, "priority"))
        state.undo(0)
        assertEquals("1 0 0 1", state.property(child.id, "color"))
        assertEquals("0", state.property(other.id, "priority"))
    }
}
