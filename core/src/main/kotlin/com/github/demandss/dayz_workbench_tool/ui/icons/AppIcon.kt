@file:Suppress("ktlint:standard:max-line-length")

package com.github.demandss.dayz_workbench_tool.ui.icons

/** How a shape is filled besides its outline: [TINT] is the icon colour at low opacity, [SOLID] has no outline. */
enum class IconFill { NONE, TINT, SOLID }

/** One SVG path on the 16x16 grid. [dash] is an on/off pattern in grid units. */
class IconShape(
    val d: String,
    val fill: IconFill = IconFill.NONE,
    val dash: FloatArray = floatArrayOf(),
    val width: Float = 1.4f,
)

/**
 * The editor's icons, drawn as outlines on a 16x16 grid with round caps.
 * [tint] is the colour an icon has by default (null: the caller's text colour).
 * File types are coloured by kind, widgets are a neutral blue grey.
 */
enum class AppIcon(
    val tint: Long?,
    val shapes: List<IconShape>,
) {
    FOLDER(
        0xFFE3B341L,
        listOf(
            IconShape("M1.5 4.5a1 1 0 0 1 1-1h3.2l1.6 1.8h6.2a1 1 0 0 1 1 1V12a1 1 0 0 1-1 1h-11a1 1 0 0 1-1-1z", IconFill.TINT),
        ),
    ),
    FOLDER_OPEN(
        0xFFE3B341L,
        listOf(
            IconShape("M1.5 12.5V4a1 1 0 0 1 1-1h3.2l1.6 1.8h5.2a1 1 0 0 1 1 1V7"),
            IconShape(
                "M1.5 12.5l1.7-5a1 1 0 0 1 .95-.7H14a.8.8 0 0 1 .77 1.05l-1.5 4.7a1 1 0 0 1-.95.7H2.5a1 1 0 0 1-1-.75z",
                IconFill.TINT,
            ),
        ),
    ),
    LAYOUT(
        0xFF8EC6FFL,
        listOf(
            IconShape("M3 2.5h10a1 1 0 0 1 1 1v9a1 1 0 0 1 -1 1h-10a1 1 0 0 1 -1 -1v-9a1 1 0 0 1 1 -1z"),
            IconShape("M2 6h12M6 6v7.5"),
        ),
    ),
    EDDS(
        0xFF5FD0C0L,
        listOf(
            IconShape("M6.5 2h6.5a1 1 0 0 1 1 1v6.5a1 1 0 0 1 -1 1h-6.5a1 1 0 0 1 -1 -1v-6.5a1 1 0 0 1 1 -1z"),
            IconShape("M2.5 5.5V13a1 1 0 0 0 1 1H11"),
            IconShape("M5.5 8.5l2-2 2.5 2.5 1.2-1.2 2.8 2.8"),
        ),
    ),
    IMAGE(
        0xFF5FD0C0L,
        listOf(
            IconShape(
                "M3.2 3h9.6a1.2 1.2 0 0 1 1.2 1.2v7.6a1.2 1.2 0 0 1 -1.2 1.2h-9.6a1.2 1.2 0 0 1 -1.2 -1.2v-7.6a1.2 1.2 0 0 1 1.2 -1.2z",
            ),
            IconShape("M4.699999999999999 6.6a1.1 1.1 0 1 0 2.2 0a1.1 1.1 0 1 0 -2.2 0z"),
            IconShape("M2.5 12l3.6-3.4 2.6 2.4 2-1.8L13.5 12"),
        ),
    ),
    IMAGESET(
        0xFFB79CF0L,
        listOf(
            IconShape(
                "M2.8 2h3.4a0.8 0.8 0 0 1 0.8 0.8v3.4a0.8 0.8 0 0 1 -0.8 0.8h-3.4a0.8 0.8 0 0 1 -0.8 -0.8v-3.4a0.8 0.8 0 0 1 0.8 -0.8z",
            ),
            IconShape(
                "M9.8 2h3.4a0.8 0.8 0 0 1 0.8 0.8v3.4a0.8 0.8 0 0 1 -0.8 0.8h-3.4a0.8 0.8 0 0 1 -0.8 -0.8v-3.4a0.8 0.8 0 0 1 0.8 -0.8z",
            ),
            IconShape(
                "M2.8 9h3.4a0.8 0.8 0 0 1 0.8 0.8v3.4a0.8 0.8 0 0 1 -0.8 0.8h-3.4a0.8 0.8 0 0 1 -0.8 -0.8v-3.4a0.8 0.8 0 0 1 0.8 -0.8z",
            ),
            IconShape(
                "M9.8 9h3.4a0.8 0.8 0 0 1 0.8 0.8v3.4a0.8 0.8 0 0 1 -0.8 0.8h-3.4a0.8 0.8 0 0 1 -0.8 -0.8v-3.4a0.8 0.8 0 0 1 0.8 -0.8z",
                IconFill.TINT,
            ),
        ),
    ),
    STYLES(
        0xFFB79CF0L,
        listOf(
            IconShape("M8 2s4.5 4.7 4.5 7.6A4.5 4.5 0 0 1 3.5 9.6C3.5 6.7 8 2 8 2z", IconFill.TINT),
            IconShape("M6 10a2.2 2.2 0 0 0 2 1.6"),
        ),
    ),
    SCRIPT(
        0xFF7CC88AL,
        listOf(
            IconShape("M5.5 4.5L2 8l3.5 3.5M10.5 4.5L14 8l-3.5 3.5M9 3l-2 10"),
        ),
    ),
    CONFIG(
        0xFFE8985AL,
        listOf(
            IconShape("M5.8 8a2.2 2.2 0 1 0 4.4 0a2.2 2.2 0 1 0 -4.4 0z"),
            IconShape("M8 1.8v1.7M8 12.5v1.7M1.8 8h1.7M12.5 8h1.7M3.6 3.6l1.2 1.2M11.2 11.2l1.2 1.2M12.4 3.6l-1.2 1.2M4.8 11.2l-1.2 1.2"),
        ),
    ),
    CSV(
        0xFF9AA0A8L,
        listOf(
            IconShape("M3 3h10a1 1 0 0 1 1 1v8a1 1 0 0 1 -1 1h-10a1 1 0 0 1 -1 -1v-8a1 1 0 0 1 1 -1z"),
            IconShape("M2 6.7h12M2 10h12M6.3 3v10"),
        ),
    ),
    JSON(
        0xFF9AA0A8L,
        listOf(
            IconShape(
                "M6 2.8c-1.5 0-2 .6-2 1.8v1.2c0 .8-.4 1.2-1.4 1.4 1 .2 1.4.6 1.4 1.4v1.2c0 1.2.5 1.8 2 1.8M10 2.8c1.5 0 2 .6 2 1.8v1.2c0 .8.4 1.2 1.4 1.4-1 .2-1.4.6-1.4 1.4v1.2c0 1.2-.5 1.8-2 1.8",
            ),
        ),
    ),
    META(
        0xFF9AA0A8L,
        listOf(
            IconShape("M2.5 8.3V3.5a1 1 0 0 1 1-1h4.8l5.2 5.2a1 1 0 0 1 0 1.4l-4.4 4.4a1 1 0 0 1-1.4 0z"),
            IconShape("M4.6 5.6a1 1 0 1 0 2 0a1 1 0 1 0 -2 0z"),
        ),
    ),
    FILE(
        0xFF9AA0A8L,
        listOf(
            IconShape("M4 1.8h5.2L12.5 5v8.2a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V2.8a1 1 0 0 1 1-1z"),
            IconShape("M9 1.8V5h3.5"),
        ),
    ),
    WIDGET_FRAME(
        0xFFA9B7CFL,
        listOf(
            IconShape("M3 3h10a1 1 0 0 1 1 1v8a1 1 0 0 1 -1 1h-10a1 1 0 0 1 -1 -1v-8a1 1 0 0 1 1 -1z", dash = floatArrayOf(2f, 1.6f)),
        ),
    ),
    WIDGET_PANEL(
        0xFFA9B7CFL,
        listOf(
            IconShape("M3 3h10a1 1 0 0 1 1 1v8a1 1 0 0 1 -1 1h-10a1 1 0 0 1 -1 -1v-8a1 1 0 0 1 1 -1z", IconFill.TINT),
        ),
    ),
    WIDGET_WINDOW(
        0xFFA9B7CFL,
        listOf(
            IconShape("M3 2.5h10a1 1 0 0 1 1 1v9a1 1 0 0 1 -1 1h-10a1 1 0 0 1 -1 -1v-9a1 1 0 0 1 1 -1z"),
            IconShape("M2 5.7h12M4 4.1h.01M5.8 4.1h.01"),
        ),
    ),
    WIDGET_IMAGE(
        0xFFA9B7CFL,
        listOf(
            IconShape(
                "M3.2 3h9.6a1.2 1.2 0 0 1 1.2 1.2v7.6a1.2 1.2 0 0 1 -1.2 1.2h-9.6a1.2 1.2 0 0 1 -1.2 -1.2v-7.6a1.2 1.2 0 0 1 1.2 -1.2z",
            ),
            IconShape("M4.699999999999999 6.6a1.1 1.1 0 1 0 2.2 0a1.1 1.1 0 1 0 -2.2 0z"),
            IconShape("M2.5 12l3.6-3.4 2.6 2.4 2-1.8L13.5 12"),
        ),
    ),
    WIDGET_TEXT(
        0xFFA9B7CFL,
        listOf(
            IconShape("M3.5 4.5v-1h9v1M8 3.5v9M6.3 12.5h3.4"),
        ),
    ),
    WIDGET_RICHTEXT(
        0xFFA9B7CFL,
        listOf(
            IconShape("M3 4V3h10v1M8 3v7.5M6 10.5h4M3 13.5h10"),
        ),
    ),
    WIDGET_BUTTON(
        0xFFA9B7CFL,
        listOf(
            IconShape("M4 4.5h8a2 2 0 0 1 2 2v3a2 2 0 0 1 -2 2h-8a2 2 0 0 1 -2 -2v-3a2 2 0 0 1 2 -2z"),
            IconShape("M5.5 8h5"),
        ),
    ),
    WIDGET_CHECKBOX(
        0xFFA9B7CFL,
        listOf(
            IconShape("M4 2.5h8a1.5 1.5 0 0 1 1.5 1.5v8a1.5 1.5 0 0 1 -1.5 1.5h-8a1.5 1.5 0 0 1 -1.5 -1.5v-8a1.5 1.5 0 0 1 1.5 -1.5z"),
            IconShape("M5.2 8.2l2 2 3.6-4"),
        ),
    ),
    WIDGET_EDITBOX(
        0xFFA9B7CFL,
        listOf(
            IconShape("M3 4.5h10a1 1 0 0 1 1 1v5a1 1 0 0 1 -1 1h-10a1 1 0 0 1 -1 -1v-5a1 1 0 0 1 1 -1z"),
            IconShape("M5 6.5v3"),
        ),
    ),
    WIDGET_SLIDER(
        0xFFA9B7CFL,
        listOf(
            IconShape("M2 8h12"),
            IconShape("M7 8a2 2 0 1 0 4 0a2 2 0 1 0 -4 0z", IconFill.TINT),
        ),
    ),
    WIDGET_PROGRESS(
        0xFFA9B7CFL,
        listOf(
            IconShape("M4 6h8a2 2 0 0 1 2 2v0a2 2 0 0 1 -2 2h-8a2 2 0 0 1 -2 -2v0a2 2 0 0 1 2 -2z"),
            IconShape("M4 6h3a2 2 0 0 1 2 2v0a2 2 0 0 1 -2 2h-3a2 2 0 0 1 -2 -2v0a2 2 0 0 1 2 -2z", IconFill.TINT),
        ),
    ),
    WIDGET_SCROLL(
        0xFFA9B7CFL,
        listOf(
            IconShape("M3.5 2h9a1 1 0 0 1 1 1v10a1 1 0 0 1 -1 1h-9a1 1 0 0 1 -1 -1v-10a1 1 0 0 1 1 -1z"),
            IconShape("M11 4v8"),
        ),
    ),
    WIDGET_GRID(
        0xFFA9B7CFL,
        listOf(
            IconShape("M2.5 2.5h11v11h-11zM8 2.5v11M2.5 8h11"),
        ),
    ),
    WIDGET_WRAP(
        0xFFA9B7CFL,
        listOf(
            IconShape(
                "M2.6 3h2.3a0.6 0.6 0 0 1 0.6 0.6v1.8a0.6 0.6 0 0 1 -0.6 0.6h-2.3a0.6 0.6 0 0 1 -0.6 -0.6v-1.8a0.6 0.6 0 0 1 0.6 -0.6z",
            ),
            IconShape(
                "M7.1 3h2.3a0.6 0.6 0 0 1 0.6 0.6v1.8a0.6 0.6 0 0 1 -0.6 0.6h-2.3a0.6 0.6 0 0 1 -0.6 -0.6v-1.8a0.6 0.6 0 0 1 0.6 -0.6z",
            ),
            IconShape(
                "M2.6 8h2.3a0.6 0.6 0 0 1 0.6 0.6v1.8a0.6 0.6 0 0 1 -0.6 0.6h-2.3a0.6 0.6 0 0 1 -0.6 -0.6v-1.8a0.6 0.6 0 0 1 0.6 -0.6z",
            ),
            IconShape("M6.5 9.5h5"),
        ),
    ),
    WIDGET_CANVAS(
        0xFFA9B7CFL,
        listOf(
            IconShape("M3 13l.8-3L11 2.8a1.2 1.2 0 0 1 1.7 0l.5.5a1.2 1.2 0 0 1 0 1.7L6 12.2z"),
        ),
    ),
    WIDGET_ITEM(
        0xFFA9B7CFL,
        listOf(
            IconShape("M8 1.8l5.5 3v6.4L8 14.2l-5.5-3V4.8z"),
            IconShape("M2.5 4.8L8 8l5.5-3.2M8 8v6.2"),
        ),
    ),
    WIDGET_PLAYER(
        0xFFA9B7CFL,
        listOf(
            IconShape("M5.6 5a2.4 2.4 0 1 0 4.8 0a2.4 2.4 0 1 0 -4.8 0z"),
            IconShape("M3 14c.4-3 2.4-4.4 5-4.4s4.6 1.4 5 4.4"),
        ),
    ),
    WIDGET_MAP(
        0xFFA9B7CFL,
        listOf(
            IconShape("M8 14s4.5-4.2 4.5-7.4a4.5 4.5 0 0 0-9 0C3.5 9.8 8 14 8 14z"),
            IconShape("M6.4 6.6a1.6 1.6 0 1 0 3.2 0a1.6 1.6 0 1 0 -3.2 0z"),
        ),
    ),
    WIDGET_LIST(
        0xFFA9B7CFL,
        listOf(
            IconShape("M3 2.5h10a1 1 0 0 1 1 1v9a1 1 0 0 1 -1 1h-10a1 1 0 0 1 -1 -1v-9a1 1 0 0 1 1 -1z"),
            IconShape("M5 6h6M5 8.5h6M5 11h4"),
        ),
    ),
    WIDGET_COMBO(
        0xFFA9B7CFL,
        listOf(
            IconShape("M3 4.5h10a1 1 0 0 1 1 1v5a1 1 0 0 1 -1 1h-10a1 1 0 0 1 -1 -1v-5a1 1 0 0 1 1 -1z"),
            IconShape("M9.6 7.4l1.4 1.4 1.4-1.4"),
        ),
    ),
    WIDGET_HTML(
        0xFFA9B7CFL,
        listOf(
            IconShape("M2 8a6 6 0 1 0 12 0a6 6 0 1 0 -12 0z"),
            IconShape("M2 8h12M8 2c2 1.8 2.6 4 2.6 6S10 12.2 8 14c-2-1.8-2.6-4-2.6-6S6 3.8 8 2z"),
        ),
    ),
    WIDGET_VIDEO(
        0xFFA9B7CFL,
        listOf(
            IconShape("M3.5 3.5h9a1.5 1.5 0 0 1 1.5 1.5v6a1.5 1.5 0 0 1 -1.5 1.5h-9a1.5 1.5 0 0 1 -1.5 -1.5v-6a1.5 1.5 0 0 1 1.5 -1.5z"),
            IconShape("M6.8 6.2v3.6l3.2-1.8z", IconFill.SOLID),
        ),
    ),
    WIDGET_EMBED(
        0xFFA9B7CFL,
        listOf(
            IconShape("M3 2.5h10a1 1 0 0 1 1 1v9a1 1 0 0 1 -1 1h-10a1 1 0 0 1 -1 -1v-9a1 1 0 0 1 1 -1z"),
            IconShape(
                "M5.8 5.5h4.4a0.8 0.8 0 0 1 0.8 0.8v3.4a0.8 0.8 0 0 1 -0.8 0.8h-4.4a0.8 0.8 0 0 1 -0.8 -0.8v-3.4a0.8 0.8 0 0 1 0.8 -0.8z",
            ),
        ),
    ),
    WIDGET_GENERIC(
        0xFFA9B7CFL,
        listOf(
            IconShape("M4 2.5h8a1.5 1.5 0 0 1 1.5 1.5v8a1.5 1.5 0 0 1 -1.5 1.5h-8a1.5 1.5 0 0 1 -1.5 -1.5v-8a1.5 1.5 0 0 1 1.5 -1.5z"),
        ),
    ),
    UI_CURSOR(
        null,
        listOf(
            IconShape("M4 2l8 4.6-3.6 1 2 4-1.5.8-2-4L4.8 11z"),
        ),
    ),
    UI_EYEDROPPER(
        null,
        listOf(
            IconShape("M9.8 2.1l4.1 4.1-1.2 1.2-4.1-4.1z", IconFill.TINT),
            IconShape("M9.2 3.1l3.7 3.7-6.6 6.6c-.5.5-1.1.7-1.8.7H2v-2.5c0-.7.2-1.3.7-1.8z"),
            IconShape("M7.7 4.6l3.7 3.7"),
        ),
    ),
    UI_HAND(
        null,
        listOf(
            IconShape(
                "M6 8V3.5a1 1 0 0 1 2 0V7M8 6V2.8a1 1 0 0 1 2 0V7M10 6.6V4.2a1 1 0 0 1 2 0V10c0 2.4-1.8 4-4 4-1.6 0-2.6-.7-3.6-2L2.6 9.6a1 1 0 0 1 1.6-1.2L6 10",
            ),
        ),
    ),
    UI_ZOOMIN(
        null,
        listOf(
            IconShape("M2.5 7a4.5 4.5 0 1 0 9 0a4.5 4.5 0 1 0 -9 0z"),
            IconShape("M10.5 10.5L14 14M7 5v4M5 7h4"),
        ),
    ),
    UI_ZOOMOUT(
        null,
        listOf(
            IconShape("M2.5 7a4.5 4.5 0 1 0 9 0a4.5 4.5 0 1 0 -9 0z"),
            IconShape("M10.5 10.5L14 14M5 7h4"),
        ),
    ),
    UI_FIT(
        null,
        listOf(
            IconShape("M2 6V2h4M10 2h4v4M14 10v4h-4M6 14H2v-4"),
        ),
    ),
    UI_GRID(
        null,
        listOf(
            IconShape("M2.5 2.5h11v11h-11zM6.2 2.5v11M9.8 2.5v11M2.5 6.2h11M2.5 9.8h11"),
        ),
    ),
    UI_MAGNET(
        null,
        listOf(
            IconShape("M3 2h3.5v6a1.5 1.5 0 0 0 3 0V2H13v6a5 5 0 0 1-10 0z"),
            IconShape("M3 5h3.5M9.5 5h3.5"),
        ),
    ),
    UI_RULER(
        null,
        listOf(
            IconShape("M2.8 5h10.4a1 1 0 0 1 1 1v4a1 1 0 0 1 -1 1h-10.4a1 1 0 0 1 -1 -1v-4a1 1 0 0 1 1 -1z"),
            IconShape("M5 5v2.5M8 5v3.2M11 5v2.5"),
        ),
    ),
    UI_MEASURE(
        null,
        listOf(
            IconShape("M2.5 8h11M2.5 5v6M13.5 5v6M5.5 6l-3 2 3 2M10.5 6l3 2-3 2"),
        ),
    ),
    UI_SCALED(
        null,
        listOf(
            IconShape("M3 6h6a1 1 0 0 1 1 1v6a1 1 0 0 1 -1 1h-6a1 1 0 0 1 -1 -1v-6a1 1 0 0 1 1 -1z"),
            IconShape("M6 6V3.5a1 1 0 0 1 1-1h6.5a1 1 0 0 1 1 1V10a1 1 0 0 1-1 1H10"),
        ),
    ),
    UI_EYE(
        null,
        listOf(
            IconShape("M1.5 8S4 3.8 8 3.8 14.5 8 14.5 8 12 12.2 8 12.2 1.5 8 1.5 8z"),
            IconShape("M6.2 8a1.8 1.8 0 1 0 3.6 0a1.8 1.8 0 1 0 -3.6 0z"),
        ),
    ),
    UI_EYEOFF(
        null,
        listOf(
            IconShape("M1.5 8S4 3.8 8 3.8 14.5 8 14.5 8 12 12.2 8 12.2 1.5 8 1.5 8z"),
            IconShape("M3 3l10 10"),
        ),
    ),
    UI_LOCK(
        null,
        listOf(
            IconShape("M4.5 7h7a1 1 0 0 1 1 1v4.5a1 1 0 0 1 -1 1h-7a1 1 0 0 1 -1 -1v-4.5a1 1 0 0 1 1 -1z"),
            IconShape("M5.5 7V5a2.5 2.5 0 0 1 5 0v2"),
        ),
    ),
    UI_UNLOCK(
        null,
        listOf(
            IconShape("M4.5 7h7a1 1 0 0 1 1 1v4.5a1 1 0 0 1 -1 1h-7a1 1 0 0 1 -1 -1v-4.5a1 1 0 0 1 1 -1z"),
            IconShape("M5.5 7V5a2.5 2.5 0 0 1 4.8-1"),
        ),
    ),
    UI_UNDO(
        null,
        listOf(
            IconShape("M5 3.5L2.5 6 5 8.5M2.8 6H9a4 4 0 0 1 0 8H6"),
        ),
    ),
    UI_REDO(
        null,
        listOf(
            IconShape("M11 3.5L13.5 6 11 8.5M13.2 6H7a4 4 0 0 0 0 8h3"),
        ),
    ),
    UI_SAVE(
        null,
        listOf(
            IconShape("M3 2.5h8l2.5 2.5v8a.5.5 0 0 1-.5.5H3.5a.5.5 0 0 1-.5-.5z"),
            IconShape("M5 2.5v3.5h5V2.5M5 13.5v-4h6v4"),
        ),
    ),
    UI_SEARCH(
        null,
        listOf(
            IconShape("M2.5 7a4.5 4.5 0 1 0 9 0a4.5 4.5 0 1 0 -9 0z"),
            IconShape("M10.5 10.5L14 14"),
        ),
    ),
    UI_CLOSE(
        null,
        listOf(
            IconShape("M4 4l8 8M12 4l-8 8"),
        ),
    ),
    UI_RIGHT(
        null,
        listOf(
            IconShape("M6 3.5L10.5 8 6 12.5"),
        ),
    ),
    UI_DOWN(
        null,
        listOf(
            IconShape("M3.5 6L8 10.5 12.5 6"),
        ),
    ),
    UI_PLUS(
        null,
        listOf(
            IconShape("M8 3v10M3 8h10"),
        ),
    ),
    UI_TRASH(
        null,
        listOf(
            IconShape("M3 4.5h10M6.2 4.5V3h3.6v1.5M4.2 4.5l.6 8.5h6.4l.6-8.5M6.8 7v4M9.2 7v4"),
        ),
    ),
    UI_COPY(
        null,
        listOf(
            IconShape("M6.5 5.5h6a1 1 0 0 1 1 1v6a1 1 0 0 1 -1 1h-6a1 1 0 0 1 -1 -1v-6a1 1 0 0 1 1 -1z"),
            IconShape("M10.5 5.5V3.5a1 1 0 0 0-1-1h-6a1 1 0 0 0-1 1v6a1 1 0 0 0 1 1h2"),
        ),
    ),
    UI_LAYERS(
        null,
        listOf(
            IconShape("M8 2l6 3.2L8 8.4 2 5.2z"),
            IconShape("M2 8.2l6 3.2 6-3.2M2 11l6 3.2 6-3.2"),
        ),
    ),
    UI_RELOAD(
        null,
        listOf(
            IconShape("M13 8a5 5 0 1 1-1.6-3.7M13 2.5v3h-3"),
        ),
    ),
    UI_WARN(
        0xFFE8B04AL,
        listOf(
            IconShape("M8 2.2l6.2 11H1.8z"),
            IconShape("M8 6.5v3.2M8 11.5h.01"),
        ),
    ),
    UI_INFO(
        0xFF6FA8FFL,
        listOf(
            IconShape("M2 8a6 6 0 1 0 12 0a6 6 0 1 0 -12 0z"),
            IconShape("M8 7.2v4M8 5h.01"),
        ),
    ),
    UI_COMPARE(
        null,
        listOf(
            IconShape("M3 2h6a1 1 0 0 1 1 1v6a1 1 0 0 1 -1 1h-6a1 1 0 0 1 -1 -1v-6a1 1 0 0 1 1 -1z"),
            IconShape("M7 6h6a1 1 0 0 1 1 1v6a1 1 0 0 1 -1 1h-6a1 1 0 0 1 -1 -1v-6a1 1 0 0 1 1 -1z"),
        ),
    ),
    UI_SAFE(
        null,
        listOf(
            IconShape(
                "M2.6 2.8h10.8a0.8 0.8 0 0 1 0.8 0.8v8.8a0.8 0.8 0 0 1 -0.8 0.8h-10.8a0.8 0.8 0 0 1 -0.8 -0.8v-8.8a0.8 0.8 0 0 1 0.8 -0.8z",
            ),
            IconShape("M4 4.8h8v6.4h-8z", dash = floatArrayOf(1.6f, 1.4f)),
        ),
    ),
    UI_ANCHOR(
        null,
        listOf(
            IconShape("M6.4 4a1.6 1.6 0 1 0 3.2 0a1.6 1.6 0 1 0 -3.2 0z"),
            IconShape("M8 5.6V14M4.5 8.5h7M3 10.5a5 5 0 0 0 10 0"),
        ),
    ),
    UI_PIXEL(
        null,
        listOf(
            IconShape("M5 8a3 3 0 1 0 6 0a3 3 0 1 0 -6 0z"),
            IconShape("M8 1.5v3M8 11.5v3M1.5 8h3M11.5 8h3"),
        ),
    ),
    UI_CMD(
        null,
        listOf(
            IconShape("M3.5 3h9a1.5 1.5 0 0 1 1.5 1.5v7a1.5 1.5 0 0 1 -1.5 1.5h-9a1.5 1.5 0 0 1 -1.5 -1.5v-7a1.5 1.5 0 0 1 1.5 -1.5z"),
            IconShape("M5 7l2 1.5L5 10M8.5 10.5H11"),
        ),
    ),
    UI_GEAR(
        null,
        listOf(
            IconShape("M3.2 8a4.8 4.8 0 1 0 9.6 0a4.8 4.8 0 1 0 -9.6 0z"),
            IconShape("M6 8a2 2 0 1 0 4 0a2 2 0 1 0 -4 0z"),
            IconShape("M8 1.4v1.8M8 12.8v1.8M1.4 8h1.8M12.8 8h1.8"),
            IconShape("M3.3 3.3l1.3 1.3M11.4 11.4l1.3 1.3M12.7 3.3l-1.3 1.3M4.6 11.4l-1.3 1.3"),
        ),
    ),
    UI_CLOCK(
        null,
        listOf(
            IconShape("M2 8a6 6 0 1 0 12 0a6 6 0 1 0 -12 0z"),
            IconShape("M8 4.5V8l2.4 1.5"),
        ),
    ),
    UI_PLAY(
        0xFFFFFFFFL,
        listOf(
            IconShape("M4.5 2.8v10.4L13 8z", IconFill.SOLID),
        ),
    ),
    UI_ALIGNL(
        null,
        listOf(
            IconShape("M2.5 2v12M5 4.5h8M5 9.5h5"),
        ),
    ),
    UI_ALIGNCH(
        null,
        listOf(
            IconShape("M8 2v12M3.5 4.5h9M5 9.5h6"),
        ),
    ),
    UI_ALIGNR(
        null,
        listOf(
            IconShape("M13.5 2v12M3 4.5h8M6 9.5h5"),
        ),
    ),
    UI_ALIGNT(
        null,
        listOf(
            IconShape("M2 2.5h12M4.5 5v8M9.5 5v5"),
        ),
    ),
    UI_ALIGNMV(
        null,
        listOf(
            IconShape("M2 8h12M4.5 3.5v9M9.5 5v6"),
        ),
    ),
    UI_ALIGNB(
        null,
        listOf(
            IconShape("M2 13.5h12M4.5 3v8M9.5 6v5"),
        ),
    ),
    UI_DOTS(
        null,
        listOf(
            IconShape("M3.5 8h.01M8 8h.01M12.5 8h.01", width = 2.4f),
        ),
    ),
}
