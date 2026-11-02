package com.github.demandss.dayz_workbench_tool.layout

import java.util.UUID

fun newWidgetResourceId(): String =
    java.lang.Long
        .toHexString(UUID.randomUUID().leastSignificantBits)
        .padStart(16, '0')
        .uppercase()

data class WidgetNode(
    val type: WidgetType,
    val originalClassName: String,
    var name: String,
    val properties: LinkedHashMap<String, String> = LinkedHashMap(),
    val children: MutableList<WidgetNode> = mutableListOf(),
    // The `{ ScriptParamsClass { ... } }` block some widgets carry after their children (the gap of a spacer, for example)
    val scriptParams: LinkedHashMap<String, String>? = null,
    val resourceId: String = newWidgetResourceId(),
    /** Whether the root widget had an ID in its header. Child widgets always write one. */
    val hasResourceId: Boolean = false,
)
