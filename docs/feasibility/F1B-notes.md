# F1B feasibility notes

## F1B-1: client GUI (2026-10-30)

Scope: disposable GUI spike under `io.github.devtalh.perde.client.spike`. No world rendering, blocks, packets or configuration. F1B-2 is a separate task.

### Version and API evidence

- Dependency: `net.dimaskama:mcef-modern:0.3.3+mc26.2.jcef146.0.10`, from `https://maven.dimaskama.net/releases`.
- The exact version is listed in [Maven metadata](https://maven.dimaskama.net/releases/net/dimaskama/mcef-modern/maven-metadata.xml). The [published POM](https://maven.dimaskama.net/releases/net/dimaskama/mcef-modern/0.3.3+mc26.2.jcef146.0.10/mcef-modern-0.3.3+mc26.2.jcef146.0.10.pom) and sources jar were downloaded and inspected. The published source uses `com.mojang.blaze3d.textures.GpuTexture/GpuTextureView`, matching [commit df61843](https://github.com/DimasKama/mcef-modern/tree/df61843bfee919c218192335906cc15aff2a9f7d), not 26.3 master.
- JCEF API: `me.friwi:jcef-api:jcef-d3de827+cef-146.0.10+g8219561+chromium-146.0.7680.179`. Its [sources jar](https://repo.maven.apache.org/maven2/me/friwi/jcef-api/jcef-d3de827+cef-146.0.10+g8219561+chromium-146.0.7680.179/jcef-api-jcef-d3de827+cef-146.0.10+g8219561+chromium-146.0.7680.179-sources.jar) confirms `CefBrowser.setWindowlessFrameRate(int)` (1-60 fps) and `getWindowlessFrameRate()`. The spike calls the setter with 30 immediately after browser resize. This is a requested limit, not a measured rendering benchmark.
- Fabric API `0.161.0+26.2` uses `ClientCommands.literal/argument`, not the older `ClientCommandManager` name. Verified with `javap` against the resolved command API jar.
- Minecraft GUI methods were verified with `javap` against Loom's actual 26.2 client jar. `GuiGraphicsExtractor.blit(GpuTextureView, GpuSampler, ...)` submits the same `GUI_TEXTURED` / `TextureSetup.singleTexture` / `BlitRenderState` path used by the [upstream test screen](https://github.com/DimasKama/mcef-modern/blob/df61843/src/testmod/java/net/dimaskama/mcef/testmod/MCEFTestModScreen.java).

### Initialization, rendering and lifetime

- `PerdeClient` checks `FabricLoader.getInstance().isModLoaded("mcef-modern")` before calling the isolated MCEF bridge. If absent, it logs a warning and the dev command reports disabled browser features.
- Client init calls `MCEFApi.initialize()` and observes `getInstanceFuture()` asynchronously. There is no call to blocking `MCEFApi.getInstance()`.
- The Screen displays `Initialization.getStage()` and `getPercentage()` until the future completes. A negative percentage is explicitly shown as unavailable. Initialization failure is logged and displayed instead of retrying every frame.
- The active Screen polls the completed future with `getNow`; browser creation happens only on the client thread. No completion callback retains or creates a browser for a screen that has already closed.
- `createBrowser(url, false)` is followed immediately by `resize(width, height)`, the 30 fps request and focus. Screen resize updates the browser dimensions. For this spike the browser uses GUI pixel dimensions, like upstream; it is not the future fixed-resolution world renderer.
- The nullable `getTextureView()` is checked each frame. The view is drawn with a clamp-to-edge linear sampler over the GUI, with UVs 0-1. Mouse, button release, movement, scroll, key release and character events are forwarded. Browser cursor requests are forwarded too. Esc closes the Screen.
- `removed()` closes and clears the browser and logs `Perde spike: browser closed.` The Screen does not pause the integrated server.
- `/perde-spike gui [url]` is registered only in Fabric's development environment. The default URL is the task's YouTube test video. Supplied URLs must have an HTTP/HTTPS scheme and a host.

### Dedicated-server isolation

- MCEF is a `clientImplementation` dependency. Its JCEF API is additionally `clientCompileOnly` because MCEF publishes it with runtime scope. Nothing is bundled into Perde's jar.
- Upstream declares `environment: client`. Perde uses metadata `suggests`, rather than `depends` or `recommends`: [Fabric's specification](https://wiki.fabricmc.net/documentation:fabric_mod_json_spec) treats `depends` as a hard requirement, `recommends` as a warning, and `suggests` as informational. A client-only optional library should not generate a misleading dedicated-server warning.
- `runtimeClasspath` contains neither MCEF nor JCEF. A dedicated dev server loaded 44 mods, without MCEF, initialized only the common Perde entrypoint and reached Done.

### Verification

Prerequisite: F0-3 is merged in [PR #1](https://github.com/devtalh/perde/pull/1), main commit `9712e2a`. JDK and javac are Temurin 25.0.4. All commands ran from the repository and all Gradle calls for build/game checks used a repository-local `GRADLE_USER_HOME`:

```powershell
$env:GRADLE_USER_HOME = Join-Path (Get-Location) '.gradle-user-home'
.\gradlew.bat build --console=plain --warning-mode all
.\gradlew.bat runServer --console=plain
.\gradlew.bat dependencies --configuration runtimeClasspath --console=plain
.\gradlew.bat runClient --console=plain
```

- First build passed in 20s, with `test NO-SOURCE`; final build passed in 9s with all 7 actionable tasks up-to-date and no Gradle warnings.
- Dedicated server passed, bound to `127.0.0.1:25565` in `run/`, then stopped through its console. Relevant log lines (Europe/Istanbul):

```text
[16:41:45] [main/INFO] (perde) Perde initialized.
[16:41:48] [Server thread/INFO] (Minecraft) Done (0.421s)! For help, type "help"
[16:42:10] [Server thread/INFO] (Minecraft) Stopping the server
[16:42:10] [Server thread/INFO] (Minecraft) Stopping server
BUILD SUCCESSFUL in 37s
```

- No MCEF entry or error appears in the dedicated-server log. The runtime dependency report has no `mcef` / `jcef` match. An initial diagnostic used nonexistent configuration `serverRuntimeClasspath`; the corrected check is `runtimeClasspath`.
- Import search:

```powershell
rg -n '^import .*mcef|^import org\.cef' src/main src/client
```

```text
src/client/java/io/github/devtalh/perde/client/spike/SpikeBrowserScreen.java:7:import net.dimaskama.mcef.api.MCEFApi;
src/client/java/io/github/devtalh/perde/client/spike/SpikeBrowserScreen.java:8:import net.dimaskama.mcef.api.MCEFBrowser;
src/client/java/io/github/devtalh/perde/client/spike/SpikeBrowserClient.java:4:import net.dimaskama.mcef.api.MCEFApi;
```

- Native first-time installation completed at `run/config/mcef-modern/jcef/`; browser data/logs are under `run/config/mcef-modern/cache/`. The first native download/initialization ran asynchronously from 16:42:31 until 16:43:35 while the title screen and local creative world remained usable.
- Codex launched the dev client, entered the existing creative world under `run/saves/`, ran the default command and visually verified the live YouTube page and advancing video. Talha took over interaction and explicitly confirmed all in-game tests worked, including playback with sound, clicking, scrolling, typing and Esc. These interaction/audio results are Talha-confirmed, not an automated audio capture. The log independently confirms browser closure:

```text
[16:43:35] [ForkJoinPool.commonPool-worker-1/INFO] (perde) Perde spike: MCEF initialization completed.
[16:43:47] [Render thread/INFO] (perde) Perde spike: browser created at 1280x720, requested 30 fps.
[16:44:08] [Render thread/INFO] (Minecraft) [System] [CHAT] Saved screenshot as 2026-10-30_16.44.08.png
[16:45:09] [Render thread/INFO] (perde) Perde spike: browser closed.
```

- F2 evidence: [`screenshots/f1b-1-youtube.png`](screenshots/f1b-1-youtube.png), copied byte-for-byte from `run/screenshots/2026-10-30_16.44.08.png`. SHA-256 of both: `f9aa0bd58439ecb72e62d037a57264aa9eb908a0f30dd5f248ced725381fe559`. The player shows 0:02, a pause control and an unmuted speaker.
- No loading screenshot: initialization finished before the command was submitted, so the progress display was not visible during this run. No substitute screenshot was fabricated.
- Talha's physical Esc stopped Computer Use; Codex sent no further game input. The dev client was left under Talha's control. No client crash was observed.

### Surprises and remaining risks

- MCEF's JCEF dependency emits a non-SemVer version warning. Chromium logs Windows registry/extension discovery messages and YouTube media/preload warnings; the video still played. Existing OSHI performance-counter and dev-account Realms/profile-key errors also remain.
- The Gradle client console emitted four bare `Exception in thread "AWT-EventQueue-0"` prefixes around browser startup, without an exception message or stack trace in either latest.log or debug.log. Their cause could not be established from the available logs. No Perde exception, client crash or observed interaction failure accompanied them; retain this as an unresolved upstream/runtime diagnostic, not a clean-client-log claim.
- Upstream MCEF starts Chromium with `--disable-web-security` and an autoplay override. This spike does not change upstream engine flags. The command validates the supplied URL; it does not add a navigation/subresource policy to Chromium. Browser security hardening and production UI are not proven by this spike.
- The missing-MCEF client path and initialization-failure UI were reviewed in code, not exercised by a second runtime launch. Dedicated-server absence was exercised.
- World quad rendering, depth/lighting/back faces and Sodium/Iris compatibility remain untested and belong to F1B-2/F1B-3. This is not the full Phase 1B GO decision.

## F1B-2: fixed world quad (2026-10-30)

Scope: one disposable, client-side screen under `io.github.devtalh.perde.client.spike`, with no blocks, saved world data, packets, configuration or renderer-mod dependencies. F1B-1 was completed and merged in [PR #2](https://github.com/devtalh/perde/pull/2), main commit `77f359679f711d94871beb9b9b6f97eb129b6f1a`; this prerequisite was checked with `gh pr view 2` before implementation. JDK and javac 25.0.4 were available.

### Reproducible source inspection

The implementation was checked against Minecraft **26.2**, Fabric API **0.161.0+26.2**, Fabric Loader **0.19.5**, and MCEF **0.3.3+mc26.2.jcef146.0.10**. These are the existing pinned dependencies, not APIs copied from older Minecraft tutorials or MCEF's 26.3 master branch.

```powershell
$env:GRADLE_USER_HOME = Join-Path (Get-Location) '.gradle-user-home'
.\gradlew.bat genSources --console=plain
Get-ChildItem .gradle/loom-cache/minecraftMaven -Recurse -Filter '*26.2-sources.jar'
```

`genSources` passed in 1m 1s. Inspect the client-only and common source jars returned by the search. The intermediate jar directory includes a Loom-generated hash and should be discovered, rather than hardcoded. Temporary extracted sources used for this task are under ignored `build/research/minecraft/`; they are not distributed with the mod.

| Source | Relevant evidence |
| --- | --- |
| Minecraft client source `net/minecraft/client/resources/MapTextureManager.java`, `MapInstance` constructor | Vanilla registers a dynamic map texture with `TextureManager` under an `Identifier`. |
| Minecraft client source `net/minecraft/client/renderer/MapRenderer.java`, `render` | Vanilla submits the map quad with `submitCustomGeometry`, `RenderTypes.text(mapRenderState.texture)`, UVs, white color and light coordinates. |
| Minecraft client source `net/minecraft/client/renderer/entity/PaintingRenderer.java`, `submit` and `renderPainting` | A painting also submits custom geometry, but uses an entity render type, directional normals, overlay coordinates and per-block lighting. |
| Minecraft client sources `renderer/rendertype/RenderSetup.java`, `prepareTextures`, and `RenderType.java`, `prepare` | Each prepared render type resolves an `Identifier` through `TextureManager` and obtains the current `AbstractTexture.getTextureView()`. There is no public `RenderSetup` overload taking a raw `GpuTextureView`. |
| Minecraft client sources `renderer/RenderPipelines.java`, `TEXT_SNIPPET`, `WORLD_TEXT_SNIPPET`, `TEXT`; `com/mojang/blaze3d/pipeline/DepthStencilState.java` | World text uses a lightmap, alpha blending, default depth testing/writes and quad topology. The default depth comparison is `GREATER_THAN_OR_EQUAL`, matching 26.2's reversed-Z depth buffer. |
| Minecraft client source `com/mojang/blaze3d/pipeline/RenderPipeline.java`, `Builder.build` | Culling defaults to true. The world-text pipeline does not disable it. |
| Minecraft client jar shaders `assets/minecraft/shaders/core/text.vsh` and `text.fsh` | The shader multiplies the supplied texture and vertex color by the sampled lightmap, applies world fog, and discards alpha below 0.1. It does not apply entity directional lighting. |
| Minecraft common source `net/minecraft/util/LightCoordsUtil.java` | `FULL_BRIGHT` is the verified 26.2 light-coordinate constant. |
| Minecraft client source `net/minecraft/client/renderer/LevelRenderer.java`, `submitEntities` and `submitBlockEntities` | Submitted world positions are translated relative to `levelRenderState.cameraRenderState.pos`. |

Fabric's aggregate [0.161.0+26.2 POM](https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/0.161.0+26.2/fabric-api-0.161.0+26.2.pom) resolves rendering module `25.3.3+515ac5339e` and lifecycle module `4.1.4+29b6eb019e`. Their exact published source jars were inspected:

- [fabric-rendering-v1 sources](https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-rendering-v1/25.3.3+515ac5339e/fabric-rendering-v1-25.3.3+515ac5339e-sources.jar): `LevelExtractionEvents`, `LevelRenderEvents`, `LevelRenderContext`, `FabricRenderState`, `RenderStateDataKey`, and the `LevelExtractorMixin` / `LevelRendererMixin` call sites.
- [fabric-lifecycle-events-v1 sources](https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-lifecycle-events-v1/4.1.4+29b6eb019e/fabric-lifecycle-events-v1-4.1.4+29b6eb019e-sources.jar): `ClientLifecycleEvents` and `MinecraftMixin` show when the client has actually started.
- [Fabric Loader 0.19.5 `EntrypointPatch`](https://github.com/FabricMC/fabric-loader/blob/0.19.5/minecraft/src/main/java/net/fabricmc/loader/impl/game/minecraft/patch/EntrypointPatch.java): the client entrypoint runs before the constructor's `Thread.currentThread()` call, before textures and the graphics device are initialized.
- [MCEF 26.2 `MCEFBrowserImpl`](https://github.com/DimasKama/mcef-modern/blob/df61843bfee919c218192335906cc15aff2a9f7d/src/main/java/net/dimaskama/mcef/impl/MCEFBrowserImpl.java): `getTextureView`, `close`, `onPaint`, and `onPaintInternal` define texture ownership and update behavior. The pinned published source jar, already inspected for F1B-1, is the definitive dependency source.

### Rendering method

`SpikeWorldRenderer` registers a plain, non-owning `AbstractTexture` bridge at `perde:spike/browser`. Its getters delegate to the active browser's current `getTextureView()` and use Minecraft's clamp-to-edge linear sampler. It never stores or closes an MCEF-owned GPU texture/view. The bridge remains registered for the client lifetime; after removal it resolves to Minecraft's missing texture, while the world renderer submits no quad. This avoids dangling references when a browser is replaced. A resource reload only reloads `ReloadableTexture` instances, so it does not replace this plain bridge.

The two verified Fabric events are:

1. `net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents.END_EXTRACTION`: attach the immutable `Placement` (center and cardinal direction) to the current `LevelRenderState` using a `RenderStateDataKey`. No `ClientLevel` or MCEF browser object is attached to render state.
2. `net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents.COLLECT_SUBMITS`: fetch the nullable browser view, skip the quad until a frame exists, translate the pose by `center - cameraRenderState.pos`, then call `context.submitNodeCollector().submitCustomGeometry(...)` with `RenderTypes.text(perde:spike/browser)`.

The actual Fabric mixin places `COLLECT_SUBMITS` at the end of `LevelRenderer.submitFeatures`, before feature preparation and frame-graph execution. The submitted texture is resolved again by the normal render-type preparation path on every frame. MCEF's `onPaintInternal` replaces and closes its old texture/view on a size change, which is why caching the first view would be incorrect. This spike does not resize its fixed 1280x720 browser, but still follows that lifetime contract.

The four vertices use white color and `LightCoordsUtil.FULL_BRIGHT`. The pipeline therefore samples maximum block/sky light instead of the screen location's ambient light, and does not apply entity directional shading. Vanilla world fog and lightmap color effects remain. Depth testing and depth writes remain enabled, so terrain between the camera and screen can hide the page. This is the ordinary world-text pipeline, not `textSeeThrough`.

Placement uses the player's nearest horizontal `Direction`, positions the center three blocks along it at the player's eye height, and takes the opposite direction as the front normal. With front normal `n`, the screen-right vector is `(n.z, 0, -n.x)`. Vertex order is bottom-left, bottom-right, top-right, top-left, producing the front normal. UVs are `(0,1)`, `(1,1)`, `(1,0)`, `(0,0)`: the top of the browser is at the top of the quad and page text is not mirrored. The reverse winding is culled, so the back does not display a reversed page. Half-width 2 and half-height 1.125 give exactly 4 x 2.25 blocks, matching 1280x720 at 16:9.

### Browser control and lifetime

`place [url]` uses the existing HTTP/HTTPS URL validation, closes any previous world browser first, records one placement and creates a browser when the existing asynchronous MCEF initialization completes. Creation is polled on the client thread with `getNow`, never a blocking wait. It immediately requests 1280x720 and 30 fps. Invalid URLs leave the existing screen intact.

`play`, `pause` and `seek <seconds>` call `getCefBrowser().executeJavaScript(...)` with an IIFE using `document.querySelector('video')`. No video is a no-op. Synchronous media errors and rejected play promises are caught. Seek rejects non-finite/negative values and clamps to the finite media duration. Command feedback confirms that JavaScript was requested, not that an asynchronous browser operation necessarily succeeded. A site's own player code, autoplay rules or media availability can still affect playback.

`remove`, disconnect and client shutdown close the browser and clear placement/pending creation. The client tick also closes it if the active `ClientLevel` changes or the player disappears, covering world/dimension changes while initialization is pending. The implementation has no persistence or networking. All new classes and imports are in the client spike package.

### Alternatives and implementation finding

- **Implemented and runtime-attempted:** the vanilla dynamic-map texture path, adapted with a non-owning MCEF bridge. No texture readback, per-frame pixel copy, raw OpenGL texture binding or custom shader is added.
- **Source-reviewed only:** painting/entity render types. These need overlay/normal attributes and normally apply directional/per-block lighting, so they add work without helping this fixed full-bright spike.
- **Source-reviewed only:** `RenderPipelines.BEACON_BEAM_OPAQUE` / `RenderTypes.beaconBeam`. The shader is light-independent and depth-tested, but the map path already provides a verified vanilla dynamic-texture route; reusing the beacon classification could also create different Iris shader-pack behavior.
- **Source-reviewed only:** a custom render pipeline/direct GPU pass. It could accept a manually bound view, but introduces unnecessary buffer, target and pipeline lifetime handling. A public `RenderSetup.withTexture` raw-view overload does not exist in this version.
- **Not attempted:** removed pre-26.2 APIs such as old `WorldRenderEvents`, old `RenderType` packages or direct GUI blits in world space.

The first client startup at 17:02:44 failed before gameplay: eager `TextureManager.register(...)` in the client entrypoint encountered a null `Minecraft.getTextureManager()`. The crash was preserved under ignored `build/f1b-2-verification/initial-startup-crash.txt`. Source inspection established the constructor ordering described above. The fix defers both bridge construction and texture registration to `ClientLifecycleEvents.CLIENT_STARTED`; event-handler registration itself stays in the client entrypoint. `AbstractTexture` also initializes a sampler, so merely postponing `register` while constructing the bridge early would not address the full initialization-order issue. No checks or assertions were weakened.

### Renderer-mod risks and remaining limits

Sodium and Iris are deliberately absent from this task. The rendering choice uses Fabric's public level events and vanilla feature submissions, without injecting into Minecraft's renderer. Compatibility still requires those events and the custom-geometry/render-type path to remain supported when Sodium replaces parts of level rendering. Iris may substitute the TEXT pipeline or shader program, affect lightmap/fog/color processing, or route geometry through different framebuffer/depth passes. Shader-pack emission, exposure and back-face behavior require the F1B-3 tests; source inspection alone is not a compatibility result.

The bridge and MCEF remain client-render-thread resources. This is a fixed one-browser development spike, not a production distance/frustum policy or performance benchmark. Existing MCEF runtime/security limitations from F1B-1 remain. No complete Phase 1B GO decision is claimed here.

### Verification

Status: **done** (pending PR review/merge). World rendering, media control and the corrected active-browser disconnect cleanup passed. Talha ran the final disconnect/rejoin test and explicitly confirmed that the spike screen was gone after rejoining; Codex audited the logs without sending game input. No code changed during the completion pass. Sodium/Iris compatibility and the overall Phase 1B GO decision remain F1B-3 work.

All Gradle commands used repository-local `GRADLE_USER_HOME` as above. Final code build:

```text
.\gradlew.bat build --console=plain --warning-mode all
> Task :test NO-SOURCE
BUILD SUCCESSFUL in 9s
7 actionable tasks: 7 up-to-date
```

Dedicated server (`.\gradlew.bat runServer --console=plain`), 44 loaded mods with neither MCEF nor JCEF:

```text
[17:02:01] [main/INFO] (perde) Perde initialized.
[17:02:04] [Server thread/INFO] (Minecraft) Starting Minecraft server on 127.0.0.1:25565
[17:02:04] [Server thread/INFO] (Minecraft) Done (0.348s)! For help, type "help"
[17:02:28] [Server thread/INFO] (Minecraft) Stopping the server
[17:02:28] [Server thread/INFO] (Minecraft) Stopping server
BUILD SUCCESSFUL in 39s
```

Common-code isolation check:

```powershell
rg -n '^import (net\.minecraft\.client|com\.mojang\.blaze3d|net\.dimaskama\.mcef|org\.cef)|mcef|spike' src/main/java
```

```text
(no matches; rg exit code 1)
```

Client observations, performed by Codex through Computer Use in the repository-local creative world:

- Default `/perde-spike place` created the browser at 17:05:28. The center was `(1.622031, 71.62, 29.491290)`, facing north, with the player at `(1.622031, 70, 26.491290)`, yaw 0. The YouTube page and video advanced live, with readable, upright text.
- A stone block at `(1,71,28)` hid the center of the page. The rest of the page remained visible around it. The test block was subsequently removed.
- `/time set night` darkened terrain while the page stayed bright. Vanilla fog/lightmap color effects remain; this is not a claim of exact color matching under all environmental effects.
- From `(1.622031,70,32.491290)`, yaw 180, the same screen's back was invisible, confirming culling rather than a mirrored back page.
- YouTube pause froze the video; play resumed it. The independent JCEF Windows render-session meter (PID 38236) measured 19 samples per phase: autoplay 19/19 nonzero, maximum 0.76579005; pause 0/19 nonzero, maximum 0; play 19/19 nonzero, maximum 0.67057985. [Raw browser-only samples](f1b-2-audio.csv) include UTC timestamps. The probe used Core Audio session enumeration and `IAudioMeterInformation.GetPeakValue`; no audio recording, microphone access or device-setting changes. This demonstrates audio submitted to Windows, not physical headphone audibility.
- The [local media fixture](media-fixture/README.md) uses generated VP8/Vorbis media, starts paused and only reads media properties. `/perde-spike seek 60` showed `time=60.00`, `paused=true`, `muted=false`, `volume=1.00`, while the independent page heartbeat continued advancing. Native controls showed 1:00 / 2:00. Its first server attempt used Python's basic `http.server`, which ignored byte-range requests and caused seek to remain at 0.00. An explicit range server fixed the test source; no change to the mod's seek code was necessary. Prefix, keyframe-offset, suffix, HEAD, unsatisfiable-range and full-file HTTP checks passed.
- On the no-video fixture, play at 17:16:23, pause at 17:16:47 and seek at 17:17:27 left the page intact without an observed error or crash.
- Repeated placement logged close before create (17:11:22, 17:14:42, 17:16:03), with only one world screen. Remove visibly removed it:

```text
[17:17:55] [Render thread/INFO] (perde) Perde spike: world browser closed (removed).
[17:17:55] [Render thread/INFO] (Minecraft) [System] [CHAT] Spike screen removed.
```

Every image below was captured with Minecraft F2, then copied byte-for-byte from `run/screenshots/` without cropping or editing. [Provenance and SHA-256](f1b-2-evidence.csv) records both paths and confirms matching hashes.

| Evidence | Original F2 filename | Result |
| --- | --- | --- |
| [Day](screenshots/f1b-2-day.png) | `2026-10-30_17.06.35.png` | Live YouTube page, correct front orientation |
| [Occlusion](screenshots/f1b-2-occlusion.png) | `2026-10-30_17.08.40.png` | Stone block hides the page behind it |
| [Night](screenshots/f1b-2-night.png) | `2026-10-30_17.10.07.png` | Bright page in a dark world |
| [Back](screenshots/f1b-2-back.png) | `2026-10-30_17.10.50.png` | Reverse face culled; coordinates/turn described above |
| [Seek 60](screenshots/f1b-2-seek-60.png) | `2026-10-30_17.15.11.png` | Readonly media clock at exactly 60.00 seconds |

#### Disconnect finding and completed verification

The first Save and Quit at 17:18:34 returned to the title screen but did not produce a browser-close log. The browser was eventually closed by `CLIENT_STOPPING` at 17:20:28. Inspection of the exact Fabric networking `6.3.4+2989c6a09e` source establishes that `ConnectionMixin.channelInactive` can invoke `ClientPlayNetworkAddon.invokeDisconnectEvent` directly on a Netty thread. MCEF `close()` closes GPU resources; Minecraft's `GlStateManager._deleteTexture` asserts the render thread. The old callback cleared placement before that off-thread cleanup, also preventing the old tick guard from recovering. This is an implementation bug, not a limitation of world rendering.

The callback now uses `client.execute(() -> close("disconnect"))`. The tick fallback checks browser, pending URL and placement, so active resources cannot be skipped merely because placement is absent. The initial corrected build passed in 4s; the completion pass repeated the build successfully in 9s as shown above. Source reference: [exact Fabric networking sources](https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-networking-api-v1/6.3.4+2989c6a09e/fabric-networking-api-v1-6.3.4+2989c6a09e-sources.jar), plus the pinned MCEF source and Loom-generated `GlStateManager.java`.

The corrected client remained available after the earlier accidental Computer Use stop. Talha completed the active-browser test himself. Codex read `run/logs/latest.log` and checked `debug.log`, confirming this sequence on 2026-10-30 (Europe/Istanbul):

```text
[18:12:16] [Render thread/INFO] (perde) Perde spike: world browser created at 1280x720, requested 30 fps.
[18:12:16] [Render thread/INFO] (Minecraft) [System] [CHAT] Spike screen placed (4 x 2.25 blocks, 1280 x 720).
[18:12:33] [Render thread/INFO] (perde) Perde spike: world browser closed (disconnect).
[18:12:33] [Server thread/INFO] (Minecraft) Player921 lost connection: Disconnected
[18:12:41] [Server thread/INFO] (Minecraft) Player921 joined the game
[18:13:05] [Server thread/INFO] (Minecraft) Player921 lost connection: Disconnected
```

The close is explicitly on the **Render thread**, not a Netty/IO thread. There is no new `world browser created` line after the 18:12:41 rejoin, through the later leave and the end of the audited log. Talha explicitly confirmed that the spike screen was completely absent after rejoining. No Perde error/exception or new crash report accompanied the test. The only file in `run/crash-reports/` remains the already documented 17:02:44 startup failure; existing dev-account profile-key HTTP 401 errors are unrelated to Perde.

This completion used the requested read-only path for a test already run by Talha. No game input was sent, no game run was restarted, and no substitute rejoin screenshot was created. The earlier five F2 proofs remain unchanged. No F1B-2 verification remains; next task is **F1B-3 Sodium + Iris test and feasibility report**, after review/merge of PR #3.

Logs are preserved under ignored `build/f1b-2-verification/`: `server.log`, `build.log`, `build-final.log`, `client-before-disconnect-fix.log`, `client-final-disconnect.log`, `initial-startup-crash.txt`. Existing OSHI performance-counter, dev-auth/Realms and Chromium registry/media diagnostics were observed. No additional gameplay crash occurred after the startup fix. No clean-upstream-log claim is made.
