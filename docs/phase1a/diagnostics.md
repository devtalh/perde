# Phase 1A diagnostic notes

## Native crashes during explicit window activation

On 2026-10-31 at 00:38:24 Europe/Istanbul, the first Player2 process (PID 19220) exited during an attempted activation of its own verified dev window. Computer Use returned `failed to activate captured window`. The next identity check found the process absent and blocked further input. The server kept running and saved Player2's disconnection. No other game process was targeted.

Its repo-local `run/client2/hs_err_pid19220.log` identifies:

```text
EXCEPTION_ACCESS_VIOLATION (0xc0000005)
Problematic frame: C [glfw.dll+0x10fa1]
Current thread: JavaThread "Render thread", in native code
Java frames:
  org.lwjgl.system.JNI.invokeV(J)V
  com.mojang.blaze3d.systems.RenderSystem.pollEvents()V
  net.minecraft.client.Minecraft.run()V
  net.minecraft.client.main.Main.main(String[])
```

There is no Perde Java frame in this crashing stack. The exact native cause is **unresolved**; the timing does not prove that activation, MCEF, the renderer, the GPU driver, or another component caused it. Do not call this harmless or fixed. A second Player2 process (PID 44528) reproduced the same native frame at 00:39:58 during explicit window activation. Further attempts using that activation path are stopped; a fresh process will be observed without an explicit activation call. Player1 PID 3596 reproduced the same native frame at 12:56:16, even with Player2 using javaw. That rules out the separate-launcher experiment as a fix; the fixture uses normal launchers again. At 13:01:39, direct F2 input to a verified owned window succeeded and the subsequent capture showed the correct game. Later UI work avoids explicit activate_window and uses only guarded normal input. This narrows the observed trigger without proving a native root cause. Completion results belong in the main report. Full logs and native report remain under ignored `run/client2/` and `build/phase1a-verification/`.

## Placeholder mip level

The first placeholder texture was a solid 1x1 PNG under `textures/block/`. The vanilla block atlas included it and reported that it reduced the whole atlas mip level from 4 to 0. It is now an original solid 16x16 PNG, matching the other Perde textures. This is a resource correction, independent of the native crash; no claim of a causal connection is made. Fresh-launch log verification follows in the main report.

At 13:04, direct F2 input while another Minecraft was foreground also reproduced the activation failure and native crash (Player2 PID 40864). Successful direct input from a non-game foreground did not establish safe game-to-game focus switching. Further cross-client UI focus attempts are stopped. Integration uses a foreground client for GUI/F2 evidence and the opt-in repository-local fixture for normal payloads from the other connected background client. This is test orchestration, not a native-crash fix.
