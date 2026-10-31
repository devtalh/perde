# Phase 2: real Perde screens and pack validation

Status: **Phase 2 complete, PR awaiting review**, F2-3 on `feat/f2-3-validation`, [PR #8](https://github.com/devtalh/perde/pull/8) targeting main. Implementation/evidence checkpoint `6797e0d` passed CI; the final evidence commit must also pass latest-head CI before ready. No merge is authorized. Next: **Phase 3 planning by Claude**.

## Scope and starting state

PR #7 was confirmed merged through GitHub and local main before branching: `bb6f68e`, merged 2026-10-31 19:33:28 Europe/Istanbul. Read AGENTS, SPEC, PROGRESS, the F2-1/F2-2/F1B reports, Notion Project Hub and Linear LEA-194/187/188/195. The older Notion hub differs from current SPEC; repository decisions govern. Linear shared field notes returned `Tool get_agent_context not found`; regular issue tools worked.

| Task | Delivered behavior | Detailed evidence |
| --- | --- | --- |
| F2-1 | Real block-entity browser texture, nearest one active, 48/52-block lifecycle and generation-safe frames | [F2-1 report](F2-1-report.md) |
| F2-2 | Server-managed site allowlist, isolated browser contexts, native privacy settings, validated control packets | [F2-2 report](F2-2-report.md) |
| F2-3 | Real-screen shader/culling/lifecycle validation, two-client state propagation, feasibility spike removal | This report, [PR #8](https://github.com/devtalh/perde/pull/8) |

All execution stays inside the repository. Java 25.0.4 and repository-local `.gradle-user-home` are used. No Prism, `.minecraft`, KuzCraft or unrelated Minecraft process is accessed or controlled. **No Computer Use, desktop input, window activation or focus switching is used.** Opt-in fixtures invoke normal Minecraft client/server/panel APIs. Vanilla F2-handler PNGs are copied unchanged to this folder, with [SHA-256 provenance](f2-3-provenance.csv). Images are visually inspected from those files.

The full profile means the exact eight render/performance mods in SPEC 2: Sodium 0.9.2, Sodium Extra 0.9.4, Reese's Sodium Options 2.2.4, Iris 1.11.4, Entity Culling 1.11.2, MoreCulling 1.8.1, Lithium 0.25.3 and Distant Horizons 3.3.3, plus required Cloth Config and existing Fabric/MCEF dependencies. This does not claim validation of every other mod in the approximately 125-mod production pack. Complementary Reimagined r5.9.3 is enabled, Iris HIGH, zero changed shader options. Shader/download/cache data stays under `run/`.

The fixture saves Minecraft master volume=0 and adds `--mute-audio` before test Chromium initialization. Media observations additionally set/read video volume=0 and muted=true. These opt-in test changes are not product behavior. The browser registry audit covers every live native CefClient, including private clients still closing.

## Full profile and culling (LEA-187)

Real creative placement, source A `(300,120,300)`, north-facing, 8 x 4.5, Player1 owner. The real server-opened panel supplies URL changes; geometry uses the existing permission-checked payload. Source B is `(320,120,300)`. Scene construction/positioning uses ordinary singleplayer commands.

| Check | Observed result | Unmodified F2 |
| --- | --- | --- |
| Live, upright YouTube | PASS. Native media time 26.126257 to 185.714008, paused=false, readyState=4; subsequent autoplay/ads are retained in evidence | [Day](screenshots/f2-3-full-day.png) |
| Depth occlusion | PASS. Stone at `(300,122,296)` covers the relevant screen area | [Stone](screenshots/f2-3-full-occlusion.png) |
| Night brightness | PASS. Readable live browser while scene is dark, accepted shader tint remains | [Night](screenshots/f2-3-full-night.png) |
| Back face | PASS. Source block remains; browser plane is absent from behind | [Back](screenshots/f2-3-full-back.png) |
| Source outside view | PASS. offsetX=6, rectangle center x=294.5, camera `(294.5,120,298.5)`; visible cropped screen survives centered and leftward camera aims with the source fully outside view | [Centered](screenshots/f2-3-culling-offscreen-final-1.png), [Leftward](screenshots/f2-3-culling-offscreen-final-2.png) |
| Source occluded by wall | PASS. Wall `(298,120,298)` through `(303,124,298)` hides the source; offset screen remains live | [Wall](screenshots/f2-3-culling-wall-final.png) |
| Nearest one / switching | PASS. Walk x=302.5 to 318.038 at z=288.5. A YouTube closes before B example.com opens; the other rectangle is inactive, actual native audit=1 | [A active](screenshots/f2-3-two-near-a.png), [B active](screenshots/f2-3-two-near-b.png) |

No renderer fix or mod-specific whitelist/config bypass is needed. Existing `shouldRenderOffScreen()=true` uses Minecraft's global renderer path and bypasses Entity Culling's `isGloballyRendered` / `shouldRenderOffScreen` gates before frustum and source-box occlusion. The renderer's existing camera-distance bounds already include the entire offset rectangle. Entity Culling stays enabled with blockEntityFrustumCulling=true, skipBlockEntityCulling=false, no perde whitelist entry. MoreCulling stays enabled with useBlockStateCulling=true, dontCull empty and modded block culling enabled. Its block-face options are not a generic block-entity bounds API.

Verified against the exact resolved 1.11.2/1.8.1 jars and their matching source copies: [Entity Culling dispatcher](https://github.com/tr7zw/EntityCulling/blob/ae786c8921de5ea52c06481dfa47180e42bbd54d/src/main/java/dev/tr7zw/entityculling/mixin/BlockEntityRenderDispatcherMixin.java), [MoreCulling block compatibility](https://github.com/fxmorin/moreculling/blob/f47fd4093ee0822f84369429b41ed0547b0f5359/common/src/main/java/ca/fxco/moreculling/mixin/blockstates/Minecraft_loadBlocksMixin.java). Culling captures remained visible across repeated camera samples; this finite test is not a guarantee for every shader/camera combination.

Earlier exploratory captures use a farther camera/narrower wall and can show an edge of the source. The third close-camera aim also turns back toward the source and shows its edge; it is a retained transition control, not strict source-offscreen proof. These are retained in the provenance manifest. The centered/leftward captures and final widened-wall capture above are the accepted offscreen/hidden-source proof.

## Memory and lifecycle (LEA-188)

Ten samples per stage, 250 ms apart plus process-query time, sum only repository-native jcef_helper working sets with the verified client's exact parent PID. Report arithmetic means / 1,048,576. This is resident working set, including shared pages, not Java heap/private bytes. Chromium shared GPU/network/utility processes can remain with zero native browsers. Helper process count is not browser count.

| Stage | Helper count | Mean helper MiB |
| --- | --- | --- |
| Initial full-profile YouTube | 6 | 752.84 |
| Two screens, A active before walking | 7 | 893.41 |
| Two screens, B active after walking | 6 | 414.79 |
| Soak cycle 1 | 6 | 784.44 |
| Soak cycle 5 | 7 | 747.24 |
| Soak cycle 10 | 6-7 | 806.16 |

**10/10 cycles PASS**, 19:53:29-19:59:30 Europe/Istanbul. [Per-cycle observations](f2-3-soak.csv) record actual save/quit native=0, rejoin native=1 and readyState=4/playing video. [Raw memory samples](f2-3-memory.csv), [cycle 1](screenshots/f2-3-soak-1.png), [cycle 5](screenshots/f2-3-soak-5.png), [cycle 10](screenshots/f2-3-soak-10.png). Full-profile client PID 24264 exited normally: **BUILD SUCCESSFUL in 22m 17s**. The entire completed stdout/stderr has **0 bare AWT exception prefixes, 0 java.sql.SQLException lines, 0 Perde browser failure logs**, maximum observed native count=1. [Exact diagnostics/counting scope](f2-3-runtime-diagnostics.txt). This run does not reproduce or resolve the earlier upstream AWT/DH issues.

Test-orchestration corrections are retained: the first fixture launch used a Mixin package containing its normal entrypoint and failed before world startup; Mixin code was moved to a dedicated subpackage and the client relaunched. An overlapping camera/soak script caused `Join a world first` in the fixture; that interrupted attempt is not counted. During cycle 5 memory sampling, a helper exited while CIM was reading its executable path; the null-path race was corrected, memory was remeasured in that same joined session, then the sequence resumed. No product assertion, test or URL/privacy policy was weakened.

## Dedicated server and two clients

**PASS on the post-removal implementation.** Plain dev clients Player1 PID 44860 / Player2 PID 41912, server PID 40868, `run/server`, offline, loopback-only 127.0.0.1:25570. Server launched with `--nogui`, stdin/stdout pipes and Windows CREATE_NO_WINDOW. Player2 was driven exclusively through its own focus-free fixture queue; neither game window was activated or switched by automation. Source `(300,120,300)` was placed by Player1; Player2 remained a viewer under OWNER_ONLY. Both clients joined normally and received authoritative state/allowlist updates.

| Check | Actual native/state observations | Player1 / Player2 F2 |
| --- | --- | --- |
| Initial YouTube | Both saved/native URLs `watch?v=dQw4w9WgXcQ`, Rick Astley title, ready=4/paused=false; observed times 8.400118 / 8.780132 | [P1](screenshots/f2-3-multi-initial-p1.png), [P2 live](screenshots/f2-3-multi-initial-p2-live.png) |
| Owner panel changes URL | Both update to `watch?v=aqz-KE-bpKQ`, Big Buck Bunny title, same existing native browser id1; ready=4/paused=false, times 11.660129 / 12.380106 | [P1](screenshots/f2-3-multi-changed-p1.png), [P2](screenshots/f2-3-multi-changed-p2.png) |
| Console removes youtube.com | Both native counts=0, status=site_not_allowed, placeholder shown. Server NBT and both clients retain the Big Buck Bunny URL | [P1](screenshots/f2-3-multi-removed-p1.png), [P2](screenshots/f2-3-multi-removed-p2.png) |
| Console adds youtube.com | No URL edit/restart. Both create native id2, same saved URL/title, ready=4/paused=false, times 20.960072 / 21.000135, volume=0/muted=true | [P1](screenshots/f2-3-multi-restored-p1.png), [P2](screenshots/f2-3-multi-restored-p2.png) |

These are observations at different wall-clock instants, not a video-sync measurement. An initial early Player2 capture showed loading; the accepted P2 live capture follows actual ready/playing media. [Player1 native/runtime evidence](f2-3-multi-player1.txt), [Player2](f2-3-multi-player2.txt), [server full log](f2-3-server.txt), [binding/process provenance](f2-3-server-binding.txt). All **26** source/evidence screenshot hash pairs match. Test data/config remains under `run/` only.

The default dedicated server loads **44 mods without MCEF/JCEF or compat mods**, reaches Done and stops via console with all dimensions saved:

```text
[20:02:57] [main/INFO] (FabricLoader) Loading 44 mods:
[20:03:09] [main/INFO] (perde) Perde initialized.
[20:03:12] [Server thread/INFO] (Minecraft) Starting Minecraft server on 127.0.0.1:25570
[20:03:13] [Server thread/INFO] (Minecraft) Done (1.209s)! For help, type "help"
[20:07:05] [Server thread/INFO] (Minecraft) Stopping the server
[20:07:05] [Server thread/INFO] (Minecraft) ThreadedAnvilChunkStorage: All dimensions are saved
BUILD SUCCESSFUL in 4m 12s
```

Server Gradle exit=0. Player1 and Player2 exit normally, BUILD SUCCESSFUL in 3m 43s / 2m 56s. Completed plain-client logs each contain zero bare AWT exception prefixes, SQL exception lines, fixture failures and Perde browser failure logs. Existing OSHI/dev-account/Chromium startup diagnostics remain. No repository DLI process or 25570 listener remains after shutdown. The post-removal real-screen smoke is included in these two successful plain-client runs.

## Spike removal and final checks

Removed all five `client.spike` classes, `/perde-spike` registration and their dedicated renderer/GUI/lifecycle hooks. MCEF initialization moves into the guarded client-only backend constructor, preserving eager asynchronous startup; lazy engine init remains Phase 4. Keep compatibility profiles and every historical docs fixture. Fixture READMEs identify retired commands and the intentional current loopback restriction. SPEC and AGENTS are unchanged.

Post-removal `.\gradlew.bat build --rerun-tasks --console=plain --warning-mode all`: **BUILD SUCCESSFUL in 10s**, all 9 tasks executed, **197 tests, zero failures/errors/skips**. [Build](f2-3-build.txt), [suite counts](f2-3-tests.csv), [jar/import/language/hash checks](f2-3-checks.txt). After the final runtime checks, a default [completion build](f2-3-completion-build.txt) also passed in 2s, all 9 tasks up-to-date. The default jar contains no bundled MCEF/JCEF/compat/spike/fixture classes or nested jars; common imports remain isolated and all 57 EN/TR keys match. Default runtimeClasspath was separately resolved without the compat property. SPEC/AGENTS unchanged; Git diff/whitespace checked.

MCEF-free windowless server and post-removal real-screen client smoke passed above. Checkpoint `6797e0d` is committed/pushed and CI green; [PR #8](https://github.com/devtalh/perde/pull/8) remains unmerged. The final evidence-only commit is subject to the PR's latest-head build check before ready. The pre-removal full-profile JVM is not counted as the post-removal client smoke. [Full-profile runtime excerpts](f2-3-full-runtime.txt) retain every native observation and per-cycle media result.

## Open risks and Phase 3 handoff

- Trusted sites remain a trust boundary. Native WebSocket bypass, DNS rebinding and upstream `--disable-web-security` remain accepted residual risks. No canary repeated, no network-sandbox claim.
- AWT exception types/cause remain unproven. This full-profile run and both final plain clients produced 0 bare prefixes; the full-profile run produced 0 SQL exception lines. The earlier AWT/DH issues were not reproduced or fixed; no suppression or upstream fix in this task.
- YouTube ads/autoplay can differ by client. Ads and automatic navigation were observed during this run. Shader tint and per-browser/shared Chromium memory remain relevant to 16 GB clients. Ten cycles cannot prove absence of long-duration leaks.
- Phase 3 planning belongs to Claude. Implement authoritative play/pause/seek, target time for late joiners, drift correction and JS lifecycle across navigation/recreated browsers. Coordinate [LEA-195](https://linear.app/leak-rake/issue/LEA-195): watch/youtu.be/shorts to youtube-nocookie embed with start time preserved; ad skip/mute/seek logic with temporary drift exemption and catch-up; optional domain filtering only if needed. These remain design/implementation work, not delivered Phase 2 features. Reassess live-site/allowlist behavior and server validation for every added action.
