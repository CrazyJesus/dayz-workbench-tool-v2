package com.github.demandss.dayz_workbench_tool.layout_editor.ui.panels

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ColorPickerInputTest {
    @Test
    fun sixDigitHexKeepsTheCurrentOpacity() {
        val color = assertNotNull(colorFromPickerText("#336699", 0.4f))

        assertEquals(0.4f, color.alpha)
    }

    @Test
    fun eightDigitHexReplacesTheCurrentOpacity() {
        val color = assertNotNull(colorFromPickerText("#33669980", 0.4f))

        assertEquals(0x80 / 255f, color.alpha)
    }
}
