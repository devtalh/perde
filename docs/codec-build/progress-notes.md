# CODEC-1 progress notes (resume point for a new session)

Newest at the bottom. Workspace D:\cef, logs in D:\cef\logs (`<stage>.out.log`, `<stage>.err.log`, `<stage>.exitcode`).
Build log (ninja/siso): `D:\cef\build\build-7680-release.log`.

- 2026-11-01 01:03 Stage 0 done (see plan.md). Stage 1 started detached: `tools\codec-build\run-automate.ps1 -Stage sources` (pid in D:\cef\logs\sources.pid).
- 01:05-02:05 Initial `gclient sync --no-history` (automate-git, default parallelism) downloaded ~17 GB fast, then the whole internet connection dropped to ~10 Mbps total and ~60 parallel shallow fetches kept stalling/restarting ("will retry", 53 retries). Killed only that process tree, removed stale `.git/shallow.lock` files, and started `tools\codec-build\resume-sync.ps1 -Jobs 4` (logs D:\cef\logs\resume.*). It finishes sync, applies CEF runhooks.patch, runs hooks, then re-runs `run-automate.ps1 -Stage sources`.
- 02:30 Network diagnosis: latency normal (1.1.1.1 7 ms) but throughput ~0.1 MB/s to every host (Cloudflare, GCS, GitHub, googlesource), after ~20 GB had downloaded at ~8 MB/s. Looks like ISP traffic shaping (fair-use quota). No local process is using the bandwidth. Checked DEPS for safe size reductions: `checkout_configuration=small` only drops benchmarks (already mostly fetched), and `non_git_source` also gates ninja/siso/node, so neither was changed. Sync continues slowly with -j 4.
- 03:01 resume attempt 1 failed after 60 min with network errors (connection reset, `schannel: server closed abruptly`, `early EOF`); attempt 2 started automatically (max 6). gclient moved partial repos to `D:\cef\build\chromium\_bad_scm\` (safe to delete later). cef-env.ps1 now sets DEPOT_TOOLS_UPDATE=0 once bootstrapped.
