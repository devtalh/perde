# F3-2: client playback sync engine

Status: **done with criterion (f) deferred to LEA-200 (codecs)**, per Talha + Claude's 2026-10-31 decisions. [PR #10](https://github.com/devtalh/perde/pull/10), branch `feat/f3-2-sync-engine`, from main `a326147`. Do not merge. Next task depends on the LEA-200 decision. The late-join timing goal was missed and explicitly accepted by Talha as a documented buffering constraint; this is not a claim that it passed.

## Delivered behavior and lead decisions

- The active managed browser follows PlaybackClock.current(ClientPlaybackClock.serverNowMs()) through a client-thread 500 ms loop. No clock sample means no media actions. Every native frame is enumerated and sampled separately with CefFrame.executeJavaScript. Playing visible media is preferred, then largest visible area. Frame-local DOM visibility cannot detect every parent overlay or occlusion.
- Perde's private CefClient owns its message router. A random per-browser 256-bit token, fixed maximum-512-character schema, expected poll, exact native browser/frame identity and one observation/frame/poll bound the channel. Stale, duplicate, persistent and unrelated queries are ignored. There are no page-provided commands or server operations. Same-world injected JS retains the existing trusted-site boundary.
- Pure decisions implement drift >1500 ms, no playbackRate changes, a 2000 ms per-element seek cooldown, no seeking on Infinity/NaN/reported live media, a 50 ms end margin followed by pause, and an exempt hook (always false for F3-2). Rejected play promises retry next loop and warn once per page. INFO logs corrections; DEBUG gives periodic drift summaries.
- The URL guard now compares **media identity**, not the complete URL. YouTube watch?v=, youtu.be, embed, shorts and live routes across the YouTube family share `youtube:<video ID>`. Parameters such as pp/si/t/feature/list/index and fragments do not change identity. No-video YouTube URLs and other sites use normalized host plus path, ignoring query/fragment and trailing slash. Host normalization handles case, leading www, IDN and a terminal DNS dot. Lookalikes are not YouTube. Invalid/private/non-HTTP URLs remain rejected by the unchanged URL policy.
- Initial redirects can commit. A later identity change restores the server URL. Explicit server navigation waits for a new main-document commit before sampling, discarding old observations. Router/frame/callback/element state is cleaned up on page/session changes.
- **(f) is deferred to LEA-200 (codecs), not failed.** Live-media unit tests remain. No codec/native/MCEF installation or modification, ad/embed changes, volume product behavior, interaction work or server/state changes were made.

## Current verification

Read AGENTS, SPEC, PROGRESS, this report, the implementation and pinned sources, Notion Perde hub, LEA-197 and [LEA-200](https://linear.app/leak-rake/issue/LEA-200) before changes. Java 25.0.4 is installed. PR #9 was already merged. Linear get_agent_context still returns Tool not found. SPEC/AGENTS/common code/build configuration remain unchanged.

Verified pinned JCEF frame enumeration/execution/disposal, router async callbacks and callback frame scope, private-client router registration and load callbacks from the [resolved sources](https://repo.maven.apache.org/maven2/me/friwi/jcef-api/jcef-d3de827+cef-146.0.10+g8219561+chromium-146.0.7680.179/jcef-api-jcef-d3de827+cef-146.0.10+g8219561+chromium-146.0.7680.179-sources.jar). Production does not use DevTools. McefCompatibility checks the required signatures.

```text
.\gradlew.bat build --rerun-tasks --console=plain --warning-mode all
BUILD SUCCESSFUL in 8s
9 actionable tasks: 9 executed
296 tests, 0 failures, 0 errors, 0 skipped
```

[Current build](f3-2-identity-build.txt), [296-test suites](f3-2-identity-tests.csv), [current checks](f3-2-identity-checks.txt). The new identity suite covers YouTube variants, housekeeping, distinct IDs, generic query/path behavior, host lookalikes, invalid inputs, accepted Unicode authorities and DNS aliases. Existing live/end/debounce/exempt/message tests remain intact. The opt-in fixture also compiles separately.

All game data/native caches stay under run/; Gradle uses repository .gradle-user-home. Only repository-owned PIDs are controlled. The dedicated server is MCEF-free, windowless, --nogui and loopback 127.0.0.1:25570. Both integration and fresh post-build server checks reach Done and save all dimensions on normal console stop. Clients leave/quit normally and native registries reach zero. No Computer Use, desktop input, focus switching, Prism, .minecraft, KuzCraft or unrelated process control. Chromium --mute-audio, Minecraft master 0 and fixture-only video muting keep output silent.

## Re-run (c): late join and buffering

Each run moved Player2 beyond range, used the real owner's Go to zero while playing, waited 60 seconds, and returned Player2. Native observations use Date.now projected to the production server clock at the same instant. Stable means ten successive observations with readyState >=3, playing, and absolute drift <1500 ms. All 180 observations/run are retained; no buffering samples are deleted.

| URL / condition | Range entry to first readyState >=3 | First readyState >=3 to stable drift <1500 ms | Guard reloads for same video | Stable drift |
| --- | ---: | ---: | ---: | ---: |
| [aqz-KE-bpKQ, 4K source](https://www.youtube.com/watch?v=aqz-KE-bpKQ) | **5.007 s** | **9.056 s** | **0** | -181.000 ms |
| [YE7VzlLtp-4, observed quality large/480p](https://www.youtube.com/watch?v=YE7VzlLtp-4) | **2.838 s** | **4.342 s** | **0** | -293.406 ms |

**The 1.5 s goal for the second interval was not met.** Talha explicitly accepted reporting this as a buffering constraint and making the PR ready after the remaining work. There is no immediate synchronized-start guarantee.

Cause: seeking to the current target requires the page to rebuffer while the authoritative target keeps moving. The mandatory two-second seek cooldown prevents another correction during that wait; after buffering, the client may already be >1500 ms behind and seek again. The first run also contains YouTube's own same-identity theme refresh/document reload, not a Perde guard reload. Native readyState=1 samples and approximately two-second correction intervals substantiate this distinction. No playback decision thresholds or assertions were weakened.

**Proposed F3-5 improvement: seek-ahead compensation.** Estimate seek/rebuffer delay and compensate the seek target for the expected moving timeline, with appropriate bounds and validation. This is a proposal only; it is **not implemented in F3-2**.

[First run CSV](f3-2-identity-late-first.csv), [second run CSV](f3-2-identity-late-second.csv), [timing summary](f3-2-identity-late-summary.json), [native timeline/guard excerpts](f3-2-identity-runtime.txt), [first F2](screenshots/f3-2-identity-late-first.png), [second F2](screenshots/f3-2-identity-late-second.png).

## Re-run (d): identity guard

Injected normal History changes adding pp, si, t, feature, list, index and fragment to the same video. The altered URL remained loaded with readyState 4 and **no guard reload**. YouTube's initial themeRefresh changes also produced zero guard reloads in both late runs.

Changed `aqz-KE-bpKQ` to `jNQXAC9IVRw` via History: server URL restored. Changed `YE7VzlLtp-4` to `aqz-KE-bpKQ` via a normal document navigation: server URL restored. Actual native returned URLs were observed, not just dispatch acknowledgements. [Excerpts](f3-2-identity-runtime.txt).

## Codec impact (LEA-200)

Read-only measurement, no fixes. Each public episode was selected from that site's own native listing. Only diziwatch.app, dizipalorijinal20.com and dizilla.now were temporarily added to run/server/config/perde-server.json through the normal console command, then removed. Vimeo/Dailymotion and subdomains were already allowed. Defaults and the product allowlist were not changed. Subframe/CDN permissions were not relaxed.

The [test-only observer](fixture/codec-observe.js) reads media readiness/time/errors, declared MIME, capability answers, request extension/host and visible play-control metadata in each native frame. It records no response bodies, media bytes, audio, video or content text. Exactly **one unchanged F2 per site**, plus technical metadata, is retained. No paywall, login, consent or age gate was bypassed. Dailymotion's conservative DOM consent signal was not treated as proof of a gate: visual inspection showed the actual player message **Browser not compatible**.

Common capability results in every observed player frame: H.264 `video/mp4; codecs="avc1.42E01E"` and AAC `audio/mp4; codecs="mp4a.40.2"`: MediaSource false / canPlayType empty. VP9, AV1 and Opus: true / probably. These are capability answers, not independent identification of the remote stream codec.

| Site / tested URL | Normal control / outcome | Any video readyState >=3 and advancing? | Detectable MIME / requested stream / error | Video's frame host | Single F2 |
| --- | --- | --- | --- | --- | --- |
| [Vimeo 76979871](https://vimeo.com/76979871) | Autoplay already attempted; no visible Play control available; UI Player error | **No**, max ready 0, time 0 | Blob source; CDN **HLS m3u8, DASH mpd and MP4** requests. HTMLMediaElement.error null; H.264/AAC negative | Inline **vimeo.com**. player.vimeo.com was an auxiliary proxy frame without video | [F2](screenshots/codec-impact/vimeo.png) |
| [Dailymotion xbsj7ry](https://www.dailymotion.com/video/xbsj7ry), from native home listing | **Browser not compatible**; no normal player Play control available, no consent action clicked | **No**, max ready 0, time 0 | Stream type not detectable in the observed frame, no media error reported; H.264/AAC negative | **geo.dailymotion.com** | [F2](screenshots/codec-impact/dailymotion.png) |
| [diziwatch episode](https://diziwatch.app/dizi/horror-collector/sezon-1/bolum-4) | Clicked normal outer **Oynat** then frame's **np-play** control | **No**, max ready 0, time 0 | Blob/MSE source, duration metadata 1503.125 s; request transport/MIME not detectable, media error null; H.264/AAC negative | **four.pichive.online** | [F2](screenshots/codec-impact/diziwatch.png) |
| [Dizipal episode](https://dizipalorijinal20.com/bolum/mobland-2-sezon-7-bolum) | Clicked visible **bp-play-btn** | **No** observed video advanced, max ready 0, time 0 | Observed inline **MP4** elements: error **4**, `DEMUXER_ERROR_NO_SUPPORTED_STREAMS / FFmpegDemuxer: no supported streams`. No main episode iframe was identified; do not infer its exact codec from the inline failure | Observed videos inline **dizipalorijinal20.com** | [F2](screenshots/codec-impact/dizipal.png) |
| [Dizilla episode](https://dizilla.now/carrie-1-sezon-1-bolum) | Clicked **Bolumu baslat** then frame's **np-play** | **No**, max ready 0, time 0 | Blob/MSE, duration metadata 3311.167 s; request transport/MIME not detectable, media error null; H.264/AAC negative | **four.pichive.online** | [F2](screenshots/codec-impact/dizilla.png) |

[All technical metadata](f3-2-codec-impact.json), [F2 SHA256 provenance](f3-2-identity-provenance.csv). These results show compatibility failures/no advancing video in the tested pages, not a claim that every failure was conclusively caused by the same codec. No forced direct-source playback or alternate-source workaround was used.

Historical actual Twitch/Kick errors remain relevant evidence for LEA-200: live Riot Games on Twitch gave #4000; Kick gave DEMUXER_ERROR_NO_SUPPORTED_STREAMS; H.264/AAC capabilities false, VP9 true. **Acceptance (f): deferred to LEA-200 (codecs).** The original error evidence does not make the F3-2 criterion failed after the lead deferral.

## Original supported-media acceptance, retained

| Check | Previously verified result, unchanged implementation |
| --- | --- |
| Clean YouTube drift, 121 samples/client | P1 median/max **57.451/76.941 ms**, P2 **56.906/68.353 ms**, over 120.876/120.705 s, no ads/buffering/exclusions. [242 samples](f3-2-drift.csv) |
| Real panel Pause / Play / +30 s / Go to 1:00 | Both ready videos respond in **101..601 ms**. [Latency CSV](f3-2-control-latency.csv) |
| No-video page | example.com renders with zero media actions after commit |
| Invalid router queries | Plain, wrong-token and 1000-character requests rejected with -1 |
| Cross-origin media | Main page has zero videos; native httpbingo.org frame's WebM seeks to exactly 2 s and pauses |
| End handling | 5.059 s video with target >=10 s ends at **5.009 s / paused** |
| Play rejection | Five retries, one warning; restoring real play resumes media |
| Original unit checkpoint | 274 tests passed before this continuation; current total **296** |

[Original native excerpts](f3-2-runtime.txt), [original checks](f3-2-checks.txt), [original screenshots/provenance](f3-2-provenance.csv). Their former partial/live-blocker status and exact-URL startup measurements are superseded by this report's current lead decisions and identity re-runs.

## Corrections, safety and handoff

The original stale-document sampling issue was corrected before this continuation by waiting for a new main-document commit. This continuation changes only the identity decision and related tests, plus opt-in metadata verification. Unicode domains accepted by the existing URL policy can yield URI.getHost=null; canonicalization now handles that authority correctly, with a regression test.

Verification caught two fixture orchestration issues: one aim command queued before join completed, and null native frame lists during asynchronous browser initialization in the fixture-only muter. The latter was fixed to wait for native frames (production already did this). The fixture was recompiled and a fresh client joined, created its browser, left and quit without that failure. Both late-measurement windows are after initialization and contain 180 complete observations with no fixture errors. Initial failures are disclosed, not counted as accepted checks. No tests/assertions were removed or weakened.

Seek-ahead compensation remains only an F3-5 proposal. Smaller-than-threshold drift can persist by design. Ads may differ per client; no ad handling is added. Existing native-browser trust, DNS/WebSocket, memory and shader-tint limits remain. No deployment or merge.

After final-head CI succeeds, PR #10 is ready for review under Talha's explicit approval of the buffering constraint. **Next task depends on the LEA-200 decision**; do not start a codec build or the next major phase automatically.
