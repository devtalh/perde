# Phase 2: real Perde screens and pack validation

Status: **partial**, F2-3 validation in progress on `feat/f2-3-validation`. Do not mark Phase 2 complete until the remaining multi-client, final build/server/smoke and CI checks below pass. No merge is authorized.

## Scope and starting state

PR #7 was confirmed merged through GitHub and local main before branching: `bb6f68e`, merged 2026-10-31 19:33:28 Europe/Istanbul. Read AGENTS, SPEC, PROGRESS, the F2-1/F2-2/F1B reports, Notion Project Hub and Linear LEA-194/187/188/195. The older Notion hub differs from current SPEC; repository decisions govern. Linear shared field notes returned `Tool get_agent_context not found`; regular issue tools worked.

| Task | Delivered behavior | Detailed evidence |
| --- | --- | --- |
| F2-1 | Real block-entity browser texture, nearest one active, 48/52-block lifecycle and generation-safe frames | [F2-1 report](F2-1-report.md) |
| F2-2 | Server-managed site allowlist, isolated browser contexts, native privacy settings, validated control packets | [F2-2 report](F2-2-report.md) |
| F2-3 | Real-screen shader/culling/lifecycle validation, two-client state propagation, feasibility spike removal | This report; completion checks pending |

All execution stays inside the repository. Java 25.0.4 and repository-local `.gradle-user-home` are used. No Prism, `.minecraft`, KuzCraft or unrelated Minecraft process is accessed or controlled. **No Computer Use, desktop input, window activation or focus switching is used.** Opt-in fixtures invoke normal Minecraft client/server/panel APIs. Vanilla F2-handler PNGs are copied unchanged to this folder, with [SHA-256 provenance](f2-3-provenance.csv). Images are visually inspected from those files.

The full profile means the exact eight render/performance mods in SPEC 2, plus required Cloth Config and the existing Fabric/MCEF dependencies. This does not claim validation of every other mod in the approximately 125-mod production pack. Complementary Reimagined r5.9.3 is enabled, Iris HIGH, zero changed shader options. Shader/download/cache data stays under `run/`.

The fixture saves Minecraft master volume=0 and adds `--mute-audio` before test Chromium initialization. Media observations additionally set/read video volume=0 and muted=true. These opt-in test changes are not product behavior. The browser registry audit covers every live native CefClient, including private clients still closing.

## Full profile and culling (LEA-187)

Real creative placement, source A `(300,120,300)`, north-facing, 8 x 4.5, Player1 owner. The real server-opened panel supplies URL changes; geometry uses the existing permission-checked payload. Source B is `(320,120,300)`. Scene construction/positioning uses ordinary singleplayer commands.

| Check | Observed result | Unmodified F2 |
| --- | --- | --- |
| Live, upright YouTube | PASS. Native media time 26.126257 to 185.714008, paused=false, readyState=4; subsequent autoplay/ads are retained in evidence | [Day](screenshots/f2-3-full-day.png) |
| Depth occlusion | PASS. Stone at `(300,122,296)` covers the relevant screen area | [Stone](screenshots/f2-3-full-occlusion.png) |
| Night brightness | PASS. Readable live browser while scene is dark, accepted shader tint remains | [Night](screenshots/f2-3-full-night.png) |
| Back face | PASS. Source block remains; browser plane is absent from behind | [Back](screenshots/f2-3-full-back.png) |
| Source outside view | PASS. offsetX=6, rectangle center x=294.5, camera `(294.5,120,298.5)`; visible cropped screen survives three changed camera aims | [1](screenshots/f2-3-culling-offscreen-final-1.png), [2](screenshots/f2-3-culling-offscreen-final-2.png), [3](screenshots/f2-3-culling-offscreen-final-3.png) |
| Source occluded by wall | PASS. Wall `(298,120,298)` through `(303,124,298)` hides the source; offset screen remains live | [Wall](screenshots/f2-3-culling-wall-final.png) |
| Nearest one / switching | PASS. Walk x=302.5 to 318.038 at z=288.5. A YouTube closes before B example.com opens; the other rectangle is inactive, actual native audit=1 | [A active](screenshots/f2-3-two-near-a.png), [B active](screenshots/f2-3-two-near-b.png) |

No renderer fix or mod-specific whitelist/config bypass is needed. Existing `shouldRenderOffScreen()=true` uses Minecraft's global renderer path and bypasses Entity Culling's `isGloballyRendered` / `shouldRenderOffScreen` gates before frustum and source-box occlusion. The renderer's existing camera-distance bounds already include the entire offset rectangle. Entity Culling stays enabled with blockEntityFrustumCulling=true, skipBlockEntityCulling=false, no perde whitelist entry. MoreCulling stays enabled with useBlockStateCulling=true, dontCull empty and modded block culling enabled. Its block-face options are not a generic block-entity bounds API.

Verified against the exact resolved 1.11.2/1.8.1 jars and their matching source copies: [Entity Culling dispatcher](https://github.com/tr7zw/EntityCulling/blob/ae786c8921de5ea52c06481dfa47180e42bbd54d/src/main/java/dev/tr7zw/entityculling/mixin/BlockEntityRenderDispatcherMixin.java), [MoreCulling block compatibility](https://github.com/fxmorin/moreculling/blob/f47fd4093ee0822f84369429b41ed0547b0f5359/common/src/main/java/ca/fxco/moreculling/mixin/blockstates/Minecraft_loadBlocksMixin.java). Culling captures remained visible across repeated camera samples; this finite test is not a guarantee for every shader/camera combination.

Earlier exploratory captures use a farther camera/narrower wall and can show an edge of the source. They are retained in the provenance manifest but are not the accepted offscreen/hidden-source proof. The final captures above correct that scene setup.

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

Pending. Required profile: plain dev clients Player1/Player2, server `run/server`, offline, loopback-only 127.0.0.1:25570, `--nogui` with CREATE_NO_WINDOW. Player2 must be driven exclusively by its own focus-free fixture queue. Both native page results and F2 captures must prove initial YouTube, changed URL, live server-console allowlist remove and restoration.

## Spike removal and final checks

Removed all five `client.spike` classes, `/perde-spike` registration and their dedicated renderer/GUI/lifecycle hooks. MCEF initialization moves into the guarded client-only backend constructor, preserving eager asynchronous startup; lazy engine init remains Phase 4. Keep compatibility profiles and every historical docs fixture. Fixture READMEs identify retired commands and the intentional current loopback restriction. SPEC and AGENTS are unchanged.

Post-removal `.\gradlew.bat build --rerun-tasks --console=plain --warning-mode all`: **BUILD SUCCESSFUL in 10s**, all 9 tasks executed, **197 tests, zero failures/errors/skips**. [Build](f2-3-build.txt), [suite counts](f2-3-tests.csv), [jar/import/language/hash checks](f2-3-checks.txt). The default jar contains no bundled MCEF/JCEF/compat/spike/fixture classes or nested jars; common imports remain isolated and all 57 EN/TR keys match. Default runtimeClasspath was separately resolved without the compat property. SPEC/AGENTS unchanged; Git diff/whitespace checked.

Pending: MCEF-free windowless server boot/console stop, final real-screen client smoke, pushed branch/PR and latest-head CI. Do not count the pre-removal full-profile JVM as the post-removal client smoke.

## Open risks and Phase 3 handoff

- Trusted sites remain a trust boundary. Native WebSocket bypass, DNS rebinding and upstream `--disable-web-security` remain accepted residual risks. No canary repeated, no network-sandbox claim.
- AWT exception types/cause remain unproven; report exact observed counts below at completion. DH shutdown SQL errors belong to the upstream teardown path if reproduced; no suppression or upstream fix in this task.
- YouTube ads/autoplay can differ by client. Ads and automatic navigation were observed during this run. Shader tint and per-browser/shared Chromium memory remain relevant to 16 GB clients. Ten cycles cannot prove absence of long-duration leaks.
- Phase 3 planning belongs to Claude. Implement authoritative play/pause/seek, target time for late joiners, drift correction and JS lifecycle across navigation/recreated browsers. Coordinate [LEA-195](https://linear.app/leak-rake/issue/LEA-195): watch/youtu.be/shorts to youtube-nocookie embed with start time preserved; ad skip/mute/seek logic with temporary drift exemption and catch-up; optional domain filtering only if needed. These remain design/implementation work, not delivered Phase 2 features. Reassess live-site/allowlist behavior and server validation for every added action.
