# F2-1: real browser on Perde screens

Status: **complete**, branch `feat/f2-1-browser-manager`, [PR #6](https://github.com/devtalh/perde/pull/6) targeting `main`. PR #5 was confirmed merged at main `5eb1773` before implementation. No merge is performed here. Next: **F2-2 browser navigation security (LEA-185)**.

## Implementation

- `client.browser`: loaded block entities tracked through Fabric load/unload events. Every client tick selects at most `MAX_ACTIVE_BROWSERS = 1` using camera distance to the actual screen rectangle, including offsets and all four horizontal orientations. Open within 48 blocks, retain through 52, switch only when another eligible screen is at least 4 blocks closer. Equal distances retain the active screen.
- MCEF implementation is behind a loader-guarded backend. Initialization is polled without blocking; creation, resize to 1280x720, 30 fps and focus=false run on the client thread. Synced URL changes use `loadURL` on the same browser after HTTP(S) revalidation. Empty/invalid URL, replacement, removal, chunk unload, level change, disconnect and client stopping close it. Cleanup asserts the client thread; the disconnect callback uses `client.execute`.
- BER retains the proven `submitCustomGeometry` / `RenderTypes.text` path, full brightness, depth testing, back-face culling and global `shouldRenderOffScreen`. Renderer distance includes the 52-block retention band. Immutable generation tickets prevent stale extraction from displaying a replacement browser on the old screen. The registered texture bridge resolves MCEF's current texture view each frame and never owns/closes it.
- No page, loading, missing, inactive and failure text use matched English/Turkish translation keys. `ScreenState` truncates every owner/allowed-player name to 16 characters, makes an immutable copy, and the panel payload writer applies the same bound defensively.
- The spike package is unchanged. Its development commands remain separate diagnostics, outside the real-screen slot. Tests exercise the spike only while no managed screen is in range.

Source checks: matching Minecraft 26.2 and Fabric lifecycle/camera/render sources generated/extracted locally in Phase 1A; [MCEF API at df61843](https://github.com/DimasKama/mcef-modern/blob/df61843/src/main/java/net/dimaskama/mcef/api/MCEFApi.java), [browser implementation](https://github.com/DimasKama/mcef-modern/blob/df61843/src/main/java/net/dimaskama/mcef/impl/MCEFBrowserImpl.java), and resolved JCEF API bytecode (`loadURL`, protected `CefClient.getAllBrowser`). Test-only reflection reads that registry; production code uses only the public browser API. Linear LEA-193 and the Notion hub were read; current repository/task decisions supersede the older hub. Linear field notes still return `Tool get_agent_context not found`.

## Verification

All commands used Java/Javac 25.0.4 and repository-local `GRADLE_USER_HOME=.gradle-user-home`. All game data/natives/cache stayed below `run/`. Only this task's verified repository DLI clients and their native helpers were controlled/measured. No Minecraft window activation was used.

Final `gradlew.bat build --rerun-tasks --console=plain --warning-mode all`: **BUILD SUCCESSFUL in 6s**, all 9 tasks executed. **42 fresh tests, zero failures/errors/skips**. [Build output](f2-1-build.txt), [test counts](f2-1-tests.csv). New tests cover selection boundaries/ties/hysteresis/unavailable candidates/rectangle edges in every facing, immutable name truncation, and the actual OpenPanel codec round trip with oversized names. Default main runtime and rebuilt jar contain no MCEF/JCEF dependency/classes/nested jars or fixture. Common code has no client/MCEF references. Language key parity: 43 each. Git diff checked; SPEC, AGENTS and spike unchanged.

Dedicated `runServer`, without MCEF, loaded **44 mods**, bound only `127.0.0.1:25571`, reached Done and shut down through console `stop` with all dimensions saved:

```text
[14:15:27] [main/INFO] (FabricLoader) Loading 44 mods:
[14:15:33] [main/INFO] (perde) Perde initialized.
[14:15:35] [Server thread/INFO] (Minecraft) Starting Minecraft server on 127.0.0.1:25571
[14:15:37] [Server thread/INFO] (Minecraft) Done (1.791s)! For help, type "help"
[14:15:53] [Server thread/INFO] (Minecraft) Stopping the server
[14:15:53] [Server thread/INFO] (Minecraft) ThreadedAnvilChunkStorage: All dimensions are saved
BUILD SUCCESSFUL in 29s
5 actionable tasks: 1 executed, 4 up-to-date
```

This run used an ignored init script setting `loom.runs.server.runDir = 'run/phase2-server'`, because the earlier repository server remains running separately. An initial attempt to override the Java task's workingDir was superseded by Loom, hit that existing world's lock and exited; no server was killed or lock removed. The corrected run above is the accepted boot check. [Retained exact runtime excerpts](f2-1-runtime-evidence.txt). Raw complete logs remain under ignored `run/phase2-verification/`.

## Singleplayer acceptance

Actual creative placement at A `(100,120,100)` and B `(120,120,100)`, both facing north, size 8x4.5, owned by Player1. URLs were entered using the real server-opened panel's EditBox and Open Button callback. The fixture does not directly set screen state, bypass permissions or replace browsers. Walking uses the normal forward key, and breaking uses `MultiPlayerGameMode.startDestroyBlock` within ordinary creative reach. Scene setup and repositioning use normal singleplayer commands.

| Check | Result / evidence |
| --- | --- |
| a: near A | PASS. A live YouTube (including normal ads), B inactive. [F2](screenshots/f2-1-a-near-a.png). JCEF registry: one browser, id=5. |
| b: walk to B | PASS. Walk from x=102.5 to x=118.038, same y/z. A closed before B creation at 14:24:45; B shows example.com, A inactive. [F2](screenshots/f2-1-b-near-b.png). JCEF registry: one browser, id=6. |
| c: panel URL change | PASS. B example.com to Wikipedia at 14:25:02, same native id=6 and extraction ticket=249, no recreate. [Panel](screenshots/f2-1-panel.png), [new live page](screenshots/f2-1-c-url-changed.png). |
| d: beyond 52 | PASS. Playing YouTube browser id=2, [before walk F2](screenshots/f2-1-d-video-before-range-close.png). Walk north to z=42.698, rectangle at z=99.998. Browser closed at 14:23:30; JCEF's browser registry became 0, destroying the playback/audio source. This is native browser teardown evidence, not a separate loopback audio recording. |
| e: break active block | PASS, repeated. B closed with `block removed/unloaded` at 14:25:39, client reports air/no entity/no ticket; no stale B rectangle. [F2](screenshots/f2-1-e-block-broken.png). Remaining A legitimately becomes the nearest active screen. |
| f: Save and Quit/rejoin | PASS, repeated. At 14:25:45 native registry=0 after normal world save/quit; at 14:26:03 rejoin recreated nearest A, native registry=1/id=8. [F2](screenshots/f2-1-f-rejoined.png). Direct client stop with A active also logged cleanup and exited successfully. |
| g: spike regression | PASS. `/perde-spike place` shows live YouTube, registry=1/id=1; remove returns registry=0. [F2](screenshots/f2-1-g-spike.png). No managed screen in range. |
| missing MCEF | PASS. Runtime dependency excluded via opt-in fixture property; client/world boots, [MCEF missing F2](screenshots/f2-1-mcef-missing.png), native helper count=0 in every sample, clean exit in 53s. |
| empty URL | PASS. Real panel submitted an empty string at 14:28:38, browser closed, native registry=0, [No page F2](screenshots/f2-1-empty-url.png). Restoring URL creates exactly one browser again. |
| dimension change | PASS. Overworld to Nether at 14:29:11 closes browser and registry=0; return at 14:29:14 creates nearest screen, registry=1. Final client stopping also closes it. |

All images are unchanged vanilla F2-handler captures, invoked through the isolated fixture, not desktop screenshots or physical Windows key inputs. [SHA-256 provenance](F2-provenance.csv) matches every original under `run/screenshots/`. Ten copies were visually inspected. A first extra empty-URL attempt exposed `strip()` dropping the fixture's trailing tab; the fixture parser was corrected to preserve empty values, then the test was rerun successfully. This was a test-orchestration failure, not a suppressed product failure.

## Memory and lifetime evidence

[Raw samples](f2-1-memory.csv): ten samples per stage, 250 ms apart (plus query time), exact client PID, helper PIDs, UTC and resident bytes. Helpers must have the verified client as parent and an executable under the repository's `run/config/mcef-modern/`. Working sets include shared resident pages and are not Java heap/private bytes. The JCEF native registry was sampled separately each second and on status requests; observed native browser counts never exceeded 1.

| Stage | Helper count | Mean helper working set |
| --- | --- | --- |
| First A before walking | 6-7 | 741.23 MiB |
| First B after walking | 6 | 371.02 MiB |
| Audited playing video | 7 | 849.12 MiB |
| Same video beyond 52 | 5 | 311.70 MiB |
| Final A before walking (YouTube/ad) | 8-9 | 1118.40 MiB |
| Final B after walking | 6 | 399.73 MiB |
| Final Save and Quit | 6 | 553.37 MiB |
| MCEF absent | 0 | 0 MiB |

Chromium's shared GPU/network/utility/spare processes can remain with zero native browsers. Process count alone is not a browser count or proof of a leak. The manager bounds live browsers to one; it cannot promise a fixed MB ceiling for arbitrary sites. YouTube/ad renderer and shared-context memory varies considerably. Every tested client exited normally, and all of its helpers disappeared without process kills.

## Risks and follow-up

- F2-2 (LEA-185) still owns browser navigation security and upstream `--disable-web-security`. Initial/synced URL validation is implemented; no navigation/subresource protection is claimed.
- No Phase 3 sync, Phase 4 distance audio/config, Phase 5 input, culling compatibility changes or spike removal. Audio currently plays while its browser exists and is torn down when the browser closes. Full Sodium/Iris/Entity Culling/MoreCulling checks remain F2-3.
- Known OSHI/performance-counter and dev Realms/profile-key errors remained. MCEF runs still print bare AWT-EventQueue exception prefixes after successful Gradle exit (19 in the audited run, 8 in the final run); their upstream cause is unresolved. No new Perde Java failure or native GLFW activation crash occurred. No cross-window activation was attempted.
- Forced MCEF initialization failure and long-duration soak tests were not added to this acceptance matrix. The code retries failed browser creation only after URL/selection changes and shows localized failure text.

## Manual reproduction

1. Set repository-local `GRADLE_USER_HOME`, run `gradlew.bat runClient`, enter a creative world under `run/saves`.
2. Place two Perde blocks about 20 blocks apart. Right-click each, set YouTube/example.com URLs and a readable size.
3. Walk between them; verify only the nearer page is live and the other says inactive. Move around the midpoint to check the 4-block switching advantage.
4. Edit the active URL in its panel, then walk more than 52 blocks from every screen rectangle. Check creation/navigation/closure logs.
5. Return, break the active block, Save and Quit, rejoin, and verify no stale image and one nearest browser.
6. Outside managed-screen range, run `/perde-spike place` and `/perde-spike remove`.
7. For the reproducible focus-free test fixture and the missing-MCEF launch, see [fixture instructions](fixture/README.md).
