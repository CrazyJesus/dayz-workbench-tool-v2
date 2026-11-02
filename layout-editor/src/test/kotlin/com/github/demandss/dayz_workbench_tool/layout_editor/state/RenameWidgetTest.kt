package com.github.demandss.dayz_workbench_tool.layout_editor.state

import com.github.demandss.dayz_workbench_tool.layout.WidgetNode
import com.github.demandss.dayz_workbench_tool.layout.WidgetType
import com.github.demandss.dayz_workbench_tool.layout_editor.EditorWidgetNode
import com.github.demandss.dayz_workbench_tool.layout_editor.findNode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.io.File
import java.util.UUID
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RenameWidgetTest {
    @Test
    fun `rename changes only the chosen widget and can be undone`() {
        val folder = createTempDirectory("rename-widget").toFile()
        try {
            val child = EditorWidgetNode(node = WidgetNode(WidgetType.IMAGE, WidgetType.IMAGE.className, "Icon"))
            val root =
                EditorWidgetNode(
                    node = WidgetNode(WidgetType.FRAME, WidgetType.FRAME.className, "Root"),
                    children = mutableListOf(child),
                )
            val state = LayoutEditorState(folder.path, CoroutineScope(SupervisorJob() + Dispatchers.Default))
            state.openFiles += LayoutDocument(File(folder, "sample.layout").path, root)
            state.activeFileIndex = 0

            fun childName(): String {
                val document = state.activeFile!!
                val currentChild = document.root.findNode(child.id)!!
                return currentChild.node.name
            }

            state.renameWidget(child.id, " Icon ")
            assertFalse(state.activeFile!!.isModified)
            assertTrue(state.activeFile!!.undoStack.isEmpty())

            state.renameWidget(child.id, "New Icon")
            assertEquals("New_Icon", childName())
            val currentRoot = state.activeFile!!.root
            assertEquals("Root", currentRoot.node.name)
            assertEquals(1, state.activeFile!!.undoStack.size)
            assertTrue(state.activeFile!!.isModified)

            state.undo(0)
            assertEquals("Icon", childName())
            state.redo(0)
            assertEquals("New_Icon", childName())

            state.renameWidget(UUID.randomUUID(), "Missing")
            assertEquals(1, state.activeFile!!.undoStack.size)
        } finally {
            folder.deleteRecursively()
        }
    }
}
