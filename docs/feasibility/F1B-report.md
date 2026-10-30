# Phase 1B feasibility report

## Status and summary

**F1B-3 verified, 2026-10-30. Recommendation: GO-with-conditions.** The fixed world screen passed all four requested render/media configurations without a renderer change. Talha and Claude make the final GO/NO-GO decision. No later phase has started.

F1B-2 was confirmed merged as PR #3 at main `7ac575a` using both `git log` and `gh pr view 3`. Java/javac 25.0.4 are available. The Notion Perde Project Hub and Linear F1B task/LEA-185 were read before implementation.

The unchanged F1B-2 renderer works with the exact Sodium profile, Iris without shaders, Iris with Complementary Reimagined r5.9.3, and all eight pack mods with that shader. The screen remains live, upright, depth-tested, bright at night and culled from behind. Pause, seek 60 and play resumption passed in each configuration. Shader tint/contrast changes remain visible. The full run also exposed a Distant Horizons SQL error during shutdown; AWT exception prefixes remain unexplained. These are open risks, not clean-log passes.

Computer Use stopped on physical Escape at approximately 18:55; no further UI calls or game input followed until Talha explicitly resumed work. Completion used separate Gradle clients at 23:10-23:35. Talha's existing server-connected Minecraft window was excluded by its returned window ID from every automation target and was still open after testing. No input or close action targeted it. The original full title-screen-only run is not counted as the full world test.

## Exact dependencies and isolation

All eight requested filenames exist on Modrinth for Minecraft **26.2** and Fabric. No replacement versions were used. The [version/hash manifest](f1b-3-versions.csv) records immutable Modrinth version IDs and upstream SHA-512 hashes. Every resolved Maven jar was hashed and matched its corresponding Modrinth primary file, including Cloth Config. Modrinth Maven renames cached files to `project-versionID.jar`; their bytes still match the requested upstream filenames.

| Profile | Modrinth coordinates added to development runtime |
| --- | --- |
| `sodium` | `sodium:xJZxADzI` (0.9.2), `sodium-extra:kGXOsmfx` (0.9.4), `reeses-sodium-options:nnz8j4Hu` (2.2.4) |
| `iris` | Sodium profile plus `iris:gxZWWnKH` (1.11.4) |
| `full` | Iris profile plus `entityculling:RWjup6Jf` (1.11.2), `moreculling:D5oVCouK` (1.8.1), `lithium:f7vZ0VWU` (0.25.3), `distanthorizons:aAow4a9f` (3.3.3), `cloth-config:Nv3xnWXd` (26.2.155) |

The group is `maven.modrinth`. `exclusiveContent` restricts `https://api.modrinth.com/maven` to that group. MoreCulling requires Cloth Config; Entity Culling requires the already-pinned Fabric API; Iris requires the exact pinned Sodium version. Other Modrinth dependencies for the selected releases are optional or already covered. No additional required external mod was found in dependency resolution/startup.

Loom 26.x uses **`localRuntime`**, not the older remapped `modLocalRuntime`. Verified against [LoomConfigurations](https://github.com/FabricMC/fabric-loom/blob/dev/1.17/src/main/java/net/fabricmc/loom/configuration/LoomConfigurations.java), [Constants](https://github.com/FabricMC/fabric-loom/blob/dev/1.17/src/main/java/net/fabricmc/loom/util/Constants.java) and the upstream [publication isolation test](https://github.com/FabricMC/fabric-loom/blob/dev/1.17/src/test/groovy/net/fabricmc/loom/test/integration/LocalRuntimeTest.groovy). Local resolved Loom is 1.17.21. This configuration extends development `runtimeClasspath`, not the published Java runtime variant. With a profile set it also affects development server runs; the dedicated-server isolation test deliberately omits the property. Profiles do not create compile dependencies, `include` entries or Fabric metadata requirements.

Omitting `perde.compat` adds no compatibility mods. Invalid profile values fail configuration instead of silently selecting a different pack. No common/client implementation, CI, `fabric.mod.json`, SPEC or AGENTS file was changed. The jar contains no compat/MCEF/JCEF classes or nested jars. Default `runtimeClasspath` contains neither compat mods nor MCEF/JCEF. Default client MCEF remains the pre-existing optional client dependency.

**SPEC mismatch:** section 2 and the older Notion/Linear text mention ImmediatelyFast. Talha explicitly says it is absent. The eight-mod task list takes precedence for F1B-3; ImmediatelyFast was not added, and SPEC was not edited.

Shader: [Complementary Reimagined r5.9.3](https://modrinth.com/shader/complementary-reimagined/version/Bqen1mJX), the latest release returned for MC 26.2/Iris, published 2026-09-15. Downloaded only to ignored `run/shaderpacks/ComplementaryReimagined_r5.9.3.zip`. Iris logged `Profile: HIGH (+0 options changed by user)` and the selected filename. No shader option was changed. No shader/test binaries are committed.

## Method and matrix

Codex performed all checks through Computer Use in the same existing creative `run/saves/New World` world. Every screen was placed with `/perde-spike place` without a URL argument, using the default YouTube URL. Each launch starts with no persisted spike screen; replacement closes the previous browser. The resumed Iris test reset the camera and replaced an initial off-position screen before its final pause/play proof.

Front camera: `(1.5, 70, 26.5)`, yaw/pitch `0,0`. Screen center: `(1.5,71.62000000476837,29.5)`, facing north, size 4 x 2.25, browser 1280x720/30 fps requested. Occlusion: stone at `(1,71,28)`. Back camera: `(1.5,70,32.5)`, yaw/pitch `180,0`. Night uses `time set night`.

An ignored, world-local test datapack combines only vanilla scene commands into `perde_test:front`, `night`, `occlusion`, `back`, and `away`. It resets the camera/time, clears weather, freezes time with `gamerule minecraft:advance_time false` and adds/removes the test stone. This is a test fixture in `run/`, not Perde gameplay code or a published mod dependency. Vanilla baseline snapshots predate the frozen-time fixture, so changing clouds/time add noise to those rough numbers.

**PASS** means visually exercised. Play/pause were checked by comparing changing/frozen frames across successive observations; command feedback alone was not counted as proof. `seek 60` visibly jumped to the same scene in all four configurations (the exact decoded frame varies slightly). YouTube hides its controls, so these screenshots do not independently display an exact numeric media clock. F1B-2's separate local fixture already demonstrated exact `time=60.00`; no new numeric-readback claim is made here.

| Configuration | Page visible/live | Upright/unmirrored | Block occlusion | Night brightness | Back face culled | Pause | Seek 60 | Play resumes | Log review |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| (a) Sodium + Extra + Reese's | PASS | PASS | PASS | PASS | PASS | PASS | PASS, visible jump | PASS | Reviewed, diagnostics below |
| (b) (a) + Iris, shaders OFF | PASS | PASS | PASS | PASS | PASS | PASS | PASS, visible jump | PASS | Reviewed, diagnostics below |
| (c) (b) + Complementary ON | PASS | PASS | PASS | PASS, tinted | PASS | PASS | PASS, visible jump | PASS | Reviewed; AWT prefixes on exit |
| (d) All eight + Complementary ON | PASS | PASS | PASS | PASS, tinted | PASS | PASS | PASS, visible jump | PASS | Reviewed; DH SQL error and AWT prefixes on exit |

| Configuration | Original F2 evidence copied without editing |
| --- | --- |
| (a) | [day/ads](screenshots/f1b-3-sodium-day.png), [playing video](screenshots/f1b-3-sodium-play.png), [occlusion](screenshots/f1b-3-sodium-occlusion.png), [night](screenshots/f1b-3-sodium-night.png), [back](screenshots/f1b-3-sodium-back.png), [paused after seek](screenshots/f1b-3-sodium-seek.png) |
| (b) | [day](screenshots/f1b-3-iris-off-day.png), [occlusion](screenshots/f1b-3-iris-off-occlusion.png), [night](screenshots/f1b-3-iris-off-night.png), [back](screenshots/f1b-3-iris-off-back.png), [paused after seek](screenshots/f1b-3-iris-off-seek.png) |
| (c) | [day after shader activation](screenshots/f1b-3-iris-on-day.png), [occlusion after fresh shader-on launch](screenshots/f1b-3-iris-on-occlusion.png), [night](screenshots/f1b-3-iris-on-night.png), [back](screenshots/f1b-3-iris-on-back.png), [paused after seek](screenshots/f1b-3-iris-on-seek.png), [resumed play](screenshots/f1b-3-iris-on-play.png) |
| (d) | [day/resumed play](screenshots/f1b-3-full-play.png), [occlusion](screenshots/f1b-3-full-occlusion.png), [night](screenshots/f1b-3-full-night.png), [back](screenshots/f1b-3-full-back.png), [paused after seek](screenshots/f1b-3-full-seek.png), [earlier day/video transition](screenshots/f1b-3-full-day.png) |

The [F2 provenance manifest](f1b-3-evidence.csv) records original filenames, copied filenames and identical SHA-256 hashes. Inspection-only crops under `run/` were used to read small F3 text; those crops are not committed or substituted for F2 proof.

Iris OFF was confirmed by `Shaders are disabled because no valid shaderpack is selected`. Shader ON was exercised both by applying the pack in the UI and by restarting `runClient '-Pperde.compat=iris'` with the pack already selected. The full world run loaded **73 mods**, including the exact eight plus Cloth Config. Iris logged HIGH with zero changed shader options; its DH event handlers bound, and DH logged successful OpenGL renderer setup. DH kept its default renderer mode and 256-chunk LOD radius. No disappearance/flicker was observed with DH active at the test location. This does not test a screen outside vanilla render distance.

## Rough performance and memory snapshot

Windows 11, Temurin Java 25.0.4, Ryzen AI 7 350, RTX 5070 Ti Laptop GPU, NVIDIA 616.56, OpenGL. Minecraft F2 resolution **1920x1200**, fullscreen, GUI scale 1, vanilla render distance 16, simulation distance 12, fancy graphics, FOV option 0.525, VSync off. `maxFps=260` is the unlimited endpoint; F3 reports `T: inf (immediate)`. DH changes cloud/transparency handling and performs background LOD work. The full run used a temporary AWT handler watcher once per second, without verbose JVM exception tracing. Talha's separate AFK Minecraft stayed running during the full measurements, unlike the earlier vanilla baseline. These are single stationary F3 readings, not an averaged benchmark or causal renderer-cost comparison. Ads, autoplay, entities, clouds, LOD generation, JVM warmup and background load were not controlled.

Toward is yaw 0; away is yaw 180 at the same `(1.5,70,26.5)` position. With no screen, toward means the position where the screen will be placed. One screen uses a playing YouTube page. MCEF is already initialized in both states; no-screen JCEF processes are therefore expected.

| Profile | Screens | F3 FPS toward | F3 FPS away | Java working set, mean MiB | JCEF helpers total working set, mean MiB | Helper count |
| --- | --- | --- | --- | --- | --- | --- |
| Vanilla, no compat mods | 0 | [381](screenshots/f1b-3-vanilla-none-toward.png) | [552](screenshots/f1b-3-vanilla-none-away.png) | 1741.91 | 98.55 | 3 |
| Vanilla, no compat mods | 1 | [427](screenshots/f1b-3-vanilla-one-toward.png) | [397](screenshots/f1b-3-vanilla-one-away.png) | 1810.81 | 727.16 | 8 |
| Full + Complementary | 0 | [132](screenshots/f1b-3-full-none-toward.png) | [98](screenshots/f1b-3-full-none-away.png) | 3208.00 | 98.93 | 3 |
| Full + Complementary | 1 | [114](screenshots/f1b-3-full-one-toward.png) | [116](screenshots/f1b-3-full-one-away.png) | 3925.47 | 754.27 | 7 |

Memory method: identify the repo's `java.exe` by the `net.fabricmc.devlaunchinjector.Main` command line and repository path, then identify its direct `jcef_helper.exe` children with `Get-CimInstance Win32_Process`. Sample **`Get-Process -Id ...` / `WorkingSet64`** ten times, 250 ms apart, separately for no screen and one screen; sum all matching helper working sets per sample. Report the arithmetic mean divided by 1,048,576. [Raw samples with UTC, PIDs and byte counts](f1b-3-memory.csv) are committed. This is resident working set, not Java heap or private bytes; shared resident pages can be counted more than once in the helper total. The observed vanilla means differ by +68.90 MiB Java and +628.61 MiB JCEF, without implying a stable per-screen budget.

Full memory blocks began at 23:17:29 and 23:26:00 local time, roughly 8.5 minutes apart. The one-screen block overlaps the default video's end/autoplay transition; no autoplay setting was changed. The default URL was subsequently placed again for pause/seek/play and the toward FPS screenshot. The page kept running during away FPS capture. Full means differ by +717.47 MiB Java and +655.34 MiB helpers, but DH/background generation and elapsed runtime prevent attributing those differences solely to one browser. The other AFK game's processes are excluded from every reported working set.

## Render method for Phase 2

Recommendation: preserve the F1B-2 **vanilla dynamic texture/map submission path**: immutable extracted placement, `LevelRenderEvents.COLLECT_SUBMITS`, `submitCustomGeometry`, `RenderTypes.text`, non-owning live texture bridge, full-bright coordinates, depth testing and back-face culling. All four requested configurations passed without an Iris/Sodium API dependency. Do not replace it solely to bypass shader post-processing without testing depth, fog, transparency and shader integration again. Talha and Claude must accept the demonstrated shader color difference or request a separate color-correction investigation.

No renderer fix or alternate RenderType was necessary or applied. No invisible, black whole-page, mirrored, depth-through-block or flickering screen was observed in (a)-(d). The video area briefly goes black at YouTube transitions while the page UI stays rendered; the earlier full day screenshot retains that transition rather than hiding it. Complementary visibly warms/tints and changes page contrast, but the exercised day/night screen remains readable. Paused seek images allow scene comparison across profiles. This is not proof of color accuracy or other shader packs/settings.

Source context: the [Iris 26.2 ShaderKey](https://github.com/IrisShaders/Iris/blob/4ce5bb6ddd4b964b2b63442ebfca7e101ff128d0/common/src/main/java/net/irisshaders/iris/pipeline/programs/ShaderKey.java) routes `TEXT` to `EntitiesTrans` with lightmap/fog, and `TEXT_BE` to `BlockTrans`. The downloaded Complementary `shaders/program/gbuffers_entities.glsl` applies `DoLighting` and `DoFog`; later shader composition can further change color. This supports the interpretation that full-bright coordinates do not bypass all shader color processing. It is source-based context, not a measured attribution of every visible color change to one function. The future block-entity context also needs its own retest.

## Open risks

- **Iris/shader behavior:** visible tint/contrast change in (c)/(d), no exact color guarantee. Repeat with a real block entity, different times, weather, underwater/fog, resource reloads and representative settings in Phase 2. Full/DH passed at the fixed scene; distant-only screens and long-distance transitions remain untested.
- **Entity Culling:** [1.11.2 Config](https://github.com/tr7zw/EntityCulling/blob/ae786c8921de5ea52c06481dfa47180e42bbd54d/EntityCulling-Versionless/src/main/java/dev/tr7zw/entityculling/versionless/Config.java) has `blockEntityWhitelist`, `skipBlockEntityCulling=false`, `blockEntityFrustumCulling=true`. The [dispatcher mixin](https://github.com/tr7zw/EntityCulling/blob/ae786c8921de5ea52c06481dfa47180e42bbd54d/src/main/java/dev/tr7zw/entityculling/mixin/BlockEntityRenderDispatcherMixin.java) checks frustum/AABB and occlusion; it bypasses these for globally rendered entities or `shouldRenderOffScreen()`. The [cull task](https://github.com/tr7zw/EntityCulling/blob/ae786c8921de5ea52c06481dfa47180e42bbd54d/src/main/java/dev/tr7zw/entityculling/CullTask.java) uses a block-entity bounding box. A future screen extending beyond its source block may be visible while the block is hidden/off-camera. This spike has no block entity, so it cannot prove safe bounds or a successful opt-out. Phase 2 must test that geometry and the real registry ID's whitelist/bypass behavior.
- **MoreCulling:** [1.8.1 config](https://github.com/fxmorin/moreculling/blob/f47fd4093ee0822f84369429b41ed0547b0f5359/common/src/main/java/ca/fxco/moreculling/config/MoreCullingConfig.java) exposes `dontCull`, per-namespace `modCompatibility`, `useOnModdedBlocksByDefault`, `useBlockStateCulling`, and `signTextCulling`. [Minecraft_loadBlocksMixin](https://github.com/fxmorin/moreculling/blob/f47fd4093ee0822f84369429b41ed0547b0f5359/common/src/main/java/ca/fxco/moreculling/mixin/blockstates/Minecraft_loadBlocksMixin.java) applies namespace settings and disables block face culling for IDs in `dontCull`. These are block-face opt-outs, not a proven generic block-entity renderer whitelist. Its [mixin list](https://github.com/fxmorin/moreculling/blob/f47fd4093ee0822f84369429b41ed0547b0f5359/common/src/main/resources/moreculling.mixins.json) includes sign-text, map, painting and feature-rendering changes, but does not establish arbitrary oversized Perde block-entity safety. Retain the Phase 2 risk separately from Entity Culling; do not claim either option already fixes it.
- **AWT:** bare prefixes persist after successful Gradle exits. Vanilla emitted four, Sodium two, first Iris one, fresh shader-on Iris two, resumed Iris three and the full world run three. Complete merged stdout/stderr, a default uncaught-handler agent, a targeted AWT event-thread handler watcher, and JVM exception logging on the title-only run did not recover a stack. The full world run repeated the targeted watcher without verbose JVM tracing; no uncaught capture file was produced. **The exception types and cause remain unexplained.** See [retained log excerpts](f1b-3-diagnostics.md). Probes remain temporary under `run/`, not in Perde. Do not label this harmless or resolved.
- **Distant Horizons shutdown:** at 23:35:05, its regen queue logged `java.sql.SQLException: stmt pointer is closed` in `FullDataSourceV2Repo.getChildPositionsToRegen`, through `FullDataUpdatePropagatorV2.queueRegeneration`, during concurrent DH level/database shutdown. The full stack is retained in [diagnostics](f1b-3-diagnostics.md); no Perde frame appears. This identifies a closed DH SQLite statement during teardown, but the exact upstream race and data-integrity impact are not proven. Worlds/chunks finished saving and Gradle exited successfully. No product fix, suppression, database deletion or mod substitution was attempted. Follow up with a controlled upstream reproduction before claiming robust full-pack lifetime behavior.
- **Browser security:** upstream `--disable-web-security` and autoplay overrides remain. Initial URL validation does not enforce navigation/subresource policy. [LEA-185](https://linear.app/leak-rake/issue/LEA-185/perde-tarayici-guvenligi-navigasyon-politikasi-mcef-disable-web) remains a Phase 2 prerequisite; this spike does not close it.
- **Per-client YouTube ads/autoplay:** multiple different ads were actually observed before the same default video. The full run also auto-advanced to another video after the default video ended; the default URL was placed again before media-control verification. These behaviors can disrupt future sync. No multiplayer/ad synchronization or navigation policy was implemented/tested.
- **Existing diagnostics:** OSHI performance-counter errors, dev-account Realms/profile-key failures, JCEF non-SemVer warning, Chromium registry/extension discovery messages, renderer-mod dev refmap warnings. Iris ignores a `minecraft:stone_slab` mapping with a nonexistent `variant` property. Full adds DH's high vanilla render-distance/G1 advice, deprecated DH API caller-name warnings from Iris and shader stage-type warnings. These did not block the observed checks. No clean-log claim is made.
- **Scale/lifetime:** only one 1280x720 browser and one 4 x 2.25 plane were exercised. Multiple screens, lazy distance lifecycle, dimensions, actual block-entity bounds, multiplayer and long-duration resource use remain later-phase work.

## Build and server verification

All Gradle commands used `.\gradlew.bat` with repository-local `GRADLE_USER_HOME=.gradle-user-home`. Default build passed initially in 3s, at handoff in 9s, and after all game tests in **2s**, 7 tasks up-to-date, `test NO-SOURCE`. Default runtimeClasspath was resolved again successfully and contains no compat/MCEF/JCEF artifacts; jar entries and unchanged metadata confirm no required/bundled compat mods. [PR #4](https://github.com/devtalh/perde/pull/4) contains this report and all evidence; its checks record CI status.

Dedicated server, default property omitted, 44 loaded mods, no MCEF/JCEF or compatibility mods:

```text
[18:29:54] [main/INFO] (perde) Perde initialized.
[18:29:56] [Server thread/INFO] (Minecraft) Starting Minecraft server on 127.0.0.1:25565
[18:29:56] [Server thread/INFO] (Minecraft) Done (0.431s)! For help, type "help"
[18:30:25] [Server thread/INFO] (Minecraft) Stopping the server
[18:30:25] [Server thread/INFO] (Minecraft) Stopping server
BUILD SUCCESSFUL in 47s
5 actionable tasks: 1 executed, 4 up-to-date
```

Completion repeated the default server **without diagnostic agents** and with an interactive console, again loading 44 mods without MCEF/compat:

```text
[23:44:47] [main/INFO] (perde) Perde initialized.
[23:44:49] [Server thread/INFO] (Minecraft) Starting Minecraft server on 127.0.0.1:25565
[23:44:50] [Server thread/INFO] (Minecraft) Done (0.324s)! For help, type "help"
[23:46:34] [Server thread/INFO] (Minecraft) Stopping the server
[23:46:35] [Server thread/INFO] (Minecraft) Stopping server
[23:46:35] [Server thread/INFO] (Minecraft) ThreadedAnvilChunkStorage: All dimensions are saved
BUILD SUCCESSFUL in 1m 58s
5 actionable tasks: 1 executed, 4 up-to-date
```

An intervening noninteractive server run also reached Done, but its stdin was closed. A temporary, PID-guarded diagnostic invoked that repository server's existing `Server Shutdown Thread` (`halt(true)`, verified in Loom's MC 26.2 `Main.java`) and saved/stopped it normally. Two earlier probe guards rejected unavailable DLI runtime identity information and produced probe exceptions; they are not application failures. The final agents-free console run above verifies normal boot/stop independently. No other Minecraft process was attached to or stopped. Use an interactive terminal for future `runServer` tests.

Raw local logs: ignored `build/f1b-3-verification/` contains default build/server/dependency reports and per-profile client consoles/log snapshots. Sources, shader extraction, test diagnostics and game data remain under ignored `run/`.

The resumed Iris run exited successfully in 5m 28s; full world run in 19m 32s. All four matrix configurations were actually launched and visually exercised by Codex, with **31 unmodified F2 copies** and **40 raw memory samples** recorded. No renderer code changed, so no post-fix matrix retest was needed.

## Recommendation and decision conditions

**GO-with-conditions for Phase 1B feasibility.** The central technical question is answered: this fixed, live MCEF world plane works with all requested render configurations, including Complementary and Distant Horizons. Default jar/server isolation also holds. An unconditional GO would overstate what a one-plane spike proves.

Before treating the future implementation as ready for the pack:

1. Talha and Claude review the shader-on screenshots and accept the visible tint/contrast behavior or commission a focused color-accuracy investigation.
2. Test real block-entity bounds and Entity Culling/MoreCulling opt-outs in Phase 2, and resolve navigation/browser security under LEA-185 before exposing arbitrary user-controlled browsing.
3. Track the unexplained AWT exceptions and DH teardown SQL failure with reproducible upstream diagnostics; validate repeated world exits/reloads, multiple-screen memory/lifetime and YouTube ads/autoplay rather than inferring production reliability from this spike.

This recommendation is advisory. **Final decision: pending Talha and Claude.** No Phase 1A/2 implementation is included or authorized by this report.
