# CODEC-1 report - CEF 146 with proprietary codecs (H.264/AAC) for MCEF

Linear LEA-200. Branch `feat/codec-build` from main `35b22fb`. Claude Code, 2026-11-01 (overnight, paused 09:25-18:20, resumed in the evening).
Status: **in progress** (this file is completed as stages finish).

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
| Build | started 18:57 (attempt 2), MIDL fix, attempt 3 from 19:08 |

_(Build duration, final disk use, replaced-file manifest, test matrix and memory are added below when done.)_

## 6. How to rebuild for a future CEF version

1. Read the new `release_tag` from MCEF's `build_meta.json` (or `jcef_api_version` in gradle.properties): `cef-<ver>+g<hash>+chromium-<ver>`. Find the CEF branch (Chromium build number, e.g. 7680) and the full commit for `g<hash>`.
2. Check the branch's VS/SDK requirements on chromiumembedded.github.io/cef/branches_and_building.
3. Run `tools\codec-build\run-automate.ps1 -Stage sources -CefBranch <branch> -CefCommit <full sha>` in a fresh workspace (or `-Workspace E:\cef2`). If the network breaks: `resume-sync.ps1`, then `repair-empty-deps.ps1`.
4. `run-automate.ps1 -Stage build -CefBranch ... -CefCommit ... -BuildFailureLimit 0`. On a MIDL mismatch, compare outputs and rebaseline only typelibs that differ (see section 4).
5. `tools\codec-pack\New-PerdeCodecPack.ps1 -DistribDir <...\binary_distrib\cef_binary_..._minimal> -ReleaseTag <new release_tag>`, then `Install-PerdeCodecs.ps1`.
