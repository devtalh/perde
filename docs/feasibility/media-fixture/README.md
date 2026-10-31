# F1B-2 reproducible media fixture

Historical procedure for the F1B-2 revision. F2-3 removed all `/perde-spike` commands, and F2-2 intentionally blocks loopback pages. The commands below no longer run on current Perde. Keep this fixture as the original feasibility record; use the [Phase 2 fixture](../../phase2/fixture/README.md) with a trusted public HTTP(S) page for current real-screen checks. Do not relax the production URL policy to reuse the old loopback test.

This HTTP page makes orientation, live page updates and exact video time visible on the world quad. Its JavaScript only reads media state. The first video starts paused; native controls remain available. `empty.html` contains no video or JavaScript.

From the repository root, copy the fixture sources into the ignored Gradle run directory and generate the test media with an existing FFmpeg installation:

```powershell
New-Item -ItemType Directory -Force run/f1b-2-test | Out-Null
Copy-Item -LiteralPath docs/feasibility/media-fixture/index.html,docs/feasibility/media-fixture/empty.html,docs/feasibility/media-fixture/serve.py -Destination run/f1b-2-test
$mediaArgs = @(
    '-hide_banner', '-loglevel', 'warning', '-y',
    '-f', 'lavfi', '-i', 'testsrc2=size=640x360:rate=5:duration=120',
    '-f', 'lavfi', '-i', 'sine=frequency=440:sample_rate=48000:duration=120',
    '-c:v', 'libvpx', '-deadline', 'realtime', '-cpu-used', '8', '-threads', '2',
    '-b:v', '350k', '-g', '25', '-c:a', 'libvorbis', '-q:a', '3',
    '-t', '120', 'run/f1b-2-test/sample.webm'
)
& ffmpeg @mediaArgs
python run/f1b-2-test/serve.py --port 8765
```

The generated 120-second WebM uses VP8 video and Vorbis audio (640x360, 5 fps, 440 Hz mono tone). It stays under `run/` and is not committed. The server binds only to `127.0.0.1`, serves the four explicit fixture routes and supports HTTP byte ranges. Python 3.12's basic `http.server` ignores Range requests; the initial in-game seek stayed at 0.00 until it was replaced with this server. Stop any existing server on port 8765 first. Stop this server with Ctrl+C after testing.

In a Gradle client creative world:

```text
/perde-spike place http://127.0.0.1:8765/
/perde-spike play
/perde-spike pause
/perde-spike seek 60
```

After pausing and seeking, the status must show `time=60.00`, `paused=true`, `muted=false`, `volume=1.00`. The independent heartbeat keeps increasing while paused. `TOP LEFT` / `TOP RIGHT` make mirroring and vertical orientation visible. Recreate the browser with `place` after changing servers so it loads the new media resource.

For the no-video check, place `http://127.0.0.1:8765/empty.html`, then repeat play, pause and seek. These commands must safely do nothing.

The range server passed prefix, keyframe-offset, suffix, HEAD, unsatisfiable-range and full-file HTTP checks. The actual MCEF world test then showed `time=60.00` while paused, captured with F2 at 17:15:11 on 2026-10-30 (Europe/Istanbul).

Independent audio evidence is in [`../f1b-2-audio.csv`](../f1b-2-audio.csv), filtered to the JCEF render session during the earlier YouTube autoplay, pause and play checks. Windows Core Audio peak polling sampled the browser's render stream. No audio was recorded, no microphone was accessed and no audio/device settings were changed. Nonzero peaks prove that JCEF submitted audio; they do not measure physical headphone or speaker audibility.
