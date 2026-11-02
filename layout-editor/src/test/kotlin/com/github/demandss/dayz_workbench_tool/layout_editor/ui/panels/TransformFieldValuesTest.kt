package com.github.demandss.dayz_workbench_tool.layout_editor.ui.panels

import com.github.demandss.dayz_workbench_tool.layout_editor.ui.canvas.convertUnit
import com.github.demandss.dayz_workbench_tool.layout_editor.ui.canvas.formatNumber
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TransformFieldValuesTest {
    @Test
    fun `a stored share is shown as a percentage and written back as a share`() {
        assertEquals("10", displayTransformValue("0.1", isPx = false))
        assertEquals("100", displayTransformValue("1", isPx = false))
        assertEquals("12.5", displayTransformValue("0.125", isPx = false))
        assertEquals("-10", displayTransformValue("-0.1", isPx = false))
        assertEquals("0.1", displayTransformValue("0.1", isPx = true))

        assertEquals("0.1 0.25", storedTransformPair("0 0.25", 0, "10", isPx = false))
        assertEquals("0.11 0.25", storedTransformPair("0.1 0.25", 0, "11", isPx = false))
        assertEquals("0.125 0.25", storedTransformPair("0 0.25", 0, "12.5", isPx = false))
        assertEquals("1.25 0.25", storedTransformPair("0 0.25", 0, "125", isPx = false))
    }

    @Test
    fun `editing one axis leaves the other axis and its units unchanged`() {
        assertEquals("20 0.1", storedTransformPair("12 0.1", 0, "20", isPx = true))
        assertEquals("12 0.3", storedTransformPair("12 0.1", 1, "30", isPx = false))
        assertNull(storedTransformPair("12 0.1", 1, "invalid", isPx = false))
    }

    @Test
    fun `switching between pixels and percent keeps the same geometry`() {
        val parentWidth = 1920f
        val fraction = 0.1f
        val pixels = convertUnit(fraction, parentWidth, toPixels = true)
        assertEquals("192", displayTransformValue(formatNumber(pixels), isPx = true))

        val restored = convertUnit(pixels, parentWidth, toPixels = false)
        assertEquals("10", displayTransformValue(formatNumber(restored), isPx = false))
    }
}
