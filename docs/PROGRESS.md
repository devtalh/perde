# Perde - Progress Log

Every Codex task MUST update this file before finishing (in the same PR).
This is the handoff point between Codex sessions and accounts: the next session starts from here.

## Current state
- Phase 1B complete, awaiting GO/NO-GO decision by Talha and Claude
- Last merged task: F1B-2 world rendering and disconnect cleanup, [PR #3](https://github.com/devtalh/perde/pull/3), main `7ac575a` (verified with git log and gh).
- Current task: F1B-3 verified on `spike/f1b-3-compat`, [PR #4](https://github.com/devtalh/perde/pull/4). All four configurations and default build/server checked; recommendation GO-with-conditions.
- Next task: await Talha and Claude's decision. No later phase started.
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

### F1B-3 - Render-mod compatibility and Phase 1B report
- Branch / PR: `spike/f1b-3-compat`, [PR #4](https://github.com/devtalh/perde/pull/4). Checkpoint commits pushed with passing CI; completion evidence follows on the same branch.
- Status: done (feasibility verification complete; final GO/NO-GO decision belongs to Talha and Claude)
- What changed: opt-in `perde.compat=sodium|iris|full` development profiles using Loom 26.x `localRuntime`, restricted Modrinth Maven and immutable version IDs for all eight exact pack jars. Full adds MoreCulling's required Cloth Config 26.2.155. No compat dependencies with the property omitted; no metadata, common code, SPEC or AGENTS changes.
- How it was verified (2026-10-30, Europe/Istanbul): clean main at merged `7ac575a`, JDK/javac 25.0.4; Notion hub, Linear F1B task and LEA-185 read. Exact Modrinth API file/version/game-version checks passed for all eight jars. Full localRuntime resolution passed; all nine resolved compat/Cloth Config jars match upstream SHA-512. Initial default build passed in 3s; handoff build passed in 9s (7 tasks up-to-date, test NO-SOURCE). Default runServer loaded 44 mods without MCEF/compat, bound 127.0.0.1:25565, reached `Done (0.431s)!` at 18:29:56, stopped via console 18:30:25, `BUILD SUCCESSFUL in 47s`. Default runtimeClasspath and jar-entry searches found no compat/MCEF/JCEF artifacts. First commit `583e431` pushed, GitHub CI passed. Complementary Reimagined r5.9.3 stays only in ignored `run/shaderpacks/`.
  - Initial checkpoint: same creative world/default YouTube URL tested in Sodium and Iris shaders OFF; all render/media checks passed. Shader ON render/pause/seek passed before interruption. F2 originals copied unchanged with provenance/hash CSV. Completion evidence below closes the interrupted checks; see [report](feasibility/F1B-report.md).
  - Vanilla F3 snapshots, no screen toward/away: 381/552 FPS; one playing screen: 427/397 FPS. Ten-sample Get-Process working-set means (MiB), no screen Java/JCEF: 1741.91/98.55; one screen: 1810.81/727.16. Rough single snapshots, not a benchmark; exact method/limitations and raw CSV in report.
  - Physical Escape stopped Computer Use at approximately 18:55. No UI call or game input followed until Talha explicitly resumed work. Read-only logs show disconnect at 18:55:40 and quit at 18:55:42. The initial full launch reached only the title screen, then quit normally at 18:58:04; it was not counted as world verification.
- Completion verification (23:10-23:46): Talha resumed work while keeping his server-connected Minecraft open. Every Computer Use target excluded that window; only separate Gradle clients were controlled/closed. Resumed Iris shader-on pause/play passed with changing-frame proof. Full loaded 73 mods, default Complementary HIGH, active DH renderer, then passed live page, orientation, stone occlusion, night, back face, pause, seek 60 visible jump and resumed play. No renderer change needed. Full FPS no screen toward/away 132/98; one screen 114/116. Full working-set means no screen Java/JCEF 3208.00/98.93 MiB (3 helpers); one screen 3925.47/754.27 MiB (7 helpers). Report explains background AFK game, LOD work, elapsed time and autoplay confounders. Total evidence: 31 unchanged F2 copies with hash provenance, 40 raw memory samples.
  - Default completion build passed in 2s, 7 tasks up-to-date, test NO-SOURCE. Default runtimeClasspath rechecked, no compat/MCEF/JCEF; jar and metadata remain isolated. Final agents-free default server: 44 mods, 127.0.0.1, `Done (0.324s)!` at 23:44:50, console stop 23:46:34, all dimensions saved, `BUILD SUCCESSFUL in 1m 58s`. An earlier extra server run had closed stdin; repo-only diagnostic guards/normal shutdown-hook recovery are documented in the report, followed by this independent console-stop test.
- Not done / left: final decision/review by Talha and Claude. No Phase 1A/2 work. All requested F1B-3 game/build/server checks are verified. Raw logs under `build/f1b-3-verification/`; scene fixture, downloads and temporary diagnostics remain under ignored `run/` only. The earlier 18:58 title-only launch is not counted as the full world test.
- Known issues / risks: ImmediatelyFast SPEC mismatch preserved without editing SPEC. Shader tint/contrast remains readable but requires acceptance. Future oversized block-entity culling bounds/opt-outs need Phase 2 testing. AWT prefixes remain unexplained despite console/default/direct-handler/JVM tracing attempts; resumed Iris and full world exit each emitted three prefixes, no stack captured. Full also logged a DH regen-queue closed SQLite statement during shutdown, with stack retained in [diagnostics](feasibility/f1b-3-diagnostics.md); worlds saved and Gradle exited normally, but no harmlessness/data-integrity claim is made. LEA-185 browser security, per-client ads/autoplay and existing OSHI/dev-auth/Chromium diagnostics remain. [Final report](feasibility/F1B-report.md) recommends GO-with-conditions and lists decision conditions. Linear shared field-notes tool returned `Tool get_agent_context not found`; normal issue tools worked.

### F1B-2 - Fixed MCEF world quad and JavaScript media controls
- Branch / PR: `spike/f1b-2-world-quad`, [PR #3](https://github.com/devtalh/perde/pull/3) targeting `main`; implementation/evidence commit `48f7b51` pushed, completion verification recorded in this follow-up.
- Status: done (pending PR review/merge)
- What changed: development-only place/remove/play/pause/seek commands, one fixed 4 x 2.25 block screen and one 1280x720 browser. Uses 26.2's vanilla map rendering path (`COLLECT_SUBMITS`, `submitCustomGeometry`, `RenderTypes.text`) with a live, non-owning texture bridge, full-bright light coordinates, depth testing and back-face culling. All implementation changes remain in the client spike package. Added source research, F2 evidence, audio output measurements and a reproducible local media fixture. No common code, SPEC, AGENTS, dependency or world-data changes.
- How it was verified (2026-10-30, Europe/Istanbul):
  - Prerequisite: `gh pr view 2` confirmed MERGED at `77f3596`; clean local main matched origin. JDK/javac 25.0.4 available. Notion project hub and Linear LEA-182 read before implementation.
  - `.\gradlew.bat genSources --console=plain`: passed in 1m 1s; exact Minecraft/Fabric/MCEF APIs inspected.
  - `.\gradlew.bat build --console=plain --warning-mode all`: initial corrected code passed in 4s; completion pass ran the build again successfully in 9s, 7 actionable tasks (7 up-to-date), `test NO-SOURCE`.
  - `.\gradlew.bat runServer --console=plain`: 44 mods, MCEF/JCEF absent, bound to 127.0.0.1; `Done (0.348s)!` at 17:02:04, console stop at 17:02:28, `BUILD SUCCESSFUL in 39s`.
  - Main/common reference search for client Minecraft, Blaze3D, MCEF/JCEF and spike references: no matches. Exact command/output are in the feasibility notes.
  - `.\gradlew.bat runClient --console=plain`: creative world under `run/saves/`; default YouTube page live and correctly oriented. F2 evidence: [day](feasibility/screenshots/f1b-2-day.png), [occlusion](feasibility/screenshots/f1b-2-occlusion.png), [night](feasibility/screenshots/f1b-2-night.png), [back](feasibility/screenshots/f1b-2-back.png). Originals remain in `run/screenshots/`; copied without editing.
  - JavaScript pause stopped the visible video and JCEF audio output (0/19 nonzero peak samples); play resumed video and audio (19/19 nonzero). Local readonly media fixture confirmed exactly `time=60.00`, paused, with an advancing page heartbeat: [F2 seek evidence](feasibility/screenshots/f1b-2-seek-60.png). No-video play/pause/seek caused no observed error or crash. Browser replacement closed the old browser before creation. Remove visibly removed the quad and logged `world browser closed (removed)` at 17:17:55.
  - First startup exposed premature texture registration; fixed by deferring construction/registration to `CLIENT_STARTED`, then rendering tests passed. First disconnect exposed off-thread GPU cleanup; callback now schedules on the client thread and tick cleanup checks all active/pending state. Final build passed after that fix.
  - Corrected disconnect/rejoin test run by Talha, then audited by Codex in `run/logs/latest.log` and `debug.log`: browser created at 18:12:16; `[18:12:33] [Render thread/INFO] (perde) Perde spike: world browser closed (disconnect).`; rejoined at 18:12:41; no new browser creation after rejoin; left at 18:13:05. Talha explicitly confirmed that no spike screen remained after rejoining. No Perde exception or new crash report. Codex sent no game input during this completion pass and changed no code. Exact log lines are in the feasibility notes; the read-only/manual-test path did not require a new F2 screenshot.
- Not done / left for next task: no remaining F1B-2 checks. Review/merge PR #3, then F1B-3 Sodium + Iris test and feasibility report. No F1B-3 work started.
- Known issues / risks: Sodium/Iris deliberately not installed or tested. Vanilla world fog/lightmap color effects remain. Browser security/autoplay and OSHI/dev-auth/Chromium diagnostics from F1B-1 remain. Audio was verified through the JCEF Windows render-session meter, not physical headphone listening. Details and provenance are in [F1B notes](feasibility/F1B-notes.md).

### F1B-1 - MCEF Modern client-only dependency + GUI smoke test
- Branch / PR: `spike/f1b-1-mcef-gui`, [PR #2](https://github.com/devtalh/perde/pull/2) targeting `main`
- Status: done (game interaction and audio confirmed by Talha after taking over input; pending PR review/merge)
- What changed: pinned MCEF `0.3.3+mc26.2.jcef146.0.10` as a client dependency, verified from Maven metadata, the published sources and upstream commit df61843. Added optional `suggests` metadata, a missing-MCEF warning/disabled command, asynchronous initialization, and dev-only `/perde-spike gui [url]`. The disposable spike Screen draws the live 26.2 texture view, forwards input/cursors, requests 30 fps and closes the browser in `removed()`. Added [feasibility notes](feasibility/F1B-notes.md) and [F2 YouTube evidence](feasibility/screenshots/f1b-1-youtube.png). Main Java code, SPEC and AGENTS are unchanged.
- How it was verified (2026-10-30, Europe/Istanbul):
  - F0-3 merge verified with `gh pr list --state all --head chore/f0-3-scaffold` and matching local/remote main `9712e2a`. JDK/javac 25.0.4 available.
  - `.\gradlew.bat build --console=plain --warning-mode all`: passed (first build 20s, final build 9s; `test NO-SOURCE`). No Gradle warnings in the final build.
  - `.\gradlew.bat runServer --console=plain`: common init at 16:41:45, `Done (0.421s)!` at 16:41:48, console `stop` at 16:42:10, `BUILD SUCCESSFUL in 37s`. MCEF/JCEF absent from the loaded mods, server log and `runtimeClasspath` dependency report. Server remains repo-local and bound to 127.0.0.1.
  - `rg -n '^import .*mcef|^import org\.cef' src/main src/client`: three matches, all in `src/client/.../spike` (Screen lines 7-8, client bridge line 4). No common MCEF imports or references.
  - `.\gradlew.bat runClient --console=plain`: native first-time install stayed in `run/config/mcef-modern/`; async init completed at 16:43:35. Codex entered the local creative world, submitted the default command and observed YouTube playing. Talha then tested and confirmed playback with sound, clicks, scroll, typing and Esc. Browser creation at 16:43:47 (1280x720, 30 fps requested), closure independently logged at 16:45:09. No crash observed. Computer Use stopped on Talha's physical Esc; game input remained with him.
  - F2 `run/screenshots/2026-10-30_16.44.08.png` copied byte-for-byte to `docs/feasibility/screenshots/f1b-1-youtube.png`; matching SHA-256 `f9aa0bd58439ecb72e62d037a57264aa9eb908a0f30dd5f248ced725381fe559`. Loading screenshot not available because initialization finished before the command opened the Screen.
  - Final review: `git diff --cached --check` passed; common Java reference search returned no MCEF/JCEF/spike matches. Jar inspection confirms spike classes are packaged and MCEF/JCEF are not bundled. Implementation committed as `6838f37` and pushed; PR opened without merging.
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
