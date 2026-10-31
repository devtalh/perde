# F3-1: authoritative playback and server clock

Status: partial, runtime acceptance checks in progress. Branch `feat/f3-1-playback-state` from merged main `5e8fbd8` (PR #8). LEA-196.

Implemented server play/pause/absolute and relative seek with existing control gates; pure clock/parser/estimator tests; connection reset, five initial samples then 15-second sampling; max five time requests per rolling second; panel controls, watch-only disabling and live position; EN/TR translations. No browser/video changes.

Persistence serializes a consistent running position/timestamp checkpoint without changing the in-memory anchor. Same-session chunk reload preserves elapsed time. A saved timestamp before the current server start rebases the saved position at load. A server-only ticker marks running chunks dirty without neighbor updates or client traffic, so autosave/unload/shutdown includes the advancing timeline.

Verification so far: Java 25.0.4; required main ancestry confirmed; build SUCCESSFUL in 11s, nine tasks executed, 240 tests, zero failures/errors/skips; fixture compilation passes. Windowless MCEF-free dedicated server reaches Done (1.410s) on 127.0.0.1:25570. Two-client acceptance and restart results are pending and must pass before completion. All execution stays in this repo, Gradle user home is local, no Computer Use/window activation or unrelated process control.
