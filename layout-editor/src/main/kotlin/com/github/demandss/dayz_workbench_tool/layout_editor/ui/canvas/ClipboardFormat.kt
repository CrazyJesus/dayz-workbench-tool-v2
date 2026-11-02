package com.github.demandss.dayz_workbench_tool.layout_editor.ui.canvas

import com.github.demandss.dayz_workbench_tool.layout.LayoutParser
import com.github.demandss.dayz_workbench_tool.layout.LayoutSerializer
import com.github.demandss.dayz_workbench_tool.layout.WidgetNode
import com.github.demandss.dayz_workbench_tool.layout.WidgetType
import com.github.demandss.dayz_workbench_tool.layout_editor.EditorWidgetNode
import com.github.demandss.dayz_workbench_tool.layout_editor.toEditorNode
import com.github.demandss.dayz_workbench_tool.layout_editor.toWidgetNode

/** The name of the frame that carries several copied widgets, so that they travel as one piece of text in the layout format. */
internal const val CLIPBOARD_FRAME = "__clipboard__"

/** The widgets as text for the system clipboard. */
internal fun clipboardText(nodes: List<EditorWidgetNode>): String =
    LayoutSerializer.serialize(
        WidgetNode(
            WidgetType.FRAME,
            WidgetType.FRAME.className,
            CLIPBOARD_FRAME,
            LinkedHashMap(),
            nodes.map { it.toWidgetNode() }.toMutableList(),
        ),
    )

/** The widgets in clipboard text, or null when the text is not a layout. A whole layout comes as one widget. */
internal fun parseClipboard(text: String): List<EditorWidgetNode>? {
    if (text.isBlank() || !text.contains("{")) return null
    val parsed = runCatching { LayoutParser.parse(text) }.getOrNull() ?: return null
    if (parsed.type == WidgetType.UNKNOWN) return null
    val nodes = if (parsed.name == CLIPBOARD_FRAME) parsed.children else listOf(parsed)
    return nodes.map { it.toEditorNode().copiedWithNewIds() }
}

/**
 * The name a new widget of [type] gets: the name of its type (`TextWidget`), and when that is taken the same with a number
 * (`TextWidget0`, `TextWidget1`), so that every widget has a name of its own and the user can rename it later.
 */
internal fun technicalName(
    type: com.github.demandss.dayz_workbench_tool.layout.WidgetType,
    taken: Set<String>,
): String {
    val base = type.className.removeSuffix("Class")
    if (base !in taken) return base
    var n = 0
    while ("$base$n" in taken) n++
    return "$base$n"
}

/** [name] if it is free among [taken], otherwise the name with `_copy`, `_copy2`... */
internal fun uniqueName(
    name: String,
    taken: Set<String>,
): String {
    if (name !in taken) return name
    val base = name.removeSuffix("_copy").ifEmpty { name }
    var candidate = base + "_copy"
    var n = 2
    while (candidate in taken) candidate = base + "_copy" + n++
    return candidate
}
