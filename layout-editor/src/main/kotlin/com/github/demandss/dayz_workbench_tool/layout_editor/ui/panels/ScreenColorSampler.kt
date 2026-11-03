package com.github.demandss.dayz_workbench_tool.layout_editor.ui.panels

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.Color
import java.awt.Cursor
import java.awt.Dialog
import java.awt.Graphics
import java.awt.GraphicsEnvironment
import java.awt.Rectangle
import java.awt.Robot
import java.awt.event.ActionEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.image.BufferedImage
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.AbstractAction
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JWindow
import javax.swing.KeyStroke
import javax.swing.SwingUtilities

/**
 * A frozen desktop image captures the entire left click, including its release, before the picker is shown again.
 * The window is exempt from the picker's modality, so clicks outside the picker never reach another application.
 */
@Composable
internal fun ScreenColorSampler(
    onPick: (Color) -> Unit,
    onCancel: () -> Unit,
) {
    val latestPick by rememberUpdatedState(onPick)
    val latestCancel by rememberUpdatedState(onCancel)
    DisposableEffect(Unit) {
        val disposed = AtomicBoolean(false)
        var overlay: JWindow? = null
        SwingUtilities.invokeLater {
            if (disposed.get()) return@invokeLater
            runCatching {
                val devices = GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices
                val desktop = devices.map { it.defaultConfiguration.bounds }.reduce(Rectangle::union)
                val image = BufferedImage(desktop.width, desktop.height, BufferedImage.TYPE_INT_RGB)
                val graphics = image.createGraphics()
                try {
                    devices.forEach { device ->
                        val bounds = device.defaultConfiguration.bounds
                        val captured = Robot(device).createScreenCapture(bounds)
                        graphics.drawImage(captured, bounds.x - desktop.x, bounds.y - desktop.y, null)
                    }
                } finally {
                    graphics.dispose()
                }

                val pane =
                    object : JPanel() {
                        override fun paintComponent(graphics: Graphics) {
                            super.paintComponent(graphics)
                            graphics.drawImage(image, 0, 0, width, height, null)
                        }
                    }
                pane.cursor = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR)
                pane.isFocusable = true
                pane.addMouseListener(
                    object : MouseAdapter() {
                        private var pending: Color? = null

                        override fun mousePressed(event: MouseEvent) {
                            if (event.button != MouseEvent.BUTTON1) return
                            val x = event.x.coerceIn(0, image.width - 1)
                            val y = event.y.coerceIn(0, image.height - 1)
                            pending = Color(image.getRGB(x, y))
                            event.consume()
                        }

                        override fun mouseReleased(event: MouseEvent) {
                            if (event.button != MouseEvent.BUTTON1) return
                            val picked = pending ?: return
                            pending = null
                            event.consume()
                            latestPick(picked)
                        }
                    },
                )
                val window = JWindow()
                overlay = window
                window.modalExclusionType = Dialog.ModalExclusionType.APPLICATION_EXCLUDE
                window.isAlwaysOnTop = true
                window.bounds = desktop
                window.contentPane = pane
                window.rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"), "cancel-sampling")
                window.rootPane.actionMap.put(
                    "cancel-sampling",
                    object : AbstractAction() {
                        override fun actionPerformed(event: ActionEvent?) {
                            latestCancel()
                        }
                    },
                )
                if (disposed.get()) {
                    window.dispose()
                } else {
                    window.isVisible = true
                    window.toFront()
                    pane.requestFocusInWindow()
                }
            }.onFailure { latestCancel() }
        }
        onDispose {
            disposed.set(true)
            SwingUtilities.invokeLater { overlay?.dispose() }
        }
    }
}
