# Perde - Progress Log

Every Codex task MUST update this file before finishing (in the same PR).
This is the handoff point between Codex sessions and accounts: the next session starts from here.

## Current state
- Phase: 1B (feasibility, GUI spike verified)
- Last merged task: F0-3 Fabric mod scaffold ([PR #1](https://github.com/devtalh/perde/pull/1), main `9712e2a`)
- Last completed task: F1B-1 MCEF dependency + GUI smoke test (branch `spike/f1b-1-mcef-gui`, pending merge)
- Next task: F1B-2 world quad rendering (separate task, not started)
- Confirmed Minecraft version: 26.2 (Fabric API 0.161.0+26.2, MCEF 26.2 build)

## Task log
Newest first. Format:

```
### <task id> - <title>
- Branch / PR:
- Status: done | partial (session ended) | blocked
- What changed:
- How it was verified (commands + result):
- Not done / left for next task:
- Known issues / risks:
```

### F1B-1 - MCEF Modern client-only dependency + GUI smoke test
- Branch / PR: [`spike/f1b-1-mcef-gui` targeting `main`](https://github.com/devtalh/perde/compare/main...spike/f1b-1-mcef-gui)
- Status: done (game interaction and audio confirmed by Talha after taking over input; pending PR review/merge)
- What changed: pinned MCEF `0.3.3+mc26.2.jcef146.0.10` as a client dependency, verified from Maven metadata, the published sources and upstream commit df61843. Added optional `suggests` metadata, a missing-MCEF warning/disabled command, asynchronous initialization, and dev-only `/perde-spike gui [url]`. The disposable spike Screen draws the live 26.2 texture view, forwards input/cursors, requests 30 fps and closes the browser in `removed()`. Added [feasibility notes](feasibility/F1B-notes.md) and [F2 YouTube evidence](feasibility/screenshots/f1b-1-youtube.png). Main Java code, SPEC and AGENTS are unchanged.
- How it was verified (2026-10-30, Europe/Istanbul):
  - F0-3 merge verified with `gh pr list --state all --head chore/f0-3-scaffold` and matching local/remote main `9712e2a`. JDK/javac 25.0.4 available.
  - `.\gradlew.bat build --console=plain --warning-mode all`: passed (first build 20s, final build 9s; `test NO-SOURCE`). No Gradle warnings in the final build.
  - `.\gradlew.bat runServer --console=plain`: common init at 16:41:45, `Done (0.421s)!` at 16:41:48, console `stop` at 16:42:10, `BUILD SUCCESSFUL in 37s`. MCEF/JCEF absent from the loaded mods, server log and `runtimeClasspath` dependency report. Server remains repo-local and bound to 127.0.0.1.
  - `rg -n '^import .*mcef|^import org\.cef' src/main src/client`: three matches, all in `src/client/.../spike` (Screen lines 7-8, client bridge line 4). No common MCEF imports or references.
  - `.\gradlew.bat runClient --console=plain`: native first-time install stayed in `run/config/mcef-modern/`; async init completed at 16:43:35. Codex entered the local creative world, submitted the default command and observed YouTube playing. Talha then tested and confirmed playback with sound, clicks, scroll, typing and Esc. Browser creation at 16:43:47 (1280x720, 30 fps requested), closure independently logged at 16:45:09. No crash observed. Computer Use stopped on Talha's physical Esc; game input remained with him.
  - F2 `run/screenshots/2026-10-30_16.44.08.png` copied byte-for-byte to `docs/feasibility/screenshots/f1b-1-youtube.png`; matching SHA-256 `f9aa0bd58439ecb72e62d037a57264aa9eb908a0f30dd5f248ced725381fe559`. Loading screenshot not available because initialization finished before the command opened the Screen.
- Not done / left for next task: F1B-2 world quad rendering; F1B-3 Sodium/Iris checks and overall GO/NO-GO decision. Missing-MCEF client and failure/loading UI were source-reviewed, not separately exercised at runtime. No world/block/network/config changes.
- Known issues / risks: four bare AWT exception prefixes appeared in Gradle stdout without details/stack traces or observed functional failure; cause unresolved. Upstream JCEF SemVer, Chromium registry/media/preload warnings and existing OSHI/dev-auth errors remain. Upstream enables autoplay and `--disable-web-security`; no browser hardening is claimed. Details and evidence in the feasibility notes.

### F0-3 - Fabric mod scaffold
- Branch / PR: [`chore/f0-3-scaffold` targeting `main`](https://github.com/devtalh/perde/compare/main...chore/f0-3-scaffold)
- Status: done
- What changed: added the Fabric 26.2 Gradle scaffold, Java 25 toolchain, split common/client source sets, logging-only entrypoints, English/Turkish placeholder translations under client resources, build CI, README, MIT license, ignore rules and line-ending rules. Added the title-screen F2 proof at `docs/f0-3/title-screen.png`. No gameplay or MCEF dependency.
- Version evidence: Fabric API `0.161.0+26.2` POM returned HTTP 200 from [Fabric Maven](https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/0.161.0+26.2/fabric-api-0.161.0+26.2.pom). Loom `1.17-SNAPSHOT` and Gradle `9.5.1` match [MCEF Modern commit df61843](https://github.com/DimasKama/mcef-modern/tree/df61843); [Fabric's 26.2 example](https://github.com/FabricMC/fabric-example-mod/tree/26.2) confirms the plugin, `implementation` dependencies and split source sets. Loader compilation version and runtime minimum are both `0.19.5`.
- How it was verified (commands + result):
  - `java -version` and `javac -version`: Temurin JDK 25.0.4. `git pull`: clean main fast-forwarded to the docs bootstrap.
  - Gradle wrapper jar SHA-256 matched the official checksum; distribution SHA-256 is pinned. `GRADLE_USER_HOME` was set to the repo-local `.gradle-user-home/` for every Gradle command.
  - `.\gradlew.bat build --console=plain`: passed in 2m 2s. A license-renaming closure's `Task.project` deprecation was fixed. After moving language assets into client resources, the final `.\gradlew.bat build --console=plain --warning-mode all` passed in 10s without warnings. There are no unit test sources yet (`test NO-SOURCE`).
  - Jar inspection: both entrypoints, expanded metadata, both lang files and MIT license are present. `javap -verbose` confirms Java 25 bytecode (major version 69).
  - `.\gradlew.bat runServer --console=plain`: passed, with `run/eula.txt` set to `eula=true` and the test server bound to `127.0.0.1:25565`. Exactly one common init log at 16:17:08, no client init log, `Done (2.415s)!` at 16:17:13, then console `stop` at 16:17:29; Gradle exited successfully. All times are 2026-10-30 Europe/Istanbul.
  - `.\gradlew.bat runClient --console=plain`: passed after the client resource move. Both init lines appear exactly once at 16:25:23; no missing-client-resources classpath warning remains. Reached the Minecraft 26.2 title screen. At Talha's request, he handled game-window input while Codex ran commands and checked logs/files. Talha also entered a local world, returned to the title screen, pressed F2 and used Quit Game; `Stopping!` appears at 16:26:24 and Gradle exited successfully (`BUILD SUCCESSFUL in 1m 26s`).
  - F2 source: `run/screenshots/2026-10-30_16.26.23.png`. Copied byte-for-byte to [`f0-3/title-screen.png`](f0-3/title-screen.png) and visually confirmed the title screen. Matching SHA-256: `df374af50ec811f14419505ae9954ecd129f7226255999c7d8c2eb56255436fa`.
  - `git diff --cached --check`: passed. `AGENTS.md` and `docs/SPEC.md` are unchanged; game data and Gradle caches are gitignored. `gradlew` is executable in Git; shell scripts use LF and batch scripts CRLF.
- Not done / left for next task: F1B-1 MCEF dependency + GUI smoke test. MCEF, blocks, rendering, packets and all gameplay remain unimplemented as intended. This task's build, server boot and client boot checks have passed; merge remains Talha's review step.
- Known issues / risks: Loom uses the upstream reference's mutable `1.17-SNAPSHOT` (resolved to 1.17.21 during these checks). Minecraft's OSHI library logs Windows performance-counter registry warnings/errors on this PC. The client dev run also logs Realms JWT/profile-key authentication failures. These did not prevent the title screen, local world or normal shutdown; no Perde exception was logged. No system/account settings were changed.

### F0-2 - Docs bootstrap
- Branch / PR: committed directly to main by Claude (lead)
- Status: done
- What changed: added AGENTS.md, docs/SPEC.md, docs/PROGRESS.md
- How it was verified: n/a (docs only)
- Not done / left for next task: everything else
- Known issues / risks: none

## Known issues
- Dev smoke-test logs include Windows OSHI performance-counter errors and client Realms/profile-key authentication failures. Boot/shutdown checks pass; see F0-3 above.
- Loom `1.17-SNAPSHOT` follows the verified 26.2 upstream reference but is mutable.

## Decisions log (short; full rationale lives in Notion)
- 2026-10-30: Codex accounts are used sequentially, never in parallel. Order: Phase 0, then 1B (feasibility), then 1A only if 1B succeeds.
- 2026-10-30: Radio feature added as Phase 7 (after screen phases). Command root `/radyo`. Separate jar `perde-radyo`.
- 2026-10-30: Target Minecraft 26.2 confirmed. Repo public, license MIT, recipe confirmed.
- 2026-10-30: Codex runs locally on Talha's Windows PC, tests the game itself in Gradle `run/` only, attaches screenshots.
