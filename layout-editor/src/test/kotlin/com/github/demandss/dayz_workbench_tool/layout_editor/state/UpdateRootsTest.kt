package com.github.demandss.dayz_workbench_tool.layout_editor.state

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UpdateRootsTest {
    private fun folder(name: String): File = createTempDirectory(name).toFile()

    // A work drive that exists only for the test
    private val work: String = folder("work").path

    @Test
    fun theTreeAndTheWorkDriveFollowTheSettingsOfTheMainWindow() {
        val first = folder("modA")
        val second = folder("modB")
        val state = LayoutEditorState("", CoroutineScope(SupervisorJob() + Dispatchers.Default), "", listOf(first.path))
        assertEquals(listOf(first.name), state.treeRoots?.map { it.label })

        // A mod is enabled in the main window, and the work drive is set
        state.updateRoots(work, "", listOf(first.path, second.path))
        assertEquals(listOf(first.name, second.name), state.treeRoots?.map { it.label })
        assertEquals(work, state.workDrivePath)

        // Switched off again: only the first one is left
        state.updateRoots(work, "", listOf(first.path))
        assertEquals(listOf(first.name), state.treeRoots?.map { it.label })

        first.deleteRecursively()
        second.deleteRecursively()
    }

    @Test
    fun vanillaSourceOnTheWorkDriveIsShownInsteadOfAnArchive() {
        val source = File(work, "gui")
        val styles = File(source, "looknfeel/dayzwidgets.styles")
        styles.parentFile.mkdirs()
        styles.writeText("<WidgetStyles />")

        val state = LayoutEditorState(work, CoroutineScope(SupervisorJob() + Dispatchers.Default), "", emptyList())
        val root = state.treeRoots!!.single()
        assertEquals("DayZ", root.label)
        assertEquals(source.path, root.path)
        assertNull(root.archive)

        source.deleteRecursively()
    }
}
