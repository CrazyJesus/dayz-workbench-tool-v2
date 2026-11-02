package com.github.demandss.dayz_workbench_tool.layout

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class LayoutFormatsTest {
    @Test
    fun `legacy names become Name properties and child resource ids`() {
        val source =
            """
            FrameWidgetClass Root {
             size 1 1
             {
              PanelWidgetClass Body {
               style blank
               {
                TextWidgetClass Caption {
                 text "Hello"
                }
               }
              }
             }
            }
            """.trimIndent()

        val root = LayoutParser.parse(source)
        val converted = LayoutSerializer.serialize(root)
        val reopened = LayoutParser.parse(converted)

        assertTrue(converted.startsWith("FrameWidgetClass {\n\tName \"Root\"\n"), converted)
        assertTrue(Regex("""PanelWidgetClass "\{[0-9A-F]{16}}" \{""").containsMatchIn(converted), converted)
        assertTrue(Regex("""TextWidgetClass "\{[0-9A-F]{16}}" \{""").containsMatchIn(converted), converted)
        assertFalse(converted.contains("PanelWidgetClass Body {"), converted)
        val names = listOf(reopened.name, reopened.children[0].name, reopened.children[0].children[0].name)
        assertEquals(listOf("Root", "Body", "Caption"), names)
        assertEquals("blank", reopened.children[0].properties["style"])
        assertEquals(converted, LayoutSerializer.serialize(reopened))
    }

    @Test
    fun `new format keeps quoted resource ids and names separate`() {
        val source =
            """
            FrameWidgetClass {
             Name "mainframe"
             size 1 1
             {
              ImageWidgetClass "{000000005F801B73}" {
               Name "mainframe_framer"
               color 1 1 1 0
               {
                PanelWidgetClass "{00000000B3D497EA}" {
                 Name "content_base"
                 style rover_sim_colorable
                }
               }
              }
             }
            }
            """.trimIndent()

        val root = LayoutParser.parse(source)
        val saved = LayoutSerializer.serialize(root)

        assertEquals("mainframe", root.name)
        assertEquals("mainframe_framer", root.children[0].name)
        assertEquals("000000005F801B73", root.children[0].resourceId)
        assertEquals("00000000B3D497EA", root.children[0].children[0].resourceId)
        assertFalse(root.properties.containsKey("Name"))
        assertTrue(saved.startsWith("FrameWidgetClass {"), saved)
        assertTrue(saved.contains("ImageWidgetClass \"{000000005F801B73}\" {"), saved)
        assertEquals(saved, LayoutSerializer.serialize(LayoutParser.parse(saved)))
    }

    @Test
    fun `legacy children get distinct resource ids`() {
        val root = LayoutParser.parse("FrameWidgetClass Root {\n {\n PanelWidgetClass A {}\n PanelWidgetClass B {}\n }\n}")

        assertNotEquals(root.children[0].resourceId, root.children[1].resourceId)
    }
}
