package com.github.demandss.dayz_workbench_tool.layout_editor.ui.panels

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.demandss.dayz_workbench_tool.i18n.str
import com.github.demandss.dayz_workbench_tool.layout.WidgetType
import com.github.demandss.dayz_workbench_tool.layout_editor.EditorWidgetNode
import com.github.demandss.dayz_workbench_tool.layout_editor.behavior.ScriptParam
import com.github.demandss.dayz_workbench_tool.layout_editor.behavior.ScriptParams
import com.github.demandss.dayz_workbench_tool.layout_editor.behavior.scriptClassName
import com.github.demandss.dayz_workbench_tool.layout_editor.behavior.setScriptParam
import com.github.demandss.dayz_workbench_tool.layout_editor.findNode
import com.github.demandss.dayz_workbench_tool.layout_editor.state.LayoutEditorState
import com.github.demandss.dayz_workbench_tool.layout_editor.ui.canvas.MIXED
import com.github.demandss.dayz_workbench_tool.layout_editor.ui.canvas.convertUnit
import com.github.demandss.dayz_workbench_tool.layout_editor.ui.canvas.documentColors
import com.github.demandss.dayz_workbench_tool.layout_editor.ui.canvas.formatNumber
import com.github.demandss.dayz_workbench_tool.layout_editor.ui.canvas.formatShort
import com.github.demandss.dayz_workbench_tool.layout_editor.ui.canvas.layoutTree
import com.github.demandss.dayz_workbench_tool.layout_editor.ui.canvas.mergedProperties
import com.github.demandss.dayz_workbench_tool.layout_editor.ui.canvas.moveSelectionTo
import com.github.demandss.dayz_workbench_tool.layout_editor.ui.canvas.parseWidgetColor
import com.github.demandss.dayz_workbench_tool.layout_editor.ui.canvas.scaleSelectionTo
import com.github.demandss.dayz_workbench_tool.layout_editor.ui.canvas.screenRect
import com.github.demandss.dayz_workbench_tool.layout_editor.ui.canvas.selectionRect
import com.github.demandss.dayz_workbench_tool.layout_editor.ui.canvas.setOnSelected
import com.github.demandss.dayz_workbench_tool.layout_editor.ui.canvas.toFileColor
import com.github.demandss.dayz_workbench_tool.layout_editor.updateNode
import com.github.demandss.dayz_workbench_tool.resources.*
import com.github.demandss.dayz_workbench_tool.stringtable.StringtableRegistry
import com.github.demandss.dayz_workbench_tool.ui.components.Tip
import com.github.demandss.dayz_workbench_tool.ui.icons.AppIcon
import com.github.demandss.dayz_workbench_tool.ui.icons.AppIconView
import com.github.demandss.dayz_workbench_tool.ui.icons.widgetIcon
import com.github.demandss.dayz_workbench_tool.ui.pickFile
import com.github.demandss.dayz_workbench_tool.ui.theme.*
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import java.util.UUID
import kotlin.collections.iterator

// ---------------------------------------------------------------------------
// Constants
// ---------------------------------------------------------------------------

private val ROW_H = 22.dp
private val LABEL_W = 80.dp

private val HALIGN_GRID = listOf("left", "center_ref", "right_ref")
private val VALIGN_GRID = listOf("top", "center_ref", "bottom_ref")

private val FIXASPECT_OPTS = listOf("", "fixwidth", "fixheight")
private val MODE_OPTS = listOf("", "blend", "add", "multiply", "color", "alpha", "overlay")
private val CLAMP_OPTS = listOf("", "clamp", "wrap", "mirror")
private val STRETCH_OPTS = listOf("", "tile", "stretch")

private val WIDGET_KEYS =
    setOf(
        "position",
        "size",
        "hexactpos",
        "vexactpos",
        "hexactsize",
        "vexactsize",
        "halign",
        "valign",
        "visible",
        "disabled",
        "clipchildren",
        "inheritalpha",
        "ignorepointer",
        "ignoregloballv",
        "keepsafezone",
        "color",
        "scaled",
        "fixaspect",
        "priority",
        "userID",
        "draggable",
        "scriptclass",
        "style",
    )

private val IMAGE_KEYS =
    setOf(
        "image0",
        "imageTexture",
        "mode",
        "src alpha",
        "clamp mode",
        "stretch mode",
        "flip u",
        "flip v",
        "filter",
        "nocache",
        "rotation",
        "pivot",
        "mask",
        "transition width",
        "progress",
    )

private val IMAGE_TYPES = setOf(WidgetType.IMAGE, WidgetType.VIDEO)

/** Every property the editor has a row for on a widget of this class, lower case: what a tool can tell a typo from. */
internal fun knownPropertyKeys(
    type: WidgetType,
    originalClassName: String = "",
): Set<String> =
    (
        WIDGET_KEYS + (
            if (type in
                IMAGE_TYPES
            ) {
                IMAGE_KEYS
            } else {
                emptySet()
            }
        ) + schemaKeys(type, originalClassName)
    ).map { it.lowercase() }.toSet()

/** What each property does, shown when the pointer rests on its name. */
private val PROPERTY_HINTS =
    mapOf(
        "visible" to Res.string.on_shows_the_widget_off_hides_the_widget,
        "disabled" to Res.string.on_the_widget_does_not_respond_to_input,
        "clipchildren" to Res.string.on_children_are_cut_off_at_the_borders,
        "inheritalpha" to Res.string.on_the_transparency_of_the_parent,
        "ignorepointer" to Res.string.on_the_mouse_passes_through_the_widget,
        "ignoregloballv" to
            Res.string.on_the_ignore_global_lv_flag_from_the,
        "keepsafezone" to
            Res.string.on_the_widget_respects_the_safe_zone_of,
        "color" to Res.string.the_colours_in_the_file_are_linear_2,
        "scaled" to
            Res.string.scaled_hint,
        "fixaspect" to Res.string.keeps_the_proportions_of_a_widget_with_a,
        "priority" to Res.string.the_drawing_order_among_siblings_larger,
        "userID" to Res.string.a_numeric_identifier_of_the_widget_that,
        "draggable" to Res.string.on_the_widget_can_be_dragged_with_the,
        "scriptclass" to Res.string.the_script_class_attached_to_the_widget,
        "style" to Res.string.a_style_from_a_styles_file_draws_the,
        "image0" to Res.string.image_a_path_to_a_texture_or_an_entry,
        "imageTexture" to Res.string.the_texture_file_the_widget_draws_the,
        "mode" to Res.string.how_the_image_blends_with_what_is_under,
        "src alpha" to Res.string.on_transparency_is_taken_from_the_alpha,
        "clamp mode" to Res.string.what_lies_beyond_the_edge_of_the_texture,
        "stretch mode" to Res.string.how_the_image_fills_the_widget_tile,
        "flip u" to Res.string.on_the_image_is_flipped_horizontally_off,
        "flip v" to Res.string.on_the_image_is_flipped_vertically_off,
        "filter" to Res.string.on_the_image_is_smoothed_when_scaled_off,
        "nocache" to Res.string.on_the_texture_is_not_kept_in_the_cache,
        "rotation" to Res.string.rotation_of_the_image_in_degrees,
        "pivot" to Res.string.the_point_the_image_rotates_around_from,
        "pivot x" to Res.string.the_pivot_horizontally_from_0_to_1,
        "pivot y" to Res.string.the_pivot_vertically_from_0_to_1,
        "Mask" to Res.string.the_texture_of_the_mask_the_image_shows,
        "mask" to Res.string.the_texture_of_the_mask_the_image_shows,
        "Transition width" to Res.string.the_width_of_the_soft_edge_of_the_mask,
        "transition width" to Res.string.the_width_of_the_soft_edge_of_the_mask,
        "Progress" to Res.string.how_much_of_the_image_has_shown_through,
        "progress" to Res.string.how_much_of_the_image_has_shown_through,
        "X" to Res.string.horizontal_position_pixels_or_a_share_of,
        "Y" to Res.string.vertical_position_pixels_or_a_share_of,
        "W" to Res.string.width_pixels_or_a_share_of_the_parent_as,
        "H" to Res.string.height_pixels_or_a_share_of_the_parent,
    )

private val UNKNOWN_PROPERTY_HINT = Res.string.a_property_from_the_file_that_is_not_in

// ---------------------------------------------------------------------------
// Fonts
// ---------------------------------------------------------------------------

/** The fonts for the select of a widget: the game's and the mods', and the one the widget has now. */
@Composable
private fun fontOptions(
    state: LayoutEditorState,
    nearPath: String?,
    current: String?,
): List<Pair<String, String>> = remember(nearPath, current, state.fontsVersion) { state.fontChoices(nearPath, current.orEmpty()) }

// ---------------------------------------------------------------------------
// Commit helpers
// ---------------------------------------------------------------------------

/** The Transform fields show percentages on a 0–100 scale while layouts store shares on a 0–1 scale. */
internal fun displayTransformValue(
    raw: String,
    isPx: Boolean,
): String = if (isPx) raw else raw.toFloatOrNull()?.let { formatNumber(it * 100f) } ?: raw

/** Change one axis without converting the other axis, which may use different units. */
internal fun storedTransformPair(
    raw: String?,
    axis: Int,
    fieldValue: String,
    isPx: Boolean,
): String? {
    val value = fieldValue.trim().toFloatOrNull()?.takeIf { it.isFinite() } ?: return null
    val parts = (raw ?: "0 0").trim().split(Regex("\\s+")).toMutableList()
    while (parts.size < 2) parts.add("0")
    parts[axis] = formatNumber(if (isPx) value else value / 100f)
    return "${parts[0]} ${parts[1]}"
}

private fun commitType(
    state: LayoutEditorState,
    id: UUID,
    type: WidgetType,
) {
    val idx = state.activeFileIndex
    val doc = state.openFiles.getOrNull(idx) ?: return
    state.pushUndo(idx, doc.root, str(Res.string.type_x, type.className.removeSuffix("WidgetClass")))
    state.updateActiveRoot(doc.root.updateNode(id) { it.copy(node = it.node.copy(type = type, originalClassName = type.className)) })
}

private fun commitProps(
    state: LayoutEditorState,
    id: UUID,
    values: Map<String, String>,
    label: String = values.keys.joinToString(", "),
) {
    val idx = state.activeFileIndex
    val doc = state.openFiles.getOrNull(idx) ?: return
    state.pushUndo(idx, doc.root, label)
    state.updateActiveRoot(
        doc.root.updateNode(id) { n ->
            n.copy(node = n.node.copy(properties = LinkedHashMap(n.node.properties).also { it.putAll(values) }))
        },
    )
}

/** Flips pixels and share of the parent for one axis of position or size and recalculates the number, so the widget stays where it is. */
private fun switchUnit(
    state: LayoutEditorState,
    id: UUID,
    props: Map<String, String>,
    key: String,
    axis: Int,
    flag: String,
    toPixels: Boolean,
) {
    val doc = state.activeFile ?: return
    val screen = screenRect(state)
    val parent = layoutTree(doc.root, screen).parentRects[id] ?: screen
    val parts = (props[key] ?: "0 0").trim().split(Regex("\\s+")).toMutableList()
    while (parts.size < 2) parts.add("0")
    val value = parts[axis].toFloatOrNull() ?: 0f
    parts[axis] = formatNumber(convertUnit(value, if (axis == 0) parent.width else parent.height, toPixels))
    commitProps(
        state,
        id,
        mapOf(key to "${parts[0]} ${parts[1]}", flag to if (toPixels) "1" else "0"),
        "$key: ${if (toPixels) "px" else "%"}",
    )
}

private fun commitProp(
    state: LayoutEditorState,
    id: UUID,
    key: String,
    value: String,
) {
    val idx = state.activeFileIndex
    val doc = state.openFiles.getOrNull(idx) ?: return
    state.pushUndo(idx, doc.root, "$key = $value")
    state.updateActiveRoot(
        doc.root.updateNode(id) { n ->
            // An empty value takes the property out of the file
            n.copy(
                node =
                    n.node.copy(
                        properties =
                            LinkedHashMap(n.node.properties).also {
                                if (value.isEmpty()) {
                                    it.remove(key)
                                } else {
                                    it[key] =
                                        value
                                }
                            },
                    ),
            )
        },
    )
}

// ---------------------------------------------------------------------------
// Root
// ---------------------------------------------------------------------------

@Composable
fun PropertiesContent(
    state: LayoutEditorState,
    modifier: Modifier = Modifier,
) {
    val selectedId = state.selectedIds.firstOrNull()
    val doc = state.activeFile
    val node =
        remember(selectedId, doc?.root) {
            if (selectedId != null && doc != null) doc.root.findNode(selectedId) else null
        }
    // Several widgets are one selection, as in Figma: what they share is shown, what differs is Mixed, and a change goes to all
    val selected = state.selectedIds.toList()
    val nodes = remember(selected, doc?.root) { doc?.let { d -> selected.mapNotNull { d.root.findNode(it) } }.orEmpty() }

    Column(modifier.background(FrameBackground)) {
        if (nodes.size > 1) {
            MultiProperties(state, nodes, doc?.path)
            return@Column
        }
        if (node == null) {
            Box(Modifier.fillMaxWidth().padding(8.dp)) {
                Text(stringResource(Res.string.nothing_selected_2), color = GhostlyText, fontSize = 11.sp)
            }
            return@Column
        }

        val props = node.node.properties
        val commit: (String, String) -> Unit = { key, value -> commitProp(state, node.id, key, value) }

        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
            PropertiesHeader(state, node.id, node.node.name, node.node.type, props, commit)
            WidgetSection(
                props,
                commit,
                remember(doc?.root) {
                    doc?.root?.let { documentColors(it) }.orEmpty()
                },
                StyleChoice(state, node.node.type, doc?.path),
                { ScriptParamRows(state, node) },
            ) {
                ScriptClassPicker(state, node, props["scriptclass"]?.trim()?.trim('"').orEmpty()) { commit("scriptclass", it) }
            }
            if (node.node.type in IMAGE_TYPES) ImageWidgetSection(props, commit, state.workDrivePath, state, node, doc?.path)
            SchemaSections(node.node.type, node.node.originalClassName, props, commit, fontOptions(state, doc?.path, props["font"]), {
                state.fontImportOpen =
                    true
            }, { onPick -> state.stringPicker = onPick })
            val schema = schemaKeys(node.node.type, node.node.originalClassName)
            val extra = props.filter { it.key !in WIDGET_KEYS && it.key !in IMAGE_KEYS && it.key !in schema }
            if (extra.isNotEmpty()) RawSection(extra, commit)
        }
    }
}

// ---------------------------------------------------------------------------
// Name, type and Transform
// ---------------------------------------------------------------------------

@Composable
private fun PropertiesHeader(
    state: LayoutEditorState,
    id: UUID,
    name: String,
    type: WidgetType,
    props: Map<String, String>,
    commit: (String, String) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AppIconView(widgetIcon(type), size = 18.dp)
        StringField(name, Modifier.weight(1f), focusTick = state.focusNameTick) { state.renameWidget(id, it) }
        TypeDropdown(type) { commitType(state, id, it) }
    }

    var open by remember { mutableStateOf(true) }
    SectionHeader("Transform", open) { open = !open }
    if (!open) return

    val hexactpos = props["hexactpos"] == "1"
    val vexactpos = props["vexactpos"] == "1"
    val hexactsize = props["hexactsize"] == "1"
    val vexactsize = props["vexactsize"] == "1"

    val posParts = (props["position"] ?: "0 0").trim().split(Regex("\\s+"))
    val szParts = (props["size"] ?: "0 0").trim().split(Regex("\\s+"))
    val px = displayTransformValue(posParts.getOrNull(0) ?: "0", hexactpos)
    val py = displayTransformValue(posParts.getOrNull(1) ?: "0", vexactpos)
    val sw = displayTransformValue(szParts.getOrNull(0) ?: "0", hexactsize)
    val sh = displayTransformValue(szParts.getOrNull(1) ?: "0", vexactsize)

    Column(Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            NumberField("X", px, hexactpos, Modifier.weight(1f), { switchUnit(state, id, props, "position", 0, "hexactpos", it) }) { v ->
                storedTransformPair(props["position"], 0, v, hexactpos)?.let { commit("position", it) }
            }
            NumberField("Y", py, vexactpos, Modifier.weight(1f), { switchUnit(state, id, props, "position", 1, "vexactpos", it) }) { v ->
                storedTransformPair(props["position"], 1, v, vexactpos)?.let { commit("position", it) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            NumberField("W", sw, hexactsize, Modifier.weight(1f), { switchUnit(state, id, props, "size", 0, "hexactsize", it) }) { v ->
                storedTransformPair(props["size"], 0, v, hexactsize)?.let { commit("size", it) }
            }
            NumberField("H", sh, vexactsize, Modifier.weight(1f), { switchUnit(state, id, props, "size", 1, "vexactsize", it) }) { v ->
                storedTransformPair(props["size"], 1, v, vexactsize)?.let { commit("size", it) }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AlignmentGrid(props["halign"].orEmpty(), props["valign"].orEmpty()) { h, v ->
                state.reanchor(id, h, v)
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                AlignText("halign", props["halign"])
                AlignText("valign", props["valign"])
            }
        }

        if (props["halign"] == "center" || props["valign"] == "center") {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF3A3220))
                    .border(1.dp, Color(0xFF6B5A2B))
                    .padding(8.dp),
                verticalAlignment = Alignment.Top,
            ) {
                AppIconView(AppIcon.UI_WARN, size = 16.dp)
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(Res.string.the_file_has_a_bare_center_such_a_widget),
                    color = Color(0xFFF0D9A0),
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                )
            }
        }
    }
}

@Composable
private fun AlignText(
    key: String,
    value: String?,
) {
    val shown =
        when (value) {
            null, "", "left", "top" -> "— (${value?.ifEmpty { null } ?: if (key == "halign") "left" else "top"})"
            "center" -> stringResource(Res.string.alignment_center_bare)
            else -> value
        }
    Row {
        Text("$key ", color = GhostlyText, fontSize = 11.sp)
        Text(shown, color = TextForeground, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ---------------------------------------------------------------------------
// AlignmentGrid 3×3
// ---------------------------------------------------------------------------

@Composable
private fun AlignmentGrid(
    halign: String,
    valign: String,
    onSet: (String, String) -> Unit,
) {
    val h0 = halign.ifEmpty { "left" }
    val v0 = valign.ifEmpty { "top" }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        for (v in VALIGN_GRID) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                for (h in HALIGN_GRID) {
                    val on = h0 == h && v0 == v
                    Tip("halign $h, valign $v") {
                        Box(
                            Modifier
                                .size(18.dp)
                                .background(if (on) SubSelectColor else DarkenedBorder)
                                .border(1.dp, if (on) SubSelectColor else Color(0xFF3A3C40))
                                .clickable(remember { MutableInteractionSource() }, null) { onSet(h, v) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(Modifier.size(4.dp).background(if (on) Color.White else Color(0xFF8A8F96)))
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Widget section
// ---------------------------------------------------------------------------

@Composable
private fun WidgetSection(
    props: Map<String, String>,
    commit: (String, String) -> Unit,
    documentColors: List<Color>,
    styles: StyleChoice,
    scriptParams: @Composable () -> Unit,
    scriptClass: @Composable RowScope.() -> Unit,
) {
    var expanded by remember { mutableStateOf(true) }
    SectionHeader("Widget", expanded) { expanded = !expanded }
    if (!expanded) return

    CheckRow("visible", props["visible"], commit, default = true)
    CheckRow("disabled", props["disabled"], commit)
    CheckRow("clipchildren", props["clipchildren"], commit, default = true)
    CheckRow("inheritalpha", props["inheritalpha"], commit)
    CheckRow("ignorepointer", props["ignorepointer"], commit)
    CheckRow("ignoregloballv", props["ignoregloballv"], commit)
    CheckRow("keepsafezone", props["keepsafezone"], commit)

    ColorRows(props, commit, documentColors)

    CheckRow("scaled", props["scaled"], commit)
    PropRow(
        "fixaspect",
        "fixaspect" in props,
        stringResource(Res.string.keeps_the_proportions_of_a_widget_with_a),
    ) {
        CompactDropdown(props["fixaspect"] ?: "", FIXASPECT_OPTS) { commit("fixaspect", it) }
    }
    PropRow("priority", "priority" in props, stringResource(Res.string.the_drawing_order_among_siblings_larger)) {
        StringField(props["priority"] ?: "", Modifier.weight(1f)) { commit("priority", it) }
    }
    PropRow("userID", "userID" in props) {
        StringField(props["userID"] ?: "", Modifier.weight(1f)) { commit("userID", it) }
    }
    CheckRow("draggable", props["draggable"], commit)
    PropRow("scriptclass", "scriptclass" in props, content = scriptClass)
    scriptParams()
    PropRow("style", "style" in props) {
        val names = styles.names
        val current = props["style"]?.trim('"').orEmpty()
        if (names.isEmpty()) {
            StringField(current, Modifier.weight(1f)) { commit("style", it) }
        } else {
            // The list holds the styles of the game for this widget; a style from elsewhere stays in the list as it is
            val options = (listOf("") + names + listOfNotNull(current.takeIf { it.isNotEmpty() && it !in names })).distinct()
            DropdownField(
                current,
                options,
                {
                    if (it.isEmpty()) {
                        str(Res.string.none)
                    } else if (it !in names) {
                        str(Res.string.x_custom, it)
                    } else {
                        it
                    }
                },
                Modifier.weight(1f),
                tooltip = stringResource(Res.string.widget_style),
            ) { commit("style", it) }
        }
    }
}

// ---------------------------------------------------------------------------
// ImageWidget section
// ---------------------------------------------------------------------------

@Composable
private fun ImageWidgetSection(
    props: Map<String, String>,
    commit: (String, String) -> Unit,
    workDrivePath: String,
    state: LayoutEditorState,
    node: EditorWidgetNode,
    nearPath: String?,
) {
    var expanded by remember { mutableStateOf(true) }
    SectionHeader("ImageWidget", expanded) { expanded = !expanded }
    if (!expanded) return

    val scope = rememberCoroutineScope()
    val basePath = workDrivePath.ifBlank { null }

    Row(
        Modifier.fillMaxWidth().height(ROW_H).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        PropLabel("image0", "image0" in props, null, Modifier.width(LABEL_W))
        // The quotes of the file are not shown; Enter or leaving the field confirms what was typed
        StringField(shownPath(props["image0"]), Modifier.weight(1f)) { commit("image0", storedPath(it, state)) }
        Tip(stringResource(Res.string.pick_an_icon_from_the_imagesets_of_the)) {
            SmallButton("...") { state.iconPicker = { ref -> commit("image0", "\"$ref\"") } }
        }
    }
    HLine()

    Row(
        Modifier.fillMaxWidth().height(ROW_H).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PropLabel("imageTexture", "imageTexture" in props, null, Modifier.width(LABEL_W))
        // The `{GUID}` that Workbench wants in front of the path is kept in the file and not shown
        StringField(shownPath(props["imageTexture"]), Modifier.weight(1f)) { commit("imageTexture", storedPath(it, state)) }
        Spacer(Modifier.width(2.dp))
        SmallButton("..") {
            scope.launch {
                pickFile(
                    str(Res.string.choose_a_texture),
                    "*",
                    basePath,
                )?.let { commit("imageTexture", storedPath(it, state)) }
            }
        }
    }
    HLine()
    ImagePreview(state.pictureFor(node, nearPath), (props["image0"] ?: props["imageTexture"]).orEmpty().isNotBlank())

    PropRow("mode", "mode" in props) { CompactDropdown(props["mode"] ?: "", MODE_OPTS) { commit("mode", it) } }
    CheckRow("src alpha", props["src alpha"], commit)
    PropRow("clamp mode", "clamp mode" in props) { CompactDropdown(props["clamp mode"] ?: "", CLAMP_OPTS) { commit("clamp mode", it) } }
    PropRow("stretch mode", "stretch mode" in props) {
        CompactDropdown(props["stretch mode"] ?: "", STRETCH_OPTS) { commit("stretch mode", it) }
    }
    CheckRow("flip u", props["flip u"], commit)
    CheckRow("flip v", props["flip v"], commit)
    CheckRow("filter", props["filter"], commit)
    CheckRow("nocache", props["nocache"], commit)

    PropRow("rotation", "rotation" in props) { StringField(props["rotation"] ?: "", Modifier.weight(1f)) { commit("rotation", it) } }

    val pivotParts = (props["pivot"] ?: "0 0").trim().split(Regex("\\s+"))
    var pivotX by remember(props["pivot"]) { mutableStateOf(pivotParts.getOrNull(0) ?: "0") }
    var pivotY by remember(props["pivot"]) { mutableStateOf(pivotParts.getOrNull(1) ?: "0") }
    SubRow("  pivot x") {
        FloatInput("", pivotX, Modifier.weight(1f)) { v ->
            pivotX = v
            commit("pivot", "$v $pivotY")
        }
    }
    SubRow("  pivot y") {
        FloatInput("", pivotY, Modifier.weight(1f)) { v ->
            pivotY = v
            commit("pivot", "$pivotX $v")
        }
    }

    Row(
        Modifier.fillMaxWidth().height(ROW_H).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PropLabel("Mask", "mask" in props, null, Modifier.width(LABEL_W))
        Spacer(Modifier.weight(1f))
        SmallButton("..") { scope.launch { pickFile(str(Res.string.choose_a_mask), "*", basePath)?.let { commit("mask", it) } } }
    }
    HLine()

    SliderRow("Transition width", props["transition width"], 0f, 1f) { commit("transition width", it) }
    SliderRow("Progress", props["progress"], 0f, 1f) { commit("progress", it) }
}

/** A path of an image property as the user sees it: without quotes and without the `{GUID}` in front. */
private fun shownPath(raw: String?): String =
    raw
        .orEmpty()
        .trim()
        .trim('"')
        .replace(Regex("^\\{[0-9A-Fa-f]+}"), "")

/** What goes into the file for a path typed or picked: quoted, `set:` references as they are, files with their `{GUID}`. */
private fun storedPath(
    text: String,
    state: LayoutEditorState,
): String {
    val clean = text.trim().trim('"')
    if (clean.isEmpty()) return ""
    return "\"" + (if (clean.startsWith("set:")) clean else state.textureReference(clean)) + "\""
}

/** A small look at the picture of an image widget, on a chequered ground so that a transparent one can be seen. */
@Composable
private fun ImagePreview(
    picture: androidx.compose.ui.graphics.ImageBitmap?,
    named: Boolean,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Spacer(Modifier.width(LABEL_W))
        androidx.compose.foundation.Canvas(
            Modifier
                .size(72.dp)
                .border(1.dp, Color(0xFF3A3C40)),
        ) {
            val cell = 8.dp.toPx()
            val cols = (size.width / cell).toInt() + 1
            val rows = (size.height / cell).toInt() + 1
            for (y in 0 until rows) {
                for (x in 0 until cols) {
                    drawRect(
                        if ((x + y) % 2 == 0) Color(0xFF3A3A3A) else Color(0xFF2C2C2C),
                        Offset(x * cell, y * cell),
                        Size(cell, cell),
                    )
                }
            }
            if (picture != null) {
                val scale = minOf(size.width / picture.width, size.height / picture.height)
                val w = (picture.width * scale).toInt().coerceAtLeast(1)
                val h = (picture.height * scale).toInt().coerceAtLeast(1)
                drawImage(
                    picture,
                    srcSize =
                        androidx.compose.ui.unit
                            .IntSize(picture.width, picture.height),
                    dstOffset =
                        androidx.compose.ui.unit.IntOffset(
                            ((size.width - w) / 2).toInt(),
                            ((size.height - h) / 2).toInt(),
                        ),
                    dstSize =
                        androidx.compose.ui.unit
                            .IntSize(w, h),
                    filterQuality = androidx.compose.ui.graphics.FilterQuality.Medium,
                )
            }
        }
        Text(
            when {
                picture != null -> "${picture.width} × ${picture.height}"
                named -> stringResource(Res.string.not_found)
                else -> stringResource(Res.string.no_image_3)
            },
            color = GhostlyText,
            fontSize = 10.sp,
        )
    }
    HLine()
}

// ---------------------------------------------------------------------------
// Raw extra properties
// ---------------------------------------------------------------------------

@Composable
private fun SchemaSections(
    type: WidgetType,
    originalClassName: String,
    props: Map<String, String>,
    commit: (String, String) -> Unit,
    fonts: List<Pair<String, String>> = emptyList(),
    importFont: () -> Unit = {},
    pickString: (((String) -> Unit)) -> Unit = {},
) {
    for ((index, section) in schemaFor(type, originalClassName).withIndex()) {
        // The group of the focus keys is rare to touch, so it starts closed unless the file sets one of them
        var expanded by remember(section.title) { mutableStateOf(section.title != "Focus" || section.specs.any { it.key in props }) }
        SectionHeader(section.title, expanded) { expanded = !expanded }
        if (expanded) for (spec in section.specs) SchemaRow(importFont, spec, props, commit, fonts, pickString)
    }
}

/** A row of a property of the widget class: set in the file or not, with a button that takes it out of the file again. */
@Composable
private fun SchemaRow(
    importFont: () -> Unit,
    spec: PropSpec,
    props: Map<String, String>,
    commit: (String, String) -> Unit,
    fonts: List<Pair<String, String>> = emptyList(),
    pickString: (((String) -> Unit)) -> Unit = {},
) {
    val raw = props[spec.key]
    val hint = stringResource(spec.hint) + if (spec.default.isNotEmpty()) "\n" + stringResource(Res.string.default_x, spec.default) else ""
    if (spec.kind == PropKind.CHECK) {
        CheckRow(spec.key, raw, commit, hint, spec.default == "1")
        return
    }
    PropRow(spec.key, raw != null, hint) {
        when (spec.kind) {
            PropKind.ENUM -> {
                val current = raw?.trim()?.trim('"').orEmpty()
                CompactDropdown(
                    current,
                    (
                        listOf("") + spec.options +
                            listOfNotNull(current.takeIf { it.isNotEmpty() && it !in spec.options })
                    ).distinct(),
                ) {
                    commit(spec.key, it)
                }
            }

            PropKind.TEXT -> {
                val current = raw?.trim()?.removeSurrounding("\"").orEmpty()
                if (spec.key == "font" && fonts.isNotEmpty()) {
                    // The fonts the game and the connected mods have, like the styles: a select, with an empty choice for the default font
                    val labels = fonts.toMap()
                    DropdownField(
                        current,
                        listOf("") + fonts.map { it.first },
                        { if (it.isEmpty()) str(Res.string.none) else labels[it] ?: it },
                        Modifier.weight(1f),
                        tooltip = stringResource(Res.string.font_of_the_text),
                    ) { commit(spec.key, if (it.isEmpty()) "" else "\"$it\"") }
                    SmallButton("+") { importFont() }
                } else {
                    StringField(current, Modifier.weight(1f)) {
                        commit(spec.key, if (it.isEmpty()) "" else "\"$it\"")
                    }
                    // The text of a widget may be a key of a string table: the button lists the keys of the game and of the mods
                    if (spec.key == "text") {
                        SmallButton("#") { pickString { key -> commit(spec.key, "\"#$key\"") } }
                    }
                }
            }

            PropKind.COLOR -> {
                val shown = raw?.let { parseWidgetColor(it) }
                if (shown != null) Box(Modifier.size(14.dp).background(shown).border(1.dp, DarkenedBorder))
                StringField(raw.orEmpty(), Modifier.weight(1f)) { commit(spec.key, it.trim()) }
            }

            else -> {
                StringField(raw.orEmpty(), Modifier.weight(1f)) { commit(spec.key, it.trim()) }
            }
        }
        if (raw != null) SmallButton("×") { commit(spec.key, "") }
    }
    if (spec.key == "text") StringPreview(raw?.trim()?.removeSurrounding("\"").orEmpty())
}

@Composable
private fun RawSection(
    props: Map<String, String>,
    commit: (String, String) -> Unit,
) {
    var expanded by remember { mutableStateOf(true) }
    SectionHeader("Extra", expanded) { expanded = !expanded }
    if (!expanded) return
    for ((key, value) in props) {
        PropRow(key, set = true, hint = stringResource(PROPERTY_HINTS[key] ?: UNKNOWN_PROPERTY_HINT)) {
            StringField(value, Modifier.weight(1f)) { commit(key, it) }
        }
    }
}

// ---------------------------------------------------------------------------
// Color rows
// ---------------------------------------------------------------------------

@Composable
private fun ColorRows(
    props: Map<String, String>,
    commit: (String, String) -> Unit,
    documentColors: List<Color>,
) {
    val cv = props["color"] ?: ""
    val parts = cv.trim().split(Regex("\\s+"))
    var r by remember(cv) { mutableStateOf(parts.getOrNull(0) ?: "0") }
    var g by remember(cv) { mutableStateOf(parts.getOrNull(1) ?: "0") }
    var b by remember(cv) { mutableStateOf(parts.getOrNull(2) ?: "0") }
    var a by remember(cv) { mutableStateOf(parts.getOrNull(3) ?: "1") }

    fun save() = commit("color", "$r $g $b $a")

    val shown = remember(r, g, b, a) { parseWidgetColor("$r $g $b $a") }

    Row(
        Modifier.fillMaxWidth().height(ROW_H).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        PropLabel("color", "color" in props, stringResource(Res.string.the_colours_in_the_file_are_linear), Modifier.width(LABEL_W))
        var picking by remember { mutableStateOf(false) }
        val anchorHeight = with(LocalDensity.current) { 22.dp.roundToPx() }
        Box {
            Tip(stringResource(Res.string.pick_a_colour)) {
                Box(
                    Modifier
                        .size(22.dp)
                        .background(Color(0xFF2B2B30))
                        .background(shown)
                        .border(1.dp, Color(0xFF3A3C40))
                        .clickable(remember { MutableInteractionSource() }, null) { picking = true },
                )
            }
            if (picking) {
                ColorPickerPopup(
                    initial = shown,
                    anchorHeightPx = anchorHeight,
                    onDismiss = { picking = false },
                    documentColors = documentColors,
                    onCommit = { commit("color", it.toFileColor()) },
                )
            }
        }
        FloatInput("R", r, Modifier.weight(1f)) { v ->
            r = v
            save()
        }
        FloatInput("G", g, Modifier.weight(1f)) { v ->
            g = v
            save()
        }
        FloatInput("B", b, Modifier.weight(1f)) { v ->
            b = v
            save()
        }
        FloatInput("A", a, Modifier.weight(1f)) { v ->
            a = v
            save()
        }
    }
    HLine()
}

// ---------------------------------------------------------------------------
// Slider row
// ---------------------------------------------------------------------------

@Composable
private fun SliderRow(
    label: String,
    raw: String?,
    min: Float,
    max: Float,
    onCommit: (String) -> Unit,
) {
    val current = raw?.toFloatOrNull()?.coerceIn(min, max) ?: min
    var text by remember(raw) { mutableStateOf(raw ?: formatShort(min)) }
    Row(
        Modifier.fillMaxWidth().height(ROW_H).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        PropLabel(label, raw != null, null, Modifier.width(LABEL_W))
        Slider(
            value = current,
            onValueChange = { v ->
                text = formatShort(v)
                onCommit(formatShort(v))
            },
            valueRange = min..max,
            modifier = Modifier.weight(1f).height(ROW_H),
            colors = SliderDefaults.colors(thumbColor = SelectColor, activeTrackColor = SelectColor, inactiveTrackColor = DarkenedBorder),
        )
        var focused by remember { mutableStateOf(false) }
        BasicTextField(
            value = text,
            onValueChange = { text = it },
            singleLine = true,
            textStyle = TextStyle(color = TextForeground, fontSize = 10.sp, fontFamily = FontFamily.Monospace),
            cursorBrush = SolidColor(TextForeground),
            decorationBox = { inner ->
                Box(
                    Modifier
                        .width(
                            40.dp,
                        ).background(FrameBackground)
                        .border(1.dp, DarkenedBorder)
                        .padding(horizontal = 3.dp, vertical = 2.dp),
                ) { inner() }
            },
            modifier =
                Modifier
                    .onFocusChanged { fs ->
                        if (focused && !fs.isFocused) onCommit(text)
                        focused = fs.isFocused
                    }.onKeyEvent { ev ->
                        if (ev.key == Key.Enter && ev.type == KeyEventType.KeyDown) {
                            onCommit(text)
                            true
                        } else {
                            false
                        }
                    },
        )
    }
    HLine()
}

// ---------------------------------------------------------------------------
// Row primitives
// ---------------------------------------------------------------------------

@Composable
private fun SectionHeader(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(24.dp)
            .background(ElementsBackground)
            .clickable(remember { MutableInteractionSource() }, null, onClick = onToggle)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIconView(if (expanded) AppIcon.UI_DOWN else AppIcon.UI_RIGHT, size = 12.dp, tint = GhostlyText)
        Spacer(Modifier.width(6.dp))
        Text(title, color = TextForeground, fontSize = 11.sp)
    }
}

@Composable
private fun PropRow(
    label: String,
    set: Boolean = false,
    hint: String? = null,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = ROW_H).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        PropLabel(label, set, hint, Modifier.width(LABEL_W))
        content()
    }
    HLine()
}

/** A property name. Bold with a lighter colour when the file sets it; a hint shows in a tooltip. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PropLabel(
    label: String,
    set: Boolean,
    hint: String?,
    modifier: Modifier = Modifier,
) {
    val tip = hint ?: PROPERTY_HINTS[label.trim()]?.let { stringResource(it) }
    val text: @Composable () -> Unit = {
        Text(
            label.trim(),
            color = if (set) TextForeground else GhostlyText,
            fontWeight = if (set) FontWeight.SemiBold else null,
            fontSize = 10.sp,
            modifier = modifier,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
    if (tip == null) {
        text()
    } else {
        TooltipArea(
            tooltip = {
                Text(
                    tip,
                    color = TextForeground,
                    fontSize = 11.sp,
                    modifier =
                        Modifier
                            .widthIn(
                                max = 240.dp,
                            ).background(ElementsBackground)
                            .border(1.dp, DarkenedBorder)
                            .padding(8.dp, 4.dp),
                )
            },
            delayMillis = 400,
        ) { text() }
    }
}

@Composable
private fun SubRow(
    label: String,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = ROW_H).padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        PropLabel(label, false, null, Modifier.width(LABEL_W))
        content()
    }
    HLine()
}

@Composable
private fun CheckRow(
    key: String,
    value: String?,
    commit: (String, String) -> Unit,
    hint: String? = null,
    default: Boolean = false,
) {
    val checked = value?.let { it.trim() == "1" } ?: default
    Row(
        Modifier
            .fillMaxWidth()
            .height(24.dp)
            .clickable(remember { MutableInteractionSource() }, null) { commit(key, if (checked) "0" else "1") }
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(14.dp)
                .background(if (checked) SubSelectColor else DarkenedBorder)
                .border(1.dp, if (checked) SubSelectColor else Color(0xFF3A3C40)),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) Text("✓", color = Color.White, fontSize = 10.sp, lineHeight = 10.sp)
        }
        Spacer(Modifier.width(8.dp))
        PropLabel(key, value != null, hint, Modifier)
    }
}

@Composable
private fun HLine() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x22FFFFFF)))
}

// ---------------------------------------------------------------------------
// NumberField
// ---------------------------------------------------------------------------

/** A label, a one-line number and its own px/% switch. Arrows and the wheel step by 1, with Shift by 10. */
@Composable
private fun NumberField(
    label: String,
    value: String,
    isPx: Boolean,
    modifier: Modifier = Modifier,
    onUnit: (Boolean) -> Unit,
    onCommit: (String) -> Unit,
) {
    var text by remember(value) { mutableStateOf(value) }
    var focused by remember { mutableStateOf(false) }
    val latestCommit = rememberUpdatedState(onCommit)

    fun step(delta: Float) {
        val v = text.toFloatOrNull() ?: 0f
        val result = formatShort(v + delta)
        text = result
        latestCommit.value(result)
    }

    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        PropLabel(label, false, null, Modifier.width(14.dp))
        BasicTextField(
            value = text,
            onValueChange = { text = it },
            singleLine = true,
            textStyle =
                TextStyle(
                    color = TextForeground,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = if (text.toFloatOrNull()?.let { it != 0f } == true) FontWeight.Bold else null,
                    textAlign = TextAlign.End,
                ),
            cursorBrush = SolidColor(TextForeground),
            decorationBox = { inner ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .background(DarkenedBorder)
                        .border(1.dp, if (focused) SubSelectColor else Color(0xFF3A3C40))
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.CenterEnd,
                ) { inner() }
            },
            modifier =
                Modifier
                    .weight(1f)
                    .pointerInput(value, isPx) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Main)
                                if (event.type != PointerEventType.Scroll) continue
                                val dy =
                                    event.changes
                                        .firstOrNull()
                                        ?.scrollDelta
                                        ?.y ?: 0f
                                if (dy == 0f) continue
                                val delta = if (event.keyboardModifiers.isShiftPressed) 10f else 1f
                                step(if (dy > 0f) -delta else delta)
                                event.changes.forEach { it.consume() }
                            }
                        }
                    }.onFocusChanged { fs ->
                        if (focused && !fs.isFocused && text != value) latestCommit.value(text)
                        focused = fs.isFocused
                    }.onKeyEvent { ev ->
                        when {
                            ev.key == Key.Enter && ev.type == KeyEventType.KeyDown -> {
                                if (text != value) latestCommit.value(text)
                                true
                            }

                            ev.key == Key.DirectionUp && ev.type == KeyEventType.KeyDown -> {
                                step(if (ev.isShiftPressed) 10f else 1f)
                                true
                            }

                            ev.key == Key.DirectionDown && ev.type == KeyEventType.KeyDown -> {
                                step(if (ev.isShiftPressed) -10f else -1f)
                                true
                            }

                            else -> {
                                false
                            }
                        }
                    },
        )
        UnitSwitch(isPx, onUnit)
    }
}

@Composable
private fun UnitSwitch(
    isPx: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Tip(
        if (isPx) {
            stringResource(
                Res.string.pixels_click_to_switch_to_a_share_of_the,
            )
        } else {
            stringResource(Res.string.a_share_of_the_parent_click_to_switch_to)
        },
    ) {
        Box(
            Modifier
                .height(24.dp)
                .widthIn(min = 24.dp)
                .background(ElementsBackground)
                .border(1.dp, Color(0xFF3A3C40))
                .clickable(remember { MutableInteractionSource() }, null) { onChange(!isPx) }
                .padding(horizontal = 3.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(if (isPx) "px" else "%", color = GhostlyText, fontSize = 10.sp)
        }
    }
}

// ---------------------------------------------------------------------------
// Controls
// ---------------------------------------------------------------------------

@Composable
private fun TypeDropdown(
    current: WidgetType,
    onSelect: (WidgetType) -> Unit,
) {
    DropdownField(
        current,
        WidgetType.entries.filter {
            it != WidgetType.UNKNOWN
        },
        { it.className.removeSuffix("WidgetClass") },
        onSelect = onSelect,
    )
}

@Composable
private fun CompactDropdown(
    value: String,
    options: List<String>,
    onCommit: (String) -> Unit,
) {
    DropdownField(value, options, { it.ifEmpty { "—" } }, onSelect = onCommit)
}

@Composable
private fun SmallButton(
    label: String,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .background(ElementsBackground)
            .border(1.dp, DarkenedBorder)
            .clickable(remember { MutableInteractionSource() }, null, onClick = onClick)
            .padding(horizontal = 5.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = GhostlyText, fontSize = 10.sp)
    }
}

@Composable
private fun StringField(
    value: String,
    modifier: Modifier = Modifier,
    focusTick: Int = 0,
    onCommit: (String) -> Unit,
) {
    var field by remember(value) { mutableStateOf(TextFieldValue(value)) }
    var focused by remember { mutableStateOf(false) }
    val requester = remember { FocusRequester() }
    // F2: the cursor comes here and the whole name is selected, so that typing replaces it
    LaunchedEffect(focusTick) {
        if (focusTick > 0) {
            runCatching { requester.requestFocus() }
            field = field.copy(selection = TextRange(0, field.text.length))
        }
    }
    BasicTextField(
        value = field,
        onValueChange = { field = it },
        singleLine = true,
        textStyle = TextStyle(color = TextForeground, fontSize = 11.sp, fontFamily = FontFamily.Monospace),
        cursorBrush = SolidColor(TextForeground),
        decorationBox = { inner ->
            Box(Modifier.background(FrameBackground).border(1.dp, DarkenedBorder).padding(horizontal = 4.dp, vertical = 2.dp)) { inner() }
        },
        modifier =
            modifier
                .focusRequester(requester)
                .onFocusChanged { fs ->
                    if (focused && !fs.isFocused && field.text != value) onCommit(field.text)
                    focused = fs.isFocused
                }.onKeyEvent { ev ->
                    if (ev.key == Key.Enter && ev.type == KeyEventType.KeyDown) {
                        if (field.text != value) onCommit(field.text)
                        true
                    } else {
                        false
                    }
                },
    )
}

@Composable
private fun FloatInput(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onCommit: (String) -> Unit,
) {
    var text by remember(value) { mutableStateOf(value) }
    var focused by remember { mutableStateOf(false) }
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        if (label.isNotEmpty()) {
            Text(label, color = GhostlyText, fontSize = 9.sp, modifier = Modifier.padding(end = 2.dp))
        }
        BasicTextField(
            value = text,
            onValueChange = { text = it },
            singleLine = true,
            textStyle = TextStyle(color = TextForeground, fontSize = 11.sp, fontFamily = FontFamily.Monospace),
            cursorBrush = SolidColor(TextForeground),
            decorationBox = { inner ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(
                            FrameBackground,
                        ).border(1.dp, DarkenedBorder)
                        .padding(horizontal = 3.dp, vertical = 2.dp),
                ) {
                    inner()
                }
            },
            modifier =
                Modifier
                    .weight(1f)
                    .onFocusChanged { fs ->
                        if (focused && !fs.isFocused && text != value) onCommit(text)
                        focused = fs.isFocused
                    }.onKeyEvent { ev ->
                        if (ev.key == Key.Enter && ev.type == KeyEventType.KeyDown && text != value) {
                            onCommit(text)
                            true
                        } else {
                            false
                        }
                    },
        )
    }
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

/** The styles that can be chosen for the selected widget: the vanilla ones of its type. */
private class StyleChoice(
    state: LayoutEditorState,
    type: WidgetType,
    layoutPath: String?,
) {
    val names: List<String> = state.vanillaStylesFor(type.className, layoutPath).map { it.name }.distinct()
}

/** The fields of `ScriptParamsClass` of the script class on the widget: those the class reads, then any others the layout has. */
@Composable
private fun ScriptParamRows(
    state: LayoutEditorState,
    node: EditorWidgetNode,
) {
    val className = node.scriptClassName() ?: return
    val known = remember(className, state.scriptClasses) { state.scriptParamsOf(className) }
    val given = node.node.scriptParams.orEmpty()
    val extra =
        given.keys
            .filter { key ->
                known.none { it.name == key }
            }.map { ScriptParam(it, "", stringResource(Res.string.present_in_the_layout_the_class_does_not)) }
    if (known.isEmpty() && extra.isEmpty()) return
    Text(
        stringResource(Res.string.script_class_parameters),
        color = GhostlyText,
        fontSize = 9.sp,
        modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 6.dp, bottom = 2.dp),
    )
    for (param in known + extra) {
        val value = given[param.name]?.trim()?.trim('"')
        val set = { text: String ->
            state.setScriptParam(
                node.id,
                param.name,
                ScriptParams.formatted(param.takeIf { it.type.isNotEmpty() }, given[param.name], text),
            )
        }
        if (param.isFlag) {
            CheckRow(param.name, value, { _, v -> set(v) }, param.hint())
        } else {
            PropRow(param.name, param.name in given, param.hint()) {
                Box(Modifier.weight(1f)) {
                    StringField(value.orEmpty(), Modifier.fillMaxWidth()) { set(it) }
                    if (value.isNullOrEmpty() && param.default.isNotEmpty()) {
                        Text(
                            param.default,
                            color = GhostlyText,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Several widgets at once
// ---------------------------------------------------------------------------

/** The properties of the selected widgets as one: the same value where they agree, [MIXED] where they do not, and a change goes to all. */
@Composable
private fun ColumnScope.MultiProperties(
    state: LayoutEditorState,
    nodes: List<EditorWidgetNode>,
    layoutPath: String?,
) {
    val merged = mergedProperties(nodes)
    // A colour that differs shows as unset; the channels cannot be Mixed one by one
    val props = merged.filter { (key, value) -> !(key == "color" && value == MIXED) }
    val commit: (String, String) -> Unit = { key, value -> state.setOnSelected(key, value) }
    val types = nodes.map { it.node.type }.distinct()

    Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
        Row(
            Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AppIconView(AppIcon.UI_LAYERS, size = 18.dp)
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(Res.string.x_widgets, nodes.size),
                    color = TextForeground,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    nodes
                        .groupingBy {
                            it.node.type.className
                                .removeSuffix("WidgetClass")
                        }.eachCount()
                        .entries
                        .joinToString(", ") { (type, count) -> if (count > 1) "$type × $count" else type },
                    color = GhostlyText,
                    fontSize = 10.sp,
                    maxLines = 2,
                )
            }
        }
        MultiTransform(state)
        WidgetSection(
            props,
            commit,
            remember(state.activeFile?.root) {
                state.activeFile
                    ?.root
                    ?.let { documentColors(it) }
                    .orEmpty()
            },
            StyleChoice(state, types.singleOrNull() ?: WidgetType.UNKNOWN, layoutPath),
            {},
        ) {
            StringField(props["scriptclass"]?.trim('"').orEmpty(), Modifier.weight(1f)) { commit("scriptclass", it) }
        }
        if (types.all { it in IMAGE_TYPES }) ImageWidgetSection(props, commit, state.workDrivePath, state, nodes.first(), layoutPath)
        val sameType = nodes.map { it.node.type to it.node.originalClassName }.distinct().singleOrNull()
        if (sameType !=
            null
        ) {
            SchemaSections(sameType.first, sameType.second, props, commit, fontOptions(state, layoutPath, props["font"]), {
                state.fontImportOpen =
                    true
            }, { onPick -> state.stringPicker = onPick })
        }
        val schema = sameType?.let { schemaKeys(it.first, it.second) }.orEmpty()
        val extra = props.filter { it.key !in WIDGET_KEYS && it.key !in IMAGE_KEYS && it.key !in schema }
        if (extra.isNotEmpty()) RawSection(extra, commit)
    }
}

/** The rectangle around the selected widgets: moving its corner moves them, changing its size scales them together. */
@Composable
private fun MultiTransform(state: LayoutEditorState) {
    var open by remember { mutableStateOf(true) }
    SectionHeader("Transform", open) { open = !open }
    if (!open) return
    val bounds = state.selectionRect() ?: return
    Column(Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            NumberField("X", formatNumber(bounds.left), true, Modifier.weight(1f), {
            }) { v -> v.toFloatOrNull()?.let { state.moveSelectionTo(it, null) } }
            NumberField("Y", formatNumber(bounds.top), true, Modifier.weight(1f), {
            }) { v -> v.toFloatOrNull()?.let { state.moveSelectionTo(null, it) } }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            NumberField("W", formatNumber(bounds.width), true, Modifier.weight(1f), {}) { v ->
                v.toFloatOrNull()?.takeIf { it > 0f }?.let { state.scaleSelectionTo(it, null) }
            }
            NumberField("H", formatNumber(bounds.height), true, Modifier.weight(1f), {}) { v ->
                v.toFloatOrNull()?.takeIf { it > 0f }?.let { state.scaleSelectionTo(null, it) }
            }
        }
        Text(
            stringResource(Res.string.selection_frame_hint),
            color = GhostlyText,
            fontSize = 10.sp,
            lineHeight = 13.sp,
        )
    }
}

/** Under the text of a widget that is a `#key`: what the key says in the language of the editor, or that it is not found. */
@Composable
private fun StringPreview(text: String) {
    if (!text.startsWith("#") || text.length < 2) return
    val key = text.substring(1)
    val entry = StringtableRegistry.find(key)
    val shown = StringtableRegistry.translate(key)
    val line =
        when {
            entry == null -> stringResource(Res.string.the_key_is_not_in_the_string_tables_the)
            shown == null -> stringResource(Res.string.the_table_has_no_text_for_the_language)
            else -> shown
        }
    Row(Modifier.fillMaxWidth().padding(start = LABEL_W, end = 4.dp, bottom = 3.dp)) {
        Text(line, color = if (shown != null) TextForeground else GhostlyText, fontSize = 10.sp, maxLines = 2)
    }
}
