package com.github.demandss.dayz_workbench_tool.layout_editor.ui.canvas

import androidx.compose.ui.geometry.Rect
import com.github.demandss.dayz_workbench_tool.layout.WidgetNode
import com.github.demandss.dayz_workbench_tool.layout.WidgetType
import com.github.demandss.dayz_workbench_tool.layout_editor.EditorWidgetNode
import kotlin.test.Test
import kotlin.test.assertEquals

/** Every expected size here was read from a Workbench 1.30 screenshot of the fixaspect probe layouts. */
class FixAspectTest {
    private fun child(
        mode: String,
        size: String = "1 1",
        exact: Boolean = false,
        type: WidgetType = WidgetType.PANEL,
    ) = EditorWidgetNode(
        node =
            WidgetNode(
                type,
                type.className,
                "child",
                linkedMapOf(
                    "position" to "0 0",
                    "size" to size,
                    "hexactsize" to if (exact) "1" else "0",
                    "vexactsize" to if (exact) "1" else "0",
                    "fixaspect" to mode,
                ),
            ),
    )

    /** The size the child ends up with inside a container of [w] x [h] at the origin. */
    private fun sizeIn(
        mode: String,
        w: Float,
        h: Float,
        size: String = "1 1",
        exact: Boolean = false,
        textureAspect: Float? = null,
    ): Pair<Float, Float> {
        val parent = Rect(0f, 0f, w, h)
        val node = child(mode, size, exact)
        val r = fixAspect(node, computeRect(node, parent), parent, textureAspect)
        assertEquals(0f, r.left)
        assertEquals(0f, r.top)
        return r.width to r.height
    }

    @Test
    fun `none keeps the box that the size gave`() {
        assertEquals(160f to 60f, sizeIn("none", 160f, 60f))
    }

    @Test
    fun `inside fits the largest square into a wide, a tall and a square parent`() {
        assertEquals(60f to 60f, sizeIn("inside", 160f, 60f))
        assertEquals(60f to 60f, sizeIn("inside", 60f, 160f))
        assertEquals(90f to 90f, sizeIn("inside", 90f, 90f))
    }

    @Test
    fun `outside covers the parent with a square, which sticks out`() {
        assertEquals(160f to 160f, sizeIn("outside", 160f, 60f))
        assertEquals(160f to 160f, sizeIn("outside", 60f, 160f))
        assertEquals(90f to 90f, sizeIn("outside", 90f, 90f))
    }

    @Test
    fun `fixwidth keeps the height, so a wide parent gives its height and a tall one gives its height too`() {
        assertEquals(60f to 60f, sizeIn("fixwidth", 160f, 60f))
        assertEquals(160f to 160f, sizeIn("fixwidth", 60f, 160f))
        assertEquals(90f to 90f, sizeIn("fixwidth", 90f, 90f))
    }

    @Test
    fun `the ratio comes from the numbers of size, 0 5 by 0 25 is two to one`() {
        // in a 160x160 parent
        assertEquals(160f to 80f, sizeIn("inside", 160f, 160f, size = "0.5 0.25"))
        assertEquals(320f to 160f, sizeIn("outside", 160f, 160f, size = "0.5 0.25"))
        assertEquals(80f to 40f, sizeIn("fixwidth", 160f, 160f, size = "0.5 0.25"))
        assertEquals(80f to 40f, sizeIn("none", 160f, 160f, size = "0.5 0.25"))
    }

    @Test
    fun `a pixel size is left alone`() {
        for (mode in listOf("inside", "outside", "fixwidth")) {
            assertEquals(100f to 50f, sizeIn(mode, 160f, 160f, size = "100 50", exact = true), mode)
        }
    }

    @Test
    fun `a texture gives the ratio when it is known`() {
        // a 2:1 texture in a square parent, relative size 1 1
        assertEquals(160f to 80f, sizeIn("inside", 160f, 160f, textureAspect = 2f))
    }

    @Test
    fun `true means inside and an unknown value changes nothing`() {
        assertEquals(60f to 60f, sizeIn("true", 160f, 60f))
        assertEquals(160f to 60f, sizeIn("sideways", 160f, 60f))
    }

    @Test
    fun `the tree layout applies it to the children of a widget`() {
        val screen = Rect(0f, 0f, 1000f, 500f)
        val kid = child("inside")
        val root = EditorWidgetNode(node = WidgetNode(WidgetType.FRAME, WidgetType.FRAME.className, "root"), children = mutableListOf(kid))

        val layout = layoutTree(root, screen)

        assertEquals(Rect(0f, 0f, 500f, 500f), layout.rects[kid.id])
    }

    @Test
    fun `fixaspect keeps the center anchor after expanding a child`() {
        val screen = Rect(0f, 0f, 1920f, 1080f)
        val kid = child("inside", size = "0.16 0.09")
        kid.node.properties["halign"] = "center_ref"
        kid.node.properties["valign"] = "center_ref"
        val root = EditorWidgetNode(node = WidgetNode(WidgetType.FRAME, WidgetType.FRAME.className, "root"), children = mutableListOf(kid))

        val rect = layoutTree(root, screen, textureAspect = { 16f / 9f }).rects.getValue(kid.id)

        assertEquals(0f, rect.left, 0.01f)
        assertEquals(0f, rect.top, 0.01f)
        assertEquals(screen.width, rect.width, 0.01f)
        assertEquals(screen.height, rect.height, 0.01f)
    }

    @Test
    fun `fixaspect keeps right and bottom anchors when the final size changes`() {
        val parent = Rect(0f, 0f, 160f, 60f)
        val kid = child("inside")
        kid.node.properties["halign"] = "right_ref"
        kid.node.properties["valign"] = "bottom_ref"

        assertEquals(Rect(100f, 0f, 160f, 60f), fixAspect(kid, computeRect(kid, parent), parent))
    }
}
