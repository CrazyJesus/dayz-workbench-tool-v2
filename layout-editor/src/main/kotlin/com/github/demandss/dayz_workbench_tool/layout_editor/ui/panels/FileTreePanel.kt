package com.github.demandss.dayz_workbench_tool.layout_editor.ui.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.github.demandss.dayz_workbench_tool.i18n.str
import com.github.demandss.dayz_workbench_tool.layout.NewFileKind
import com.github.demandss.dayz_workbench_tool.layout_editor.state.Confirmation
import com.github.demandss.dayz_workbench_tool.layout_editor.state.LayoutEditorState
import com.github.demandss.dayz_workbench_tool.layout_editor.state.ToastKind
import com.github.demandss.dayz_workbench_tool.pbo.PboArchive
import com.github.demandss.dayz_workbench_tool.pbo.PboFile
import com.github.demandss.dayz_workbench_tool.resources.*
import com.github.demandss.dayz_workbench_tool.texture.EddsCodec
import com.github.demandss.dayz_workbench_tool.ui.components.DarkIconButton
import com.github.demandss.dayz_workbench_tool.ui.components.Tip
import com.github.demandss.dayz_workbench_tool.ui.icons.AppIcon
import com.github.demandss.dayz_workbench_tool.ui.icons.AppIconView
import com.github.demandss.dayz_workbench_tool.ui.icons.fileIcon
import com.github.demandss.dayz_workbench_tool.ui.theme.*
import kotlinx.coroutines.*
import org.jetbrains.compose.resources.stringResource
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.nio.file.*
import java.nio.file.StandardWatchEventKinds.*
import kotlin.math.roundToInt

// ---------------------------------------------------------------------------
// Data model
// ---------------------------------------------------------------------------

/** Whether a path points into a game PBO. */
private fun isInPbo(path: Path): Boolean = PboFile.resolve(path.toString()) != null

private val TEXTURE_EXTENSIONS = listOf(".edds", ".png", ".jpg", ".jpeg", ".fnt")

private data class TreeEntry(
    val path: Path,
    val name: String,
    val isDir: Boolean,
    val depth: Int,
    // Folder of a search result relative to the work drive, empty in the tree
    val hint: String = "",
)

/** A top level folder of the tree shown under [label]. */
private class TreeTop(
    val path: Path,
    val label: String,
    // Set for a PBO: its folders are read from the archive; the path of an entry is the path of the archive plus its inner path
    val archive: PboArchive? = null,
) {
    /** The inner path of [entry] in the archive, empty for the archive itself. */
    fun innerOf(entry: Path): String =
        entry
            .toString()
            .substring(path.toString().length)
            .trimStart('\\', '/')
            .replace('\\', '/')

    fun isFolder(entry: Path): Boolean = if (archive != null) archive.list(innerOf(entry)).isNotEmpty() else Files.isDirectory(entry)

    fun children(dir: Path): List<Path> =
        if (archive != null) {
            archive
                .list(innerOf(dir))
                .sortedWith(compareBy({ !it.endsWith("/") }, { it.lowercase() }))
                .map { dir.resolve(it.trimEnd('/')) }
        } else {
            runCatching {
                Files
                    .list(dir)
                    .sorted(
                        Comparator { a, b ->
                            val da = if (Files.isDirectory(a)) 0 else 1
                            val db = if (Files.isDirectory(b)) 0 else 1
                            if (da != db) {
                                da - db
                            } else {
                                a.fileName
                                    .toString()
                                    .lowercase()
                                    .compareTo(b.fileName.toString().lowercase())
                            }
                        },
                    ).toList()
            }.getOrDefault(emptyList())
        }
}

private fun buildFlatTree(
    tops: List<TreeTop>,
    expandedDirs: Set<Path>,
    // Lists the contents of the folder instead of showing the folder as a node
    listContents: Boolean = false,
): List<TreeEntry> {
    val result = mutableListOf<TreeEntry>()

    fun traverse(
        top: TreeTop,
        dir: Path,
        depth: Int,
    ) {
        for (child in top.children(dir)) {
            val isDir = top.isFolder(child)
            result.add(TreeEntry(child, child.fileName.toString(), isDir, depth))
            if (isDir && expandedDirs.contains(child)) traverse(top, child, depth + 1)
        }
    }
    for (top in tops) {
        if (!Files.exists(top.path)) continue
        if (listContents) {
            traverse(top, top.path, 0)
            continue
        }
        result.add(TreeEntry(top.path, top.label, true, 0))
        if (expandedDirs.contains(top.path)) traverse(top, top.path, 1)
    }
    return result
}

// ---------------------------------------------------------------------------
// Main composable
// ---------------------------------------------------------------------------

@Composable
fun FileTreePanel(
    state: LayoutEditorState,
    width: Dp,
) {
    val workRoot = state.workDrivePath
    val scope = rememberCoroutineScope()

    val expandedDirs =
        remember {
            mutableStateListOf<Path>().apply {
                state.savedExpandedDirs.forEach { dir ->
                    runCatching { Path.of(dir) }.getOrNull()?.takeIf { Files.isDirectory(it) || isInPbo(it) }?.let { add(it) }
                }
            }
        }
    var refreshTick by remember { mutableStateOf(0) }
    var renamingPath by remember { mutableStateOf<Path?>(null) }
    var deletingEntry by remember { mutableStateOf<TreeEntry?>(null) }
    var creating by remember { mutableStateOf<Pair<Path, NewFileKind>?>(null) }
    var selectedPath by remember {
        mutableStateOf(
            state.savedSelectedPath
                .takeIf { it.isNotEmpty() }
                ?.let { runCatching { Path.of(it) }.getOrNull() }
                ?.takeIf { Files.exists(it) },
        )
    }

    LaunchedEffect(expandedDirs.toList(), selectedPath) {
        state.saveTreeState(expandedDirs.map { it.toString() }, selectedPath?.toString().orEmpty())
    }

    // The connected mods and the game; without a list of mods (the editor on its own) the work drive itself
    val treeRoots = state.treeRoots
    val tops =
        remember(treeRoots?.map { it.path to it.label }, workRoot) {
            treeRoots?.mapNotNull { r -> runCatching { TreeTop(Path.of(r.path), r.label, r.archive) }.getOrNull() }
                ?: listOfNotNull(runCatching { TreeTop(Path.of(workRoot), "") }.getOrNull())
        }
    val wholeDrive = treeRoots == null

    val flatList =
        remember(refreshTick, expandedDirs.toList(), tops) {
            if (wholeDrive) {
                tops.firstOrNull()?.let { buildFlatTree(listOf(it), expandedDirs.toHashSet(), listContents = true) }.orEmpty()
            } else {
                buildFlatTree(tops, expandedDirs.toHashSet())
            }
        }

    var query by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<TreeEntry>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    val searchActive = query.isNotBlank()

    LaunchedEffect(query, refreshTick, tops) {
        if (query.isBlank()) {
            searchResults = emptyList()
            searching = false
            return@LaunchedEffect
        }
        searching = true
        delay(250) // typing restarts this effect, so only the last query is searched
        searchResults = withContext(Dispatchers.IO) { tops.flatMap { searchTree(it, query) }.take(SEARCH_LIMIT) }
        searching = false
    }
    val shownList = if (searchActive) searchResults else flatList
    // Where a new file goes with nothing selected: the first mod, or the work drive
    val rootPath = (tops.firstOrNull { it.archive == null } ?: tops.firstOrNull())?.path
    val isInsideTops: (Path) -> Boolean = { dir -> tops.any { dir.startsWith(it.path) } }

    // File watcher — watch root and all expanded dirs
    DisposableEffect(tops, expandedDirs.toList()) {
        val ws = FileSystems.getDefault().newWatchService()
        val toWatch = mutableListOf<Path>()
        tops.forEach { toWatch.add(it.path) }
        expandedDirs.forEach { toWatch.add(it) }

        for (dir in toWatch) {
            runCatching { dir.register(ws, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY) }
        }

        val job =
            scope.launch(Dispatchers.IO) {
                while (isActive) {
                    val key = runCatching { ws.take() }.getOrNull() ?: break
                    key.pollEvents()
                    withContext(Dispatchers.Main) { refreshTick++ }
                    key.reset()
                }
            }
        onDispose {
            job.cancel()
            runCatching { ws.close() }
        }
    }

    Column(Modifier.width(width).fillMaxHeight().background(FrameBackground)) {
        // Header
        Row(
            Modifier
                .fillMaxWidth()
                .height(26.dp)
                .background(ElementsBackground)
                .padding(start = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Project", color = GhostlyText, fontSize = 10.sp, modifier = Modifier.weight(1f))
            if (workRoot.isNotBlank()) {
                Text(
                    java.io
                        .File(workRoot)
                        .name
                        .ifEmpty { workRoot },
                    color = GhostlyText,
                    fontSize = 10.sp,
                    maxLines = 1,
                    modifier =
                        Modifier
                            .widthIn(max = 90.dp)
                            .background(FrameBackground)
                            .border(1.dp, DarkenedBorder)
                            .padding(horizontal = 6.dp, vertical = 1.dp),
                )
            }
            DarkIconButton(AppIcon.UI_LAYERS, stringResource(Res.string.collapse_all_folders), onClick = { expandedDirs.clear() })
        }

        SearchField(query) { query = it }

        if (workRoot.isBlank()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(Res.string.the_workdrive_is_not_set), color = GhostlyText, fontSize = 11.sp)
            }
            return@Column
        }

        if (searchActive && !searching) {
            val note =
                when {
                    searchResults.isEmpty() -> stringResource(Res.string.nothing_found)
                    searchResults.size >= SEARCH_LIMIT -> stringResource(Res.string.the_first_x_are_shown_narrow_the_query, SEARCH_LIMIT)
                    else -> stringResource(Res.string.found_x, searchResults.size)
                }
            Text(note, color = GhostlyText, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
        }

        var bgMenu by remember { mutableStateOf<IntOffset?>(null) }
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Final)
                            val change = event.changes.firstOrNull() ?: continue
                            // A row consumes its own right click, so only the empty space gets here
                            if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed && !change.isConsumed) {
                                bgMenu = IntOffset(change.position.x.roundToInt(), change.position.y.roundToInt())
                            }
                        }
                    }
                },
        ) {
            LazyColumn(Modifier.fillMaxSize()) {
                // Inline creation row at the top of the list
                creating?.let { (parentDir, kind) ->
                    item(key = "creating") {
                        InlineNameField(
                            depth = 0,
                            placeholder = kind.placeholder,
                            onCommit = { name ->
                                creating = null
                                if (name.isNotBlank()) {
                                    scope.launch(Dispatchers.IO) {
                                        val result = runCatching { kind.create(parentDir, name) }
                                        result.exceptionOrNull()?.let {
                                            state.notify(
                                                str(Res.string.not_created_x, it.message ?: it.javaClass.simpleName),
                                                ToastKind.ERROR,
                                            )
                                        }
                                        val created = result.getOrNull()
                                        withContext(Dispatchers.Main) {
                                            if (created != null) {
                                                var dir: Path? = parentDir
                                                while (dir != null && isInsideTops(dir)) {
                                                    if (!expandedDirs.contains(dir)) expandedDirs.add(dir)
                                                    dir = dir.parent
                                                }
                                                selectedPath = created
                                                if (kind == NewFileKind.LAYOUT) state.openFile(created.toString())
                                            }
                                            refreshTick++
                                        }
                                    }
                                }
                            },
                            onCancel = { creating = null },
                        )
                    }
                }

                items(shownList, key = { it.path.toString() }) { entry ->
                    val isRenaming = renamingPath == entry.path
                    var showMenu by remember { mutableStateOf(false) }
                    var menuPos by remember { mutableStateOf(IntOffset.Zero) }

                    if (isRenaming) {
                        InlineNameField(
                            depth = entry.depth,
                            placeholder = entry.name,
                            initialValue = entry.name,
                            onCommit = { newName ->
                                renamingPath = null
                                val clean = newName.trim()
                                if (clean.isNotEmpty() && clean != entry.name) {
                                    // The same names that a new file may not have: nothing that leads out of the folder
                                    if (clean.any { it in "\\/:*?\"<>|" } || clean == "." || clean == "..") {
                                        state.notify(str(Res.string.could_not_rename_x_x, entry.name, clean), ToastKind.ERROR)
                                        return@InlineNameField
                                    }
                                    // The open tabs of the file would keep the old path, and saving them would bring the old file back
                                    state.closeTabsUnder(entry.path)
                                    scope.launch(Dispatchers.IO) {
                                        runCatching {
                                            Files.move(entry.path, entry.path.parent.resolve(clean))
                                        }.onFailure {
                                            state.notify(
                                                str(Res.string.could_not_rename_x_x, entry.name, it.message ?: it.javaClass.simpleName),
                                                ToastKind.ERROR,
                                            )
                                        }
                                        withContext(Dispatchers.Main) { refreshTick++ }
                                    }
                                }
                            },
                            onCancel = { renamingPath = null },
                        )
                    } else {
                        Box {
                            val realPath =
                                remember(
                                    entry.path,
                                ) { runCatching { entry.path.toFile().canonicalPath }.getOrDefault(entry.path.toString()) }
                            val layoutDoc = state.openFiles.firstOrNull { it.path == realPath }
                            val textureDoc = state.openTextures.firstOrNull { it.path == realPath }
                            val imageSetDoc = state.openImageSets.firstOrNull { it.path == realPath }
                            TreeRow(
                                entry = entry,
                                isSelected = selectedPath == entry.path,
                                isExpanded = expandedDirs.contains(entry.path),
                                isOpen = layoutDoc != null || textureDoc != null || imageSetDoc != null,
                                isModified = layoutDoc?.isModified == true || imageSetDoc?.isModified == true,
                                hasError = textureDoc?.error != null,
                                onSelect = { selectedPath = entry.path },
                                onToggle = {
                                    if (expandedDirs.contains(entry.path)) {
                                        expandedDirs.remove(entry.path)
                                    } else {
                                        expandedDirs.add(entry.path)
                                    }
                                },
                                onDoubleClick = {
                                    if (entry.isDir && searchActive) {
                                        var dir: Path? = entry.path
                                        while (dir != null && isInsideTops(dir)) {
                                            if (!expandedDirs.contains(dir)) expandedDirs.add(dir)
                                            dir = dir.parent
                                        }
                                        selectedPath = entry.path
                                        query = ""
                                    } else if (entry.isDir) {
                                        if (expandedDirs.contains(entry.path)) {
                                            expandedDirs.remove(entry.path)
                                        } else {
                                            expandedDirs.add(entry.path)
                                        }
                                    } else if (entry.name.endsWith(".layout")) {
                                        state.openFile(entry.path.toString())
                                    } else if (entry.name.endsWith(".imageset", ignoreCase = true)) {
                                        state.openImageSet(entry.path.toString())
                                    } else if (entry.name.endsWith(".styles", ignoreCase = true)) {
                                        state.openStyles(entry.path.toString())
                                    } else if (TEXTURE_EXTENSIONS.any { entry.name.endsWith(it, ignoreCase = true) }) {
                                        state.openTexture(entry.path.toString())
                                    }
                                },
                                onRightClick = { pos ->
                                    menuPos = IntOffset(pos.x.roundToInt(), pos.y.roundToInt())
                                    showMenu = true
                                },
                            )
                            if (showMenu) {
                                ContextMenu(menuPos, onDismiss = { showMenu = false }) {
                                    if (!entry.isDir &&
                                        (
                                            entry.name.endsWith(".layout", ignoreCase = true) ||
                                                entry.name.endsWith(".imageset", ignoreCase = true) ||
                                                entry.name.endsWith(".styles", ignoreCase = true) ||
                                                TEXTURE_EXTENSIONS.any { entry.name.endsWith(it, ignoreCase = true) }
                                        )
                                    ) {
                                        ContextMenuItem(str(Res.string.open), icon = fileIcon(entry.name)) {
                                            showMenu = false
                                            state.openAny(entry.path.toString())
                                        }
                                        ContextMenuSeparator()
                                    }
                                    if (!entry.isDir) {
                                        ContextMenuItem(str(Res.string.copy_file_name), icon = AppIcon.UI_COPY) {
                                            showMenu = false
                                            runCatching {
                                                Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(entry.name), null)
                                            }.onSuccess {
                                                state.notify(str(Res.string.copied_x, entry.name), ToastKind.SUCCESS)
                                            }.onFailure {
                                                state.notify(
                                                    str(Res.string.could_not_copy_x, it.message ?: it.javaClass.simpleName),
                                                    ToastKind.ERROR,
                                                )
                                            }
                                        }
                                        ContextMenuSeparator()
                                    }
                                    if (isInPbo(entry.path)) return@ContextMenu
                                    // Next to a file, inside a folder
                                    val createDir = if (entry.isDir) entry.path else entry.path.parent
                                    CreateMenuItems { kind ->
                                        showMenu = false
                                        if (createDir != null) creating = createDir to kind
                                    }
                                    ContextMenuSeparator()
                                    val isEdds = !entry.isDir && entry.name.endsWith(".edds", ignoreCase = true)
                                    val isPng = !entry.isDir && entry.name.endsWith(".png", ignoreCase = true)
                                    if (isEdds || isPng) {
                                        ContextMenuItem(
                                            if (isEdds) str(Res.string.convert_to_png) else str(Res.string.convert_to_edds),
                                            icon = if (isEdds) AppIcon.IMAGE else AppIcon.EDDS,
                                        ) {
                                            showMenu = false
                                            scope.launch(Dispatchers.IO) {
                                                runCatching {
                                                    if (isEdds) {
                                                        EddsCodec.edds2png(entry.path.toFile())
                                                    } else {
                                                        EddsCodec.png2edds(entry.path.toFile())
                                                    }
                                                }.onSuccess {
                                                    state.notify(
                                                        str(Res.string.converted_x_x, entry.name, if (isEdds) "PNG" else "EDDS"),
                                                        ToastKind.SUCCESS,
                                                    )
                                                }.onFailure {
                                                    state.notify(
                                                        str(
                                                            Res.string.could_not_convert_x_x,
                                                            entry.name,
                                                            it.message ?: it.javaClass.simpleName,
                                                        ),
                                                        ToastKind.ERROR,
                                                    )
                                                }
                                                withContext(Dispatchers.Main) {
                                                    state.reloadTextures()
                                                    refreshTick++
                                                }
                                            }
                                        }
                                    }
                                    ContextMenuItem(str(Res.string.rename), icon = AppIcon.FILE) {
                                        showMenu = false
                                        renamingPath = entry.path
                                    }
                                    ContextMenuItem(str(Res.string.show_in_explorer), icon = AppIcon.UI_SEARCH) {
                                        showMenu = false
                                        scope.launch(Dispatchers.IO) {
                                            runCatching {
                                                ProcessBuilder("explorer.exe", "/select,${entry.path.toAbsolutePath()}").start()
                                            }
                                        }
                                    }
                                    ContextMenuSeparator()
                                    ContextMenuItem(str(Res.string.delete_2), color = Color(0xFFFF5555), icon = AppIcon.UI_TRASH) {
                                        showMenu = false
                                        deletingEntry = entry
                                    }
                                }
                            }
                        }
                    }
                }
            }
            bgMenu?.let { pos ->
                ContextMenu(pos, onDismiss = { bgMenu = null }) {
                    CreateMenuItems { kind ->
                        bgMenu = null
                        rootPath?.let { creating = it to kind }
                    }
                }
            }
        }
        RecentFiles(state)
    }

    // Delete confirmation, asked over the whole window
    deletingEntry?.let { target ->
        deletingEntry = null
        state.confirmation =
            Confirmation(
                title = stringResource(Res.string.delete_x, target.name),
                message = stringResource(Res.string.this_action_cannot_be_undone),
                confirmLabel = stringResource(Res.string.delete),
                danger = true,
            ) {
                state.closeTabsUnder(target.path)
                scope.launch(Dispatchers.IO) {
                    runCatching {
                        if (target.isDir) {
                            Files.walk(target.path).sorted(Comparator.reverseOrder()).forEach(Files::delete)
                        } else {
                            Files.deleteIfExists(target.path)
                        }
                    }.onFailure {
                        state.notify(
                            str(Res.string.could_not_delete_x_x, target.name, it.message ?: it.javaClass.simpleName),
                            ToastKind.ERROR,
                        )
                    }
                    withContext(Dispatchers.Main) { refreshTick++ }
                }
            }
    }
}

// ---------------------------------------------------------------------------
// Tree row
// ---------------------------------------------------------------------------

@Composable
private fun TreeRow(
    entry: TreeEntry,
    isSelected: Boolean,
    isExpanded: Boolean,
    isOpen: Boolean,
    isModified: Boolean,
    hasError: Boolean,
    onSelect: () -> Unit,
    onToggle: () -> Unit,
    onDoubleClick: () -> Unit,
    onRightClick: (Offset) -> Unit,
) {
    var clickTime by remember { mutableStateOf(0L) }
    val hover = remember { MutableInteractionSource() }
    val hovered by hover.collectIsHoveredAsState()

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(22.dp)
                .background(
                    if (isSelected) {
                        SelectColor
                    } else if (hovered) {
                        Color(0x14FFFFFF)
                    } else {
                        Color.Transparent
                    },
                ).hoverable(hover)
                .padding(start = (entry.depth * 12 + 2).dp)
                .pointerInput(entry.path) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            when {
                                event.type == PointerEventType.Press && event.buttons.isPrimaryPressed -> {
                                    onSelect()
                                    val now = System.currentTimeMillis()
                                    if (now - clickTime < 400) onDoubleClick()
                                    clickTime = now
                                    event.changes.forEach { it.consume() }
                                }

                                event.type == PointerEventType.Press && event.buttons.isSecondaryPressed -> {
                                    val pos = event.changes.first().position
                                    // Position is inside the row padding, the menu is anchored to the whole row
                                    onRightClick(Offset(pos.x + (entry.depth * 12 + 2).dp.toPx(), pos.y))
                                    event.changes.forEach { it.consume() }
                                }
                            }
                        }
                    }
                },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Expand arrow (dirs only)
        Tip(
            if (entry.isDir) {
                (
                    if (isExpanded) {
                        stringResource(
                            Res.string.collapse_the_folder,
                        )
                    } else {
                        stringResource(Res.string.expand_the_folder)
                    }
                )
            } else {
                null
            },
        ) {
            Box(
                Modifier
                    .size(14.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = entry.isDir,
                        onClick = onToggle,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (entry.isDir) {
                    AppIconView(if (isExpanded) AppIcon.UI_DOWN else AppIcon.UI_RIGHT, size = 12.dp, tint = GhostlyText)
                }
            }
        }

        // Icon: the file type is told by its shape and colour
        AppIconView(fileIcon(entry.name, entry.isDir, isExpanded), modifier = Modifier.padding(end = 6.dp))

        // Name
        Text(
            entry.name,
            color = TextForeground,
            fontSize = 11.sp,
            fontWeight = if (isOpen) FontWeight.SemiBold else null,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (entry.hint.isNotEmpty()) {
            Text(
                entry.hint,
                color = GhostlyText,
                fontSize = 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 6.dp, end = 4.dp),
            )
        }
        Spacer(Modifier.weight(1f))
        if (isModified) Box(Modifier.padding(end = 8.dp).size(6.dp).background(Color(0xFF4A9EFF), CircleShape))
        if (hasError) AppIconView(AppIcon.UI_WARN, size = 14.dp, modifier = Modifier.padding(end = 6.dp))
    }
}

// ---------------------------------------------------------------------------
// Inline name field (rename / create)
// ---------------------------------------------------------------------------

@Composable
private fun InlineNameField(
    depth: Int,
    placeholder: String,
    initialValue: String = "",
    onCommit: (String) -> Unit,
    onCancel: () -> Unit,
) {
    var text by remember { mutableStateOf(initialValue) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Row(
        Modifier
            .fillMaxWidth()
            .height(20.dp)
            .padding(start = (depth * 12 + 18).dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicTextField(
            value = text,
            onValueChange = { text = it },
            singleLine = true,
            textStyle = TextStyle(color = TextForeground, fontSize = 11.sp),
            cursorBrush = SolidColor(TextForeground),
            decorationBox = { inner ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(FrameBackground)
                        .padding(horizontal = 3.dp, vertical = 1.dp),
                ) {
                    if (text.isEmpty()) {
                        Text(placeholder, color = GhostlyText, fontSize = 11.sp)
                    }
                    inner()
                }
            },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onKeyEvent { ev ->
                        when {
                            ev.key == Key.Enter && ev.type == KeyEventType.KeyDown -> {
                                onCommit(text)
                                true
                            }

                            ev.key == Key.Escape && ev.type == KeyEventType.KeyDown -> {
                                onCancel()
                                true
                            }

                            else -> {
                                false
                            }
                        }
                    },
        )
    }
}

// ---------------------------------------------------------------------------
// Context menu
// ---------------------------------------------------------------------------

/** Compact popup menu shown with its top-left corner at [position] inside the anchor. */
@Composable
internal fun ContextMenu(
    position: IntOffset,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Popup(
        alignment = Alignment.TopStart,
        offset = position,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        Column(
            Modifier
                .width(IntrinsicSize.Max)
                .widthIn(min = 150.dp)
                .background(ElementsBackground)
                .border(1.dp, DarkenedBorder)
                .padding(vertical = 2.dp),
            content = content,
        )
    }
}

@Composable
internal fun ContextMenuItem(
    text: String,
    color: Color = TextForeground,
    icon: AppIcon? = null,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Box(
        Modifier
            .fillMaxWidth()
            .height(22.dp)
            .background(if (hovered) SubSelectColor else Color.Transparent)
            .hoverable(interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (icon != null) AppIconView(icon, size = 14.dp, tint = color.takeIf { it != TextForeground })
            Text(text, color = color, fontSize = 11.sp, maxLines = 1)
        }
    }
}

@Composable
internal fun ContextMenuSeparator() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .height(1.dp)
            .background(DarkenedBorder),
    )
}

@Composable
internal fun ContextMenuCaption(text: String) {
    Text(text.uppercase(), color = GhostlyText, fontSize = 10.sp, modifier = Modifier.padding(start = 10.dp, top = 4.dp, bottom = 2.dp))
}

/** The "Create" group: a layout, styles, an imageset or a folder. */
@Composable
private fun ColumnScope.CreateMenuItems(onPick: (NewFileKind) -> Unit) {
    ContextMenuCaption(stringResource(Res.string.create))
    NewFileKind.entries.forEach { kind ->
        ContextMenuItem(kind.menuLabel, icon = kind.menuIcon) { onPick(kind) }
    }
}

private val NewFileKind.menuLabel: String
    get() =
        when (this) {
            NewFileKind.LAYOUT -> "Layout (.layout)"
            NewFileKind.STYLES -> "Styles (.styles)"
            NewFileKind.IMAGESET -> "Imageset (.imageset)"
            NewFileKind.FOLDER -> str(Res.string.folder)
        }

private val NewFileKind.menuIcon: AppIcon
    get() =
        when (this) {
            NewFileKind.LAYOUT -> AppIcon.LAYOUT
            NewFileKind.STYLES -> AppIcon.STYLES
            NewFileKind.IMAGESET -> AppIcon.IMAGESET
            NewFileKind.FOLDER -> AppIcon.FOLDER
        }

private val NewFileKind.placeholder: String
    get() =
        when (this) {
            NewFileKind.LAYOUT -> str(Res.string.name_layout)
            NewFileKind.STYLES -> str(Res.string.name_styles)
            NewFileKind.IMAGESET -> str(Res.string.name_imageset)
            NewFileKind.FOLDER -> str(Res.string.folder_name)
        }

// ---------------------------------------------------------------------------
// Search
// ---------------------------------------------------------------------------

private const val SEARCH_LIMIT = 300

/** Case-insensitive match of every space separated word against the file or folder name, anywhere under [root]. */
private suspend fun searchTree(
    top: TreeTop,
    query: String,
): List<TreeEntry> =
    coroutineScope {
        val root = top.path
        // A result of a mod or of the game tells which one it is in
        val prefix =
            top.label
                .takeIf { it.isNotEmpty() }
                ?.let { "$it/" }
                .orEmpty()
        val words =
            query
                .trim()
                .lowercase()
                .split(Regex("\\s+"))
                .filter { it.isNotEmpty() }
        val found = ArrayList<TreeEntry>()

        fun visit(
            path: Path,
            isDir: Boolean,
        ): FileVisitResult {
            ensureActive() // a newer query cancels this walk
            if (path != root) {
                val name = path.fileName?.toString().orEmpty()
                val lower = name.lowercase()
                if (name.isNotEmpty() && words.all { lower.contains(it) }) {
                    val folder = prefix + path.parent?.let { root.relativize(it).toString() }.orEmpty()
                    found.add(TreeEntry(path, name, isDir, 0, folder.trimEnd('/')))
                }
            }
            return if (found.size >= SEARCH_LIMIT) FileVisitResult.TERMINATE else FileVisitResult.CONTINUE
        }

        // A PBO is searched through its list of files
        if (top.archive != null) {
            val seen = HashSet<String>()
            for (entry in top.archive.entries) {
                val parts = entry.path.split('/')
                for (i in parts.indices) {
                    val name = parts[i]
                    val full = parts.take(i + 1).joinToString("/")
                    if (!seen.add(full.lowercase()) || !words.all { name.lowercase().contains(it) }) continue
                    val folder = prefix + parts.take(i).joinToString("/")
                    found.add(TreeEntry(root.resolve(full), name, i < parts.lastIndex, 0, folder.trimEnd('/')))
                    if (found.size >= SEARCH_LIMIT) break
                }
                ensureActive()
                if (found.size >= SEARCH_LIMIT) break
            }
            return@coroutineScope found.sortedWith(compareBy({ !it.isDir }, { it.name.lowercase() }))
        }
        // A folder that cannot be read is skipped instead of ending the whole search
        runCatching {
            Files.walkFileTree(
                root,
                object : SimpleFileVisitor<Path>() {
                    override fun preVisitDirectory(
                        dir: Path,
                        attrs: java.nio.file.attribute.BasicFileAttributes,
                    ) = visit(dir, true)

                    override fun visitFile(
                        file: Path,
                        attrs: java.nio.file.attribute.BasicFileAttributes,
                    ) = visit(file, false)

                    override fun visitFileFailed(
                        file: Path,
                        exc: java.io.IOException,
                    ) = FileVisitResult.CONTINUE
                },
            )
        }
        found.sortedWith(compareBy({ !it.isDir }, { it.name.lowercase() }))
    }

@Composable
internal fun SearchField(
    value: String,
    placeholder: String = stringResource(Res.string.search_by_name),
    onChange: (String) -> Unit,
) {
    val focus = remember { MutableInteractionSource() }
    val focused by focus.collectIsFocusedAsState()
    // A padded field of its own, lighter than the panel and with a frame that lights up while typing
    Box(Modifier.fillMaxWidth().background(FrameBackground).padding(horizontal = 6.dp, vertical = 5.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(26.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(ElementsBackground)
                .border(1.dp, if (focused) SelectColor else DarkenedBorder, RoundedCornerShape(4.dp))
                .padding(horizontal = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIconView(AppIcon.UI_SEARCH, size = 13.dp, tint = if (focused) TextForeground else GhostlyText)
            Spacer(Modifier.width(6.dp))
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                BasicTextField(
                    value = value,
                    onValueChange = onChange,
                    singleLine = true,
                    textStyle = TextStyle(color = TextForeground, fontSize = 11.sp),
                    cursorBrush = SolidColor(TextForeground),
                    interactionSource = focus,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (value.isEmpty()) {
                    Text(placeholder, color = GhostlyText, fontSize = 11.sp)
                }
            }
            if (value.isNotEmpty()) {
                Tip(stringResource(Res.string.clear_the_search)) {
                    Box(
                        Modifier
                            .size(16.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onChange("") },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("×", color = GhostlyText, fontSize = 14.sp, lineHeight = 14.sp)
                    }
                }
            }
        }
    }
}

/** The files used last, three of them, under the tree. */
@Composable
private fun RecentFiles(state: LayoutEditorState) {
    val recent = state.recentFiles.take(3)
    if (recent.isEmpty()) return
    Column(Modifier.fillMaxWidth().background(FrameBackground)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(DarkenedBorder))
        Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            AppIconView(AppIcon.UI_CLOCK, size = 12.dp, tint = GhostlyText)
            Spacer(Modifier.width(6.dp))
            Text(stringResource(Res.string.recent), color = GhostlyText, fontSize = 10.sp)
        }
        recent.forEach { path ->
            val name = java.io.File(path).name
            Tip(path) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(22.dp)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { state.openAny(path) }
                        .padding(start = 14.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppIconView(fileIcon(name), size = 14.dp)
                    Spacer(Modifier.width(6.dp))
                    Text(name, color = TextForeground, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
