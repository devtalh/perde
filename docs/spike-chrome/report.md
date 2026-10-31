# SPIKE-CHROME: Chrome CDP feasibility

**GO with conditions for a separate backend prototype. Keep the product backend unchanged pending integration acceptance.** Chrome supplies the missing H.264/AAC capabilities and usable frames/control. Successful playback on Vimeo, Dailymotion and the three dizi pages was not established. There is no successful episode-playing memory result. RAM and actual Minecraft texture-upload cost remain adoption gates.

Prefer **E2, pinned official Chrome for Testing**, for the next prototype: portable isolated binary, reproducible version selection and working unpacked-extension launch flag. E1 also works, including extension installation over a debugging pipe, but its update cadence belongs to the user. This sample does not establish an intrinsic memory advantage for either build.

Branch `spike/chrome-cdp`, based on main `35b22fb` (merged F3-2). Measurements: 2026-10-31 UTC, extending into 2026-11-01 Europe/Istanbul. Product code, dependencies, SPEC and AGENTS unchanged. No game/server launch or merge.

## Environment and reproduction

Java 25.0.4; Windows 11 Pro 10.0.26200; Ryzen AI 7 350 / Radeon 860M, 8 cores / 16 logical CPUs, approximately 32 GB RAM. This is not a 16 GB hardware test. E1 is installed Chrome **156.0.8078.74**, used as installed. E2 is official Stable CfT **156.0.8078.80**, win64 Chrome zip only. uBlock Origin Lite is official Chromium release **2026.1026.1812**. Downloads/profiles/caches/native extraction stay under `run/chrome-spike/`; no installer, software update or real profile access.

Every launch has a fresh profile, `--no-first-run`, `--no-default-browser-check`, `--disable-background-mode`, `--disable-component-update`, `--disable-sync`, `--autoplay-policy=no-user-gesture-required`, `--window-size=640,480`, `--window-position=-32000,-32000`, and `--disable-features=CalculateNativeWinOcclusion`. Headless adds `--headless=new`. Debugging is an ephemeral **127.0.0.1** port or inherited pipes. Final launches have `--mute-audio`; earlier bounded tone checks used media muting and a two-second 0.05-gain oscillator.

The launcher uses `SW_SHOWNOACTIVATE`. Observed visible normal windows were clamped by Windows to **(-26214,-26214)-(-25574,-25734)**, outside monitor **(0,0)-(1536,960)**, and not foreground. Hidden internal windows are separately listed. No Computer Use, desktop input, activation or focus-switch API was used. [Window audit](evidence/mem-e2-o/window-bounds.txt).

Java uses built-in HttpClient/WebSocket and a small JSON codec, with no added dependency. STB reuses Minecraft-resolved LWJGL 3.4.1 jars, including its `unsafe` base classifier. PowerShell/.NET provides native no-activation launch and an optional inherited-pipe relay; this helper is fixture infrastructure. [Commands and runner](../../tools/chrome-spike/README.md), [system](evidence/system.json), [downloads/hashes](evidence/downloads.json), [provenance](evidence/provenance.json). Published JSON removes repository paths/expiring query strings; PNG/CSV copies retain their source hashes. Original full logs/JPEGs remain in ignored `run/`.

API verification used live `/json/protocol` and primary sources: [CfT endpoints](https://googlechromelabs.github.io/chrome-for-testing/), [CDP schema](https://github.com/ChromeDevTools/devtools-protocol/blob/master/json/browser_protocol.json), [Page](https://chromedevtools.github.io/devtools-protocol/tot/Page/), [Extensions](https://chromedevtools.github.io/devtools-protocol/tot/Extensions/), [Chromium extension implementation](https://github.com/chromium/chromium/blob/main/chrome/browser/devtools/protocol/extensions_handler.cc), [pipe implementation](https://github.com/chromium/chromium/blob/main/content/browser/devtools/devtools_pipe_handler.cc), [Chrome profile/debugging restriction](https://developer.chrome.com/blog/remote-debugging-port), [official uBOL releases](https://github.com/uBlockOrigin/uBOL-home/releases). Repo documents, Notion hub and LEA-200 were read; Linear's field-notes endpoint was unavailable.

## 1. Launch modes

Startup includes launcher, CDP attach/domain setup and 500 ms stabilization. These are individual launches, not startup percentiles; services may start after initial readiness.

| Engine / mode | Startup ms | Processes at readiness | Mean count blank / 720p | Blank Chrome WS mean / max MiB |
| --- | ---: | ---: | ---: | ---: |
| E1 headless | 1496.40 | 10 | 10.9 / 12.0 | 615.10 / 623.02 |
| E1 off-screen | 1334.41 | 10 | 10.4 / 13.0 | 605.86 / 645.90 |
| E2 headless | 1413.75 | 11 | 11.9 / 14.0 | 624.75 / 641.33 |
| E2 off-screen | 1414.06 | 11 | 11.9 / 14.0 | 640.83 / 652.84 |

[E1 H](evidence/final-e1-h-memory/startup.json), [E1 O](evidence/mem-e1-o/startup.json), [E2 H](evidence/mem-e2-h/startup.json), [E2 O](evidence/mem-e2-o/startup.json). Headless avoids a native browser window. Continuous normal off-screen capture also works; an early static capture after a YouTube player error is not counted as a throughput limit.

## 2. Codecs

Both launch modes in both engines give these literal answers. HEVC capability on this machine does not guarantee a cousin's hardware/OS support. Capability probes do not identify remote stream codecs.

| Codec | Exact MIME probe | E1 MSE / canPlayType | E2 MSE / canPlayType |
| --- | --- | --- | --- |
| H.264 baseline | `video/mp4; codecs="avc1.42E01E"` | true / probably | true / probably |
| H.264 high | `video/mp4; codecs="avc1.640028"` | true / probably | true / probably |
| AAC | `audio/mp4; codecs="mp4a.40.2"` | true / probably | true / probably |
| HEVC | `video/mp4; codecs="hvc1.1.6.L93.B0"` | true / probably | true / probably |
| VP9 | `video/webm; codecs="vp09.00.10.08"` | true / probably | true / probably |
| AV1 | `video/mp4; codecs="av01.0.05M.08"` | true / probably | true / probably |
| Opus | `audio/webm; codecs="opus"` | true / probably | true / probably |

[E1 probe](evidence/final-e1-h-memory/codecs.json), [E2 probe](evidence/mem-e2-h/codecs.json). Actual H.264/AAC decoding was observed for the controlled [W3C Sintel MP4](https://media.w3.org/2010/05/sintel/trailer.mp4) and advertising media, including D3D11 video / FFmpeg AAC decoder selection. Codec/decoder properties are retained in `diagnostics.jsonl`. Existing MCEF H.264/AAC probes were false; its Chromium 146 and these Chromium 156 binaries also differ in major version.

## 3. Real playback and UA retries

Success requires primary media readyState >=3 and advancing time. Short banner videos, paused two-second ad elements, prior history pages and metadata-only durations are excluded. Media stayed muted/volume 0. No login, payment, consent, age or human-verification gate was bypassed. Native play/ad-close/scroll controls used CDP Input; no forced third-party direct-source playback.

| Site / primary URL | E1 headless | E2 headless | Error or UA result |
| --- | --- | --- | --- |
| [YouTube](https://www.youtube.com/watch?v=dQw4w9WgXcQ) | **PASS**, 720p 25.984 -> 86.304 s | **PASS**, 720p 26.396 -> 86.829 s | Ready 4, decoded 1280x720, content duration 213.061 s; preroll waited out/excluded. |
| [Twitch PoineMaia](https://www.twitch.tv/poinemaia), from [live listing](evidence/twitch-listing.json) | **PASS**, 8.346 -> 14.401 s | **PASS**, 4.124 -> 10.257 s | Ready 4, 1280x720 live. No observed #4000/media error; no UA override. |
| [Kick Rammus53](https://kick.com/rammus53), from [native live links](evidence/kick-listing2.json) | **PASS**, 5.718 -> 11.869 s | **PASS**, 6.016 -> 12.108 s | Ready 4, 1280x720 live. Separate paused `black_2s.mp4` excluded. |
| [Vimeo 76979871](https://vimeo.com/76979871) | **NO** | **NO** | Default headless: human verification. Normal Chrome UA reaches native Play/player, but ready 0/time 0, duration metadata 61.853 s, media error null. |
| [Dailymotion xbsj7ry](https://www.dailymotion.com/video/xbsj7ry) | **NO** | **NO** | Black player area, no observed video/advancement with default or normal UA. No explicit unsupported/headless text or conclusive codec error. |
| [diziwatch episode](https://diziwatch.app/dizi/horror-collector/sezon-1/bolum-4) | **NO** | **NO** | Returns to preceding history entry before a stable Play control is available; same with normal UA. Cause unproven. Previous Dailymotion/Kick media is not episode success. |
| [Dizilla episode](https://dizilla.now/carrie-1-sezon-1-bolum) | **NO** | **NO** | Normal `Bölümü başlat` clicked. `four.pichive.online` frame refused by `X-Frame-Options: sameorigin` / `ERR_BLOCKED_BY_RESPONSE`. Preroll excluded. |
| [Dizipal episode](https://dizipalorijinal20.com/bolum/mobland-2-sezon-7-bolum) | **NO** | **NO** | Native ad-close + `bp-play-btn` reaches `/api/embed.php`; ready 0/time 0. Error 4 / `MEDIA_ELEMENT_ERROR: Format error` in repeated probes; some E2 observations remain loading/error-null. Inline 6.033 s banners play. Exact episode codec unproven. |

Vimeo/Dailymotion/diziwatch default-versus-normal-UA and Dizilla were repeated on the final harness in both engines. No explicit Chrome `headless/unsupported browser` message was observed; human verification, missing initialization, history navigation and frame-policy failures are separate outcomes. No frame/header bypass was attempted.

[E1 final observations](evidence/final-e1-ws/), [E2 final observations](evidence/final-e2-ws/), [E1 episode](evidence/final-e1-h-memory/dizipal-memory-after.json), [E2 episode](evidence/mem-e2-h/dizipal-memory-after.json). Streamed PNGs: [Vimeo verification](evidence/final-e1-ws/vimeo-final-proof-frame1.png), [Vimeo UA](evidence/final-e1-ws/vimeo-final-ua-proof-frame1.png), [Dailymotion](evidence/final-e2-ws/dailymotion-final-proof-frame1.png), [Dizilla](evidence/final-e1-ws/dizilla-final-proof-frame1.png), [Dizipal](evidence/final-e1-h-memory/dizipal-memory-proof-frame1.png).

The first Big Buck Bunny URLs initially played but later reached a player error, including after quality selection. Their failed soaks remain under `run/chrome-spike/mem-e1-h/` and are superseded by the accepted Rick Astley content: E1 H **25.984 -> 86.304**, E1 O **0.542 -> 61.149**, E2 H **26.396 -> 86.829**, E2 O **24.971 -> 85.366 s** over the recorded 60-second windows. All four subsequent memory points retain playing 1280x720 content in **10/10** samples.

## 4. Frames, decode, CPU and quality

`Page.startScreencast`: JPEG Q80, max 1280x720, everyNthFrame 1; every frame acknowledged, Base64 decoded and decoded with ImageIO plus STB when available. Frames 1/30 are retained as PNG/JPEG and every frame has a CSV row. Lower setting: max 854x480/Q55, actual aspect-preserving output **853x480**.

| Engine / case | Transport / video | Frames | Received fps median / p10 (1 s bins) | JPEG bytes median / p90 | Capture-to-receive ms median / p90 | ImageIO / STB median ms | Chrome / Java CPU (one core %) |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| E1 headless | WS, Sintel 720 Q80 | 481 | 24.00 / 24.00 | 53150.00 / 79796.00 | 8.29 / 11.75 | 7.60 / 3.50 | 53.47 / 32.16 |
| E1 headless | WS, Sintel 480 Q55 | 289 | 24.00 / 24.00 | 17628.00 / 25002.00 | 2.72 / 3.62 | 2.37 / 1.28 | 21.97 / 21.84 |
| E1 off-screen | WS, Sintel 720 Q80 | 361 | 24.00 / 23.00 | 43756.00 / 89101.00 | 6.18 / 8.50 | 7.16 / 2.85 | 65.46 / 27.94 |
| E2 off-screen | WS, Sintel 720 Q80 | 361 | 24.00 / 24.00 | 43819.00 / 89175.00 | 4.68 / 6.61 | 6.02 / 2.42 | 40.89 / 24.78 |
| E1 off-screen, initial pipe | Unbuffered pipe, YouTube 720 Q80 | 320 | 9.00 / 8.00 | 120155.00 / 134567.00 | 317.21 / 356.87 | 9.72 / 3.98 | 158.40 / 112.16 |
| E2 headless, initial pipe | Unbuffered pipe, YouTube 720 Q80 | 347 | 10.00 / 9.00 | 122750.00 / 139673.00 | 271.98 / 322.78 | 9.16 / 3.93 | 25.46 / 111.69 |
| E2 headless, initial pipe | Unbuffered pipe, YouTube 480 Q55 | 301 | 25.00 / 24.00 | 45072.00 / 48452.00 | 38.25 / 48.00 | 3.95 / 1.71 | 21.29 / 102.65 |
| E1 headless, final | Buffered pipe, YouTube 720 Q80 | 876 | 25.00 / 25.00 | 130464.00 / 144377.00 | 6.25 / 9.43 | 6.48 / 2.97 | 32.83 / 30.93 |
| E1 headless, final | Buffered pipe, YouTube 480 Q55 | 301 | 25.00 / 25.00 | 47665.00 / 49990.00 | 3.71 / 4.94 | 3.86 / 1.66 | 18.64 / 14.33 |
| E2 headless, final | WS, Sintel 720 Q80 | 361 | 24.00 / 24.00 | 45362.00 / 92740.00 | 6.05 / 7.73 | 6.65 / 2.66 | 62.83 / 29.88 |
| E2 headless, final | WS, YouTube 720 Q80 | 375 | 25.00 / 25.00 | 131794.00 / 139488.00 | 7.82 / 9.14 | 8.21 / 3.60 | 45.60 / 36.11 |
| E2 headless, final | WS, YouTube 480 Q55 | 301 | 25.00 / 25.00 | 47181.00 / 49201.00 | 3.71 / 4.83 | 3.83 / 1.58 | 36.42 / 19.79 |

FPS median/p10 use full one-second receive bins anchored at the first frame, including empty bins. Raw summaries also retain inverse-interarrival percentiles and duration-based averages. CPU is percent of **one logical CPU**; divide by 16 for this workstation's total-CPU percentage. E2 final YouTube is approximately 2.85% Chrome + 2.26% Java of this machine, before game rendering. Java includes both decoders, JSON/Base64, logging and proof writes. Owned-process CPU deltas can be sensitive to process churn.

Latency is capture timestamp to Java receipt, excluding texture upload, game queue and scanout. STB timing includes native decode/allocation to RGBA but excludes direct-input-buffer preparation/free; ImageIO includes stream/image construction. A product should use one decoder and pooled buffers.

Initial pipe code read one socket byte per call and used separate delimiter writes. That fixture bottleneck produced 9-10 fps / 272-317 ms. Buffered block reads, complete framed writes and TCP_NODELAY yield final E1 **25/25 fps, 6.25 ms**; E2's independent WebSocket result is **25/25 fps, 7.82 ms**. These initial pipe numbers are retained as diagnostics, not an engine limit. Controlled off-screen capture works in both engines at approximately 24 fps.

Final E2 lower-size JPEG median falls **131,794 -> 47,181 bytes** (about 64%), latency **7.82 -> 3.71 ms**, Java CPU **36.11 -> 19.79% of one core**, with 25 fps bins retained. A 1280x720 RGBA upload at 30 fps would be approximately **105.47 MiB/s**, not implemented here.

Visual inspection: Q80 gives readable text/useful video; Q55/480 visibly softens small text and details on enlargement. Shots are different timestamps, so no pixelwise quality score is claimed. [Q80](evidence/final-e1-h-memory/youtube720-memory-stream-frame30.png), [Q55](evidence/final-e1-h-memory/youtube720-low-q55-frame30.png), [E2 Q80](evidence/final-e2-ws/youtube720-q80-frame30.png), [derived stream data](tables/streams.json). The controlled Sintel source is smaller than its 1280x720 page capture; the YouTube memory content has actual 1280x720 decoded dimensions.

## 5. Audio

CDP transports frames/control here, not PCM. Sound uses Chrome's Windows output. Core Audio session enumeration was read-only and filtered to owned PIDs on the default render endpoint.

| Engine / mode | Observation |
| --- | --- |
| E1 headless | Owned audio-service PID 9596: peak up to 0.05 during the single two-second check, then zero. |
| E2 headless | Owned PID 5500: peak up to 0.05 during its single two-second check, then zero. |
| E1 off-screen | Owned session seen active, peak 0 with muted media; nonzero audible output not retested. |
| E2 off-screen | Owned session seen, peak 0/inactive at muted observation; nonzero output not established. |

[E1 tone](evidence/e1-headless/audio-tone.json), [E2 tone](evidence/e2-h/audio-tone.json), [E1 normal muted](evidence/e1-o/audio-muted.json), [E2 normal muted](evidence/e2-o/audio-muted.json). The one audible allowance per engine was spent in headless mode. Session peaks prove an output graph, not physical speaker audibility. **Off-screen audible output remains unverified.**

Element `volume`/`muted` is controllable through Runtime in accessible native frames/OOPIFs. Web Audio needs graph control or the owned Windows audio session. No general CDP per-page gain command was established. Global muting is test safety, not distance attenuation; a product needs element replacement/Web Audio coverage and engine close as its out-of-range boundary.

## 6. Control and policy

Both engines pass final assertions: navigation to HTTPS fixture; CDP click/type reads exactly **`Perde CDP 25` / one click**; wheel reads **scrollY=400**; cross-site example.com OOPIF Runtime evaluation and `Runtime.addBinding` events are distinct from example.org; Fetch pauses/fails **pagead2.googlesyndication.com/pagead/js/adsbygoogle.js**, and page fetch rejects; camera/microphone/geolocation/notification permissions read back **denied**; popup closes and the opener remains responsive with one original page.

[E1 control](evidence/final-e1-ws/control.json), [E2 control](evidence/final-e2-ws/control.json), per-engine `diagnostics.jsonl`. A still-paused popup could stall the opener in an early fixture; resume-before-close was implemented and retested.

These probes do not establish a complete product network boundary. Workers, service workers, WebSockets, redirects, downloads and all privacy paths need a later regression suite. Bindings are untrusted data requiring bounds, source/context identity and document-generation checks. Server authorization/allowlist remain authoritative and unchanged.

## 7. Extensions and ads

| Engine | `--load-extension` | Unsafe debugging pipe + `Extensions.loadUnpacked` |
| --- | --- | --- |
| E1 installed | No unpacked extension listed | **Works**, uBOL enabled |
| E2 CfT | **Works**, uBOL enabled | **Works** as well |

[E1 flag](evidence/e1-ext2/load-flag.json), [E1 CDP](evidence/e1-ext2/load-cdp.json), [enabled](evidence/e1-ext2/extensions.json), [E2 flag](evidence/e2-ext/load-flag.json), [E2 CDP](evidence/e2-ext/load-cdp.json). Version 2026.1026.1812; unpacked id at this directory `gphegpfnhpagigdooakiaeooaphgiaje`. Default settings only; enhanced/cosmetic host-access configuration is not claimed.

With Fetch disabled, paired same-URL Dizipal windows record **0** failed-by-client requests without uBOL and **1** with it in every engine/mode. Redirects are not counted as failed requests. First-party banner videos/player overlay remain visible: **no complete visual ad removal or episode improvement demonstrated**. [Without](evidence/final-e1-h-memory/ad-no-extension-frame30.png), [with](evidence/final-e1-h-memory/ad-with-extension-frame30.png), [counter](evidence/mem-e2-h/ad-with-extension.json).

An ad fetch resolved under extension rewriting before the original Fetch URL pattern saw it. The Fetch assertion was retained; uBOL was uninstalled from the owned fixture profile and the isolated test passed. Combined ad/policy interception needs this interaction covered.

## 8. Memory and the 16 GB budget

**24 points x 10 samples = 240 accepted process snapshots.** All currently alive owned Chrome descendants are summed; every PID has role/working-set/private-byte data. Java uses its exact PID. Sampling is one second plus query time; early per-PID CIM queries were slower than batched queries. Memory cases ran sequentially with one owned browser. Some earlier playback/performance probes had other isolated instances alive; those are not the memory matrix.

The 720p point follows the recorded 60-second content soak and samples with screencast/both decoders active. All four points have **10/10** playing 720p samples. **Successful episode-playing memory is unavailable**: row c explicitly measures a failed episode page after a 60-second attempt. The extension pair uses the same URL/wait/scroll. E1 normal's pair was repeated in reverse order after removing uBOL; other pairs are plain then extension. These are one navigation per condition with ten samples, not ten independent loads or a causal ad-auction experiment.

MiB = bytes / 1,048,576. WS sums include shared resident pages and are not unique physical RAM. Private bytes are committed private memory, not resident RAM. GPU/WDDM allocations outside process counters are excluded. Historical MCEF maxima are recomputed from the original ten-sample helper CSVs; its full Minecraft JVM is not compared with the standalone spike JVM.

| Engine / mode | Point (10 samples) | Chrome WS mean / max MiB | Chrome private mean / max MiB | Java WS mean / max MiB | Java private mean / max MiB | Chrome + Java WS mean / max MiB |
| --- | --- | ---: | ---: | ---: | ---: | ---: |
| E1 headless | a: initial blank | 615.10 / 623.02 | 319.68 / 323.79 | 81.32 / 83.74 | 624.08 / 626.56 | 696.42 / 706.76 |
| E1 headless | b: 720p after 60 s | 1156.68 / 1162.54 | 838.03 / 842.85 | 516.37 / 520.50 | 646.43 / 649.62 | 1673.05 / 1680.11 |
| E1 headless | c: failed episode page, not playing | 1095.48 / 1095.66 | 899.30 / 900.04 | 528.08 / 528.62 | 655.40 / 655.91 | 1623.56 / 1624.05 |
| E1 headless | d: same dizi URL, no uBOL | 1111.92 / 1135.80 | 937.13 / 967.19 | 529.04 / 529.39 | 650.21 / 650.64 | 1640.96 / 1664.12 |
| E1 headless | d: same dizi URL, uBOL | 1011.72 / 1018.68 | 873.08 / 883.10 | 533.98 / 534.04 | 654.68 / 654.90 | 1545.70 / 1552.59 |
| E1 headless | e: returned to blank | 1006.69 / 1011.77 | 832.63 / 835.11 | 533.87 / 533.92 | 654.73 / 654.78 | 1540.56 / 1545.62 |
| E1 off-screen | a: initial blank | 605.86 / 645.90 | 327.73 / 345.28 | 452.95 / 455.29 | 632.13 / 634.68 | 1058.80 / 1098.61 |
| E1 off-screen | b: 720p after 60 s | 1865.79 / 1888.73 | 1539.69 / 1566.39 | 515.03 / 517.44 | 638.38 / 641.27 | 2380.83 / 2403.14 |
| E1 off-screen | c: failed episode page, not playing | 1312.83 / 1313.92 | 1043.26 / 1045.00 | 518.20 / 518.35 | 640.28 / 640.87 | 1831.02 / 1832.18 |
| E1 off-screen | d: same dizi URL, no uBOL | 1033.42 / 1037.54 | 825.71 / 830.37 | 524.06 / 524.38 | 649.41 / 649.78 | 1557.48 / 1561.40 |
| E1 off-screen | d: same dizi URL, uBOL | 1348.74 / 1352.00 | 986.66 / 990.29 | 520.60 / 520.68 | 648.30 / 648.58 | 1869.34 / 1872.54 |
| E1 off-screen | e: returned to blank | 1128.69 / 1131.59 | 876.99 / 881.03 | 515.15 / 515.25 | 637.29 / 637.36 | 1643.85 / 1646.74 |
| E2 headless | a: initial blank | 624.75 / 641.33 | 332.61 / 350.96 | 526.42 / 526.98 | 651.73 / 652.29 | 1151.17 / 1167.62 |
| E2 headless | b: 720p after 60 s | 1366.69 / 1397.58 | 958.27 / 1003.37 | 530.48 / 533.42 | 652.44 / 655.60 | 1897.17 / 1928.66 |
| E2 headless | c: failed episode page, not playing | 1616.28 / 1623.75 | 1173.67 / 1181.60 | 525.87 / 525.88 | 647.07 / 647.08 | 2142.15 / 2149.62 |
| E2 headless | d: same dizi URL, no uBOL | 1516.06 / 1518.39 | 1085.66 / 1088.29 | 531.17 / 531.25 | 652.75 / 653.06 | 2047.23 / 2049.42 |
| E2 headless | d: same dizi URL, uBOL | 1424.59 / 1426.95 | 1035.18 / 1037.93 | 530.04 / 531.04 | 651.67 / 653.35 | 1954.63 / 1956.87 |
| E2 headless | e: returned to blank | 1489.53 / 1501.99 | 1053.46 / 1066.52 | 531.04 / 531.04 | 653.35 / 653.35 | 2020.57 / 2033.03 |
| E2 off-screen | a: initial blank | 640.83 / 652.84 | 345.45 / 355.90 | 531.78 / 532.98 | 654.18 / 655.43 | 1172.61 / 1184.14 |
| E2 off-screen | b: 720p after 60 s | 1547.26 / 1577.26 | 1119.73 / 1158.58 | 536.17 / 539.95 | 657.91 / 661.57 | 2083.43 / 2115.42 |
| E2 off-screen | c: failed episode page, not playing | 1662.20 / 1662.65 | 1129.98 / 1130.51 | 535.01 / 535.14 | 657.32 / 658.06 | 2197.22 / 2197.79 |
| E2 off-screen | d: same dizi URL, no uBOL | 1594.88 / 1599.11 | 1111.72 / 1116.02 | 536.37 / 536.37 | 658.93 / 658.93 | 2131.24 / 2135.47 |
| E2 off-screen | d: same dizi URL, uBOL | 1391.64 / 1393.61 | 953.35 / 955.21 | 536.61 / 536.62 | 663.09 / 663.10 | 1928.25 / 1930.23 |
| E2 off-screen | e: returned to blank | 1438.07 / 1448.70 | 942.91 / 953.68 | 536.61 / 536.63 | 663.07 / 663.10 | 1974.68 / 1985.30 |
| MCEF, F2 full-profile YouTube | Historical, 10 samples | 752.84 / 806.79 | n/a | Full Minecraft JVM, not comparable | n/a | n/a |
| MCEF, F2-1 audited YouTube | Historical, 10 samples | 849.12 / 892.84 | n/a | Full Minecraft JVM, not comparable | n/a | n/a |
| MCEF, F2-1 YouTube/ad | Historical, 10 samples | 1118.40 / 1124.48 | n/a | Full Minecraft JVM, not comparable | n/a | n/a |

[Derived memory/combined private data](tables/memory.json), [process roles by point](tables/process-roles.csv), [F2-1 baseline](../phase2/F2-1-report.md), [F2 baseline](../phase2/F2-report.md), full `memory-*-samples.json` in [evidence](evidence/). Roles observed: **browser, GPU, network, utility, crashpad utility, renderer, extension renderer**. Built-in extension renderers exist at blank; their presence alone is not uBOL proof. The pipe helper adds approximately **83-84 MiB WS / 65-66 MiB private**, separately reported, removable in a native/JDK product transport.

Blank navigation retains substantial memory: initial -> returned Chrome mean **615.10 -> 1006.69 (E1 H), 605.86 -> 1128.69 (E1 O), 624.75 -> 1489.53 (E2 H), 640.83 -> 1438.07 MiB (E2 O)**. Full close leaves zero tracked Chrome processes. A backend should close the complete isolated engine when no screen is active, instead of relying on blank navigation to reclaim RAM.

Standalone Java warms from about **81 MiB WS** to **516-537 MiB** around accepted video points. Several cases reuse a warm controller; later blank Java values include retained heap and are not intrinsic engine overhead. The first series uses the slower initial pipe reader; final E1 headless uses the corrected path. Java's approximately 624-663 MiB private commit differs from resident WS. Both decoders/fixture allocations make this a conservative product estimate.

**Expected extra RAM/player:** measured browser contribution is about **1.13-1.82 GiB WS**, versus historical MCEF helper means **0.74-1.09 GiB**. Chrome + standalone spike means are **1.63-2.33 GiB**, max **2.35 GiB**, or approximately **2.43 GiB** including the relay. Largest combined private video sample is about **2.15 GiB**. Plan roughly **2 GiB additional resident budget for one active browser, reserve 3 GiB until integration is measured**. This is a planning estimate, not a ceiling: successful episode memory is unknown, pages/ads vary, and UMA GPU use is partly outside these counters. A Minecraft implementation reuses its existing JVM and needs one pooled decoder, so adding the entire standalone Java WS to an existing game JVM overstates some overhead. Real pack/GPU upload contention remains unmeasured. Single active browser, full out-of-range teardown and a real 16 GB acceptance run are conditions for proceeding.

## 9. Robustness

Both engines passed two simultaneous independent profiles: same-origin storage **A/B** stays separate; kill A's exact root PID, B remains responsive with **B**; restart A in a fresh profile, storage is empty. Surviving owned descendants are collected and stopped. [E1 isolation](evidence/isolation-isolate-e1.json), [E2 isolation](evidence/isolation-isolate-e2.json), [final cleanup](evidence/final-cleanup.json).

No image-name kill, force push, history rewrite, real-profile access, unrelated process/window control or software installation. Owned kill sets are disjoint from baseline user Chrome PIDs; surviving original processes retain their start times. Ordinary helper churn is recorded rather than described as an unchanged-process guarantee. [Baseline audit](evidence/baseline-survival.json).

## Proposed architecture and separate follow-up tasks

Keep the client-only `BrowserBackend` contract/server-authoritative state. A Chrome backend returns an owning `GpuTextureView` through existing `textureView()`. Decode/network work stays off render/game threads; use pooled STB RGBA buffers, bounded latest-frame handoff, navigation/close generations and render-thread upload/cleanup. One 1280x720 RGBA buffer is 3.52 MiB. The existing world renderer is the target, not yet validated with these uploads.

Use native-frame Runtime evaluation for play/pause/seek/drift/volume and bounded `addBinding` messages for observations. Apply policy across all native targets/redirects; preserve server validation, permissions/download/dialog/popup restrictions. Native Chrome plays audio; element/Web Audio distance control needs coverage. uBOL is optional ad reduction, Fetch an explicit policy layer. Profile/process lifetime must follow the isolated owned engine.

Suggested tasks, rough effort only, **not started**:

1. **CHROME-1, lifecycle/transport (1-2 days):** factory selection/version probe, isolated profiles, owned teardown, WS/pipe and crash recovery.
2. **CHROME-2, renderer (2-3 days):** pooled decode/upload, bounded handoff, resolution/fps and actual game latency/CPU/GPU accounting.
3. **CHROME-3, sync/audio/policy (2-3 days):** frame lifecycle/guarded messages, existing sync decisions, volume, complete policy regressions.
4. **CHROME-4, distribution/update (1-2 days):** official artifacts/version/hash provenance, failure/upgrade/rollback and terms/distribution review. No browser binary committed.
5. **CHROME-5, acceptance (1-2 days):** real 16 GB/pack, Sodium/Iris, two clients/late join, long soak, dedicated-server isolation and site matrix. Product adoption needs a later explicit architecture decision/SPEC update.

If bounded single-decoder streaming with 480p/Q55 remains too expensive, investigate a separate native owned-window/GPU capture bridge or LEA-200's ABI-matched codec-enabled CEF path. Shared-texture/off-screen behavior needs its own feasibility task. No fallback implementation or security-header bypass is included here.

## Verification and corrections

Java 25 standalone compilation and real CDP probes completed; four 720p/60-second runs, 240 memory snapshots, actual control/OOPIF/binding/Fetch/permission/popup assertions and per-engine isolation/restart pass. Repository-local `GRADLE_USER_HOME`, `.\gradlew.bat build --rerun-tasks --console=plain`: **SUCCESSFUL in 10 s, 9 tasks, 296 tests, zero failures/errors/skips**. [Build](evidence/build-final.txt), [verification](evidence/verification.json). No assertion/test weakened or removed.

Fixture corrections: OOPIF resume order; pipe buffering/delimiter handling; stale contexts; muter writing unchanged properties; popup resume-before-close; preroll versus content-quality selection; extension redirect versus Fetch assertion. An early overlapping site driver and previous-history diziwatch media are not acceptance evidence. Failed logs remain under `run/`; final repeats/accepted matrix are explicit above.

No product/common/client/resource/build change, Minecraft screenshot or in-game integration claim. The report is a measurement result; no next phase, deployment or merge was started.
