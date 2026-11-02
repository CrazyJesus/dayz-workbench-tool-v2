package com.github.demandss.dayz_workbench_tool.layout_editor.mcp

import com.github.demandss.dayz_workbench_tool.layout_editor.state.LayoutEditorState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The tools of the MCP server on a real editor state, working in a folder of their own. */
class McpToolsTest {
    private val workDrive = createTempDirectory("mcp-work").toFile()
    private val mod = File(workDrive, "Mod").also { File(it, "GUI").mkdirs() }
    private val state =
        LayoutEditorState(workDrive.path, CoroutineScope(SupervisorJob() + Dispatchers.Default), "", listOf(mod.path))
    private val tools = McpTools(state)

    private class Answer(
        val text: String,
        val isError: Boolean,
    )

    private fun call(
        name: String,
        args: JsonObject = buildJsonObject { },
    ): Answer {
        val result = tools.call(name, args).jsonObject
        val text =
            result["content"]!!
                .jsonArray[0]
                .jsonObject["text"]!!
                .jsonPrimitive.content
        return Answer(text, result["isError"]?.jsonPrimitive?.booleanOrNull == true)
    }

    private fun ok(
        name: String,
        args: JsonObject = buildJsonObject { },
    ): String {
        val answer = call(name, args)
        assertFalse(answer.isError, "$name failed: ${answer.text}")
        return answer.text
    }

    private fun newLayout(path: String = File(mod, "GUI/menu.layout").path): String {
        ok("create_layout", buildJsonObject { put("path", path) })
        return path
    }

    @Test
    fun `a new layout is written and opened with its root`() {
        val path = newLayout()
        assertTrue(File(path).readText().startsWith("FrameWidgetClass Root"))
        val status = ok("editor_status")
        assertTrue(path.replace("\\", "\\\\") in status || path in status, status)
        assertTrue("work_drive" in status && "mod_folders" in status && mod.path.replace("\\", "\\\\") in status, status)
    }

    @Test
    fun `a widget is added and changed, and the changes are undone`() {
        newLayout()
        ok(
            "add_widget",
            buildJsonObject {
                put("type", "TextWidget")
                put("name", "Title")
            },
        )
        assertTrue("Title" in ok("get_tree"))

        ok(
            "set_properties",
            buildJsonObject {
                put("name", "Title")
                put("properties", buildJsonObject { put("text", "\"Hello\"") })
            },
        )
        assertTrue("Hello" in ok("get_widget", buildJsonObject { put("name", "Title") }))

        ok("undo")
        assertFalse("Hello" in ok("get_widget", buildJsonObject { put("name", "Title") }))
        ok("undo")
        assertFalse("Title" in ok("get_tree"))
    }

    @Test
    fun `saving writes the widgets into the file`() {
        val path = newLayout()
        ok(
            "add_widget",
            buildJsonObject {
                put("type", "TextWidget")
                put("name", "Title")
            },
        )
        ok("save_layout")
        val saved = File(path).readText()
        assertTrue("Name \"Title\"" in saved, saved)
        assertTrue(Regex("""TextWidgetClass "\{[0-9A-F]{16}}" \{""").containsMatchIn(saved), saved)
    }

    @Test
    fun `a layout outside the work drive and the mods is refused`() {
        val outside = createTempDirectory("mcp-outside").toFile()
        val answer = call("create_layout", buildJsonObject { put("path", File(outside, "x.layout").path) })
        assertTrue(answer.isError)
        assertTrue("outside" in answer.text, answer.text)
        assertFalse(File(outside, "x.layout").exists())
    }

    @Test
    fun `a layout is found by words of its path in the connected mods`() {
        newLayout(File(mod, "GUI/inventory/bag.layout").path)
        newLayout(File(mod, "GUI/map.layout").path)

        val all = ok("list_layouts")
        assertTrue("bag.layout" in all && "map.layout" in all, all)

        val some = ok("list_layouts", buildJsonObject { put("query", "inventory") })
        assertTrue("bag.layout" in some && "map.layout" !in some, some)

        val none = ok("list_layouts", buildJsonObject { put("query", "no-such-layout") })
        assertTrue("\"count\":0" in none, none)
    }

    @Test
    fun `an unknown tool is an error of that tool`() {
        val answer = call("no_such_tool")
        assertTrue(answer.isError, answer.text)
    }

    @Test
    fun `a tool that only looks says so, and one that changes does not`() {
        val hints =
            McpTools.definitions().associate {
                it["name"]!!.jsonPrimitive.content to it["annotations"]!!.jsonObject["readOnlyHint"]!!.jsonPrimitive.booleanOrNull
            }
        assertEquals(true, hints["get_tree"])
        assertEquals(true, hints["list_layouts"])
        assertEquals(false, hints["set_properties"])
        assertEquals(false, hints["delete_widget"])
        assertEquals(false, hints["save_layout"])
    }

    private fun args(vararg pairs: Pair<String, Any>): JsonObject =
        buildJsonObject {
            for ((key, value) in pairs) {
                when (value) {
                    is JsonObject -> put(key, value)
                    is JsonArray -> put(key, value)
                    is Boolean -> put(key, value)
                    is Int -> put(key, value)
                    else -> put(key, value.toString())
                }
            }
        }

    private fun widget(
        type: String,
        name: String,
    ) = ok("add_widget", args("type" to type, "name" to name))

    @Test
    fun `one property is set on many widgets in one step`() {
        newLayout()
        widget("TextWidget", "Row_1")
        widget("TextWidget", "Row_2")
        widget("TextWidget", "Other")

        ok("set_properties_many", args("name_regex" to "^Row_", "properties" to args("color" to "0.25 0.5 0.75 1")))
        assertTrue("0.25 0.5 0.75 1" in ok("get_widget", args("name" to "Row_1")))
        assertTrue("0.25 0.5 0.75 1" in ok("get_widget", args("name" to "Row_2")))
        assertFalse("0.25 0.5 0.75 1" in ok("get_widget", args("name" to "Other")))

        // The whole change is one step of undo
        ok("undo")
        assertFalse("0.25 0.5 0.75 1" in ok("get_widget", args("name" to "Row_1")))
        assertFalse("0.25 0.5 0.75 1" in ok("get_widget", args("name" to "Row_2")))
    }

    @Test
    fun `a widget is copied, renamed, moved and deleted`() {
        newLayout()
        widget("PanelWidget", "Box")
        widget("TextWidget", "Label")

        ok("copy_widget", args("name" to "Label", "count" to 2))
        val tree = ok("get_tree")
        assertTrue("Label_1" in tree && "Label_2" in tree, tree)

        ok("rename_widget", args("name" to "Label_1", "new_name" to "Caption"))
        assertTrue("Caption" in ok("get_tree"))

        ok("move_widget", args("name" to "Caption", "new_parent_name" to "Box"))
        ok("get_widget", args("path" to "Root/Box/Caption"))

        ok("delete_widget", args("name" to "Box"))
        assertFalse("Caption" in ok("get_tree"))
    }

    @Test
    fun `widgets are found by words and by type`() {
        newLayout()
        widget("TextWidget", "Alpha")
        widget("PanelWidget", "Beta")

        val byName = ok("find_widgets", args("query" to "alp"))
        assertTrue("Alpha" in byName && "Beta" !in byName, byName)
        val byType = ok("find_widgets", args("type" to "PanelWidget"))
        assertTrue("Beta" in byType && "Alpha" !in byType, byType)
    }

    @Test
    fun `several widgets are added at once`() {
        newLayout()
        val widgets =
            JsonArray(
                listOf(
                    args("type" to "TextWidget", "name" to "First"),
                    args("type" to "ImageWidget", "name" to "Second"),
                ),
            )
        ok("add_widgets", args("widgets" to widgets))
        val tree = ok("get_tree")
        assertTrue("First" in tree && "Second" in tree, tree)
    }

    @Test
    fun `redo brings back what undo took`() {
        newLayout()
        widget("TextWidget", "Title")
        ok("undo")
        assertFalse("Title" in ok("get_tree"))
        ok("redo")
        assertTrue("Title" in ok("get_tree"))
    }

    @Test
    fun `the whole text of a layout is read and replaced`() {
        newLayout()
        val current = ok("get_layout_text")
        assertTrue("FrameWidgetClass {" in current && "Name \"Root\"" in current, current)

        val text = "FrameWidgetClass Root {\n size 100 100\n {\n  TextWidgetClass Hello {\n   text \"Hi\"\n  }\n }\n}\n"
        ok("replace_layout_text", args("text" to text))
        assertTrue("Hello" in ok("get_tree"))
    }

    @Test
    fun `a layout with unsaved changes is closed only when it is discarded`() {
        newLayout()
        widget("TextWidget", "Title")

        val refused = call("close_layout")
        assertTrue(refused.isError)
        assertTrue("unsaved" in refused.text, refused.text)

        ok("close_layout", args("discard" to true))
        assertTrue("\"open_layouts\":[]" in ok("editor_status"))
    }

    @Test
    fun `the view is changed and the status tells it`() {
        newLayout()
        ok("set_view", args("show_grid" to true, "grid_size" to 20))
        val status = ok("editor_status")
        assertTrue("\"show_grid\":true" in status && "\"grid_size\":20" in status, status)
    }

    @Test
    fun `the script of a layout names its widgets`() {
        newLayout()
        widget("TextWidget", "Title")
        val script = ok("get_script_template", args("kind" to "widgets"))
        assertTrue("Title" in script && "TextWidget" in script, script)
    }

    @Test
    fun `a missing argument is said by name`() {
        newLayout()
        val noType = call("add_widget", args("name" to "X"))
        assertTrue(noType.isError, noType.text)
        val noProperties = call("set_properties", args("name" to "Root"))
        assertTrue(noProperties.isError && "properties" in noProperties.text, noProperties.text)
    }

    @Test
    fun `a tool that needs a layout says so when none is open`() {
        val answer = call("get_tree")
        assertTrue(answer.isError, answer.text)
    }

    @Test
    fun `the answer of a tool is a content list`() {
        val result = tools.call("editor_status", buildJsonObject { })
        val content = result.jsonObject["content"] as JsonArray
        assertEquals("text", content[0].jsonObject["type"]!!.jsonPrimitive.content)
        assertTrue(JsonPrimitive(content[0].jsonObject["text"]!!.jsonPrimitive.content).isString)
    }
}
