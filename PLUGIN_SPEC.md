# JetBrains AI Quota Plugin — Spec

## Objective

IntelliJ Platform plugin that displays AI quota usage in the IDE status bar. Parses the current IDE's `idea.log` for quota data and renders a compact widget with a detail popup. Replaces the need for the CLI script when working inside an IDE.

**Target user:** Developer using any JetBrains IDE (IntelliJ, WebStorm, GoLand, PyCharm, etc.)

**Target platforms:** macOS, Linux, Windows

## Behavior

### Data source

The plugin reads the **current IDE's own log file** using `PathManager.getLogPath()` — no cross-IDE scanning needed.

### Log parsing

Parse `idea.log` for two line types (same as CLI spec):

**Quota state** — last line matching `QuotaManager2Impl - New quota state is: Available`:
```
2026-06-05 09:03:28,123 [...]   INFO - #c.i.m.l.c.q.QuotaManager2Impl - New quota state is: Available(current=6685635.02, maximum=25000000, until=..., ...)
```
Extract: `current` (consumed), `maximum`, timestamp.

**Refill state** — last line matching `QuotaManager2Impl - New quota refill state is: Known`:
```
2026-06-05 09:03:28,456 [...]   INFO - #c.i.m.l.c.q.QuotaManager2Impl - New quota refill state is: Known(next=2026-06-13T14:00:00.008Z, tariff=QuotaRefillInfoTariff(amount=25000000, duration=30d))
```
Extract: `next` refill date (ISO date, discard time).

### Calculation

- `used = current` (log stores *consumed* tokens, monotonically increasing)
- `percent = current * 100 / maximum` (integer, truncated)
- `days_until_refill = refill_date - today`

### Refresh

- Auto-refresh every **10 minutes 30 seconds** via a background timer (aligned to IDE's ~10-min quota polling + 30s delay)
- Manual refresh via clicking the widget or an action

### Display

**Status bar widget (compact):** Shows percentage and a mini visual indicator.
```
AI: 72%
```

**Popup (on click):** Monospaced, selectable text panel:
```
[####################--------] 72%
Used:       18,143,812 / 25,000,000
Refill:     2026-06-13 (8 days)
Updated:    09:03 (2h ago)
Since last: +312,400
```

- Progress bar: 28 chars wide, `#` = filled, `-` = unfilled
- Numbers formatted with thousand separators (`,`)
- "Since last" shows token delta since previous refresh (`+` prefix); omitted on first run
- Refill shows ISO date + relative days
- "Updated" shows log timestamp as `HH:mm` + relative time (`Xm ago` / `Xh ago` / `Xd ago`)
- All text is selectable (uses `JTextArea` instead of `JLabel`)
- If no quota data found: widget shows `AI: --` and popup shows "No quota data found"

### Color coding (status bar text)

- `< 50%` used: default text color (no highlight)
- `50-79%` used: yellow/warning
- `>= 80%` used: red/error

## Tech Stack

- **Language:** Kotlin
- **Build:** Gradle with [IntelliJ Platform Gradle Plugin](https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html) (`org.jetbrains.intellij.platform`)
- **Min IDE version:** 2024.2 (broad compatibility)
- **JDK:** 17+

## Project Structure

```
jetbrains-ai-quota-plugin/
  build.gradle.kts
  settings.gradle.kts
  gradle.properties
  gradle/
    wrapper/
      gradle-wrapper.jar
      gradle-wrapper.properties
  src/
    main/
      kotlin/
        com/github/pihofmann/aiquota/
          QuotaData.kt              # data classes: QuotaInfo, RefillInfo
          QuotaLogParser.kt         # parses idea.log, returns QuotaData
          QuotaService.kt           # project service: holds state, schedules refresh
          QuotaStatusBarWidget.kt   # status bar widget factory + widget impl
          QuotaPopup.kt             # popup panel shown on click
      resources/
        META-INF/
          plugin.xml                # plugin descriptor
    test/
      kotlin/
        com/github/pihofmann/aiquota/
          QuotaLogParserTest.kt     # unit tests for log parsing
  jb-quota                          # existing CLI script (kept as-is)
  SPEC.md                           # CLI spec (kept as-is)
  PLUGIN_SPEC.md                    # this file
```

## Code Style

- Idiomatic Kotlin (data classes, extension functions, null safety)
- No external dependencies — only IntelliJ Platform SDK
- All file I/O on background thread (never block EDT)
- Use `com.intellij.openapi.diagnostic.Logger` for plugin logging
- Small, focused classes — one responsibility each

## Testing Strategy

- **Unit tests** for `QuotaLogParser` with sample log lines (covers parsing, edge cases, malformed input)
- **Manual testing** for widget rendering and popup display in IDE

## Components Detail

### `QuotaData.kt`
```kotlin
data class QuotaInfo(
    val current: Long,      // consumed quota (monotonically increasing)
    val maximum: Long,      // total quota
    val timestamp: String   // log timestamp "2026-06-05 09:03:28"
)

data class RefillInfo(
    val date: LocalDate     // next refill date
)

data class QuotaState(
    val quota: QuotaInfo?,
    val refill: RefillInfo?,
    val error: String?      // set if parsing failed
)
```

### `QuotaLogParser.kt`
- `fun parse(logFile: Path): QuotaState`
- Reads file line-by-line from end (reverse scan for efficiency on large logs)
- Regex for quota: `current=(\d+(?:\.\d+)?),\s*maximum=(\d+)`
- Regex for refill: `next=(\d{4}-\d{2}-\d{2})T`
- Returns as soon as both matches found (no need to read entire file)

### `QuotaService.kt`
- Application-level service (singleton across projects)
- Holds current `QuotaState`
- Schedules refresh every 10 min 30s via `com.intellij.util.Alarm`
- Tracks `previousQuota` (token count before last refresh) for delta calculation
- Exposes `fun refresh()` for manual trigger
- Notifies listeners (widgets) on state change

### `QuotaStatusBarWidget.kt`
- Implements `StatusBarWidget.TextPresentation`
- Registers via `StatusBarWidgetFactory`
- Compact text: `AI: 72%` or `AI: --`
- Click opens `QuotaPopup`
- Text color based on usage threshold

### `QuotaPopup.kt`
- `JBPopup` with monospaced, selectable `JTextArea`
- Renders the 5-line detail view (bar, used, since last, refill, updated)
- Shows on widget click

## Boundaries

### Always
- Read-only: never modify log files or IDE state
- All I/O off EDT
- Graceful fallback when no quota data exists
- Work on macOS, Linux, and Windows

### Never
- Write to any JetBrains/IDE directory
- Make network requests
- Access private/internal IntelliJ APIs (only public SDK)

### Ask first
- Notifications/alerts when quota exceeds threshold
- Settings panel for refresh interval
- Publishing to JetBrains Marketplace
