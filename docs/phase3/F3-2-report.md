# F3-2: client playback sync engine

Status: **partial, real live-stream acceptance blocked**. [Draft PR #10](https://github.com/devtalh/perde/pull/10), branch `feat/f3-2-sync-engine`, from merged main `a326147`. PR #9 was confirmed merged before coding. Do not mark done or merge. F3-3 follows only after F3-2 acceptance.

## Blocking acceptance result

The actual live Riot Games channel failed on both sites in the pinned native browser:

- Twitch `https://www.twitch.tv/riotgames`: visible **Error #4000**, time 0, readyState 0, duration NaN.
- Kick `https://kick.com/riotgames`: **PipelineStatus::DEMUXER_ERROR_NO_SUPPORTED_STREAMS: FFmpegDemuxer: no supported streams**, time 0, readyState 0, duration NaN.
- Native MediaSource.isTypeSupported: H.264 `avc1.42E01E` **false**, AAC `mp4a.40.2` **false**, VP9 **true**. video.canPlayType for H.264 returned the empty string. These capabilities and errors are consistent with missing native codecs; the remote stream itself was not independently demuxed.

Monstercat was tried first and was actually offline, so it is not counted as a live test. Riot was selected from the sites' current live listings. No sign-in, consent/age gate, codec install, native modification or engine replacement was performed. Twitch/Kick already belong to the default list. Only example.com was added to the repository test server config; product defaults are unchanged.

Pure live decisions pass unit tests, but **real live playback with pause/play and zero seeks is NOT verified**. This is an unmet criterion. SPEC promises these sites with the pinned MCEF build; a lead decision on native codec support or acceptance scope is needed. SPEC.md and AGENTS.md remain unchanged. No F3-3 work is included.

[Runtime evidence](f3-2-runtime.txt), [Twitch F2](screenshots/f3-2-twitch-codec-error.png), [Kick F2](screenshots/f3-2-kick-codec-error.png).

## Delivered implementation

- The active browser is sampled every 500 ms from the client tick. Without a server clock sample there are no media queries/actions. The URL guard still operates.
- Enumerate every native frame and use CefFrame.executeJavaScript separately, including cross-origin renderers. Each frame picks nonzero-size visible videos, preferring playing media, then largest viewport intersection. Java selects the winning frame with the same ordering. This is frame-local visibility, not arbitrary overlay/parent-occlusion detection.
- A CefMessageRouter belongs only to Perde's private CefClient. Each browser gets a cryptographically random 256-bit token. Accept only the fixed 12-field observation schema, maximum 512 characters, current poll and an outstanding native frame identity. Reject persistent, stale, duplicate, unrelated and wrong-browser queries. Each enumerated frame can submit only once per poll. There are no page-provided commands, URLs or server operations. Same-world JS cannot isolate injected code from a compromised trusted page; the existing trusted-site boundary remains.
- Collect observations for 100 ms and decide on the client thread using the latest PlaybackClock.current(ClientPlaybackClock.serverNowMs()). The asynchronous query success callback applies Java-authored actions to the captured video in that frame. Native callback frame references are not retained. Enumerated wrappers are disposed, and page/session changes clear pending callbacks and cooldown identities.
- Pure decisions implement strict drift >1500 ms, no playbackRate changes, a 2000 ms per-element seek cooldown, no seeking on Infinity/NaN/reported live media, a 50 ms end margin followed by pause, and the future exempt flag (always false here). Transport is independent of seek cooldown. Rejected play promises retry next loop and warn once per page.
- INFO logs issued seek/play/pause and drift; DEBUG logs ten-second drift summaries. A page without visible video warns once after ten seconds. Native observations independently verify effects.
- Initial main-document redirects may finish before the committed baseline is locked. Later document/History address changes reload the server URL. Explicit server navigation suspends media sampling until the new main document commits.

## Sources and verification method

Read AGENTS, SPEC, PROGRESS, F3-1/F2-2 reports, browser/clock code, the Notion Perde hub and Linear LEA-197 before implementation. Java 25.0.4 was present. Linear get_agent_context still returned Tool not found.

Verified the pinned [JCEF sources](https://repo.maven.apache.org/maven2/me/friwi/jcef-api/jcef-d3de827+cef-146.0.10+g8219561+chromium-146.0.7680.179/jcef-api-jcef-d3de827+cef-146.0.10+g8219561+chromium-146.0.7680.179-sources.jar), extracted under ignored run/f2-2-sources/jcef: frame enumeration/execution/disposal, router creation and asynchronous nonpersistent callbacks, callback frame scope, private client router registration/removal, and load-start/load-end callbacks. McefCompatibility verifies the added signatures before backend creation. Production does not use DevTools.

All game data/natives stay under run/, Gradle uses .gradle-user-home, and the windowless --nogui server binds 127.0.0.1:25570. Only repository-owned processes are targeted. No Computer Use, desktop input, focus switching, unrelated Minecraft process access, Prism, .minecraft or KuzCraft access. The opt-in fixture uses real panel buttons, ordinary networking, native observations and vanilla F2. Chromium --mute-audio, Minecraft master 0 and test-only media muting keep runs silent. Fixtures are excluded from production jars.

## Acceptance results

Final product code: `5d30670` (implementation checkpoint), following `e375283`. Both checkpoints passed CI. Evidence/docs head must also pass CI. The PR remains draft because the live-stream criterion is unmet.

| Check | Final observed result |
| --- | --- |
| Fresh build | `.\gradlew.bat build --rerun-tasks --console=plain --warning-mode all`: **BUILD SUCCESSFUL in 8s**, all 9 tasks executed. **274 tests**, zero failures/errors/skips (240 existing + 34 new). [Build](f3-2-build.txt), [suites](f3-2-tests.csv). |
| Player1 clean drift | **121 samples / 120.876 s**, median **57.451 ms**, max **76.941 ms** absolute drift. |
| Player2 clean drift | **121 samples / 120.705 s**, median **56.906 ms**, max **68.353 ms** absolute drift. |
| Clean sample conditions | Both playing, readyState 4, no ads, no excluded samples, muted/volume 0. Native JS epoch projected to the production clock at that same instant. [All 242 samples](f3-2-drift.csv). |
| Panel pause | P1 **101 ms**, P2 **133 ms**. |
| Panel play | P1 **368 ms**, P2 **305 ms**. |
| Panel +30 s | P1 **400 ms**, P2 **369 ms**. |
| Panel Go to 1:00 | P1 **601 ms**, P2 **433 ms**. Every result requires the new timeline plus actual readyState-4 media in the correct transport state, not an old pre-action target. [Latency samples](f3-2-control-latency.csv). |
| Late range entry | P2 returned **60.043 s** after owner Go to zero. Initial ready metadata sample was time 0.224 s / target 63.018 s. Two YouTube housekeeping URL reloads occurred; first selected corrective seek targeted 66.326 s, another after 2.002 s targeted 68.328 s. First stable readyState-4 sample: video **68.393204 s**, target **68.538 s**, drift **144.796 ms**. Startup was about **8.508 s after range entry**, including loading/reloads/buffering; immediate synchronized startup is not claimed. [All 180 startup samples](f3-2-late.csv). |
| URL guard | Current-code History pushState and full document navigation to `jNQXAC9IVRw` both restored authoritative `aqz-KE-bpKQ`; server state unchanged. Native restored URL/complete document observed. |
| Message rejection | Plain `play`, wrong-token shaped request and oversized 1000-character request all returned native query error **-1**, with no actions from those messages. |
| No-video/frame/end/retry | Current-code native checks below passed. |
| Dedicated server | MCEF absent, **44 mods**, windowless --nogui, **127.0.0.1:25570**, Done **0.914s**. Normal console stop saved all dimensions; Gradle success in 23m 57s. [Server log](f3-2-server.txt). |
| Cleanup/isolation | Both native registries reached **0**, normal client exits/Gradle success (11m 26s / 11m). No repo game JVM or 25570 listener remains. Default jar has no bundled MCEF/JCEF, nested jars or fixture; common source and SPEC/AGENTS/build unchanged. [Checks](f3-2-checks.txt). |
| Live Twitch/Kick | **Blocked/unverified**, actual stream failures and missing codec capabilities above. No live success claimed. |

[Player1 observations](f3-2-player1.txt), [Player2 observations](f3-2-player2.txt), [native excerpts](f3-2-runtime.txt). The same-PC measurements do not guarantee these drift/latency values over arbitrary networks.

Preliminary 121-sample series before the document-transition correction: spans 120.753/120.806 seconds; P1 median/max absolute drift **251.886/266.459 ms**, P2 **160.658/171.153 ms**, no ads/buffering. All panel controls reached both readyState-4 videos in **93..483 ms**. A first late entry at start+60.047 s issued a seek immediately but needed URL housekeeping/rebuffering before a stable sample at **157.026 ms** drift. These are preliminary results, not substitutes for the final rerun.

Current-code additional native checks:

- Allowed example.com had zero videos and continued rendering. No media actions after the no-video page committed. A malformed `play` query returned -1.
- Cross-origin iframe: example.com had zero main-frame videos; native httpbingo.org/html hosted test-only MDN flower.webm. Production sought to exactly **2.000 s**, paused=true, readyState=4. The first example.org setup was refused by its embedding policy and is excluded, with raw logs retained.
- End handling in the same frame: duration **5.059 s**, server target >=10 s/playing=true, native position **5.009 s**, paused=true, readyState=4.
- Test-only play rejection: five attempts, one warning, retry each loop; restoring the real play function resumed playback. A fresh panel seek started the separate clean drift series.
- Both preliminary and final History/full-document guard checks restored the unchanged server URL.
- Real live-stream criterion remains blocked as described above.

## Corrections and limits

An early explicit URL change allowed a new poll against the old document before navigation committed. Logs caught an old YouTube seek while navigating to example.com. The backend now waits for the new main document's load-start commit and clears old observations there, rather than at load end. No tests/assertions were weakened. An early arbitrary frame-count ceiling was removed before final runtime.

YouTube adds/removes initial housekeeping URL parameters. The requested exact URL guard may cause extra reloads/buffering before settling. Late-join startup samples are retained separately from steady-state results. No ad reduction, skipping or embed conversion is implemented; clean measured intervals showed no ads.

The threshold intentionally leaves drift <=1500 ms alone. A <500 ms median is an observed clean-run goal, not a guarantee after every rejection/buffering event. Existing native-browser trust, DNS/WebSocket, shader tint and memory limits remain.

The final accepted client logs contain zero fixture/sample failures, Perde browser failures, incompatibility failures or bare AWT exception prefixes. Existing Windows OSHI/performance counter, Chromium registry and dev-account/Realms diagnostics remain. Initial phase wrappers and earlier test runs are retained under ignored run/f3-2-verification; no process termination or system setting changes were needed.

## Visual proof

Eleven unchanged vanilla F2 files have identical source/evidence SHA256 pairs in [provenance](f3-2-provenance.csv). Owner/viewer, late join, panel, restored URL, video-free page, cross-origin video and native failure views were inspected.

[Owner playing](screenshots/f3-2-owner-youtube.png), [viewer playing](screenshots/f3-2-viewer-youtube.png), [panel](screenshots/f3-2-owner-panel.png), [late join](screenshots/f3-2-late-join.png), [restored URL](screenshots/f3-2-url-restored.png), [no-video page](screenshots/f3-2-no-video-final.png), [cross-origin pause](screenshots/f3-2-cross-frame-paused.png), [end clamp](screenshots/f3-2-cross-frame-ended.png).

The fixture README documents sync-sample/sync-record. The optional frame-evaluate command enumerates only the manager-owned browser's native frames and runs a test script in a matching public frame URL. Periodic fixture-only frame muting supplements Chromium's global test mute; startup metadata can briefly report the page's initial volume before the test muter reaches a new element, while Chromium output remains muted throughout.

## Manual review and handoff

1. Start the repository's MCEF-free server and two fixture clients. Open an allowed YouTube watch URL as owner.
2. Record both native videos once per second for two minutes against the production clock; review ads, readiness and raw CSV.
3. Use the real owner panel for pause/play, +30 s and Go to 1:00. Verify both ready videos respond in one second.
4. Move Player2 out of range, start at zero, return after 60 seconds, and inspect first seek, URL housekeeping, readiness and stable drift separately.
5. Force History and document navigation to another video, verify restoration, then open a video-free allowed page and check for no media actions.
6. Review frame/end/retry evidence and tests. Resolve native codecs or acceptance scope through a lead decision before marking F3-2 done or starting F3-3.
