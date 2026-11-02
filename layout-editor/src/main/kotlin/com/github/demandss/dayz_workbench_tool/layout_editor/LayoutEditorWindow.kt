package com.github.demandss.dayz_workbench_tool.layout_editor

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.rememberWindowState
import com.github.demandss.dayz_workbench_tool.i18n.I18n
import com.github.demandss.dayz_workbench_tool.i18n.str
import com.github.demandss.dayz_workbench_tool.layout_editor.mcp.McpHost
import com.github.demandss.dayz_workbench_tool.layout_editor.state.DragSession
import com.github.demandss.dayz_workbench_tool.layout_editor.state.DragSource
import com.github.demandss.dayz_workbench_tool.layout_editor.state.LayoutDocument
import com.github.demandss.dayz_workbench_tool.layout_editor.state.LayoutEditorState
import com.github.demandss.dayz_workbench_tool.layout_editor.state.PaletteMode
import com.github.demandss.dayz_workbench_tool.layout_editor.state.TabCloseAction
import com.github.demandss.dayz_workbench_tool.layout_editor.ui.canvas.FontViewer
import com.github.demandss.dayz_workbench_tool.layout_editor.ui.canvas.LayoutCanvas
import com.github.demandss.dayz_workbench_tool.layout_editor.ui.canvas.TextureViewer
import com.github.demandss.dayz_workbench_tool.layout_editor.ui.canvas.findProblems
import com.github.demandss.dayz_workbench_tool.layout_editor.ui.imageset.ImageSetEditor
import com.github.demandss.dayz_workbench_tool.layout_editor.ui.panels.Breadcrumbs
import com.github.demandss.dayz_workbench_tool.layout_editor.ui.panels.BulkEditDialog
import com.github.demandss.dayz_workbench_tool.layout_editor.ui.panels.CanvasContextMenu
import com.github.demandss.dayz_workbench_tool.layout_editor.ui.panels.CommandPalette
import com.github.demandss.dayz_workbench_tool.layout_editor.ui.panels.ConfirmDialog
import com.github.demandss.dayz_workbench_tool.layout_editor.ui.panels.ContextMenu
import com.github.demandss.dayz_workbench_tool.layout_editor.ui.panels.ContextMenuItem
import com.github.demandss.dayz_workbench_tool.layout_editor.ui.panels.ContextMenuSeparator
import com.github.demandss.dayz_workbench_tool.layout_editor.ui.panels.EmptyState
import com.github.demandss.dayz_workbench_tool.layout_editor.ui.panels.FileTreePanel
import com.github.demandss.dayz_workbench_tool.layout_editor.ui.panels.FlatDialog
import com.github.demandss.dayz_workbench_tool.layout_editor.ui.panels.FontImportDialog
import com.github.demandss.dayz_workbench_tool.layout_editor.ui.panels.FpsOverlay
import com.github.demandss.dayz_workbench_tool.layout_editor.ui.panels.HierarchyPanel
import com.github.demandss.dayz_workbench_tool.layout_editor.ui.panels.HistoryPanel
import com.github.demandss.dayz_workbench_tool.layout_editor.ui.panels.IconPickerWindow
import com.github.demandss.dayz_workbench_tool.layout_editor.ui.panels.McpConnectionWindow
import com.github.demandss.dayz_workbench_tool.layout_editor.ui.panels.Minimap
import com.github.demandss.dayz_workbench_tool.layout_editor.ui.panels.PaletteContent
import com.github.demandss.dayz_workbench_tool.layout_editor.ui.panels.ProblemsPanel
import com.github.demandss.dayz_workbench_tool.layout_editor.ui.panels.PropertiesContent
import com.github.demandss.dayz_workbench_tool.layout_editor.ui.panels.ReferencePanel
import com.github.demandss.dayz_workbench_tool.layout_editor.ui.panels.ResolutionCompare
import com.github.demandss.dayz_workbench_tool.layout_editor.ui.panels.Rulers
import com.github.demandss.dayz_workbench_tool.layout_editor.ui.panels.SelectionBar
import com.github.demandss.dayz_workbench_tool.layout_editor.ui.panels.ShortcutsDialog
import com.github.demandss.dayz_workbench_tool.layout_editor.ui.panels.StatusBar
import com.github.demandss.dayz_workbench_tool.layout_editor.ui.panels.StringPickerDialog
import com.github.demandss.dayz_workbench_tool.layout_editor.ui.panels.ToastHost
import com.github.demandss.dayz_workbench_tool.layout_editor.ui.panels.ViewportChips
import com.github.demandss.dayz_workbench_tool.layout_editor.ui.styles.StylesEditor
import com.github.demandss.dayz_workbench_tool.layout_editor.ui.toolbar.Toolbar
import com.github.demandss.dayz_workbench_tool.resources.*
import com.github.demandss.dayz_workbench_tool.ui.components.DarkButton
import com.github.demandss.dayz_workbench_tool.ui.components.DarkTabRow
import com.github.demandss.dayz_workbench_tool.ui.components.Tip
import com.github.demandss.dayz_workbench_tool.ui.icons.AppIcon
import com.github.demandss.dayz_workbench_tool.ui.icons.AppIconView
import com.github.demandss.dayz_workbench_tool.ui.icons.fileIcon
import com.github.demandss.dayz_workbench_tool.ui.pickFile
import com.github.demandss.dayz_workbench_tool.ui.theme.*
import com.github.demandss.dayz_workbench_tool.ui.theme.AppColors
import com.github.demandss.dayz_workbench_tool.ui.theme.BuiltinThemes
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import javax.imageio.ImageIO
import kotlin.math.roundToInt

private val appIcon by lazy {
    BitmapPainter(
        ImageIO
            .read(
                Thread
                    .currentThread()
                    .contextClassLoader!!
                    .getResourceAsStream("assets/logo/logo.png"),
            ).toComposeImageBitmap(),
    )
}

@Composable
fun LayoutEditorWindow(
    state: LayoutEditorState,
    themeColors: AppColors = BuiltinThemes.Dark.toAppColors(),
    onCloseRequest: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val windowTitle =
        buildString {
            append("DayZ Layout Editor")
            val shownName =
                state.activeStyles?.fileName ?: state.activeImageSet?.fileName ?: state.activeTexture?.fileName
                    ?: state.activeFile?.fileName
            shownName?.let { append(" — $it") }
        }

    // The window comes back in the size it was left in, kept within the part of the screen the taskbar leaves free
    val saved =
        remember {
            val usable =
                java.awt.GraphicsEnvironment
                    .getLocalGraphicsEnvironment()
                    .maximumWindowBounds
            state.savedWindow().fittedTo(usable.width, usable.height)
        }
    val windowState =
        rememberWindowState(
            width = saved.width.dp,
            height = saved.height.dp,
            placement = if (saved.maximized) WindowPlacement.Maximized else WindowPlacement.Floating,
        )
    // A change of the size is written down a moment after it stops, so that a crash or a kill does not lose it
    LaunchedEffect(windowState) {
        snapshotFlow {
            Triple(
                windowState.size.width.value
                    .toInt(),
                windowState.size.height.value
                    .toInt(),
                windowState.placement,
            )
        }.collectLatest { (width, height, placement) ->
            delay(400)
            state.saveWindow(width, height, placement == WindowPlacement.Maximized)
        }
    }

    Window(
        onCloseRequest = {
            state.saveWindow(
                windowState.size.width.value
                    .toInt(),
                windowState.size.height.value
                    .toInt(),
                windowState.placement == WindowPlacement.Maximized,
            )
            state.close()
            onCloseRequest()
        },
        title = windowTitle,
        icon = appIcon,
        state = windowState,
        resizable = true,
    ) {
        // The tools of the MCP server work on this editor while its window is open
        DisposableEffect(state) {
            state.hostWindow = window
            McpHost.attach(state)
            onDispose {
                McpHost.detach(state)
                state.hostWindow = null
            }
        }
        AppTheme(themeColors) {
            // The strings of the resources are read once: a new language draws the content again, not the window
            key(I18n.language) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(FrameBackground)
                        .onPreviewKeyEvent { event ->
                            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                            if (event.key == Key.F3) {
                                state.debugOverlay = !state.debugOverlay
                                return@onPreviewKeyEvent true
                            }
                            if (event.key == Key.F5) {
                                state.livePreview = !state.livePreview
                                return@onPreviewKeyEvent true
                            }
                            if (event.key == Key.F7) {
                                state.showWidgetNames = !state.showWidgetNames
                                state.savePreferences()
                                return@onPreviewKeyEvent true
                            }
                            if (event.key == Key.F6) {
                                state.workbenchColors = !state.workbenchColors
                                return@onPreviewKeyEvent true
                            }
                            if (event.key == Key.F1) {
                                state.shortcutsOpen = !state.shortcutsOpen
                                return@onPreviewKeyEvent true
                            }
                            if (handleGlobalKey(state, event)) return@onPreviewKeyEvent true
                            val styles = state.activeStyles
                            if (styles != null && !state.commandPaletteOpen) {
                                return@onPreviewKeyEvent when {
                                    event.isCtrlPressed && event.key == Key.S -> {
                                        state.saveActiveStyles()
                                        true
                                    }

                                    event.isCtrlPressed && event.key == Key.Z -> {
                                        styles.undo()
                                        true
                                    }

                                    event.isCtrlPressed && event.key == Key.Y -> {
                                        styles.redo()
                                        true
                                    }

                                    event.isCtrlPressed && event.key == Key.W -> {
                                        state.closeStyles(state.activeStylesIndex)
                                        true
                                    }

                                    else -> {
                                        false
                                    }
                                }
                            }
                            val imageSet = state.activeImageSet
                            if (imageSet != null && !state.commandPaletteOpen) {
                                return@onPreviewKeyEvent when {
                                    event.isCtrlPressed && event.key == Key.S -> {
                                        state.saveActiveImageSet()
                                        true
                                    }

                                    event.isCtrlPressed && event.key == Key.Z -> {
                                        imageSet.undo()
                                        true
                                    }

                                    event.isCtrlPressed && event.key == Key.Y -> {
                                        imageSet.redo()
                                        true
                                    }

                                    event.isCtrlPressed && event.key == Key.W -> {
                                        state.closeImageSet(state.activeImageSetIndex)
                                        true
                                    }

                                    else -> {
                                        false
                                    }
                                }
                            }
                            if (state.activeTexture != null) {
                                // The viewport shows a texture: only closing the tab applies
                                if (event.isCtrlPressed && event.key == Key.W) {
                                    state.closeTexture(state.activeTextureIndex)
                                    return@onPreviewKeyEvent true
                                }
                                return@onPreviewKeyEvent false
                            }
                            when {
                                state.commandPaletteOpen -> {
                                    false
                                }

                                event.isCtrlPressed && event.key == Key.D -> {
                                    state.duplicateSelected()
                                    true
                                }

                                event.isCtrlPressed && !event.isShiftPressed && event.key == Key.S -> {
                                    state.saveActiveFile()
                                    true
                                }

                                event.isCtrlPressed && event.key == Key.Z -> {
                                    state.undo(state.activeFileIndex)
                                    true
                                }

                                event.isCtrlPressed && event.key == Key.Y -> {
                                    state.redo(state.activeFileIndex)
                                    true
                                }

                                event.isCtrlPressed && event.key == Key.W -> {
                                    val idx = state.activeFileIndex
                                    if (idx >= 0) state.closeFile(idx)
                                    true
                                }

                                else -> {
                                    false
                                }
                            }
                        }
                        // Drag interceptor on the root itself — parent Initial pass sees events
                        // before children's Main pass, and never consumes, so clicks work normally
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    val pos = event.changes.firstOrNull()?.position ?: continue
                                    when (event.type) {
                                        PointerEventType.Move -> {
                                            if (state.dragSession != null) {
                                                state.dragScreenPos = pos
                                                state.dragHoverNodeId =
                                                    state.hierarchyRowBounds
                                                        .entries
                                                        .firstOrNull { (_, rect) -> rect.contains(pos) }
                                                        ?.key
                                            }
                                        }

                                        PointerEventType.Release -> {
                                            val session = state.dragSession
                                            if (session != null) {
                                                handleDrop(state, session, pos)
                                                state.dragSession = null
                                                state.dragHoverNodeId = null
                                            }
                                        }

                                        else -> {}
                                    }
                                    // Never consume — just observe
                                }
                            }
                        },
                ) {
                    // ── Main layout ───────────────────────────────────────────
                    Column(Modifier.fillMaxSize()) {
                        Row(Modifier.weight(1f).fillMaxWidth()) {
                            var treeWidth by remember { mutableStateOf(220.dp) }
                            if (state.showFileTree) {
                                FileTreePanel(state, width = treeWidth)

                                ResizeDivider { delta -> treeWidth = (treeWidth + delta).coerceIn(160.dp, 600.dp) }
                            }

                            Column(Modifier.weight(1f).fillMaxHeight()) {
                                FileTabBar(state)

                                val texture = state.activeTexture
                                val imageSet = state.activeImageSet
                                val styles = state.activeStyles
                                if (state.openFiles.isEmpty() && state.openTextures.isEmpty() && state.openImageSets.isEmpty() &&
                                    state.openStyles.isEmpty()
                                ) {
                                    EmptyState(state, Modifier.weight(1f).fillMaxWidth()) {
                                        scope.launch {
                                            pickFile(
                                                str(Res.string.open_a_file),
                                                "*",
                                                state.workDrivePath.ifBlank { null },
                                            )?.let { state.openAny(it) }
                                        }
                                    }
                                } else if (styles != null) {
                                    StylesEditor(state, styles, Modifier.weight(1f).fillMaxWidth())
                                } else if (imageSet != null) {
                                    ImageSetEditor(state, imageSet, Modifier.weight(1f).fillMaxWidth())
                                } else if (texture != null && texture.path.endsWith(".fnt", ignoreCase = true)) {
                                    FontViewer(state, texture, Modifier.weight(1f).fillMaxWidth())
                                } else if (texture != null) {
                                    TextureViewer(state, texture, Modifier.weight(1f).fillMaxWidth())
                                } else {
                                    Toolbar(state) {
                                        scope.launch {
                                            val path =
                                                pickFile(
                                                    str(Res.string.open_a_layout_file),
                                                    "layout",
                                                    state.workDrivePath.ifBlank { null },
                                                )
                                            if (path != null) state.openFile(path)
                                        }
                                    }

                                    val changedPath =
                                        state.activeFile
                                            ?.path
                                            ?.takeIf { state.filesChangedOnDisk.contains(it) }
                                    if (changedPath != null) {
                                        ChangedOnDiskBanner(
                                            onReload = { state.reloadFile(changedPath) },
                                            onIgnore = { state.dismissChangedNotification(changedPath) },
                                        )
                                    }

                                    Breadcrumbs(state)

                                    Row(Modifier.weight(1f).fillMaxWidth()) {
                                        Box(Modifier.weight(1f).fillMaxHeight()) {
                                            LayoutCanvas(
                                                state,
                                                Modifier
                                                    .fillMaxSize()
                                                    .onGloballyPositioned { coords ->
                                                        state.canvasBoundsInWindow = coords.boundsInWindow()
                                                    },
                                            )
                                            CanvasContextMenu(state)
                                            ReferencePanel(state, Modifier.align(Alignment.TopEnd).padding(12.dp))
                                            if (state.showRulers && !state.livePreview) Rulers(state)
                                            if (state.showMinimap &&
                                                !state.livePreview
                                            ) {
                                                Minimap(state, Modifier.align(Alignment.BottomEnd).padding(10.dp))
                                            }
                                            if (state.compareResolutions && !state.livePreview) {
                                                ResolutionCompare(state, Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp))
                                            }
                                            ViewportChips(
                                                state,
                                                Modifier
                                                    .align(
                                                        Alignment.BottomStart,
                                                    ).padding(10.dp)
                                                    .let { if (state.showRulers) it.padding(start = 18.dp) else it },
                                            )
                                            SelectionBar(state, Modifier.align(Alignment.TopCenter).padding(top = 10.dp))
                                        }

                                        Box(Modifier.width(1.dp).fillMaxHeight().background(DarkenedBorder))

                                        RightPanel(state)
                                    }
                                }
                            }
                        }

                        StatusBar(state)
                    }

                    // ── Drag ghost ────────────────────────────────────────────
                    val drag = state.dragSession
                    if (drag != null) {
                        val label =
                            when (val src = drag.source) {
                                is DragSource.FromPalette -> {
                                    src.type.className.removeSuffix("WidgetClass")
                                }

                                is DragSource.FromHierarchy -> {
                                    state.activeFile?.root?.findNode(src.nodeId)?.node?.let { n ->
                                        n.type.className.removeSuffix("WidgetClass") +
                                            if (n.name.isNotEmpty()) " · ${n.name}" else ""
                                    } ?: "Widget"
                                }
                            }
                        val gx = (state.dragScreenPos.x + 14f).roundToInt()
                        val gy = (state.dragScreenPos.y + 14f).roundToInt()
                        Box(
                            Modifier
                                .size(0.dp)
                                .absoluteOffset { IntOffset(gx, gy) }
                                .wrapContentSize(Alignment.TopStart, unbounded = true)
                                .background(ElementsBackground)
                                .border(1.dp, Color(0xFF4A9EFF))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            Text(label, color = TextForeground, fontSize = 11.sp)
                        }
                    }

                    BulkEditDialog(state)
                    FontImportDialog(state)
                    StringPickerDialog(state)
                    IconPickerWindow(state)
                    McpConnectionWindow(state)
                    ConfirmDialog(state)
                    ShortcutsDialog(state)
                    if (state.debugOverlay) FpsOverlay(Modifier.align(Alignment.TopEnd).padding(top = 34.dp, end = 12.dp))
                    ToastHost(state, Modifier.align(Alignment.BottomEnd).padding(bottom = 22.dp))
                    CommandPalette(state) {
                        scope.launch {
                            pickFile(str(Res.string.open_a_file), "*", state.workDrivePath.ifBlank { null })?.let { state.openAny(it) }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Drop handler
// ---------------------------------------------------------------------------

private fun screenToWorld(
    screen: Offset,
    zoom: Float,
    pan: Offset,
) = (screen - pan) / zoom

private fun handleDrop(
    state: LayoutEditorState,
    session: DragSession,
    releasePos: Offset,
) {
    val canvasBounds = state.canvasBoundsInWindow
    val hierarchyBounds = state.hierarchyBoundsInWindow
    val hoverNode = state.dragHoverNodeId

    when (val src = session.source) {
        is DragSource.FromPalette -> {
            when {
                canvasBounds.contains(releasePos) -> {
                    val localPos = releasePos - Offset(canvasBounds.left, canvasBounds.top)
                    val worldPos = screenToWorld(localPos, state.zoom, state.panOffset)
                    state.addWidgetOfType(src.type, null, worldPos)
                }

                hierarchyBounds.contains(releasePos) && hoverNode != null -> {
                    state.addWidgetOfType(src.type, hoverNode, null)
                }
            }
        }

        is DragSource.FromHierarchy -> {
            if (hoverNode != null && hoverNode != src.nodeId) {
                state.reparentNode(src.nodeId, hoverNode)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Name dialog
// ---------------------------------------------------------------------------

// ---------------------------------------------------------------------------
// File tab bar
// ---------------------------------------------------------------------------

@Composable
private fun FileTabBar(state: LayoutEditorState) {
    if (state.openFiles.isEmpty() && state.openTextures.isEmpty() && state.openImageSets.isEmpty() && state.openStyles.isEmpty()) return
    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    Row(
        Modifier
            .fillMaxWidth()
            .height(30.dp)
            .background(ElementsBackground)
            // The wheel scrolls the tabs sideways, there is no vertical scrolling to give it to
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type == PointerEventType.Scroll) {
                            val dy =
                                event.changes
                                    .firstOrNull()
                                    ?.scrollDelta
                                    ?.y ?: 0f
                            scope.launch { scroll.scrollBy(dy * 40f) }
                            event.changes.forEach { it.consume() }
                        }
                    }
                }
            }.horizontalScroll(scroll),
    ) {
        state.openFiles.forEachIndexed { index, doc ->
            FileTab(
                icon = AppIcon.LAYOUT,
                label = doc.fileName,
                modified = doc.isModified,
                hint = doc.path,
                isActive =
                    state.activeTextureIndex < 0 && state.activeImageSetIndex < 0 && state.activeStylesIndex < 0 &&
                        index == state.activeFileIndex,
                onClick = { state.selectLayout(index) },
                onClose = { state.closeFile(index) },
                onContextAction = { action -> state.closeTabs(0, index, action) },
            )
        }
        state.openTextures.forEachIndexed { index, doc ->
            FileTab(
                icon = fileIcon(doc.fileName),
                label = doc.fileName,
                isActive = index == state.activeTextureIndex && state.activeImageSetIndex < 0 && state.activeStylesIndex < 0,
                onClick = { state.selectTexture(index) },
                onClose = { state.closeTexture(index) },
                onContextAction = { action -> state.closeTabs(1, index, action) },
            )
        }
        state.openImageSets.forEachIndexed { index, doc ->
            FileTab(
                icon = fileIcon(doc.fileName),
                label = doc.fileName,
                modified = doc.isModified,
                hint = doc.path,
                isActive = index == state.activeImageSetIndex && state.activeStylesIndex < 0,
                onClick = { state.selectImageSet(index) },
                onClose = { state.closeImageSet(index) },
                onContextAction = { action -> state.closeTabs(2, index, action) },
            )
        }
        state.openStyles.forEachIndexed { index, doc ->
            FileTab(
                icon = fileIcon(doc.fileName),
                label = doc.fileName,
                modified = doc.isModified,
                hint = doc.path,
                isActive = index == state.activeStylesIndex,
                onClick = { state.selectStyles(index) },
                onClose = { state.closeStyles(index) },
                onContextAction = { action -> state.closeTabs(3, index, action) },
            )
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun FileTab(
    icon: AppIcon,
    label: String,
    modified: Boolean = false,
    isActive: Boolean,
    onClick: () -> Unit,
    onClose: () -> Unit,
    onContextAction: (TabCloseAction) -> Unit,
    hint: String? = null,
) {
    val bg = if (isActive) FrameBackground else ElementsBackground
    val requester = remember { BringIntoViewRequester() }
    var menuPosition by remember { mutableStateOf<IntOffset?>(null) }
    LaunchedEffect(isActive) { if (isActive) requester.bringIntoView() }
    Box {
        Tip(hint) {
            Row(
                modifier =
                    Modifier
                        .bringIntoViewRequester(requester)
                        .background(bg)
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                                        val position = event.changes.first().position
                                        menuPosition = IntOffset(position.x.roundToInt(), position.y.roundToInt())
                                        event.changes.forEach { it.consume() }
                                    }
                                }
                            }
                        }.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onClick,
                        ).padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppIconView(icon)
                Spacer(Modifier.width(6.dp))
                Text(label, color = TextForeground, fontSize = 12.sp, maxLines = 1)
                if (modified) {
                    Spacer(Modifier.width(6.dp))
                    Box(Modifier.size(6.dp).background(Color(0xFF4A9EFF), CircleShape))
                }
                Spacer(Modifier.width(6.dp))
                Tip(stringResource(Res.string.close_the_tab_ctrl_w)) {
                    Box(
                        Modifier
                            .size(14.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onClose,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        AppIconView(AppIcon.UI_CLOSE, size = 12.dp, tint = GhostlyText)
                    }
                }
                Spacer(Modifier.width(2.dp))
            }
        }
        menuPosition?.let { position ->
            ContextMenu(position, onDismiss = { menuPosition = null }) {
                fun run(action: TabCloseAction) {
                    menuPosition = null
                    onContextAction(action)
                }
                ContextMenuItem(str(Res.string.close), icon = AppIcon.UI_CLOSE) { run(TabCloseAction.CURRENT) }
                ContextMenuSeparator()
                ContextMenuItem(str(Res.string.close_other_tabs)) { run(TabCloseAction.OTHERS) }
                ContextMenuItem(str(Res.string.close_tabs_to_right)) { run(TabCloseAction.RIGHT) }
                ContextMenuItem(str(Res.string.close_tabs_to_left)) { run(TabCloseAction.LEFT) }
                ContextMenuSeparator()
                ContextMenuItem(str(Res.string.close_all_tabs)) { run(TabCloseAction.ALL) }
            }
        }
    }
    Box(Modifier.width(1.dp).fillMaxHeight().background(DarkenedBorder))
}

// ---------------------------------------------------------------------------
// Changed-on-disk banner
// ---------------------------------------------------------------------------

@Composable
private fun ChangedOnDiskBanner(
    onReload: () -> Unit,
    onIgnore: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(Color(0xFF4A3C00))
                .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            stringResource(Res.string.the_file_was_changed_on_disk),
            color = Color(0xFFFFD04A),
            fontSize = 12.sp,
            modifier = Modifier.weight(1f),
        )
        DarkButton(
            stringResource(Res.string.reload),
            onClick = onReload,
            tooltip = stringResource(Res.string.read_the_file_from_disk_again_the_edits),
        )
        DarkButton(
            stringResource(Res.string.ignore),
            onClick = onIgnore,
            tooltip = stringResource(Res.string.keep_what_is_open_in_the_editor),
        )
    }
}

// ---------------------------------------------------------------------------
// Right panel
// ---------------------------------------------------------------------------

@Composable
private fun RightPanel(state: LayoutEditorState) {
    var rightTab by state::rightTab
    Column(Modifier.width(300.dp).fillMaxHeight()) {
        HierarchyPanel(state, Modifier.weight(1f).fillMaxWidth())
        Box(Modifier.fillMaxWidth().height(1.dp).background(DarkenedBorder))
        val problemCount = state.activeFile?.let { remember(it.root) { findProblems(it.root).size } } ?: 0
        Row(Modifier.fillMaxWidth().height(28.dp).background(ElementsBackground)) {
            listOf(
                "Properties",
                "Palette",
                if (problemCount >
                    0
                ) {
                    "Problems $problemCount"
                } else {
                    "Problems"
                },
                "History",
            ).forEachIndexed { i, title ->
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    Tip(
                        listOf(
                            stringResource(Res.string.properties_of_the_selected_widget),
                            stringResource(Res.string.widgets_you_can_add),
                            stringResource(Res.string.what_looks_wrong_in_the_layout),
                            stringResource(Res.string.actions_in_this_layout_you_can_return_to),
                        )[i],
                    ) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(if (rightTab == i) FrameBackground else ElementsBackground)
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { rightTab = i },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(title, color = if (rightTab == i) TextForeground else GhostlyText, fontSize = 12.sp, maxLines = 1)
                        }
                    }
                }
            }
        }
        when (rightTab) {
            0 -> PropertiesContent(state, Modifier.weight(1.7f).fillMaxWidth())
            1 -> PaletteContent(state, Modifier.weight(1.7f).fillMaxWidth())
            2 -> ProblemsPanel(state, Modifier.weight(1.7f).fillMaxWidth())
            3 -> HistoryPanel(state, Modifier.weight(1.7f).fillMaxWidth())
        }
    }
}

/** A one pixel line with a wider grab area: dragging it sideways reports the change of width. */
@Composable
private fun ResizeDivider(onDrag: (androidx.compose.ui.unit.Dp) -> Unit) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    Box(
        Modifier
            .width(5.dp)
            .fillMaxHeight()
            .pointerHoverIcon(PointerIcon(java.awt.Cursor(java.awt.Cursor.E_RESIZE_CURSOR)))
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, amount ->
                    change.consume()
                    onDrag(with(density) { amount.toDp() })
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.width(1.dp).fillMaxHeight().background(DarkenedBorder))
    }
}

/** Shortcuts that work in every editor, whatever has the keyboard. Returns true when the key was used. */
private fun handleGlobalKey(
    state: LayoutEditorState,
    event: androidx.compose.ui.input.key.KeyEvent,
): Boolean {
    if (!event.isCtrlPressed || event.isAltPressed) return false
    val shift = event.isShiftPressed
    val digit = listOf(Key.One, Key.Two, Key.Three, Key.Four, Key.Five, Key.Six, Key.Seven, Key.Eight, Key.Nine).indexOf(event.key) + 1
    return when {
        event.key == Key.Comma -> {
            state.openAppSettings?.invoke()
            state.openAppSettings != null
        }

        event.key == Key.P && shift -> {
            state.openPalette(PaletteMode.COMMANDS)
            true
        }

        event.key == Key.P -> {
            state.openPalette(PaletteMode.FILES)
            true
        }

        event.key == Key.B -> {
            state.showFileTree = !state.showFileTree
            true
        }

        event.key == Key.Tab -> {
            state.cycleTab(if (shift) -1 else 1)
            true
        }

        digit > 0 && !shift -> {
            state.selectTabNumber(digit)
            true
        }

        event.key == Key.F && !shift -> {
            when {
                state.activeImageSet != null -> {
                    state.imageSetSearchTick++
                }

                state.activeStyles != null -> {}

                state.activeFile != null && state.activeTexture == null -> {
                    state.openPalette(PaletteMode.WIDGETS)
                }
            }
            true
        }

        event.key == Key.Z && shift -> {
            state.activeStyles?.redo() ?: state.activeImageSet?.redo() ?: state.redo(state.activeFileIndex)
            true
        }

        else -> {
            false
        }
    }
}
