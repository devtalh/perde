# Perde - Specification

Status: living document. Owner: Talha (devtalh). Lead/reviewer: Claude. Implementer: ChatGPT Codex.
If this file and the code disagree, stop and report it in your PR. Do not silently change decisions listed here.

## 1. What Perde is

A Minecraft Java Edition **Fabric** mod. A single "Perde" block placed in the world creates a resizable screen in front of it. The screen shows a web page (YouTube, Twitch, Kick, w2g.tv, any http/https page). Every player looking at the same screen sees the same page and the same video **in sync** (play/pause/seek propagate to everyone; a player who joins late starts at the correct second).

Inspired by WebDisplays but much simpler: one block, one control panel, no extra items (no keyboard, remote, minePad).

Target: KuzCraft, a small private server (Talha + cousins).

## 2. Target environment

| Item | Value |
| --- | --- |
| Minecraft | **26.2 (confirmed 2026-10-30 from the KuzCraft server log)** |
| Fabric Loader | 0.19.5 on server, 0.20.1 on clients. Build against a loader version both accept (do not require > 0.19.5). |
| Java | 25 |
| Fabric API | Server and clients run `fabric-api-0.161.0+26.2`. Verify this version exists on meta.fabricmc.net / maven.fabricmc.net before using it; if it does not, stop and report. |
| Mappings | 26.x is unobfuscated. Use the official Fabric 26.x template style (`net.fabricmc.fabric-loom` plugin, `implementation` instead of `modImplementation`). |
| Browser engine | MCEF Modern (Modrinth `mcef-modern`, source github.com/DimasKama/mcef-modern, LGPL-2.1), client only |

Server: offline-mode with EasyAuth, ~125 mods based on the "Simply Crafted" pack. Already has C2ME, Lithium, Distant Horizons, Simple Voice Chat.
Clients (exact render mods in the pack, verified in F1B-3): Sodium 0.9.2, Sodium Extra 0.9.4, Reese's Sodium Options 2.2.4, Iris 1.11.4 (shader: Complementary Reimagined), Entity Culling 1.11.2, MoreCulling 1.8.1, Lithium 0.25.3, Distant Horizons 3.3.3. ImmediatelyFast is NOT in the pack. Rendering code must work with these; use `-Pperde.compat=sodium|iris|full` to test against them in dev.
Player hardware: Talha 32 GB RAM, cousins 16 GB RAM.

### 2.1 Verified MCEF Modern facts (read from source on 2026-10-30)

- Maven: `https://maven.dimaskama.net/releases`, artifact `net.dimaskama:mcef-modern:<version>`, or Modrinth maven `maven.modrinth:mcef-modern:<version>`.
- `fabric.mod.json` of MCEF declares `"environment": "client"`.
- Public API (package `net.dimaskama.mcef.api`):
  - `MCEFApi.initialize()` returns `Initialization` (stages: NOT_STARTED, DOWNLOADING, EXTRACTING, INSTALL, INITIALIZING, DONE; `getPercentage()`).
  - `MCEFApi.getInstanceFuture()` (non-blocking, preferred) and `MCEFApi.getInstance()` (blocking, avoid on render thread).
  - `MCEFApi#createBrowser(String url, boolean transparent)` returns `MCEFBrowser`.
  - `MCEFBrowser`: `resize(w,h)` (must be called right after creation), `setFocus`, mouse/key/char input methods, `getTexture()`, `getTextureView()` (nullable), `getCursorType()`, `close()`, `getCefBrowser()` (raw JCEF `CefBrowser`, used for `loadURL`, `executeJavaScript`, frame rate).
- The MCEF test mod only draws the texture in a GUI `Screen` (GUI blit). Drawing it on a quad in the 3D world is NOT demonstrated upstream. That is the main feasibility risk (Phase 1B).
- IMPORTANT version difference: the MC 26.2 build of MCEF uses `com.mojang.blaze3d.textures.GpuTexture/GpuTextureView`; the MC 26.3 build (master) uses `com.mojang.renderpearl.api.textures.*`. **We target 26.2, so use the 26.2 build of MCEF and the `com.mojang.blaze3d.textures` classes.** The 26.2 source is at commit `df61843` of the MCEF repo (master has moved to 26.3, do not copy code from master blindly).
- MCEF stores its natives and browser cache under `<config dir>/mcef-modern/` (`jcef/` and `cache/`). In a Gradle dev run that is inside `run/config/`, so nothing is written outside the repo.
- First launch on a client downloads the Chromium/JCEF natives (~150 MB).

## 3. Architecture (decided)

### 3.1 Client/server split
- MCEF is loaded **only on the client**. The dedicated server must start and run normally with MCEF absent.
- Separate `main` (common/server) and `client` entrypoints. No class that imports MCEF or client-only Minecraft classes may be loaded on the server.
- Perde must not hard-depend on MCEF in a way that breaks the dedicated server. On the client, if MCEF is missing, show a clear message instead of crashing.

### 3.2 Server is authoritative
The server never streams video. It only stores state. Per screen (block entity):

| Field | Meaning |
| --- | --- |
| `url` | current page (http/https only) |
| `playing` | boolean |
| `positionMs` | media position at `serverTimestamp` |
| `serverTimestamp` | server time (ms) when `positionMs` was recorded |
| `volume` | 0..1 base volume |
| `width`, `height` | screen size in blocks (1x1 to 16x9) |
| `offsetX`, `offsetY` | screen offset relative to the block |
| `owner` | UUID of the placer |
| `accessMode` | `OWNER_ONLY`, `SELECTED`, `EVERYONE` |
| `allowedPlayers` | UUID list for `SELECTED` |

State is saved to block entity NBT. Server-to-client state sync uses the vanilla block entity data sync (update tag / block update), so clients that load the chunk later also get the current state. Client-to-server control actions use Fabric custom payloads. (Lead decision 2026-10-31: this replaces "custom payloads for state sync"; same behaviour, fewer moving parts.)
Every control packet is permission-checked on the server. Never trust the client.
Note: the server is offline-mode, so UUIDs are offline UUIDs derived from player names. That is acceptable.

### 3.3 Client rendering
- A browser instance is created lazily when the player is within range of a screen and closed when they leave (lazy create/destroy).
- The Chromium frame is drawn as a texture on a plane in front of the block (BlockEntityRenderer or whatever 26.x requires).
- Browser frame rate limited (default 30 fps). Not rendered and browser closed beyond render distance (default 48 blocks, config).
- Fixed browser resolution independent of screen size (default 1280x720, config).
- Render method (proven in Phase 1B, keep it): vanilla dynamic-texture path, i.e. immutable placement extracted in `LevelExtractionEvents.END_EXTRACTION`, drawn in `LevelRenderEvents.COLLECT_SUBMITS` via `submitCustomGeometry` with `RenderTypes.text`, a non-owning texture bridge to MCEF's live `GpuTextureView`, full-bright light, depth test, back face culled. Shader packs tint it slightly; accepted.
- Memory: one browser costs about 650-750 MB (JCEF helper processes). Default limit: **1 active browser per client** (the nearest screen in range); configurable later for stronger PCs.
- Culling: the screen is larger than its block, so the block entity render bounds must cover the whole screen (Entity Culling / MoreCulling, Linear LEA-187).

### 3.4 Video sync
- Inject JavaScript into the page, find the first/active `<video>` element.
- Target time = `positionMs + (now - serverTimestamp)`. If drift > ~1.5 s, correct `currentTime`. Apply play/pause state.
- Pages without video: only URL sync.

### 3.5 Audio
- Chromium audio volume set via JS (`video.volume`) based on distance: full when close, fading with distance, silent beyond the audio range (default 24 blocks, config).

### 3.6 Security
- Accept only `http` and `https` URLs. Reject `file:`, `javascript:`, `data:` and everything else, on both client and server (server check is the one that matters).
- All control packets pass server-side permission checks.

## 4. Player experience (Talha's decisions)

1. **Block and recipe**: "Perde" block, craftable in survival. Shaped recipe (confirmed by Talha):
   ```
   C C C    C = copper ingot
   G G G    G = glowstone (block)
   R R R    R = redstone (dust)
   ```
2. **Placement**: the block faces the placement direction; the screen appears in front of it. Placer becomes owner.
3. **Control panel** (right-click the block), simple:
   - URL box + "Open" button
   - Play / Pause, seek back / forward
   - Volume slider
   - Size sliders: width and height in blocks (1x1 to 16x9), optional 16:9 ratio lock
   - Position: X/Y offset sliders + "Center" button + "Placement mode" (drag the screen with the mouse, saved on release)
   - Gear icon top-right, visible only to owner and ops: access mode "Only me" / "Selected" (player name list, add/remove) / "Everyone"
   - Players without permission can open the panel but only watch (controls disabled, info text shown)
4. **Interaction mode**: look at the screen and press a key (rebindable; default `Y`, pending Talha's check that Y is free in the pack; must not clash with V, G, B, M, C) to open a fullscreen browser and click/type normally. Exiting returns the screen to the wall. If an authorized player navigates to a new page in this mode, the new URL is propagated to everyone.
5. **Languages**: `tr_tr` and `en_us`.

## 5. Known limits (accepted)
- DRM/paid streams (beIN, TOD, Netflix, etc.) will not work: MCEF has no Widevine. Out of scope.
- Each client runs its own Chromium, so YouTube ads can differ per client and briefly break sync. Accepted for now.
- First client launch downloads ~150 MB.

## 6. Phases (sequential, one at a time)
| Phase | Content |
| --- | --- |
| 0 | Prep: repo, Codex access, Fabric template, CI, README, license, these docs |
| 1B | Feasibility: render an MCEF page on a fixed world quad, also with Sodium + Iris. GO / NO-GO |
| 1A | Skeleton: block, block entity, recipe, control panel (URL + size + position + gear), server state, NBT, permissions, packets. Screen = flat color plane + URL text. Dedicated server boot test |
| 2 | Real browser image on the Perde screen (lazy create/destroy, distance limit, fps limit) |
| 3 | Video sync (JS injection, play/pause/seek, late joiners, drift correction) |
| 4 | Distance-based audio, performance settings, config file |
| 5 | Interaction mode, placement mode (drag), access settings polish, translations |
| 6 | Deploy to KuzCraft (after backup) |
| 7 | Radio (see section 7) |

Phase 1B result (2026-10-31): **GO**, decided by Talha and Claude based on docs/feasibility/F1B-report.md.

## 7. Phase 7: Radio (after all screen phases)
"Radio" works like a Discord music bot but plays into a **Simple Voice Chat group**.
- Server side: Lavaplayer (or its maintained fork, plus youtube-source) fetches audio from YouTube or direct audio links and sends it through the Simple Voice Chat API (`voicechat-api`) ONLY to players in the related voice chat group. One queue/player per group. Players outside the group hear nothing.
- Client side: a simple panel opened with a rebindable key: URL box, queue list, play/pause/skip/back, volume slider, "now playing". Active only while the player is in a voice group.
- Permission: everyone in the group can control it (configurable later).
- Risk: YouTube sometimes blocks datacenter IPs. Direct audio links must always work; on YouTube failure show the player a clear message.
- Command root is `/radyo` (NOT `/music`, which is used by the closed-source "Simple Voice Chat Music Addon" currently on the server; it will be removed once Radio is ready).
- Decided: Radio is a **separate jar `perde-radyo`**. Perde itself never depends on Simple Voice Chat or Lavaplayer.

## 8. Development machine (local Codex on Windows)
- Codex runs locally on Talha's Windows PC (ChatGPT app). Use `gradlew.bat` (or `.\gradlew.bat` in PowerShell).
- Work ONLY inside the perde repository folder. Never read or modify Prism Launcher instances, the `.minecraft` folder, or the KuzCraft server.
- All game test runs use Gradle's `run/` folder (`runClient`, `runServer`) only. `run/` is gitignored.
- If a Java 25 JDK is not available, STOP and tell Talha. Do not install JDKs or anything else system-wide.
- Codex runs the game itself and attaches visual proof: in-game screenshots (F2, saved to `run/screenshots/`) copied into `docs/feasibility/screenshots/` (or the task's docs folder) and referenced in the PR. Talha's manual test is only the final check.
- License: MIT.

## 9. Working rules for every task
- Read `AGENTS.md`, this file and `docs/PROGRESS.md` first. Do not assume you know previous sessions.
- One task = one branch = one PR. Keep tasks small enough to finish in one session.
- Do not mark the task done until `./gradlew build` passes (and the tests the task lists).
- No out-of-scope refactors. No force push, no history rewrite, no deleting data.
- Verify APIs from source (MCEF repo, Fabric API, Minecraft sources via Loom). Do not invent method names.
- Before finishing, update `docs/PROGRESS.md`.
