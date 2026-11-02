package com.github.demandss.dayz_workbench_tool.layout

import java.nio.file.Files
import java.nio.file.Path

/** What the file browser can create, with the starting content of each kind. */
enum class NewFileKind(
    val extension: String,
) {
    LAYOUT(".layout"),
    STYLES(".styles"),
    IMAGESET(".imageset"),
    FOLDER(""),
    ;

    /** The name with this kind's extension appended unless it is already there. */
    fun fileName(name: String): String = if (extension.isEmpty() || name.endsWith(extension, ignoreCase = true)) name else name + extension

    /** Minimal valid content. An imageset names itself after the file. */
    fun template(fileName: String): String {
        val name = fileName.removeSuffix(extension)
        return when (this) {
            LAYOUT -> "FrameWidgetClass {\n Name \"RootFrame\"\n}\n"
            STYLES -> "<WidgetStyles>\n</WidgetStyles>\n"
            IMAGESET -> "ImageSetClass {\n Name \"$name\"\n RefSize 1024 1024\n Textures {\n }\n Images {\n }\n}\n"
            FOLDER -> ""
        }
    }

    /**
     * Creates the item in [parent] and returns its path, or null when [name] is empty or is not a plain file name, or when the
     * path is taken by something of the other sort (a folder where a file is wanted, or a file where a folder is).
     * An existing file is returned as it is and never overwritten.
     */
    fun create(
        parent: Path,
        name: String,
    ): Path? {
        val clean = name.trim()
        if (clean.isEmpty() || clean.any { it in FORBIDDEN } || clean == "." || clean == "..") return null
        val target = parent.resolve(fileName(clean))
        if (Files.exists(target) && Files.isDirectory(target) != (this == FOLDER)) return null
        if (this == FOLDER) {
            Files.createDirectories(target)
        } else if (!Files.exists(target)) {
            Files.createDirectories(parent)
            Files.writeString(target, template(target.fileName.toString()))
        }
        return target
    }

    private companion object {
        const val FORBIDDEN = "\\/:*?\"<>|"
    }
}
