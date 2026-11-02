# CODEC-1 report - CEF 146 with proprietary codecs (H.264/AAC) for MCEF

Linear LEA-200. Branch `feat/codec-build` from main `35b22fb`. Claude Code, 2026-11-01/02 (overnight, paused 09:25-18:20 and 20:28-23:21 by Talha).
Status: **done, pending Talha's review and manual check.** Not merged.

## Summary

- Built CEF **146.0.10+g8219561+chromium-146.0.7680.179** Windows x64 Release (the exact version MCEF uses) with the official GN args plus `proprietary_codecs=true ffmpeg_branding=Chrome`.
- Swapping only the shared CEF runtime files into the dev `run/` MCEF folder works: MCEF initializes, **H.264 (avc1.42E01E, avc1.640028) and AAC (mp4a.40.2) now report supported**, and Twitch live, Kick live, Dailymotion, diziwatch and Dizilla episodes play (`readyState 4`, time advancing). YouTube still plays and syncs (median drift 58.1 ms).
- Not played: Vimeo showed a Cloudflare bot challenge (not touched). Dizipal's episode embed returned "montana video alinamadi" (the site's provider gave no video). Neither result says anything about codecs.
- MCEF does not overwrite the files on restart. Restoring the backup brings back the original behaviour (H.264/AAC false). `run/` is left **codec-enabled**; `tools\codec-pack\Install-PerdeCodecs.ps1 -Instance run -Restore` reverts it.
- The optional Perde H.264 status log was **not** done (no product code changed in this task).

> Codec-enabled binaries are private (patent-encumbered H.264/AAC). They live only in `D:\cef` and the repository's gitignored `run/` folder. They were never committed, pushed, uploaded or attached.

## 1. Versions

| Item | Value |
| --- | --- |
| MCEF | `0.3.3+mc26.2.jcef146.0.10` (gradle.properties) |
| jcef / native bundle | `jcef-d3de827+cef-146.0.10+g8219561+chromium-146.0.7680.179`, jcefbuild release 1.0.70, windows-amd64 |
| CEF | branch **7680**, commit `82195616d8405e6081a0d90924707b82aa9e4141` ("Update to Chromium version 146.0.7680.179") |
| Chromium | `refs/tags/146.0.7680.179` (no-history checkout) |
| depot_tools | cloned into `D:\cef\depot_tools` at task start, self-update disabled after bootstrap |

## 2. Prerequisites (this PC)

VS 2022 Build Tools 17.14.44 (MSVC 14.44.35207, ATL/MFC), Windows SDK 10.0.26100 (tools 10.0.26100.7705), Debugging Tools for Windows (dbghelp 10.0.26100.7705), Python 3.12 (only to start automate-git.py; depot_tools bundles Python 3.11.8), Git 2.53. CEF requires VS 17.13.4 and SDK 10.0.26100.4654 for branch 7680; both installed versions are newer. Ryzen AI 7 350 (16 threads), 31.3 GB RAM, D: 322 GB free at start. Nothing installed or changed system-wide.

## 3. GN args

`GN_DEFINES=is_official_build=true proprietary_codecs=true ffmpeg_branding=Chrome`, `--x64-build --no-debug-build --with-pgo-profiles --no-chromium-history`. Generated [args.gn](args-Release_GN_x64.gn):

```
alternate_cdm_storage_id_key="968b476909da4373b08903c28e859454"
clang_use_chrome_plugins=false
disable_fieldtrial_testing_config=true
enable_background_mode=false
enable_cdm_host_verification=true
enable_cdm_storage_id=true
enable_downgrade_processing=false
enable_resource_allowlist_generation=false
enable_rlz=true
enable_widevine=true
ffmpeg_branding="Chrome"
is_component_build=false
is_debug=false
is_official_build=true
optimize_webui=true
proprietary_codecs=true
target_cpu="x64"
```

Packaging deviations from the official command (do not affect libcef): `--build-target=cefsimple`, `--minimal-distrib-only`, no archive, no docs.

## 4. Local adaptations (all documented, all reversible)

| Issue | Cause | Fix |
| --- | --- | --- |
| Chromium sync on Windows needs `core.autocrlf=false` | System Git config has `autocrlf=true` | Workspace-only `D:\cef\home\.gitconfig` via `HOME`/`USERPROFILE` redirect in [cef-env.ps1](../../tools/codec-build/cef-env.ps1) |
| Overnight network failure left 26 dependency repos initialised but empty; later syncs skipped them, hooks failed (missing v8 files) | Interrupted shallow fetches | [repair-empty-deps.ps1](../../tools/codec-build/repair-empty-deps.ps1) fetches exact DEPS revisions; afterwards all 224 git deps verified at their DEPS revision |
| `gn gen`: "Visual Studio Version 2022 not found" | `vs_toolchain.py` only probes `%ProgramFiles%` for VS 2022; Build Tools live in Program Files (x86) | Documented `vs2022_install` override, set from vswhere for the build process only |
| Build stopped at the MIDL consistency check | Local SDK 10.0.26100.7705 `midl.exe` emits 10-byte different `.tlb` files than Chromium's references (made with 10.0.26100.4654) | 4 `.tlb` files rebaselined as midl.py instructs (all text outputs identical). Updater/elevated-tracing COM typelibs only, not media code. Hashes: [midl-rebaseline.txt](midl-rebaseline.txt) |

## 5. Timeline and disk

| Step | Time |
| --- | --- |
| Stage 0 (plan, inventory, prerequisites) | 00:55-01:03 |
| Initial sync (~17 GB at ~8 MB/s) | 01:05-~01:40 |
| Network shaping/outage (0-100 KB/s) | ~01:40-09:10 |
| Sync completed | 09:19 |
| Paused by Talha | 09:25-18:20 |
| Hooks, dep repair, sources stage | 18:21-18:53 |
| Build attempt 1 (gn: VS not found) | 18:54 |
| Build attempt 2 (stopped at MIDL check, 6413 steps kept) | 18:57-19:05 |
| Build attempt 3, part 1 | 19:08-20:28 (paused by Talha at 21461/57082) |
| Build attempt 3, part 2 (incremental) + minimal distribution | 23:21-03:36 (256 min) |
| Stage 3 tests (install, 8 sites, restart, restore) | 03:37-04:07 |

Total build time: about **5.8 h** of compile/link on 16 threads (CPU at 100%, ~65-470 steps/min, final `libcef.dll` ThinLTO/PGO link about 8 min with lld-link at ~10.6 GB private memory). Free RAM dropped to ~0.8 GB during the link but nothing failed; the `symbol_level=1` fallback was not needed.

Disk (D: free 300.2 GiB at start, 207.8 GiB now): workspace **~92 GiB**.

| Part of `D:\cef` | Size | Can be deleted? |
| --- | ---: | --- |
| `build\chromium\src\out\Release_GN_x64` | 49.6 GiB (obj\ 19.1 GiB) | Yes, once the pack zip exists. Only needed for incremental rebuilds of this version. |
| Chromium/CEF sources (`build\chromium\src` minus out, `build\cef`, depot_tools) | ~30 GiB (estimate) | Only needed to rebuild 146.0.10. A new CEF version needs a fresh checkout anyway. |
| `build\chromium\src\cef\binary_distrib` | 5.5 GiB (most of it the `_release_symbols` PDBs) | `_release_symbols` can go unless crash debugging is planned. Keep `_minimal` or the pack. |
| `build\chromium\src\out\Debug_GN_x64` | 1.5 GiB | Yes (generated by gn, never built). |
| `cache\` (vpython, CIPD) | 1.7 GiB | Yes (re-downloadable). |
| `mock\` (script tests with stock files) | 1.1 GiB | Yes. |
| `build\chromium\_bad_scm\` | 0.65 GiB | Yes (partial repos gclient moved aside). |
| `tmp\` | 0.6 GiB | Yes. |
| `pack\` (private codec zip, 0.16 GiB) | 0.16 GiB | **Keep** (this is what gets installed). |

Nothing was deleted by this task.

## 6. Replaced files (run/ only)

Installed with `tools\codec-pack\Install-PerdeCodecs.ps1 -Instance run -Pack D:\cef\pack\perde-codecs-cef-146.0.10+g8219561+chromium-146.0.7680.179-windows-amd64.zip` (pack SHA-256 `562636aa981ce7623b17227044b2a3db8b04be29d913fb547634e1c88efb9102`, 235 files). Backup: `run/config/mcef-modern/jcef-original-backup` (241 files with SHA-256 inventory).

233 files present in both trees were replaced: 13 top-level runtime files plus 220 `locales/*.pak`. Full list with old/new SHA-256: [replaced-files.csv](replaced-files.csv). Content actually changed in 8 files:

| File | Stock size | New size |
| --- | ---: | ---: |
| libcef.dll | 262272512 | 264743936 |
| chrome_elf.dll | 2491904 | 2490880 |
| resources.pak | 18700718 | 18701216 |
| d3dcompiler_47.dll | 4741480 | 4741488 |
| libEGL.dll | 506368 | 506368 |
| libGLESv2.dll | 7824896 | 7824384 |
| vk_swiftshader.dll | 5443072 | 5443072 |
| vulkan-1.dll | 939008 | 939008 |

`icudtl.dat`, `v8_context_snapshot.bin`, `chrome_100/200_percent.pak` and all 220 locale paks are byte-identical to stock, which confirms the same source revision. Not touched: `jcef.dll`, `jcef_helper.exe`, `build_meta.json`, `install.lock`, license/README files. The distribution's extra files (`dxcompiler.dll`, `dxil.dll`, `bootstrap*.exe`) were not added, because MCEF's folder does not have them.

## 7. Test matrix (Perde dev client, run/ only)

Method: focus-free fixture (`docs/phase2/fixture`, Chromium `--mute-audio`, Minecraft master 0, media muted), single-player world, Perde screen at (300,120,300), URLs set through the real panel. Media state comes from F3-2's metadata-only [codec-observe.js](../phase3/fixture/codec-observe.js) in every frame. Only the page's normal controls were used; no login, paywall, consent, age gate or bot check was bypassed. diziwatch.app, dizilla.now and dizipalorijinal20.com were added only to the run/ allowlist with `/perde wl add` and removed afterwards (verified back to the 9 previous domains). Raw lines: [stage3-results.txt](stage3-results.txt). Screenshots: [screenshots/](screenshots/) ([provenance](screenshots/provenance.csv)).

Codec capability (live page, codec build): H.264 Baseline `avc1.42E01E`, H.264 High `avc1.640028`, AAC-LC `mp4a.40.2`, H.264+AAC: `MediaSource.isTypeSupported` **true**, `canPlayType` **"probably"**. HLS mime: false / "maybe". VP9, AV1, Opus: true / "probably". Stock (F3-2, and again after restore): H.264/AAC false / "".

| Site / URL | Control used | Result with codec build | Before (stock MCEF, F3-2) | Helpers / mean helper WS | Screenshot |
| --- | --- | --- | --- | --- | --- |
| YouTube `dQw4w9WgXcQ` | Panel Play, then panel Go 0:30 | **Plays**: 60/60 samples readyState 4, advancing. Drift ~0.99 s constant after Play (the video had autoplayed to 1.15 s before the server Play at 0 ms, which is under the 1.5 s correction threshold). After Go 0:30: 30 samples, **median 58.1 ms, max 59.4 ms** | Played, median ~57 ms | 6 / 719.6 MiB | [youtube](screenshots/youtube.png) |
| Twitch live `pgl` (picked live from the directory) | none (autoplay) | **Plays**: main player MSE, readyState 4, 63 advancing samples, 20.8 s → 36.6 s, no error | Error #4000 | 9 / 1019.2 MiB | [twitch](screenshots/twitch-pgl.png) |
| Kick live `chessbrah` (Chess category, no age gate) | none (autoplay) | **Plays**: readyState 4, 63 advancing samples, 19.1 s → 34.8 s, no error. Cookie banner not touched | DEMUXER_ERROR_NO_SUPPORTED_STREAMS | 11 / 1172.2 MiB | [kick](screenshots/kick-chessbrah.png) |
| diziwatch `horror-collector/sezon-1/bolum-4` | Oynat → site pre-roll (16 s, played to the end) → page's "Reklamı geç" → frame np-play | **Episode plays** in `four.pichive.online`: readyState 4, 91 advancing samples, duration 1503 s. Note: the site kept its ad box drawn over the player iframe, so the screenshot shows the ad overlay, not the episode picture | ready 0, never advanced | 8 / 967.1 MiB | [diziwatch](screenshots/diziwatch.png) |
| Dizilla `carrie-1-sezon-1-bolum` | Bölümü başlat → pre-roll → frame np-play | **Episode plays**: readyState 4, 98 advancing samples, duration 3311 s; episode picture visible | ready 0, never advanced | 9 / 1067.8 MiB | [dizilla](screenshots/dizilla.png) |
| Dizipal `mobland-2-sezon-7-bolum` | bp-play-btn | Inline MP4 previews now load and play (readyState 4, advancing). Episode: embed frame says **"montana video alinamadi"**, no video element, no alternative source button. Site/provider failure, **no codec conclusion** | Inline MP4 error 4 DEMUXER_ERROR_NO_SUPPORTED_STREAMS | 9 / 1027.7 MiB | [dizipal](screenshots/dizipal.png) |
| Vimeo `76979871` | none | **Cloudflare challenge** frame instead of the player (not interacted with). No codec conclusion | Did not advance, player error | not measured | [vimeo](screenshots/vimeo-cloudflare.png) |
| Dailymotion `xbsj7ry` | none (autoplay) | **Plays**: `geo.dailymotion.com` player readyState 4, 63 advancing samples, 23.6 s → 39.3 s, HLS/MP4 requests | "Browser not compatible" | 17 / 1749.9 MiB | [dailymotion](screenshots/dailymotion.png) |
| Restart | quit + relaunch client | Installer status: 233 hashes still OK; MCEF init OK; H.264/AAC probe still true. MCEF did not re-download | | | |
| Restore | `Install-PerdeCodecs.ps1 -Restore` | 241/241 files equal the Stage 0 inventory; relaunch: H.264/AAC false/"", VP9 true (original behaviour). Codec files then re-installed | | | |

Memory method as F2-1 ([Measure-Browsers.ps1](../phase2/fixture/Measure-Browsers.ps1)): ten samples per stage, 250 ms apart, working set of the verified client's `jcef_helper.exe` children under `run/config/mcef-modern/` ([memory.csv](memory.csv)). One browser was active the whole time; helper processes are shared and kept between pages, and pages with many ad iframes (Dailymotion) start many site-isolated renderers. Java working set stayed at 1.74-1.80 GiB. These numbers are working sets (they include shared pages), not private bytes.

Other checks: `.\gradlew.bat build --rerun-tasks` **BUILD SUCCESSFUL in 15s, 9 tasks executed, 296 tests, 0 failures/errors/skips** (Java 25.0.4). Each client session exited normally with zero repository helpers left. Test-only change: `docs/phase2/fixture/Send-Probe.ps1` and `Measure-Browsers.ps1` now also run on Windows PowerShell 5.1 (no PowerShell 7 on this PC): `IndexOf(..., OrdinalIgnoreCase)` instead of the .NET Core `Contains` overload, and `File.Replace`/`Move` instead of the overwrite overload of `Move`. No product code changed.

## 8. Risks

- **Licensing:** H.264/AAC decoders are patent-encumbered. The zip and the replaced files must stay private (README warnings in TR/EN; `.gitignore` guards for `*.dll`, `*.pak`, `icudtl.dat`, `v8_context_snapshot.bin`, `perde-codecs-*.zip`). Do not ship them in a public modpack or upload them.
- **Version lock:** the pack only fits `jcef-d3de827+cef-146.0.10+g8219561+chromium-146.0.7680.179`. The installer refuses other versions. An MCEF update makes MCEF wipe `jcef/` and install stock files again, so codecs silently go away until a new pack is built.
- **GPU DLLs:** the 8 changed files include our own builds of ANGLE (`libEGL`/`libGLESv2`), SwiftShader, Vulkan loader and the SDK's `d3dcompiler_47.dll`. They worked on this PC (Radeon 860M) but were not tested on the cousins' GPUs. If a GPU problem shows up, a narrower install (only `libcef.dll`, `chrome_elf.dll`, `resources.pak`) would be the first thing to try.
- **Local build differences:** VS Build Tools 17.14.44 / SDK 10.0.26100.7705 are newer than CEF's stated 17.13.4 / 10.0.26100.4654; 4 MIDL typelibs were rebaselined (section 4). The build itself was clean, and the runtime tests above passed.
- **Not tested:** 16 GB client in a full modpack, multiplayer with two codec clients, long sessions, and sites that need logins or DRM (Widevine is still not provided by MCEF).
- Memory per active browser is similar to before (~0.7-1.2 GiB helper working set, up to ~1.75 GiB on ad-heavy pages); codecs did not change the one-browser limit.

## 9. How to rebuild for a future CEF version

1. Read the new `release_tag` from MCEF's `build_meta.json` (or `jcef_api_version` in gradle.properties): `cef-<ver>+g<hash>+chromium-<ver>`. Find the CEF branch (Chromium build number, e.g. 7680) and the full commit for `g<hash>`.
2. Check the branch's VS/SDK requirements on chromiumembedded.github.io/cef/branches_and_building.
3. Run `tools\codec-build\run-automate.ps1 -Stage sources -CefBranch <branch> -CefCommit <full sha>` in a fresh workspace (or `-Workspace E:\cef2`). If the network breaks: `resume-sync.ps1`, then `repair-empty-deps.ps1`.
4. `run-automate.ps1 -Stage build -CefBranch ... -CefCommit ... -BuildFailureLimit 0`. On a MIDL mismatch, compare outputs and rebaseline only typelibs that differ (see section 4).
5. `tools\codec-pack\New-PerdeCodecPack.ps1 -DistribDir <...\binary_distrib\cef_binary_..._minimal> -ReleaseTag <new release_tag>`, then `Install-PerdeCodecs.ps1`.
