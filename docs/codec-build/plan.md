# CODEC-1 - CEF 146 with proprietary codecs for MCEF: plan (Stage 0)

Linear LEA-200. Branch `feat/codec-build` from main `35b22fb`. Written 2026-11-01 (unattended overnight session, Claude Code).

## 1. Target version (verified)

| Source | Value |
| --- | --- |
| `gradle.properties` | `mcef_version=0.3.3+mc26.2.jcef146.0.10`, `jcef_api_version=jcef-d3de827+cef-146.0.10+g8219561+chromium-146.0.7680.179` |
| `run/config/mcef-modern/jcef/build_meta.json` | `release_tag` = `jcef-d3de827+cef-146.0.10+g8219561+chromium-146.0.7680.179`, `platform` = `windows-amd64`, from jcefmaven/jcefbuild release 1.0.70 |
| CEF commit | `82195616d8405e6081a0d90924707b82aa9e4141` ("Update to Chromium version 146.0.7680.179 (#4150)", 2026-04-02), on CEF branch **7680** (2 commits behind the branch head, GitHub compare API) |
| CEF `CHROMIUM_BUILD_COMPATIBILITY.txt` at that commit | `chromium_checkout: refs/tags/146.0.7680.179` |

Target = **CEF 146.0.10+g8219561+chromium-146.0.7680.179, windows x64**: matches.

Installed native directory: 241 files (21 top-level files + 220 `locales/*.pak`). Full list with sizes and SHA-256: [mcef-original-inventory.csv](mcef-original-inventory.csv). Top-level files:

| File | Size | Kind |
| --- | ---: | --- |
| libcef.dll | 262272512 | CEF runtime (replace) |
| chrome_elf.dll | 2491904 | CEF runtime (replace) |
| d3dcompiler_47.dll, libEGL.dll, libGLESv2.dll, vk_swiftshader.dll, vk_swiftshader_icd.json, vulkan-1.dll | | CEF runtime (replace if present in the new distribution) |
| icudtl.dat, v8_context_snapshot.bin | | CEF runtime data (replace) |
| resources.pak, chrome_100_percent.pak, chrome_200_percent.pak, locales/*.pak | | CEF resources (replace) |
| jcef.dll, jcef_helper.exe | | JCEF native (keep MCEF's) |
| build_meta.json, install.lock, README.txt, LICENSE.txt, gluegen/jogl LICENSE | | jcefmaven metadata (keep unchanged) |

## 2. How MCEF decides to (re)download

Read from source:
- MCEF `MCEFApiImpl` (DimasKama/mcef-modern `df61843`) builds a jcefmaven `CefAppBuilder` with `setInstallDir(<config>/mcef-modern/jcef)` and calls `build()`, which calls `install()`.
- jcefmaven 146.0.10 `CefAppBuilder.install()` calls `CefInstallationChecker.checkInstallation(installDir)`. If it returns false it **deletes the whole install dir** (`FileUtils.deleteDir`), downloads, extracts and creates `install.lock`.
- `CefInstallationChecker.checkInstallation` returns true when `install.lock` exists, `build_meta.json` exists and parses, its `release_tag` equals the classpath build info tag, and its platform equals the current platform. **No file sizes or hashes are checked.**

Consequence: replacing runtime files while leaving `install.lock` and `build_meta.json` untouched survives restarts. Deleting either file, or changing the MCEF/jcefmaven version, makes MCEF wipe the directory and reinstall the stock (codec-free) natives. The backup must live outside `jcef/` (planned: `run/config/mcef-modern/jcef-original-backup`).

## 3. Official build instructions (CEF sources)

From `chromiumembedded.github.io/cef/automated_build_setup` and `branches_and_building` (fetched 2026-11-01), plus `tools/automate/automate-git.py` and `tools/gn_args.py` at the target commit:

- Windows x64 official command: `set GN_DEFINES=is_official_build=true`, `set GYP_MSVS_VERSION=2022`, `set CEF_ARCHIVE_FORMAT=tar.bz2`, `python3 automate-git.py --download-dir=... --branch=<branch> --minimal-distrib --client-distrib --force-clean --no-chromium-history --x64-build --with-pgo-profiles`.
- Branch 7680 requirements: Win 10+ build system with **VS2022 17.13.4**, **Windows SDK 10.0.26100.4654**, Ninja. At least 16 GB RAM and 200 GB disk. VS components: NativeDesktop workload + VC.ATLMFC. Debugging Tools for Windows are required by Chromium.
- `automate-git.py` forces `DEPOT_TOOLS_WIN_TOOLCHAIN=0` (local VS) and uses depot_tools' bundled Python (`python3.bat`) for its tools; the gclient config uses `siso_version: latest`, so `autoninja` drives Siso/Ninja.
- `gn_args.py` adds on top of GN_DEFINES: required `enable_widevine=true`, `optimize_webui=true`, `clang_use_chrome_plugins=false`, Windows `enable_cdm_host_verification=true`, `enable_cdm_storage_id=true`, `enable_rlz=true`; recommended `is_component_build=false`, `enable_background_mode=false`, `enable_resource_allowlist_generation=false`, `enable_downgrade_processing=false`, `alternate_cdm_storage_id_key=...`; official builds add `disable_fieldtrial_testing_config=true`.

### GN args for this build
`GN_DEFINES=is_official_build=true proprietary_codecs=true ffmpeg_branding=Chrome` (official Windows args plus codecs), Release x64 only, PGO profiles on, CEF's gn_args.py defaults/requirements as above. The exact generated `args.gn` is recorded in the report after `gn gen`.

## 4. Prerequisites on this PC (checked 2026-11-01)

| Item | Required | Found | OK |
| --- | --- | --- | --- |
| Visual Studio 2022 | 17.13.4+, Desktop C++, ATL/MFC | Build Tools 17.14.44 at `C:\Program Files (x86)\Microsoft Visual Studio\2022\BuildTools`; MSVC 14.44.35207 (+14.38); `atlbase.h` and `mfc140.lib` present | yes (newer than required) |
| Windows SDK | 10.0.26100.4654 | 10.0.26100 (`rc.exe` 10.0.26100.7705), include/lib dir `10.0.26100.0` | yes (newer patch) |
| Debugging Tools for Windows | required | `Debuggers\x64\dbghelp.dll` 10.0.26100.7705 | yes |
| Python | 3.x to start automate-git.py; depot_tools bundles its own | Python 3.12 (user install) | yes |
| Git | system git | 2.53.0.windows.2 | yes |
| Disk | 200 GB | D: 322 GB free | yes |
| RAM | 16 GB minimum | 31.3 GB | yes |
| CPU | | Ryzen AI 7 350, 16 logical cores | |

Not used / not changed: no Visual Studio, SDK, Python or JDK installs; no PATH, registry or Defender changes (so no Defender exclusion for D:\cef, builds will be slower). One caveat: the "Build Tools" edition instead of Community is accepted by Chromium's `vs_toolchain.py` detection only if listed; verified after sync.

## 5. Workspace isolation (D:\cef only)

Wrapper: [tools/codec-build/run-automate.ps1](../../tools/codec-build/run-automate.ps1). It clones depot_tools into `D:\cef\depot_tools`, downloads automate-git.py at the exact CEF commit, and sets for its own process tree only: `HOME`/`USERPROFILE`/`APPDATA`/`LOCALAPPDATA`/`XDG_CACHE_HOME` -> `D:\cef\home`, `TEMP`/`TMP` -> `D:\cef\tmp`, `VPYTHON_VIRTUALENV_ROOT`, `CIPD_CACHE_DIR` -> `D:\cef\cache`, `DEPOT_TOOLS_METRICS=0`, `DEPOT_TOOLS_WIN_TOOLCHAIN=0`, `GYP_MSVS_VERSION=2022`. A workspace-local `D:\cef\home\.gitconfig` sets `core.autocrlf=false` (the system Git config has `autocrlf=true`, which breaks Chromium on Windows) plus Chromium's other recommended git settings. The real global git config is not read or changed.

Layout: `D:\cef\depot_tools`, `D:\cef\automate`, `D:\cef\build\chromium\src` (+ `src\cef`), `D:\cef\build\chromium\src\out\Release_GN_x64`, distribution under `D:\cef\build\chromium\src\cef\binary_distrib`, logs in `D:\cef\logs`.

## 6. Steps

1. **Sources**: `run-automate.ps1 -Stage sources` = automate-git.py `--branch=7680 --checkout=8219561... --x64-build --no-debug-build --with-pgo-profiles --no-chromium-history --no-build --no-distrib`. Resumable: re-run the same command. Caveat from the automate-git source: with `--no-chromium-history`, an interrupted *initial* `gclient sync` leaves `src/` present, and a re-run treats the checkout as unchanged and skips hooks. If that happens: run `gclient sync --nohooks --no-history` then `gclient runhooks` in `D:\cef\build\chromium` with the same environment, then continue.
2. **Build**: `run-automate.ps1 -Stage build` = same plus `--no-update --no-depot-tools-update --force-build --build-target=cefsimple --build-log-file --minimal-distrib --minimal-distrib-only --no-distrib-docs --no-distrib-archive`. CEF's `gclient_hook.py` applies the CEF patches and runs `gn gen`, then `autoninja`. Re-running resumes incrementally. Deviation from the official command: no `--client-distrib`, no archive or docs, cefsimple instead of cefclient. These affect packaging only, not libcef. Fallback only if the link runs out of memory: `symbol_level=1` (debug info only), documented in the report.
3. **Integrate (run/ only)**: back up `jcef` -> `jcef-original-backup`; replace only files present in both trees (keep jcef.dll, jcef_helper.exe, build_meta.json, install.lock, licenses); manifest with old/new SHA-256; codec probes, YouTube drift, Twitch/Kick live, three dizi sites (temporary run/ allowlist only), Vimeo/Dailymotion, memory; restart check; restore check.
4. **Packaging scripts**: `tools/codec-pack/` (pack to a zip in D:\cef, install/restore for an instance), READMEs TR/EN. No binaries in git, ever.

## 7. Risks
- Build time on 8 cores/16 threads with ThinLTO + PGO and no Defender exclusion: estimated 6-10 h. May not finish overnight. Progress is tracked in [progress-notes.md](progress-notes.md) and PROGRESS.md.
- libcef.dll must stay ABI-compatible with MCEF's jcef.dll: same CEF commit and the same API hash, so only codec code differs. Verified at runtime (MCEF init).
- Licensing: H.264/AAC binaries are patent-encumbered. They stay local (`D:\cef`, `run/`), never committed, pushed, uploaded or attached.
