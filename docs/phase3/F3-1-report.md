# F3-1: authoritative playback, panel controls and server clock

Status: **F3-1 complete, PR awaiting review**. [PR #9](https://github.com/devtalh/perde/pull/9), branch `feat/f3-1-playback-state`, from main `5e8fbd8` (merged PR #8). [LEA-196](https://linear.app/leak-rake/issue/LEA-196). Evidence checkpoint `f986025` passed [CI run 67](https://github.com/devtalh/perde/actions/runs/41229769492); final docs-only head must pass CI before ready. No merge authorized. Next separate task: **F3-2**, applying this state to browser/video.

## Delivered behavior

- Server-authoritative Play, Pause, SeekRelative and SeekAbsolute C2S payloads use the existing server-opened panel, dimension, loaded chunk, existing screen, <=8-block distance, canControl, validation and shared 10/s rolling control limit. Seek values are doubles on the wire so NaN/infinity are explicitly rejected, with absolute range 0..86400000 ms and relative range -86400000..86400000 ms. Valid relative seeks clamp their resulting target. Every accepted playback change logs position, action, resulting positionMs/playing and player at INFO.
- Pure Java PlaybackClock implements wall-clock play/pause/seek and 0..24-hour bounds. A changed non-empty URL resets to zero/playing; clearing resets to zero/paused. Submitting the identical normalized URL preserves playback. Geometry/access/owner/player-list copies preserve playback. ScreenState retains immutable, centralized copy methods for future fields.
- Disk and vanilla BE update tags serialize current position and timestamp together without mutating the in-memory anchor. Loading a timestamp from before this server start rebases the saved position at load; a same-session chunk reload keeps elapsed time. A server-only ticker marks running chunks dirty, without neighbor updates or tick-by-tick client packets, so autosave/unload/shutdown saves an advancing timeline even without control traffic. Paused states do not need periodic writes.
- C2S TimeRequest/S2C TimeResponse echo the monotonic client token and server wall-clock ms. Each connection requests five samples about 200 ms apart, then one every 15 s. The server independently drops time requests over five per rolling second/player. The pure estimator uses the lowest RTT among the last eight accepted samples and advances from local nanoTime, independent of the client wall clock. Unknown/stale echo tokens are ignored; INIT/disconnect/rejoin reset all history and pending requests.
- Panel has Play/Pause, -30/-10/+10/+30 s, mm:ss or h:mm:ss EditBox/Go and live position. Every playback input is disabled for a viewer; the live label remains visible. Layout retains a 240 logical-pixel height. All 68 EN/TR keys match.
- Browser implementation, JS injection, video application, drift correction, ads, volume product behavior and later phases are unchanged. The fixture's existing Chromium `--mute-audio` and Minecraft master=0 keep test output silent.

## Files and verified APIs

| Area | Files |
| --- | --- |
| Timeline, persistence | `screen/PlaybackClock`, `PlaybackTime`, `PerdeServerClock`, `ScreenState`; `block/PerdeBlock`, `PerdeBlockEntity` |
| Protocol and decisions | `net/PerdePayloads`, `PerdeServerNetworking`, `ControlDecisions`, configurable `PacketRateLimiter`, pure `ServerTimeEstimator` |
| Client | `client/net/ClientPlaybackClock`, networking registration, `client/screen/PerdePanelScreen`, EN/TR resources |
| Verification | Five new JUnit suites, existing opt-in `docs/phase2/fixture` extensions, this report and evidence; `docs/PROGRESS.md` |

Read AGENTS, SPEC (including 3.2/3.4/3.6/4), PROGRESS, F2-report, Notion Project Hub and Linear LEA-196 before changes. Java 25.0.4 is present. Clean main contains `5e8fbd8`, and origin/main was fetched/confirmed before branching. The older Notion hub has known historical differences; current repository design governs. SPEC and AGENTS are unchanged.

Minecraft 26.2 Loom sources verify BlockEntity.setLevel/loadAdditional/saveAdditional, Level.blockEntityChanged and BaseEntityBlock.createTickerHelper/getTicker. The resolved Fabric lifecycle 4.1.4 source places SERVER_STARTING before player list/level loading and SERVER_STOPPED after levels close. Networking 6.3.4 sources document typed server callbacks on the server thread and client callbacks on the render thread. Source copies are available under ignored `run/phase1a-research`, with the Minecraft source jars under `.gradle/loom-cache/minecraftMaven`. No unverified API or new browser API is used.

## Acceptance results

All game data stays under `run/`, Gradle uses `.gradle-user-home`, and only repo-owned PIDs/command lines and loopback 127.0.0.1:25570 are targeted. No Computer Use, desktop input, window activation/focus switching, Prism, .minecraft or KuzCraft access. Real EditBox/Button/use-item/payload/F2 APIs are exercised by the opt-in fixture, never bundled in the production jar.

| Criterion | Observed result / evidence |
| --- | --- |
| Build and old/new tests | Fresh `.\gradlew.bat build --rerun-tasks --console=plain --warning-mode all`: SUCCESSFUL in 12s, all nine tasks executed, **240 tests, 0 failures/errors/skips**. [Build](f3-1-build.txt), [suites](f3-1-tests.csv). |
| MCEF-free windowless dedicated server | Final independent boot: 44 mods, MCEF/compat absent, --nogui/CREATE_NO_WINDOW, run/server, 127.0.0.1:25570, PID 47028. Done (0.974s), ordinary stdin stop, all dimensions saved, Gradle exit 0/SUCCESSFUL in 1m 21s. [Final server log](f3-1-server-final.txt), trailing whitespace normalized. |
| Owner panel actions | Player1 changed the real server-opened panel: play/pause, each +/-10/30 s, 01:23 and 1:02:03 Go, pause-preserving seek, lower/upper clamps. INFO server changes agree with both received vanilla BE timelines. [Server](f3-1-server-integration.txt), [P1](f3-1-player1.txt), [P2](f3-1-player2.txt). |
| Client position agreement | **21 common-reference comparisons, max difference 8 ms**, below 100 ms. Paused states are exact; after restart 620651 vs 620653 ms (2 ms). Both clients use the production estimator. [All comparisons](f3-1-skew.csv). |
| Watch-only rejection | OWNER_ONLY/Player1 owner, Player2 non-op. All playback widgets/time input disabled, live position visible. Real forged Play, Pause, SeekRelative, SeekAbsolute rejected with permission action bar, paused position 3723000 unchanged on both. Extra forged pause after restart also rejected, playing state retained. No accepted Player2 playback log. |
| Invalid values | Owner's NaN, infinity and 86400001 for both seek forms, plus negative absolute, all rejected as invalid; no state change or accepted log. Real panel 1:60 shows format error and preserves 1:02:03. Unit tests additionally cover negative overflow, both authorization and rate-limit boundaries. |
| URL reset | Changed URL log: positionMs=0, playing=true; both clients receive the same reset timeline. Clearing URL: 0/false, browser closes from existing URL lifecycle. Restoring a URL: 0/true. |
| Nobody watching / chunk reload | Both players teleported from source (300,120,300) to (1000,120,1000); source not force-loaded. `execute unless loaded 300 120 300` prints F3-1_CHUNK_UNLOADED. On return, server saved-position delta 329108-122401 = **206707 ms**, exactly equal to timestamp delta, despite intermediate unload. Both received equivalent advancing anchors. |
| Restart while playing | Normal save-all/stop, all dimensions saved, exit 0. Read actual on-disk region NBT: **position 600857**, timestamp **1793471037539**, playing=1. New clean JVM later reports position **610600**, timestamp **1793471104177**. Derived load/rebase epoch=1793471094434: advances only **9743 ms after load**, omits **56895 ms** between saved checkpoint and load. [Saved NBT](f3-1-saved-before-restart.json), [checks](f3-1-checks.txt). |
| Clock logs/reset | Five initial replies and subsequent ~15 s samples logged on each join, including Player2 rejoin in the same client JVM. Sample winner before initial checks: P1 RTT 30.1472 ms/offset 1.7933530514463713E12 ms, P2 RTT 24.6581 ms/offset 1.7933530514458467E12 ms. After restart: P1 RTT 25.4993, P2 30.6599 ms. Offsets are server epoch minus local nanoTime-ms, not a wall-clock skew measurement. Unmatched request flood leaves estimator valid. |
| Cleanup | Final fresh Player1 Gradle SUCCESSFUL in 1m 12s/exit 0; Player2 SUCCESSFUL in 8m 4s/exit 0. Native registry reaches zero. No repo DLI process or 25570 listener remains. |

Sequential client observations are projected to one common local epoch to remove fixture dispatch delay. Because clients/server are on the same PC, local wall-clock timestamps are only a test reference; production estimation and display advance exclusively from nanoTime + measured server offset. Differently anchored BE snapshots can represent the same timeline, especially for later chunk observers. This test does not claim 8 ms over arbitrary real networks.

## Log excerpts

```text
[21:19:12] ... Perde playback pos=BlockPos{x=300, y=120, z=300} action=SeekAbsolute positionMs=3723000 playing=false player=Player1
[21:19:15] ... F2-2 action bar: You do not have permission to change this screen.
[21:19:33] ... Perde playback pos=BlockPos{x=300, y=120, z=300} action=SeekRelative positionMs=86400000 playing=true player=Player1
[21:19:39] ... Perde playback pos=BlockPos{x=300, y=120, z=300} action=SetUrl positionMs=0 playing=true player=Player1
[21:20:37] ... [Server] F3-1_CHUNK_UNLOADED
[21:24:28] ... Done (0.974s)! For help, type "help"
[21:25:34] ... Stopping the server
[21:25:34] ... ThreadedAnvilChunkStorage: All dimensions are saved
BUILD SUCCESSFUL in 1m 21s
```

## Visual proof

Six unchanged vanilla F2 screenshots, SHA256 source/evidence pairs verified in [provenance](f3-1-provenance.csv). All six owner/viewer/paused/error/restarted images visually inspected.

- [Owner playing](screenshots/f3-1-owner-playing.png), [viewer disabled](screenshots/f3-1-viewer-playing.png).
- [Paused 1:02:03](screenshots/f3-1-owner-hour-paused.png), [invalid time error](screenshots/f3-1-invalid-time.png).
- [Restarted owner](screenshots/f3-1-restarted-owner.png), [restarted viewer](screenshots/f3-1-restarted-viewer.png).

## Corrections and limits

The initial boot exposed a real product lifecycle bug: calling setChanged while setLevel was constructing a chunk recursively requested that chunk and blocked the server. Thread dump identified the path; removed this dirty marking before accepted runtime. The server ticker handles running dirty state after installation. That initial hung repo-owned server alone was terminated after PID/command-line validation. Same-session chunk reload and the subsequent independent restart verify the corrected lifecycle. No assertions/tests were removed or weakened.

Two orchestration issues were separate: Python cp1254 output failed on the gear glyph (actual client file log remains complete), and the console queue thread encountered a transient Windows PermissionError during atomic queue replacement. Wrappers were corrected to UTF-8 and retry transient sharing failures. The first successful server's lost stdin forwarder was recovered once by a guarded repo-local attach helper, forwarding only normal console commands onto the server thread. Its first guard attempts refused the target because DLI clears the env property. The server then saved/stopped normally. **The final independent boot has no attach agent and stops via normal stdin.** Recovery code stays under ignored run/. [Diagnostics and original blocked stack](f3-1-diagnostics.txt). An initial too-early fixture action after the failed connection is excluded from accepted results.

Final accepted clients/server have zero bare AWT exception prefixes, fixture failures and Perde browser failures. Known Windows OSHI, dev-account/Realms and Chromium registry diagnostics remain; no machine settings or external software changed.

Clock estimation assumes approximately symmetric latency and can jump when its best sample expires or server wall time changes. Restart resumes the last persisted checkpoint, not time elapsed while the server is down. No crash-durability guarantee beyond Minecraft's normal chunk saves. Legacy pre-F3 state with an old timestamp resumes its saved position. Trusted-site/native browser residual risks, ads/autoplay, shader tint and browser memory remain. Product playback still does not affect the video until F3-2.

## Manual review / handoff

1. Start this mod's dedicated server without MCEF; join two test clients. Place Perde as Player1 and leave OWNER_ONLY.
2. Open an allowed HTTP(S) URL. In Player1's panel, pause, Go to 01:23/1:02:03, use all four relative seeks and resume. The position label should freeze/advance appropriately on both clients.
3. Open Player2's panel: controls disabled, position live. Try invalid time 1:60 as owner and confirm a clear error.
4. Change URL, confirm position starts at zero/playing; clear it, confirm zero/paused.
5. Save/stop while playing, wait, restart and rejoin. Position resumes from the saved checkpoint, excluding downtime. Video behavior is deliberately unchanged in this PR.

Evidence checkpoint CI is green. Final docs-only head must also pass CI before marking ready; then only human review/final manual acceptance remains. Do not merge. Next task F3-2 owns browser application/JS/drift correction; F3-3/F3-4 own ad/embed work.
