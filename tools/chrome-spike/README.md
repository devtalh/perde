# Chrome CDP measurement spike

Measurement only. Java 25, no new Java dependencies. Nothing here is packaged into Perde. All generated data belongs under the ignored `run/chrome-spike/`; only selected sanitized evidence is exported to `docs/spike-chrome/`.

Read the repository AGENTS/SPEC/PROGRESS and the research report first. Never use a real browser profile, activate a window, send desktop input, operate an unrelated process, install software, or kill processes by image name. The launcher uses a fresh profile, loopback-only debugging (or inherited pipes), `SW_SHOWNOACTIVATE`, off-screen coordinates, and muted audio by default. The controller requires a random header token stored only in its ignored connection file. CDP Input events act inside the owned browser, not on the Windows desktop.

Start the controller from the repository root in one PowerShell terminal:

```powershell
.\tools\chrome-spike\run-controller.ps1
```

The optional STB comparison reuses the Minecraft-resolved LWJGL 3.4.1 jars already in the repository-local Gradle cache. The base jar has the `unsafe` classifier in this resolved Minecraft version. If absent, ImageIO still works and STB is reported as unavailable (-1). No dependency download or Gradle product change is added. Native extraction and temporary files stay in `run/chrome-spike/tmp/`.

In another PowerShell terminal, create a fresh owned browser:

```powershell
. .\tools\chrome-spike\common.ps1
$installed = @(
    "${env:ProgramFiles}\Google\Chrome\Application\chrome.exe",
    "${env:LOCALAPPDATA}\Google\Chrome\Application\chrome.exe"
) | Where-Object { Test-Path -LiteralPath $_ } | Select-Object -First 1
C @{ op='start'; id='example-e1'; executable=$installed; mode='headless' }
```

Use `mode='offscreen'` for the normal no-activation window. `unsafe=$true; pipe=$true` enables the experimental Extensions domain. The Windows helper relays inherited CDP pipe bytes to a one-client loopback socket; Java still performs all protocol commands and JSON handling. This helper is test infrastructure, not a proposed product dependency. `extension='run/chrome-spike/ubol'` tests the launch flag separately from `Extensions.loadUnpacked`.

Supported controller operations: `start`, `cdp`, `eval`, `contexts`, `snapshot`, `stream` (1..60 seconds), `crash` (owned root only), `stop` (owned tree only). Run `stop` for every ID before terminating the controller. A crash test snapshots descendants before killing the root and follows with `stop` to collect surviving helpers. The original PID sets and cleanup results are retained.

Useful measurement scripts:

- `sites.ps1`: navigate, inspect all current native frame contexts, use visible native play/ad-close controls with CDP Input, preserve before/after media state and streamed PNG evidence. Gate controls are never clicked. An ad or a returned history entry is not episode success.
- `youtube720.ps1`: waits for normal pre-roll completion, selects the content video's native 720p menu entry, and checks decoded dimensions. Never count a preroll as the requested video.
- `soak.ps1`: retains thirteen observations over at least sixty seconds. Read its pass/fail and raw observations before interpreting a memory point as playing video.
- `memory-case.ps1`: sequential fresh-profile stages, ten process samples per stage, actual media observations, active screencast/both decoders during the YouTube memory point, and paired same-URL extension tests. Failed episodes are reported separately. Run cases sequentially for memory isolation.
- `memory.ps1`: all currently alive owned descendants, working set and private bytes, process roles, Java and the optional bridge separately. Values include shared resident pages, so a sum of working sets is not unique physical RAM.
- `benchmark.ps1`: a controlled W3C Sintel H.264/AAC fixture at 1280x720 JPEG Q80 and 854x480 Q55; no third-party player is modified to force its media source.
- `control.ps1`: actual click/type/scroll readback assertions, cross-site OOPIF evaluation/bindings, permission denials, Fetch block, optional popup close. Uninstall the spike's uBOL before the Fetch test: an extension redirect can legitimately resolve the original ad fetch before it matches Fetch interception.
- `audio.ps1`: read-only Core Audio session meters for owned PIDs. The task permits only one two-second 0.05 gain test per engine; the original run already used both allowances. Do not repeat a tone casually. New launches are globally muted, and the tone script is not part of the automatic matrix.

Build validation uses the repository-local cache:

```powershell
$env:GRADLE_USER_HOME = Join-Path (Get-Location) '.gradle-user-home'
.\gradlew.bat build --rerun-tasks --console=plain
```

No in-game step is required for this measurement-only branch. The existing renderer/backend is unchanged, and no Minecraft instance is launched.
