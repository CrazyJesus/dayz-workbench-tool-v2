# AGENTS.md

Guidance for AI coding agents (and people) working in this repository. See also [CONTRIBUTING.md](CONTRIBUTING.md).

## What this is

DayZ Workbench Tool: a Windows-only Kotlin / Compose Desktop program that starts the DayZ client, a local server and Workbench with the mods you pick, plus a separate editor for DayZ `.layout` files. Windows is a hard requirement: PowerShell file dialogs, symbolic links for mods, the registry to find Steam.

## Modules

```
core/            shared library, no run task
app/             the main program (single window, screens)
layout-editor/   the .layout editor; opened from the app as a second window, and runnable alone
```

- The package is `com.github.demandss.dayz_workbench_tool`, and the source folders follow it, all in lower case: `src/main/kotlin/com/github/demandss/dayz_workbench_tool/...`. Never add a folder that differs from an existing one only in case: Windows treats the two as one folder, Linux as two.
- `core` applies the Compose plugin for its dependency accessors and **disables the `run` task** it adds (`tasks.matching { it.name == "run" }.configureEach { enabled = false }`). Do not remove that.
- `core` holds: `config/` (settings and their files), `layout/` (`.layout` parser, serializer, widget model), `i18n/`, `ui/` (theme and reusable components), `update/` (release check), and the readers of game formats (`pbo`, `texture`, `imageset`, `styles`, `font`, `stringtable`).

## Commands

```
./gradlew :app:run                  # the main program
./gradlew :layout-editor:run        # the editor alone
./gradlew ktlintCheckAll            # style; CI runs it, so run it before you push
./gradlew :core:test :app:test :layout-editor:test
./gradlew :app:packagePortable :app:packageReleaseMsi
```

JDK 25 (toolchain). The version of the programs is `version = "..."` in the root `build.gradle.kts`; the release workflow writes the release version there.

## Settings

- `AppConfig` is `@Serializable`. Use `ConfigManager.App` and `ConfigManager.LayoutEditor`; the constructor that takes a `baseDir` exists for tests only.
- Files: `%APPDATA%/.dayzworkbenchtool/v2/app/settings.json` and `.../v2/layout-editor/settings.json`. `save()` writes a temp file and moves it over; a file that cannot be read is copied to `settings.json.broken` before defaults are used.
- In `AppState`, `updateConfig()` is the main write path (state, disk, mods); `persistMods()` is the second one. Never mutate `config` directly.
- The settings window edits a local copy and commits it on "Save".

## UI

- Theme "DarkSwing": take colors from `ui.theme` (`FrameBackground`, `ElementsBackground`, `SelectColor`, `SubSelectColor`, `DarkenedBorder`, `TextForeground`, `GhostlyText`), not literals.
- No ripple (`indication = null`). Press feedback with `MutableInteractionSource` + `collectIsPressedAsState`, hover with `hoverable` + `collectIsHoveredAsState`.
- File and folder pickers are blocking PowerShell dialogs: call `pickFile` / `pickDirectory` from a coroutine (`scope.launch`). They run a temporary UTF-8 script with a BOM, so text of any alphabet is safe.
- **All text the user sees goes to the string resources**, `core/src/main/composeResources/values` (English) and `values-ru` (Russian), never into the code. In composables use `stringResource`, elsewhere `str(Res.string.key, args)`. A key must be unique in the file (a duplicate breaks the resource build) and exist in both languages (`StringResourcesTest` checks it). Numbers written for files use a dot (`formatShort`), whatever the language.
- A new interface language is a value in `Language` plus a `values-xx/strings.xml`.

## `.layout` format (layout editor)

Text format, not XML or JSON:

```
WidgetClassName {
  Name "WidgetName"
  property value
  "quoted property name" value
  {
    ChildWidgetClass "{00000000A1B2C3D4}" {
      Name "ChildName"
      ...
    }
  }
}
```

- `hexactpos` / `vexactpos` / `hexactsize` / `vexactsize` choose whether position and size are pixels (`1`) or fractions 0..1 (`0`).
- Colors are `color R G B A` floats; `halign` / `valign`: `left`, `center_ref`, `right`, `right_ref`, `top`, `bottom`, `bottom_ref`.
- Children are in the second `{ }` block. `EmbededWidgetClass` points to another layout with `layout "path"`.
- The root has no resource ID. Children have a quoted 16-digit hex ID in braces after their class name; their script-facing name is the `Name` property. The parser also accepts the old `WidgetClassName WidgetName {` form, and saving writes the new form.

## MCP server (layout editor)

A local MCP server (`layout_editor/mcp`): Streamable HTTP on `127.0.0.1` only, JSON answers, a bearer token kept in the editor settings. The descriptions of the tools are in `McpTools.kt`; the guide an assistant reads on first contact is in `layout-editor/src/main/resources/mcp/` (`instructions.md`, `prompts/`), loaded by `McpGuide`. A tool that writes files must stay inside the folders the user chose. The server must never listen on anything but loopback.

## Tests

There are tests in all three modules (JUnit 5, `kotlin.test`, Compose `uiTest` for windows). Add one when you fix a bug or add a rule that can be tested.

- Test paths live in a temporary folder of the test; no paths from a real computer.
- A test that needs an installed game takes it from `DAYZ_GAME_DIR` or the default Steam folder and is skipped when there is none (`installedGame()`).

## Git

- Branches: `feature/<name>` or `fix/<name>`; do not work in `master`.
- Commit messages: English, imperative, short (`Fix the order of widgets sent to the back`). One thing per commit.
- Do not commit logs, JVM crash dumps (`hs_err_pid*`, `replay_pid*`; the JVM is set to write them into `logs/`), settings files, tokens or paths from your own computer.
