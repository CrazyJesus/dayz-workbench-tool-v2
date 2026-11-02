package com.github.demandss.dayz_workbench_tool.layout_editor.ui.panels

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.IntOffset
import com.github.demandss.dayz_workbench_tool.i18n.str
import com.github.demandss.dayz_workbench_tool.layout.WidgetType
import com.github.demandss.dayz_workbench_tool.layout_editor.state.LayoutEditorState
import com.github.demandss.dayz_workbench_tool.layout_editor.ui.canvas.parseWidgetColor
import com.github.demandss.dayz_workbench_tool.resources.*
import com.github.demandss.dayz_workbench_tool.ui.icons.widgetIcon
import kotlin.math.roundToInt

/** The right button on the canvas lists the widgets under the cursor, the topmost first; a click selects one. */
@Composable
fun CanvasContextMenu(state: LayoutEditorState) {
    val menu = state.canvasMenu ?: return
    ContextMenu(IntOffset(menu.position.x.roundToInt(), menu.position.y.roundToInt()), onDismiss = { state.canvasMenu = null }) {
        ContextMenuCaption(str(Res.string.under_the_cursor))
        menu.hits.take(MENU_LIMIT).forEach { node ->
            val picture = if (node.node.type == WidgetType.IMAGE) state.pictureFor(node, state.activeFile?.path) else null
            ContextMenuItem(
                "${node.node.name}  ·  ${node.node.type.className.removeSuffix("Class")}",
                icon = widgetIcon(node.node.type),
                image = picture,
                imageTint = node.node.properties["color"]?.let(::parseWidgetColor),
            ) {
                state.canvasMenu = null
                state.canvasFocusTick++
                state.selectedIds.clear()
                state.selectedIds.add(node.id)
            }
        }
    }
}

private const val MENU_LIMIT = 24
