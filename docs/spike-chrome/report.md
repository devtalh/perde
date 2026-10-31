# SPIKE-CHROME: Chrome CDP feasibility

Status: measurement in progress. Branch `spike/chrome-cdp`, based on main `35b22fb`. No product code, SPEC, or AGENTS changes. Do not merge this research branch.

Installed Google Chrome: 156.0.8078.74. Official Stable Chrome for Testing: 156.0.8078.80 (win64), resolved using Google's official JSON API. uBlock Origin Lite: official 2026.1026.1812 Chromium release. All profiles, downloads, temporary data and original evidence stay under `run/chrome-spike/`.

The dependency-free Java 25 controller uses `java.net.http.WebSocket`, flattened CDP sessions and automatic OOPIF attachment. A small Windows launcher starts owned processes with SW_SHOWNOACTIVATE; the optional pipe bridge uses inherited Windows pipe handles. This is a measurement fixture, not a backend implementation. Native window bounds confirm the visible normal windows are far off-screen and not foreground. No desktop input, Computer Use, unrelated process control, Chrome installation, real profile access, or Minecraft launch is used.

Initial findings, pending final evidence tables:

- Both engines advertise H.264 baseline/high, AAC, HEVC, VP9, AV1 and Opus. Capability answers do not establish remote stream codecs.
- YouTube, a Twitch channel selected from the live directory, and a Kick live channel actually advance. Primary episode playback is distinguished from short advertising videos.
- Vimeo presents a human verification page in headless mode. Normal UA retry reaches its player but does not establish advancing video. No verification, login, age or consent gate is bypassed.
- Dizilla's native player frame is rejected by Chrome because the response uses `X-Frame-Options: sameorigin`. Dizipal's normal ad-close and play controls reach a player that reports a format error. Diziwatch returns to the preceding history entry, including with a normal UA; no bypass attempted.
- Installed Chrome ignores `--load-extension`, but `--enable-unsafe-extension-debugging` plus pipe CDP `Extensions.loadUnpacked` loads uBOL. CfT also accepts the launch flag. First-party dizi overlays remain despite some blocked requests.
- A controlled H.264/AAC Sintel video streams at approximately 24 fps (its source rate) in both launch modes. ImageIO and the already-resolved Minecraft LWJGL STBImage jars are measured separately. Public-site playback interruptions are not counted as a screencast throughput limit.
- Input, cross-origin Runtime evaluation/bindings, Fetch interception and permission denial have positive observations. Final assertions and kill/restart checks are being completed.
- Memory extension requested by Talha is in progress: ten samples per engine/mode/stage, Chrome working set/private bytes by process type and Java memory, actual 720p YouTube after 60 seconds, dizi outcome, extension comparison and return to blank. A failed episode cannot provide a successful episode-playing memory result.

Fixture corrections will be disclosed in the final report: initial OOPIF resume ordering, pipe write buffering, stale execution-context observations, and a muter that wrote unchanged properties. Rejected/overlapping early site observations are not acceptance evidence. The initial 720p60 YouTube URL fails after quality selection; the native menu on `YE7VzlLtp-4` successfully selects an actual 1280x720 video for the memory soak.

Current validation: Java 25 compile passed; repository-local Gradle build SUCCESSFUL in 10 s, 9 tasks executed, 296 tests, zero failures/errors/skips. No test or assertion weakened. Final evidence export, complete report, diff review, push and PR remain.
