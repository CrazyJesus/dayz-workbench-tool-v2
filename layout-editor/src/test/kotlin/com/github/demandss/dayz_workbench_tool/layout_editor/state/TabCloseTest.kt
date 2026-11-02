package com.github.demandss.dayz_workbench_tool.layout_editor.state

import com.github.demandss.dayz_workbench_tool.layout.LayoutParser
import com.github.demandss.dayz_workbench_tool.layout_editor.toEditorNode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class TabCloseTest {
    private fun editorWithMixedTabs(): LayoutEditorState {
        val work = createTempDirectory("tab-close").toFile()
        val state = LayoutEditorState(work.path, CoroutineScope(SupervisorJob() + Dispatchers.Default), "", emptyList())
        val root = LayoutParser.parse("FrameWidgetClass Root { size 1 1 }").toEditorNode()
        listOf("first.layout", "second.layout").forEach { name ->
            val file = File(work, name)
            file.writeText("FrameWidgetClass Root { size 1 1 }")
            state.openFiles += LayoutDocument(file.path, root)
        }
        val texture = File(work, "texture.png")
        texture.writeBytes(byteArrayOf())
        state.openTextures += TextureDocument(texture.path)
        state.activeFileIndex = 1
        return state
    }

    @Test
    fun closingTabsToTheRightCrossesFileTypes() {
        val state = editorWithMixedTabs()

        state.closeTabs(0, 0, TabCloseAction.RIGHT)

        assertEquals(listOf("first.layout"), state.openFiles.map { it.fileName })
        assertEquals(0, state.openTextures.size)
    }

    @Test
    fun closingTabsToTheLeftOfATextureClosesLayouts() {
        val state = editorWithMixedTabs()

        state.closeTabs(1, 0, TabCloseAction.LEFT)

        assertEquals(0, state.openFiles.size)
        assertEquals(listOf("texture.png"), state.openTextures.map { it.fileName })
    }

    @Test
    fun closingOtherTabsKeepsTheClickedTabAndSelectsIt() {
        val state = editorWithMixedTabs()
        state.selectTexture(0)

        state.closeTabs(0, 1, TabCloseAction.OTHERS)

        assertEquals(listOf("second.layout"), state.openFiles.map { it.fileName })
        assertEquals(0, state.openTextures.size)
        assertEquals(0, state.activeFileIndex)
        assertEquals(-1, state.activeTextureIndex)
    }

    @Test
    fun closingAllTabsIncludesTheClickedTab() {
        val state = editorWithMixedTabs()

        state.closeTabs(0, 1, TabCloseAction.ALL)

        assertEquals(0, state.openFiles.size)
        assertEquals(0, state.openTextures.size)
    }

    @Test
    fun unsavedTabsWaitForConfirmationBeforeClosing() {
        val state = editorWithMixedTabs()
        state.openFiles[0].isModified = true

        state.closeTabs(1, 0, TabCloseAction.LEFT)

        assertEquals(2, state.openFiles.size)
        assertNotNull(state.confirmation).onConfirm()
        assertEquals(0, state.openFiles.size)
        assertNull(state.activeFile)
    }
}
