package com.github.demandss.dayz_workbench_tool.layout

object LayoutParser {
    fun parse(input: String): WidgetNode = ParserState(input).parseWidget()
}

private class ParserState(
    private val input: String,
) {
    var pos = 0
    private val resourceIdPattern = Regex("\\{([0-9A-Fa-f]{16})}")

    fun parseWidget(): WidgetNode {
        skipWhitespaceAndComments()
        val className = readBareWord()
        skipInlineWhitespace()

        val header =
            when {
                pos >= input.length || input[pos] == '{' -> ""
                input[pos] == '"' -> readQuotedString()
                else -> readBareWord()
            }
        val resourceId = resourceIdPattern.matchEntire(header)?.groupValues?.get(1)
        skipInlineWhitespace()
        expectChar('{')

        val props = LinkedHashMap<String, String>()
        val children = mutableListOf<WidgetNode>()
        var scriptParams: LinkedHashMap<String, String>? = null

        while (true) {
            skipWhitespaceAndComments()
            if (pos >= input.length) break
            when (input[pos]) {
                '}' -> {
                    pos++
                    break
                }

                '{' -> {
                    pos++
                    skipWhitespaceAndComments()
                    if (input.startsWith("ScriptParamsClass", pos)) {
                        scriptParams = parseScriptParams()
                    } else {
                        parseChildrenInto(children)
                    }
                }

                else -> {
                    parsePropInto(props)
                }
            }
        }

        val name = props.remove("Name") ?: if (resourceId == null) header else ""
        return WidgetNode(
            WidgetType.fromClassName(className),
            className,
            name,
            props,
            children,
            scriptParams,
            resourceId ?: newWidgetResourceId(),
            resourceId != null,
        )
    }

    /** Reads `ScriptParamsClass { key value ... }` and the brace that closes the block around it. */
    private fun parseScriptParams(): LinkedHashMap<String, String> {
        readBareWord()
        skipInlineWhitespace()
        expectChar('{')
        val params = LinkedHashMap<String, String>()
        while (true) {
            skipWhitespaceAndComments()
            if (pos >= input.length) break
            if (input[pos] == '}') {
                pos++
                break
            }
            parsePropInto(params)
        }
        skipWhitespaceAndComments()
        expectChar('}')
        return params
    }

    private fun parseChildrenInto(children: MutableList<WidgetNode>) {
        while (true) {
            skipWhitespaceAndComments()
            if (pos >= input.length) break
            if (input[pos] == '}') {
                pos++
                break
            }
            children.add(parseWidget())
        }
    }

    private fun parsePropInto(props: LinkedHashMap<String, String>) {
        val key = if (input[pos] == '"') readQuotedString() else readBareWord()
        skipInlineWhitespace()
        val value =
            if (key == "Name" && pos < input.length && input[pos] == '"') {
                readQuotedString()
            } else {
                readToEndOfLine()
            }
        if (key.isNotEmpty()) props[key] = value
    }

    private fun skipWhitespaceAndComments() {
        while (pos < input.length) {
            when {
                input[pos].isWhitespace() -> {
                    pos++
                }

                pos + 1 < input.length && input[pos] == '/' && input[pos + 1] == '/' -> {
                    while (pos < input.length && input[pos] != '\n') pos++
                }

                pos + 1 < input.length && input[pos] == '/' && input[pos + 1] == '*' -> {
                    pos += 2
                    while (pos + 1 < input.length && !(input[pos] == '*' && input[pos + 1] == '/')) pos++
                    if (pos + 1 < input.length) pos += 2
                }

                else -> {
                    break
                }
            }
        }
    }

    private fun skipInlineWhitespace() {
        while (pos < input.length && (input[pos] == ' ' || input[pos] == '\t')) pos++
    }

    private fun readBareWord(): String {
        val start = pos
        while (pos < input.length && !input[pos].isWhitespace() &&
            input[pos] != '{' && input[pos] != '}' && input[pos] != '"'
        ) {
            pos++
        }
        return input.substring(start, pos)
    }

    private fun readQuotedString(): String {
        pos++ // skip opening "
        val sb = StringBuilder()
        while (pos < input.length && input[pos] != '"' && input[pos] != '\n') {
            sb.append(input[pos++])
        }
        if (pos < input.length && input[pos] == '"') pos++ // skip closing "
        return sb.toString()
    }

    private fun readToEndOfLine(): String {
        val start = pos
        while (pos < input.length && input[pos] != '\n' && input[pos] != '\r') pos++
        return input.substring(start, pos).trimEnd()
    }

    private fun expectChar(c: Char) {
        if (pos < input.length && input[pos] == c) pos++
    }
}
