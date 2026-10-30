# Phase 1B feasibility report

## Status and summary

**Partial, 2026-10-30. F1B-3 is not complete and no final GO/NO-GO recommendation is issued yet.** Talha and Claude make the final decision. No later phase has started.

F1B-2 was confirmed merged as PR #3 at main `7ac575a` using both `git log` and `gh pr view 3`. Java/javac 25.0.4 are available. The Notion Perde Project Hub and Linear F1B task/LEA-185 were read before implementation.

The unchanged F1B-2 renderer works with the exact Sodium profile and with Iris without shaders. With Complementary Reimagined r5.9.3, the screen remains live, upright, depth-tested, bright at night and culled from behind. Shader color grading is visible. The last shader-on play check was interrupted before submission, and the full-pack world tests/performance measurements remain pending.

Computer Use explicitly stopped on a physical Escape at approximately 18:55. No further Computer Use calls or game input were sent. Logs subsequently show Talha disconnected at 18:55:40 and quit at 18:55:42. A full-profile client was then launched through Gradle for the manual handoff; it reached the title screen with the expected mods and shader selected, then quit normally at 18:58:04 without joining a world (`BUILD SUCCESSFUL in 1m 51s`). Talha asked to take over the game and continue the remaining work later.

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

Codex performed the completed checks through Computer Use in the same existing creative `run/saves/New World` world. Every screen was placed with `/perde-spike place` without a URL argument, using the default YouTube URL. Each replacement/launch starts with no persisted spike screen.

Front camera: `(1.5, 70, 26.5)`, yaw/pitch `0,0`. Screen center: `(1.5,71.62000000476837,29.5)`, facing north, size 4 x 2.25, browser 1280x720/30 fps requested. Occlusion: stone at `(1,71,28)`. Back camera: `(1.5,70,32.5)`, yaw/pitch `180,0`. Night uses `time set night`.

An ignored, world-local test datapack combines only vanilla scene commands into `perde_test:front`, `night`, `occlusion`, `back`, and `away`. It resets the camera/time, clears weather, freezes time with `gamerule minecraft:advance_time false` and adds/removes the test stone. This is a test fixture in `run/`, not Perde gameplay code or a published mod dependency. Vanilla baseline snapshots predate the frozen-time fixture, so changing clouds/time add noise to those rough numbers.

**PASS** means visually exercised. **Pending** is not a pass. Play/pause were checked by comparing changing/frozen frames across successive observations; command feedback alone was not counted as proof. `seek 60` visibly jumped to the same paused video frame in all three observed configurations. YouTube hides its controls, so these screenshots do not independently display an exact numeric media clock. F1B-2's separate local fixture already demonstrated exact `time=60.00`; no new numeric-readback claim is made here.

| Configuration | Page visible/live | Upright/unmirrored | Block occlusion | Night brightness | Back face culled | Pause | Seek 60 | Play resumes | Log review |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| (a) Sodium + Extra + Reese's | PASS | PASS | PASS | PASS | PASS | PASS | PASS, visible jump | PASS | Reviewed, diagnostics below |
| (b) (a) + Iris, shaders OFF | PASS | PASS | PASS | PASS | PASS | PASS | PASS, visible jump | PASS | Reviewed, diagnostics below |
| (c) (b) + Complementary ON | PASS | PASS | PASS | PASS, tinted | PASS | PASS | PASS, visible jump | **Pending** | Reviewed through normal exit |
| (d) All eight + Complementary ON | **Pending** | **Pending** | **Pending** | **Pending** | **Pending** | **Pending** | **Pending** | **Pending** | Title-screen startup only |

| Configuration | Original F2 evidence copied without editing |
| --- | --- |
| (a) | [day/ads](screenshots/f1b-3-sodium-day.png), [playing video](screenshots/f1b-3-sodium-play.png), [occlusion](screenshots/f1b-3-sodium-occlusion.png), [night](screenshots/f1b-3-sodium-night.png), [back](screenshots/f1b-3-sodium-back.png), [paused after seek](screenshots/f1b-3-sodium-seek.png) |
| (b) | [day](screenshots/f1b-3-iris-off-day.png), [occlusion](screenshots/f1b-3-iris-off-occlusion.png), [night](screenshots/f1b-3-iris-off-night.png), [back](screenshots/f1b-3-iris-off-back.png), [paused after seek](screenshots/f1b-3-iris-off-seek.png) |
| (c) | [day after shader activation](screenshots/f1b-3-iris-on-day.png), [occlusion after fresh shader-on launch](screenshots/f1b-3-iris-on-occlusion.png), [night](screenshots/f1b-3-iris-on-night.png), [back](screenshots/f1b-3-iris-on-back.png), [paused after seek](screenshots/f1b-3-iris-on-seek.png) |
| (d) | Pending |

The [F2 provenance manifest](f1b-3-evidence.csv) records original filenames, copied filenames and identical SHA-256 hashes. Inspection-only crops under `run/` were used to read small F3 text; those crops are not committed or substituted for F2 proof.

Iris OFF was confirmed by `Shaders are disabled because no valid shaderpack is selected`. Shader ON was exercised both by applying the pack in the UI and by restarting `runClient -Pperde.compat=iris` with the pack already selected. Full-profile startup loaded **73 mods**, including the exact eight mods plus Cloth Config, and selected Complementary. World rendering with that combination is still unverified.

## Rough performance and memory snapshot

Windows 11, Temurin Java 25.0.4, Ryzen AI 7 350, RTX 5070 Ti Laptop GPU, NVIDIA 616.56, OpenGL. Minecraft F2 resolution **1920x1200**, GUI scale 1, render distance 16, simulation distance 12, fancy graphics/clouds, FOV option 0.525, VSync off. `maxFps=260` corresponds to the unlimited endpoint here; F3 explicitly reports `T: inf (immediate)`. These are single F3 readings from stationary F2 screenshots after settling, not an averaged benchmark or a causal measurement of renderer cost. Browser/page workload, ads, entities, clouds and background PC work were not controlled.

Toward is yaw 0; away is yaw 180 at the same `(1.5,70,26.5)` position. With no screen, toward means the position where the screen will be placed. One screen uses a playing YouTube page. MCEF is already initialized in both states; no-screen JCEF processes are therefore expected.

| Profile | Screens | F3 FPS toward | F3 FPS away | Java working set, mean MiB | JCEF helpers total working set, mean MiB | Helper count |
| --- | --- | --- | --- | --- | --- | --- |
| Vanilla, no compat mods | 0 | [381](screenshots/f1b-3-vanilla-none-toward.png) | [552](screenshots/f1b-3-vanilla-none-away.png) | 1741.91 | 98.55 | 3 |
| Vanilla, no compat mods | 1 | [427](screenshots/f1b-3-vanilla-one-toward.png) | [397](screenshots/f1b-3-vanilla-one-away.png) | 1810.81 | 727.16 | 8 |
| Full + Complementary | 0 | Pending | Pending | Pending | Pending | Pending |
| Full + Complementary | 1 | Pending | Pending | Pending | Pending | Pending |

Memory method: identify the repo's `java.exe` by the `net.fabricmc.devlaunchinjector.Main` command line and repository path, then identify its direct `jcef_helper.exe` children with `Get-CimInstance Win32_Process`. Sample **`Get-Process -Id ...` / `WorkingSet64`** ten times, 250 ms apart, separately for no screen and one screen; sum all matching helper working sets per sample. Report the arithmetic mean divided by 1,048,576. [Raw samples with UTC, PIDs and byte counts](f1b-3-memory.csv) are committed. This is resident working set, not Java heap or private bytes; shared resident pages can be counted more than once in the helper total. The observed vanilla means differ by +68.90 MiB Java and +628.61 MiB JCEF, without implying a stable per-screen budget.

## Render method for Phase 2

Provisional recommendation: preserve the F1B-2 **vanilla dynamic texture/map submission path**: immutable extracted placement, `LevelRenderEvents.COLLECT_SUBMITS`, `submitCustomGeometry`, `RenderTypes.text`, non-owning live texture bridge, full-bright coordinates, depth testing and back-face culling. It has passed the exercised paths without an Iris/Sodium API dependency. Do not replace it solely to bypass shader post-processing without testing depth, fog, transparency and shader integration again. Final recommendation depends on full-pack verification and Talha/Claude's acceptance of the shader color difference.

No renderer fix or alternate RenderType has been applied. No invisible, black, mirrored, depth-through-block or flickering screen was observed in (a)-(c). Complementary visibly warms/tints and changes contrast of the page. The exact paused seek images allow comparison across profiles. This is not proof of color accuracy or of compatibility with other shader packs/settings.

Source context: the [Iris 26.2 ShaderKey](https://github.com/IrisShaders/Iris/blob/4ce5bb6ddd4b964b2b63442ebfca7e101ff128d0/common/src/main/java/net/irisshaders/iris/pipeline/programs/ShaderKey.java) routes `TEXT` to `EntitiesTrans` with lightmap/fog, and `TEXT_BE` to `BlockTrans`. The downloaded Complementary `shaders/program/gbuffers_entities.glsl` applies `DoLighting` and `DoFog`; later shader composition can further change color. This supports the interpretation that full-bright coordinates do not bypass all shader color processing. It is source-based context, not a measured attribution of every visible color change to one function. The future block-entity context also needs its own retest.

## Open risks

- **Iris/shader behavior:** visible tint/contrast change in (c), no exact color guarantee. Repeat with a real block entity, different times, weather, underwater/fog, resource reloads and representative shader settings in Phase 2. Full/Distant Horizons world behavior remains pending in this task.
- **Entity Culling:** [1.11.2 Config](https://github.com/tr7zw/EntityCulling/blob/ae786c8921de5ea52c06481dfa47180e42bbd54d/EntityCulling-Versionless/src/main/java/dev/tr7zw/entityculling/versionless/Config.java) has `blockEntityWhitelist`, `skipBlockEntityCulling=false`, `blockEntityFrustumCulling=true`. The [dispatcher mixin](https://github.com/tr7zw/EntityCulling/blob/ae786c8921de5ea52c06481dfa47180e42bbd54d/src/main/java/dev/tr7zw/entityculling/mixin/BlockEntityRenderDispatcherMixin.java) checks frustum/AABB and occlusion; it bypasses these for globally rendered entities or `shouldRenderOffScreen()`. The [cull task](https://github.com/tr7zw/EntityCulling/blob/ae786c8921de5ea52c06481dfa47180e42bbd54d/src/main/java/dev/tr7zw/entityculling/CullTask.java) uses a block-entity bounding box. A future screen extending beyond its source block may be visible while the block is hidden/off-camera. This spike has no block entity, so it cannot prove safe bounds or a successful opt-out. Phase 2 must test that geometry and the real registry ID's whitelist/bypass behavior.
- **MoreCulling:** [1.8.1 config](https://github.com/fxmorin/moreculling/blob/f47fd4093ee0822f84369429b41ed0547b0f5359/common/src/main/java/ca/fxco/moreculling/config/MoreCullingConfig.java) exposes `dontCull`, per-namespace `modCompatibility`, `useOnModdedBlocksByDefault`, `useBlockStateCulling`, and `signTextCulling`. [Minecraft_loadBlocksMixin](https://github.com/fxmorin/moreculling/blob/f47fd4093ee0822f84369429b41ed0547b0f5359/common/src/main/java/ca/fxco/moreculling/mixin/blockstates/Minecraft_loadBlocksMixin.java) applies namespace settings and disables block face culling for IDs in `dontCull`. These are block-face opt-outs, not a proven generic block-entity renderer whitelist. Its [mixin list](https://github.com/fxmorin/moreculling/blob/f47fd4093ee0822f84369429b41ed0547b0f5359/common/src/main/resources/moreculling.mixins.json) includes sign-text, map, painting and feature-rendering changes, but does not establish arbitrary oversized Perde block-entity safety. Retain the Phase 2 risk separately from Entity Culling; do not claim either option already fixes it.
- **AWT:** bare prefixes persist during/after shutdown. Vanilla emitted four, Sodium two, first Iris one, fresh shader-on Iris two. Capturing complete merged Gradle stdout/stderr and an attempted default uncaught-exception Java agent did not recover a stack. Cause remains unexplained. A second, repo-local agent targeted AWT event threads directly, with `-Xlog:exceptions=info:file=logs/f1b-3-jvm-exceptions.log:time,level,tags`, in the full-profile title-screen run. That run exited normally at 18:58:04 without a bare AWT prefix, no uncaught capture file was produced, and JVM logging did not capture an AWT shutdown exception. This does not explain the earlier failures or prove an in-world shutdown clean. The probes and outputs remain temporary under `run/`, not compiled into Perde; repeat the targeted capture after the pending full world test.
- **Browser security:** upstream `--disable-web-security` and autoplay overrides remain. Initial URL validation does not enforce navigation/subresource policy. [LEA-185](https://linear.app/leak-rake/issue/LEA-185/perde-tarayici-guvenligi-navigasyon-politikasi-mcef-disable-web) remains a Phase 2 prerequisite; this spike does not close it.
- **Per-client YouTube ads:** multiple different preroll ads were actually observed before the same default video. They delay content differently and can disrupt sync; no multiplayer sync or ad synchronization was implemented/tested.
- **Existing diagnostics:** OSHI performance-counter errors, dev-account Realms/profile-key failures, JCEF's non-SemVer warning, Chromium registry/extension discovery messages, renderer-mod dev refmap warnings. Complementary/Iris additionally warned that `minecraft:stone_slab` has no `variant` property and ignored that mapping. These did not block observed tests. Full startup also emits Distant Horizons/Iris diagnostics; final in-world review is pending. No clean-log claim is made.
- **Scale/lifetime:** only one 1280x720 browser and one 4 x 2.25 plane were exercised. Multiple screens, lazy distance lifecycle, dimensions, actual block-entity bounds, multiplayer and long-duration resource use remain later-phase work.

## Build, server and completion gate

All Gradle commands used `.\gradlew.bat` with repository-local `GRADLE_USER_HOME=.gradle-user-home`. Default build passed initially in 3s and at the handoff in **9s**, 7 actionable tasks all up-to-date, `test NO-SOURCE`. The first pushed commit's [CI run passed](https://github.com/devtalh/perde/actions/runs/41081460229). Final evidence commit CI must still be checked.

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

Raw local logs: ignored `build/f1b-3-verification/` contains default build/server/dependency reports and per-profile client consoles/log snapshots. Sources, shader extraction, test diagnostics and game data remain under ignored `run/`.

**Still required:** finish (c)'s play/resume check; exercise every (d) matrix cell with F2 proof; record (d)'s four stationary FPS readings and two working-set states; finish full-run/AWT log review; replace this partial status with a reasoned GO / NO-GO / GO-with-conditions recommendation; update Current state to the requested Phase 1B completion wording only after all checks pass; inspect final diff/build/CI and make the PR ready for review. The current evidence is insufficient to mark F1B-3 done.
