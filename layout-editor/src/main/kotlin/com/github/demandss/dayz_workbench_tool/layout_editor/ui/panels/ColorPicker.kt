package com.github.demandss.dayz_workbench_tool.layout_editor.ui.panels

import androidx.compose.ui.graphics.Color
import com.github.demandss.dayz_workbench_tool.layout_editor.ui.canvas.parseColorText

/** A colour without alpha keeps the current opacity; RGBA and eight-digit hex replace it. */
internal fun colorFromPickerText(
    text: String,
    currentAlpha: Float,
): Color? {
    val parsed = parseColorText(text) ?: return null
    val digits = text.trim().removePrefix("#").length
    return if (text.contains('(') || digits == 4 || digits == 8) parsed else parsed.copy(alpha = currentAlpha)
}
