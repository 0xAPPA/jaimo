# Jaimo — JetBrains AI (Quota Usage) Monitoring

Displays your JetBrains AI quota usage directly in the IDE status bar.
Works with any JetBrains IDE (IntelliJ, WebStorm, GoLand, PyCharm, etc.).

![Jaimo screenshot](screenshot.png)

## Features

- **Status bar widget** — compact `AI: 30%` display with color coding
- **Detail popup** — click for full breakdown: used/max tokens, progress bar, refill date
- **Auto-refresh** — updates every 5 minutes, click to refresh manually
- **Color coding** — default < 50%, yellow 50–79%, red ≥ 80%
- **Zero network** — reads only the local IDE log file

## Install

### From distribution zip

1. Download `jaimo-0.1.0.zip` from the [`dist/`](dist/) directory
2. In your IDE: **Settings → Plugins → ⚙️ → Install Plugin from Disk…**
3. Select the zip file and restart the IDE

## Build

Requires JDK 17+.

```bash
./gradlew buildPlugin
```

The distribution zip will be generated at `build/distributions/jaimo-<version>.zip`.

To run in a sandboxed IDE for testing:

```bash
./gradlew runIde
```

## How it works

Parses the current IDE's `idea.log` (via `PathManager.getLogPath()`) for `QuotaManager2Impl` entries to extract consumed/maximum tokens and next refill date. All I/O runs on a background thread — never blocks the UI.

## Requirements

- JetBrains IDE **2024.2** or newer
- JetBrains AI Assistant enabled (so quota log entries exist)
