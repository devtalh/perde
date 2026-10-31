# Optional Phase 2 integration fixture

This fixture is never part of normal builds, jars or runtime. It invokes normal client/world/panel/F2 APIs without activating Minecraft windows. It observes JCEF's own registry through test-only reflection, which is intentionally absent from product code.

From the repository in PowerShell:

```powershell
$env:GRADLE_USER_HOME = Join-Path $PWD '.gradle-user-home'
.\gradlew.bat -I docs/phase2/fixture/probe.init.gradle runClient --console=plain
```

For an actual missing-MCEF runtime (compilation still uses the declared client API):

```powershell
.\gradlew.bat -I docs/phase2/fixture/probe.init.gradle '-Pperde.testWithoutMcef' runClient --console=plain
```

The queue script refuses input unless exactly one live repository DLI process has the F2 fixture and selected player identity (Player1 by default). Do not launch duplicate fixture clients for the same identity. Commands are processed on the client thread; wait for each action's observed result before sending a dependent action. Only one orchestration script may write a client's queue at a time. `load` accepts an existing world folder name, never an external path. The fixture assumes `pauseOnLostFocus:false` in the repository dev options if the game window is unfocused.

```powershell
.\docs\phase2\fixture\Send-Probe.ps1 -Action load -Values 'New World'
.\docs\phase2\fixture\Send-Probe.ps1 -Action command -Values 'gamemode creative'
.\docs\phase2\fixture\Send-Probe.ps1 -Action open -Values @('100','120','100')
.\docs\phase2\fixture\Send-Probe.ps1 -Action panel-url -Values 'https://example.com'
.\docs\phase2\fixture\Send-Probe.ps1 -Action close
.\docs\phase2\fixture\Send-Probe.ps1 -Action walk -Values @('-90','72')
.\docs\phase2\fixture\Send-Probe.ps1 -Action status -Values @('100','120','100')
.\docs\phase2\fixture\Send-Probe.ps1 -Action f2
.\docs\phase2\fixture\Measure-Browsers.ps1 -Stage 'near-screen'
.\docs\phase2\fixture\Send-Probe.ps1 -Action leave
```

`panel-url` uses the real panel EditBox/Open Button. `equip`/`place` use the creative inventory and normal block-use packet; `break` requires creative and ordinary reach. Geometry still uses permission-checked C2S packets. `aim` takes a world x/y/z target. `walk` takes yaw/ticks and presses/releases normal forward movement. `quit` stops only the identified fixture client. F2 calls the vanilla bound screenshot handler and writes ordinary PNGs to `run/screenshots/`.

Memory sampling writes `run/phase2-verification/memory.csv`; create that directory before first use. Each stage appends ten samples. Only the identified client's repository-native helpers are counted. Retained shared Chromium processes can outlive individual browsers; pair these samples with the fixture's native-browser registry logs.

## F2-2 security probes and native observations

Talha authorized the resumed client checks; (a)-(e) and the final allowlist follow-up are complete. The historical WebSocket canary demonstrated a native callback bypass. The explicit lead decision accepts an operator-managed trusted-site boundary; these helpers do not prove network isolation. Do not repeat the local canary. See the F2-2 report for residual risk and current acceptance.

Use one managed Perde screen showing `https://example.com`, close the panel, and issue each probe separately. `policy` refuses non-development environments and non-public starting pages. It selects the manager's own browser by identity; it cannot target another mod. The native audit reads every live client in JCEF's actual registry, including a private client still closing after a selection change. The feasibility spike and its commands were removed in F2-3; `docs/feasibility/` remains historical evidence.

```powershell
.\docs\phase2\fixture\Send-Probe.ps1 -Action policy -Values file
.\docs\phase2\fixture\Send-Probe.ps1 -Action policy -Values popup
.\docs\phase2\fixture\Send-Probe.ps1 -Action policy -Values loopback
.\docs\phase2\fixture\Send-Probe.ps1 -Action policy -Values iframe
.\docs\phase2\fixture\Send-Probe.ps1 -Action policy -Values alert
.\docs\phase2\fixture\Send-Probe.ps1 -Action policy -Values media
.\docs\phase2\fixture\Send-Probe.ps1 -Action policy -Values download
.\docs\phase2\fixture\Send-Probe.ps1 -Action policy -Values data-navigation
```

Inspect `Perde browser blocked [...]` entries after each action. `issued probe` only proves dispatch, not a native denial. Verify the native URL, continued rendering, absent windows/dialogs/downloads, media rejection, and unchanged screen state. `beforeunload` is an additional probe which deliberately navigates to another public page. An actual public HTTP redirect response is also needed for acceptance (d); assigning `location` alone does not prove redirect handling. Permission-default entries record installed native defaults, not per-attempt permission callbacks, which this JCEF version does not expose. Keep that distinction in the evidence report.

`evaluate` accepts a label and a JavaScript expression, runs native Runtime.evaluate with awaitPromise/returnByValue/userGesture, and logs the actual result or a ten-second failure. Wait for `F2-2 native result <label>`, not merely the dispatch acknowledgement. `devtools` is an opt-in diagnostic command; Browser/Target context-inspection commands timed out in this JCEF and are not used by production security code.

`raw-url` accepts x/y/z/url and sends the normal SetUrl payload. Open the real panel first to establish the normal server permission context. This tests server rejection independently from the panel's correct local rejection; it does not bypass authorization. `status` now records the actual Hud action-bar component through test-only reflection verified from Minecraft 26.2 source. No fixture class is packaged by a normal build.

## Follow-up fixture controls and silent testing

The opt-in init script sets perde.testMuted=true. On the first tick, the fixture saves only this repository test client's Minecraft master volume as 0. In F2-3 a fixture-only Mixin appends Chromium's `--mute-audio` argument at the pinned MCEF builder call, before the engine or any browser is created. The Mixin is skipped when MCEF is absent. The log must contain `Test Chromium audio: --mute-audio`; helper command lines provide independent verification. This only affects the test JVM and is never bundled in Perde. Media probes additionally set video volume=0/muted=true. Earlier F2-2 used Page.enable + Page.addScriptToEvaluateOnNewDocument on a quiet page before video navigation; that historical method is retained in its report.

`connect` takes 127.0.0.1:port and uses normal ConnectScreen; it refuses non-loopback hosts and an already joined world. `chat-audit` inspects actual chat components and logs RunCommand values. Example:

```powershell
.\docs\phase2\fixture\Send-Probe.ps1 -Action connect -Values '127.0.0.1:25575'
.\docs\phase2\fixture\Send-Probe.ps1 -Action chat-audit
```

Allow example.com with the real operator command before the managed-screen examples above. Until the S2C list arrives, no public managed main page may load. Never treat a queued command as an observed result.

## Two focus-free clients

The init script supports both `runClient` (Player1, `run/`) and `runClient2` (Player2, `run/client2/`). Launch separately without switching window focus. Each has its own command queue, logs, screenshots, MCEF cache and native processes. For Player2:

```powershell
.\gradlew.bat -I docs/phase2/fixture/probe.init.gradle runClient2 --console=plain
.\docs\phase2\fixture\Send-Probe.ps1 -Player Player2 -Action connect -Values '127.0.0.1:25570'
.\docs\phase2\fixture\Send-Probe.ps1 -Player Player2 -Action status -Values @('300','120','300')
.\docs\phase2\fixture\Send-Probe.ps1 -Player Player2 -Action f2
```

Use the real server console for positioning Player2. Check both actual native page results and saved screen state for URL/allowlist changes. This fixture does not synchronize video playback.


## F3-1 playback checks

The same opt-in fixture now supports `panel-playback` with a localized key suffix (`play`, `pause`, `back_30`, `back_10`, `forward_10`, `forward_30`, `go`). `go` takes a second value such as `1:02:03`. It edits the actual second EditBox and presses the real Go Button. `panel-audit` logs widget text, active state and bounds. `raw-playback` takes x/y/z, `play|pause|absolute|relative`, and an optional numeric value to exercise genuine server validation independently from disabled controls. `clock-flood` sends ten otherwise harmless TimeRequests with unmatched echo tokens; responses cannot alter the production estimator.

`playback-status` takes x/y/z and a common reference local wall-clock epoch in ms. It records the received state, the production estimator's current server time, current media position, RTT/offset and position projected to that common instant. The latter allows two sequential fixture snapshots on the same PC to be compared without mistaking orchestration delay for clock skew. This is observation only, with no mutation of the estimator or playback. Browser/video playback is still untouched by F3-1 controls.
