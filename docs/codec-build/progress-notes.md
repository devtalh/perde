# CODEC-1 progress notes (resume point for a new session)

Newest at the bottom. Workspace D:\cef, logs in D:\cef\logs (`<stage>.out.log`, `<stage>.err.log`, `<stage>.exitcode`).
Build log (ninja/siso): `D:\cef\build\build-7680-release.log`.

- 2026-11-01 01:03 Stage 0 done (see plan.md). Stage 1 started detached: `tools\codec-build\run-automate.ps1 -Stage sources` (pid in D:\cef\logs\sources.pid).
- 01:05-02:05 Initial `gclient sync --no-history` (automate-git, default parallelism) downloaded ~17 GB fast, then the whole internet connection dropped to ~10 Mbps total and ~60 parallel shallow fetches kept stalling/restarting ("will retry", 53 retries). Killed only that process tree, removed stale `.git/shallow.lock` files, and started `tools\codec-build\resume-sync.ps1 -Jobs 4` (logs D:\cef\logs\resume.*). It finishes sync, applies CEF runhooks.patch, runs hooks, then re-runs `run-automate.ps1 -Stage sources`.
