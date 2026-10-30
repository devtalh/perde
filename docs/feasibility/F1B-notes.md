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
