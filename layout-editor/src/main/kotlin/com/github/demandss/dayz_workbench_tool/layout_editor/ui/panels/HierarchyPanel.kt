package com.github.demandss.dayz_workbench_tool.layout_editor.ui.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.demandss.dayz_workbench_tool.layout_editor.EditorWidgetNode
import com.github.demandss.dayz_workbench_tool.layout_editor.state.DragSession
import com.github.demandss.dayz_workbench_tool.layout_editor.state.DragSource
import com.github.demandss.dayz_workbench_tool.layout_editor.state.LayoutEditorState
import com.github.demandss.dayz_workbench_tool.layout_editor.ui.canvas.SelectMode
import com.github.demandss.dayz_workbench_tool.layout_editor.ui.canvas.hasBareCenter
import com.github.demandss.dayz_workbench_tool.layout_editor.ui.canvas.parentOf
import com.github.demandss.dayz_workbench_tool.layout_editor.ui.canvas.selectBy
import com.github.demandss.dayz_workbench_tool.layout_editor.ui.canvas.selectMode
import com.github.demandss.dayz_workbench_tool.resources.*
import com.github.demandss.dayz_workbench_tool.ui.components.Tip
import com.github.demandss.dayz_workbench_tool.ui.icons.AppIcon
import com.github.demandss.dayz_workbench_tool.ui.icons.AppIconView
import com.github.demandss.dayz_workbench_tool.ui.icons.widgetIcon
import com.github.demandss.dayz_workbench_tool.ui.theme.*
import org.jetbrains.compose.resources.stringResource
import java.util.UUID

private fun buildFlatList(
    node: EditorWidgetNode,
    depth: Int,
    expandedIds: Set<UUID>,
    result: MutableList<Pair<EditorWidgetNode, Int>>,
) {
    result.add(node to depth)
    if (expandedIds.contains(node.id)) {
        for (child in node.children) buildFlatList(child, depth + 1, expandedIds, result)
    }
}

@Composable
fun HierarchyPanel(
    state: LayoutEditorState,
    modifier: Modifier = Modifier,
) {
    val doc = state.activeFile
    val expandedIds = remember { mutableStateListOf<UUID>() }
    var query by remember { mutableStateOf("") }
    var editingId by remember { mutableStateOf<UUID?>(null) }
    val listState = rememberLazyListState()

    LaunchedEffect(doc?.path) {
        expandedIds.clear()
        query = ""
        editingId = null
        doc?.root?.id?.let { expandedIds.add(it) }
    }

    // A widget picked on the canvas is shown in the tree: the folders above it open up
    val picked = state.selectedIds.lastOrNull()
    LaunchedEffect(picked, doc?.root) {
        val root = doc?.root ?: return@LaunchedEffect
        var parent = picked?.let { root.parentOf(it) }
        while (parent != null) {
            if (parent.id !in expandedIds) expandedIds.add(parent.id)
            parent = root.parentOf(parent.id)
        }
    }

    Column(
        modifier
            .background(FrameBackground)
            .onGloballyPositioned { coords ->
                state.hierarchyBoundsInWindow = coords.boundsInWindow()
            },
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(22.dp)
                .background(ElementsBackground)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Hierarchy", color = GhostlyText, fontSize = 10.sp)
        }
        SearchField(query, stringResource(Res.string.search_for_a_widget)) { query = it }

        if (doc == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(Res.string.no_open_files), color = GhostlyText, fontSize = 11.sp)
            }
            return@Column
        }

        val flatList =
            remember(doc.root, expandedIds.toSet(), query) {
                mutableListOf<Pair<EditorWidgetNode, Int>>().also {
                    if (query.isBlank()) {
                        buildFlatList(doc.root, 0, expandedIds.toHashSet(), it)
                    } else {
                        // A search shows the widgets that match, flat
                        val words = query.trim().lowercase().split(Regex("\\s+"))

                        fun visit(n: EditorWidgetNode) {
                            val hay = (n.node.name + " " + n.node.type.className).lowercase()
                            if (words.all { w -> w in hay }) it.add(n to 0)
                            n.children.forEach(::visit)
                        }
                        visit(doc.root)
                    }
                }
            }

        // Scroll to the picked widget when it is not in view
        LaunchedEffect(picked, flatList.size) {
            val index = flatList.indexOfFirst { (n, _) -> n.id == picked }
            if (index >= 0 && listState.layoutInfo.visibleItemsInfo.none { it.index == index }) listState.animateScrollToItem(index)
        }

        val dragSrc = state.dragSession?.source
        val isDragging = dragSrc != null

        LazyColumn(Modifier.fillMaxSize(), state = listState) {
            items(flatList, key = { (node, _) -> node.id.toString() }) { (node, depth) ->
                val isExpanded = expandedIds.contains(node.id)
                val isSelected = state.selectedIds.contains(node.id)
                val isDragTarget = isDragging && state.dragHoverNodeId == node.id
                val isDragSource = dragSrc is DragSource.FromHierarchy && dragSrc.nodeId == node.id

                Box(
                    Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { coords ->
                            state.hierarchyRowBounds[node.id] = coords.boundsInWindow()
                        },
                ) {
                    HierarchyRow(
                        node = node,
                        depth = depth,
                        isExpanded = isExpanded,
                        isSelected = isSelected,
                        isDragTarget = isDragTarget,
                        isDragSource = isDragSource,
                        isHidden = state.hiddenIds.contains(node.id),
                        isLocked = state.lockedIds.contains(node.id),
                        isRenaming = editingId == node.id,
                        onToggleHidden = { state.toggleHidden(node.id) },
                        onToggleLocked = { state.toggleLocked(node.id) },
                        onToggleExpand = {
                            if (expandedIds.contains(node.id)) {
                                expandedIds.remove(node.id)
                            } else {
                                expandedIds.add(node.id)
                            }
                        },
                        onSelect = { mode ->
                            state.canvasFocusTick++
                            state.selectBy(node.id, flatList.map { (n, _) -> n.id }, mode)
                        },
                        onRenameRequest = { editingId = node.id },
                        onRenameCommit = { name ->
                            editingId = null
                            state.renameWidget(node.id, name)
                        },
                        onRenameCancel = { editingId = null },
                        onDragStart = {
                            state.dragSession = DragSession(DragSource.FromHierarchy(node.id))
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun HierarchyRow(
    node: EditorWidgetNode,
    depth: Int,
    isExpanded: Boolean,
    isSelected: Boolean,
    isDragTarget: Boolean,
    isDragSource: Boolean,
    isHidden: Boolean,
    isLocked: Boolean,
    isRenaming: Boolean,
    onToggleHidden: () -> Unit,
    onToggleLocked: () -> Unit,
    onToggleExpand: () -> Unit,
    onSelect: (SelectMode) -> Unit,
    onRenameRequest: () -> Unit,
    onRenameCommit: (String) -> Unit,
    onRenameCancel: () -> Unit,
    onDragStart: () -> Unit,
) {
    val bg =
        when {
            isDragTarget -> Color(0xFF1E3A5F)
            isSelected -> SelectColor
            else -> Color.Transparent
        }
    val borderMod =
        if (isDragTarget) {
            Modifier.border(1.dp, Color(0xFF4A9EFF))
        } else {
            Modifier
        }
    val rowCoordinates = remember { mutableStateOf<LayoutCoordinates?>(null) }
    val nameBounds = remember { mutableStateOf<Rect?>(null) }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(22.dp)
                .then(borderMod)
                .background(bg)
                .padding(start = (depth * 14).dp)
                .then(
                    if (isDragSource) Modifier.background(Color(0x40FFFFFF)) else Modifier,
                ).onGloballyPositioned { rowCoordinates.value = it }
                .pointerInput(node.id, isRenaming) {
                    var pressPos = Offset.Zero
                    var pressed = false
                    var lastClickTime = 0L
                    var lastClickPos = Offset.Zero
                    var lastClickOnName = false
                    var dragStarted = false

                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val pos = event.changes.firstOrNull()?.position ?: continue

                            when (event.type) {
                                PointerEventType.Press -> {
                                    if (!isRenaming && event.buttons.isPrimaryPressed && event.changes.none { it.isConsumed }) {
                                        pressPos = pos
                                        pressed = true
                                        dragStarted = false
                                        event.changes.forEach { it.consume() }
                                    }
                                }

                                PointerEventType.Move -> {
                                    if (pressed && !dragStarted) {
                                        val dist = (pos - pressPos).getDistance()
                                        if (dist > 8f) {
                                            dragStarted = true
                                            lastClickTime = 0L
                                            onDragStart()
                                        }
                                        event.changes.forEach { it.consume() }
                                    }
                                }

                                PointerEventType.Release -> {
                                    if (pressed) {
                                        pressed = false
                                        if (!dragStarted) {
                                            val now = event.changes.first().uptimeMillis
                                            val isDouble =
                                                lastClickTime != 0L && now - lastClickTime in 0..viewConfiguration.doubleTapTimeoutMillis &&
                                                    (pos - lastClickPos).getDistance() <= 8f
                                            val nameClick =
                                                rowCoordinates.value?.localToWindow(pos)?.let { nameBounds.value?.contains(it) } == true
                                            when {
                                                isDouble && nameClick && lastClickOnName -> {
                                                    onRenameRequest()
                                                }

                                                isDouble && node.children.isNotEmpty() -> {
                                                    onToggleExpand()
                                                }

                                                else -> {
                                                    onSelect(
                                                        selectMode(
                                                            event.keyboardModifiers.isShiftPressed,
                                                            event.keyboardModifiers.isCtrlPressed,
                                                        ),
                                                    )
                                                }
                                            }
                                            lastClickTime = if (isDouble) 0L else now
                                            lastClickPos = pos
                                            lastClickOnName = nameClick
                                        }
                                        dragStarted = false
                                        event.changes.forEach { it.consume() }
                                    }
                                }

                                else -> {}
                            }
                        }
                    }
                },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(16.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = node.children.isNotEmpty(),
                    onClick = onToggleExpand,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (node.children.isNotEmpty()) {
                AppIconView(if (isExpanded) AppIcon.UI_DOWN else AppIcon.UI_RIGHT, size = 12.dp, tint = GhostlyText)
            }
        }

        AppIconView(widgetIcon(node.node.type), modifier = Modifier.padding(horizontal = 3.dp))

        Box(Modifier.padding(start = 2.dp).weight(1f)) {
            if (isRenaming) {
                HierarchyNameField(node.node.name, Modifier.fillMaxWidth(), onRenameCommit, onRenameCancel)
            } else {
                Text(
                    node.node.name.ifEmpty { if (depth == 0) "(root)" else "—" },
                    color = if (isSelected) TextForeground else Color(0xCCCCCCCC),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.onGloballyPositioned { nameBounds.value = it.boundsInWindow() },
                )
            }
        }
        if (node.hasBareCenter()) AppIconView(AppIcon.UI_WARN, size = 13.dp, modifier = Modifier.padding(end = 2.dp))
        RowToggle(
            if (isHidden) AppIcon.UI_EYEOFF else AppIcon.UI_EYE,
            stringResource(Res.string.hide_on_the_canvas_editor_only),
            isHidden,
            onToggleHidden,
        )
        RowToggle(
            if (isLocked) AppIcon.UI_LOCK else AppIcon.UI_UNLOCK,
            stringResource(Res.string.lock_selection_on_the_canvas_editor_only),
            isLocked,
            onToggleLocked,
        )
    }
}

@Composable
private fun HierarchyNameField(
    name: String,
    modifier: Modifier,
    onCommit: (String) -> Unit,
    onCancel: () -> Unit,
) {
    var field by remember(name) { mutableStateOf(TextFieldValue(name, selection = TextRange(0, name.length))) }
    var focused by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }
    val requester = remember { FocusRequester() }

    fun commit() {
        if (finished) return
        finished = true
        onCommit(field.text)
    }

    fun cancel() {
        if (finished) return
        finished = true
        onCancel()
    }

    LaunchedEffect(Unit) { requester.requestFocus() }
    BasicTextField(
        value = field,
        onValueChange = { field = it },
        singleLine = true,
        textStyle = TextStyle(color = TextForeground, fontSize = 11.sp),
        cursorBrush = SolidColor(TextForeground),
        modifier =
            modifier
                .background(FrameBackground)
                .border(1.dp, SelectColor)
                .focusRequester(requester)
                .onFocusChanged { focus ->
                    if (focused && !focus.isFocused) commit()
                    focused = focus.isFocused
                }.onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.Enter, Key.NumPadEnter -> {
                            commit()
                            true
                        }

                        Key.Escape -> {
                            cancel()
                            true
                        }

                        else -> {
                            false
                        }
                    }
                },
    )
}

/** A small icon at the end of a hierarchy row. Shown dimmed until it is switched on. */
@Composable
private fun RowToggle(
    icon: AppIcon,
    hint: String,
    on: Boolean,
    onClick: () -> Unit,
) {
    Tip(hint) {
        Box(
            Modifier
                .size(20.dp)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Main)
                            if (event.type == PointerEventType.Press && event.buttons.isPrimaryPressed) {
                                onClick()
                                event.changes.forEach { it.consume() }
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            AppIconView(icon, size = 13.dp, tint = if (on) Color(0xFF4A9EFF) else GhostlyText)
        }
    }
}
