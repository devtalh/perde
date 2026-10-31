# Optional F2-1 integration fixture

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

The queue script refuses input unless exactly one live repository DLI process has the F2 fixture and Player1 identity. Do not launch multiple Player1 fixture clients. Commands are processed on the client thread; wait for each action's log before sending a dependent action. `load` accepts an existing world folder name, never an external path. The fixture assumes `pauseOnLostFocus:false` in the repository dev options if the game window is unfocused.

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
