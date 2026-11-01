# CODEC-1 progress notes (resume point for a new session)

Newest at the bottom. Workspace D:\cef, logs in D:\cef\logs (`<stage>.out.log`, `<stage>.err.log`, `<stage>.exitcode`).
Build log (ninja/siso): `D:\cef\build\build-7680-release.log`.

## How to resume (new session)
1. Is a sync/build process still running? `Get-Content D:\cef\logs\*.pid` and `Get-Process -Id <pid>`. Do not start a second one.
2. Sources not finished (`D:\cef\logs\resume.exitcode` missing or non-zero): start detached
   `Start-Process powershell.exe -WindowStyle Hidden -WorkingDirectory <repo> -ArgumentList '-NoProfile','-ExecutionPolicy','Bypass','-File','tools\codec-build\resume-sync.ps1','-Jobs','4' -RedirectStandardOutput D:\cef\logs\resume.out.log -RedirectStandardError D:\cef\logs\resume.err.log`
   (remove stale `.git\shallow.lock` files under `D:\cef\build\chromium\src` first if a sync was killed).
3. Sources done (`resume.exitcode` = 0, or `sources.exitcode` = 0): start `run-automate.ps1 -Stage build` the same way with logs `build.out.log`/`build.err.log`. Re-running resumes the build.
4. Build done: distribution in `D:\cef\build\chromium\src\cef\binary_distrib\*_minimal`. Then Stage 3 (plan.md section 6).

- 2026-11-01 01:03 Stage 0 done (see plan.md). Stage 1 started detached: `tools\codec-build\run-automate.ps1 -Stage sources` (pid in D:\cef\logs\sources.pid).
- 01:05-02:05 Initial `gclient sync --no-history` (automate-git, default parallelism) downloaded ~17 GB fast, then the whole internet connection dropped to ~10 Mbps total and ~60 parallel shallow fetches kept stalling/restarting ("will retry", 53 retries). Killed only that process tree, removed stale `.git/shallow.lock` files, and started `tools\codec-build\resume-sync.ps1 -Jobs 4` (logs D:\cef\logs\resume.*). It finishes sync, applies CEF runhooks.patch, runs hooks, then re-runs `run-automate.ps1 -Stage sources`.
- 02:30 Network diagnosis: latency normal (1.1.1.1 7 ms) but throughput ~0.1 MB/s to every host (Cloudflare, GCS, GitHub, googlesource), after ~20 GB had downloaded at ~8 MB/s. Looks like ISP traffic shaping (fair-use quota). No local process is using the bandwidth. Checked DEPS for safe size reductions: `checkout_configuration=small` only drops benchmarks (already mostly fetched), and `non_git_source` also gates ninja/siso/node, so neither was changed. Sync continues slowly with -j 4.
- 03:01 resume attempt 1 failed after 60 min with network errors (connection reset, `schannel: server closed abruptly`, `early EOF`); attempt 2 started automatically (max 6). gclient moved partial repos to `D:\cef\build\chromium\_bad_scm\` (safe to delete later). cef-env.ps1 now sets DEPOT_TOOLS_UPDATE=0 once bootstrapped.
- 03:00-07:36 Network essentially down (0-10 KB/s); resume-sync used all 6 attempts and stopped at 07:36 (no data lost, partial repos in `_bad_scm`).
- 09:12 Talha back, connection recovered (~3 MB/s). Restarted `resume-sync.ps1 -Jobs 8 -Attempts 40 -PauseSeconds 60` (old logs kept as resume-1.*).
- 09:19 gclient sync completed (all 224 git deps present), runhooks.patch applied, `gclient runhooks` started.
- ~09:25 PAUSED by Talha: resume tree and polls stopped, nothing deleted. Runhooks interrupted, so the resume step must re-run it (resume-sync.ps1 does). See PROGRESS.md CODEC-1 "Paused" entry.
- 18:21 Resumed (Talha: "devam et"). Sync was a no-op, but runhooks failed: `src\v8\tools\builtins-pgo\download_profiles.py` missing. Verification against `gclient revinfo` found 26 dependency repos with an initialised .git but no commits (fetch interrupted overnight; later syncs skip existing folders). Added `tools\codec-build\repair-empty-deps.ps1` (shallow-fetches the exact DEPS revision and checks it out, no deletion). 18:49 all 224 git deps at their DEPS revision. Re-running resume-sync.ps1.
- 18:53 Stage 1 complete (resume exit 0; final dep check OK; PGO win64 profile present; src\cef copied).
- 18:54 Build attempt 1: gn failed, `vs_toolchain.py` "Visual Studio Version 2022 not found" (it only probes %ProgramFiles% for 2022; Build Tools are under Program Files (x86)). Fix: cef-env.ps1 sets the documented `vs2022_install` override from vswhere (process env only).
- 18:57 Build attempt 2: gn gen OK for all configs; Siso build of out\Release_GN_x64 running. args.gn saved to docs/codec-build/args-Release_GN_x64.gn.
