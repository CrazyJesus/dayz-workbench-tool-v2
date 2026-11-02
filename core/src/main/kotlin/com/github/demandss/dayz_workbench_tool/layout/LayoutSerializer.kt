package com.github.demandss.dayz_workbench_tool.layout

import kotlin.collections.iterator

object LayoutSerializer {
    fun serialize(root: WidgetNode): String = serializeNode(root, depth = 0)

    private fun serializeNode(
        node: WidgetNode,
        depth: Int,
    ): String =
        buildString {
            val indent = "\t".repeat(depth)
            val innerIndent = "\t".repeat(depth + 1)

            val className = if (node.type == WidgetType.UNKNOWN) node.originalClassName else node.type.className
            val idPart = if (depth > 0 || node.hasResourceId) " \"{${node.resourceId}}\"" else ""

            append("$indent$className$idPart {\n")
            append("${innerIndent}Name \"${node.name}\"\n")

            for ((key, value) in node.properties) {
                val quotedKey = if (' ' in key) "\"$key\"" else key
                append("$innerIndent$quotedKey $value\n")
            }

            if (node.children.isNotEmpty()) {
                append("$innerIndent{\n")
                for (child in node.children) {
                    append(serializeNode(child, depth + 2))
                }
                append("$innerIndent}\n")
            }

            node.scriptParams?.let { params ->
                append("$innerIndent{\n")
                append("$innerIndent\tScriptParamsClass {\n")
                for ((key, value) in params) append("$innerIndent\t\t$key $value\n")
                append("$innerIndent\t}\n")
                append("$innerIndent}\n")
            }

            append("$indent}\n")
        }
}
