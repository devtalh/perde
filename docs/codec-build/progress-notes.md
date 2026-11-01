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
