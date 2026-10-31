# F2-2: browser navigation and privacy policy

Status: **partial, draft review only**. Branch `feat/f2-2-browser-policy`, based on main `6b75980`. [F2-1 PR #6](https://github.com/devtalh/perde/pull/6) was confirmed merged before implementation. [Linear LEA-185](https://linear.app/leak-rake/issue/LEA-185) and the Notion Perde hub were read; repository SPEC and the supplied F2-2 task govern current implementation. Linear field notes still return `Tool get_agent_context not found`.

Talha is playing Minecraft on this PC. As explicitly instructed, this stage used **no Computer Use, no runClient/runClient2, and no game windows**. Only implementation, unit tests, build, fixture compilation and a windowless loopback dedicated-server boot were performed. Acceptance checks (a) through (e) remain **pending**. Do not mark ready or merge. Resume game checks only after Talha explicitly says he has finished playing. The next task after completion is **F2-3 full-pack validation, culling, spike removal**.

## Policy and server behavior

- Pure Java `screen.UrlPolicy`, with no Minecraft/MCEF/JCEF imports, separates page entry, navigation and resource decisions. Public HTTP(S) with a host is allowed for page entry. Navigation additionally allows the exact internal `about:blank` exception. WebSocket, data and blob URLs are resource-only; blob origins are checked when present. Everything else is denied.
- Hosts are normalized for case, IDN/dot variants and a trailing dot. Userinfo is separated from the actual host. Ambiguous escaped/scoped hosts and malformed ports fail closed. Chromium-style IPv4 decimal, octal, hex, short and single-integer forms are parsed without DNS. IPv6 uses Java 25 `InetAddress.ofLiteral`, which does not resolve names; mapped IPv4 is checked with the same IPv4 rules.
- Denied literals: localhost/subdomains, 0/8, 10/8, 127/8, 172.16/12, 192.168/16, 169.254/16, 100.64/10, unspecified IPv6, ::1, fc00::/7, fe80::/10, mapped forms, and .local/.lan/.home/.internal suffixes. Multicast literals are also denied.
- **DNS-based bypasses, rebinding and public hostnames resolving to private IPs are out of scope.** This is literal validation, not a network sandbox or DNS firewall.
- `ScreenState.validateUrl` applies the same page policy on the server and client. Unauthorized requests still fail permission checks first. Invalid authorized SetUrl requests return the existing `INVALID_URL` rejection and retain the original state. English/Turkish action-bar text now explains that local/private hosts are blocked.
- Persisted URLs pass through `ScreenState.savedUrl` in `PerdeBlockEntity.loadAdditional`; newly disallowed legacy URLs become empty without discarding geometry/ownership. Tests exercise that exact normalization helper and unchanged-state server decisions.

## MCEF findings and handler wiring

Verified against [MCEF Modern df61843 source](https://github.com/DimasKama/mcef-modern/tree/df61843) and the resolved [JCEF sources jar](https://repo.maven.apache.org/maven2/me/friwi/jcef-api/jcef-d3de827+cef-146.0.10+g8219561+chromium-146.0.7680.179/jcef-api-jcef-d3de827+cef-146.0.10+g8219561+chromium-146.0.7680.179-sources.jar), not current MCEF master. The matching [JCEF native context factory](https://github.com/chromiumembedded/java-cef/blob/d3de827/native/CefRequestContext_N.cpp) confirms CreateContext uses a new default-settings context, rather than the global-context factory. Downloaded source copies remain under ignored `run/f2-2-sources/`.

`MCEFApiImpl.java:24-38` constructs one process-wide CefApp and adds `--disable-web-security` at line 28, before `build()`. Its private shared CefClient is created at line 38, and `createBrowser` at lines 58-69 uses that client and the global request context. There are **no upstream add*Handler calls** in this 26.2 MCEF implementation. Rendering and cursor handling live on `MCEFBrowserImpl` / `CustomCefBrowserOsr` themselves, rather than separately installed client handlers.

There is no per-browser API to remove this global command-line switch. `CefBrowserSettings` in this JCEF exposes only the windowless frame-rate setting. Perde does **not** mutate global command-line state, replace CefApp, patch MCEF, or claim that CORS/same-origin isolation was restored. YouTube behavior with the new policy remains pending. **Players should not sign in to accounts inside Perde.** SPEC.md is unchanged; this report proposes adding that limitation to its security section during a separate design review.

Perde now constructs the same upstream `MCEFBrowserImpl` renderer with a **dedicated CefClient and isolated CefRequestContext per managed browser**. It uses the verified public implementation constructor, then resize, setCloseAllowed and createImmediately. This intentionally couples the backend to the pinned MCEF implementation; recheck the constructor/lifecycle on an upgrade. It neither modifies nor replaces handlers on the shared MCEF client. Spike and other mods remain on their own clients. Owned callbacks additionally require exact CefBrowser object identity. Native callbacks are attached before createImmediately, so the initial public request cannot precede handler installation.

The browser initially opens only `about:blank`. In the CEF UI-thread `onAfterCreated` callback it installs and reads back the required native permission defaults, then loads the desired public URL. Missing, immutable, rejected or ineffective settings fail closed: no public page is loaded, the native browser closes, and the manager reports browser failure. A per-creation session object prevents delayed callbacks from an older browser using a replacement browser's state. The manager still owns GPU cleanup on the Minecraft client thread. JCEF's `CefBrowser_N.onBeforeClose:132-134` disposes its request context; Perde does not dispose it a second time. The dedicated client is disposed on normal manager closure.

| Handler/API | Behavior |
| --- | --- |
| CefRequestHandler.onBeforeBrowse | Validate main and subframe navigations, including redirects delivered to this callback. Cancel invalid navigation before commit. |
| onOpenURLFromTab / CefLifeSpanHandler.onBeforePopup | Cancel all new-tab/popup requests. |
| getResourceRequestHandler / onBeforeResourceLoad | Apply navigation or resource policy; cancel downloads and invalid requests. A private context handler also covers browserless requests delivered by CEF, such as worker requests. |
| onResourceRedirect | Validate the target and replace a denied target with a non-network denied sentinel (`perde-blocked:redirect`). It cannot return a cancellation boolean. The subsequent load/protocol callback rejects the sentinel. Native visual behavior remains pending. |
| onProtocolExecution | Always deny OS protocol execution. |
| CefDownloadHandler | Claim onBeforeDownload without ever calling Continue; cancel every update callback. This avoids both default download handling and the file picker. |
| CefDialogHandler | Cancel the file callback and return handled. |
| CefJSDialogHandler | Suppress alert/confirm/prompt; accept leaving without showing beforeunload UI. |
| CefContextMenuHandler | Clear the native context menu, including clipboard/print/DevTools entries. |
| Request-context preferences | Set microphone, camera, geolocation, notifications, clipboard, automatic downloads, file-system read/write guards to CONTENT_SETTING_BLOCK (2), and verify readback before navigation. |

Permission preference names and BLOCK support were verified from Chromium **146.0.7680.179** [content settings registry](https://github.com/chromium/chromium/blob/146.0.7680.179/components/content_settings/core/browser/content_settings_registry.cc) and [preference-name conversion](https://github.com/chromium/chromium/blob/146.0.7680.179/components/content_settings/core/browser/website_settings_info.cc). The latter prefixes `profile.default_content_setting_values.` and converts hyphens to underscores.

### Explicit API limits and remaining risks

- This JCEF sources jar contains **no CefPermissionHandler or permission callbacks** for media, geolocation, notifications or clipboard. Native defaults are the available mitigation. Per-attempt permission logging cannot be implemented through a nonexistent handler. `permission-default/<setting>` INFO entries record configured policy once per page, **not an observed permission request or proof of a native denial**. Runtime denial and hardware access must still be checked in (c). The strict per-attempt permission audit requirement remains an API limitation for review, even after those checks.
- `BlockedRequestLog` logs each callback category once per accepted main-frame navigation, bounds URLs to 200 characters and sanitizes control/line-separator characters. Blocked navigation does not reset the log allowance. UI and IO threads share a synchronized audit. No blocked-request native log excerpts are claimed yet because no client was launched.
- Unit tests prove policy decisions and callback behavior without Chromium natives. They do not establish complete native interception of WebSockets, renderer-local schemes, WebRTC or every worker request. These callbacks are not a general network firewall. CEF itself tracks [worker requests bypassing request callbacks](https://github.com/chromiumembedded/cef/issues/3593); the private context fallback is implemented, but complete coverage in this pinned runtime remains unproven. Do not describe this implementation as guaranteeing isolation from all local-network traffic.
- Browser creation now uses a private client/context and does not reuse MCEF's shared cookie context. Cookie/cache persistence and native teardown must be observed in the resumed client checks. No filesystem cache deletion or global MCEF cache mutation is performed.

## Completed verification (2026-10-31, Europe/Istanbul)

Java/Javac 25.0.4. Every Gradle command used repository-local `GRADLE_USER_HOME=.gradle-user-home`. Test-generated log files are confined to `build/test-runtime`; no client runtime was started.

```text
gradlew.bat test --console=plain
BUILD SUCCESSFUL in 6s

gradlew.bat -I docs/phase2/fixture/probe.init.gradle compilePhase2Probe --console=plain
BUILD SUCCESSFUL in 6s

gradlew.bat build --rerun-tasks --console=plain --warning-mode all
BUILD SUCCESSFUL in 11s
9 actionable tasks: 9 executed
147 tests, 0 failures, 0 errors, 0 skipped

gradlew.bat build --console=plain --warning-mode all
Final follow-up after the browserless-download audit adjustment:
BUILD SUCCESSFUL in 6s
9 actionable tasks: 4 executed, 5 up-to-date
147 tests, 0 failures, 0 errors, 0 skipped
```

[Build output](f2-2-build.txt), [test counts by suite](f2-2-tests.csv). Tests cover every listed host family/scheme, parser tricks, boundary public addresses, IDN dots, userinfo, ports, saved-URL clearing, server rejection/state retention, handler browser identity, navigation/resources/redirect sentinel, browserless resource callback, popups, file/JS dialogs, download continuation refusal/cancel, native preference failure paths and bounded audit/reset behavior. Initial test-double compilation failed because JCEF constructors are package-private; test-only doubles were moved into their API packages. No test/assertion was weakened or disabled.

Default built jar inspection: no MCEF/JCEF classes, nested jars, tests or fixture classes bundled. Common Java sources contain no client/MCEF/JCEF imports. English/Turkish key parity remains 43 each. SPEC, AGENTS and production spike code are unchanged. Git diff and whitespace checks passed.

Dedicated-server boot used fresh `run/f2-2-server`, `server-ip=127.0.0.1`, `server-port=25572`, explicit `--nogui`, redirected stdin/stdout and Windows `CREATE_NO_WINDOW`. Only that launcher's stdin received `stop`. The OS listener check verified **exactly one loopback listener**, PID 50304, whose command line contains this repository's DLI config. No other Minecraft process was controlled. Both owned launcher/server exited; no listener remained. [Complete server log](f2-2-server.txt), [socket/PID ownership evidence](f2-2-server-binding.txt).

```text
[15:02:57] [main/INFO] (FabricLoader) Loading 44 mods:
[15:03:10] [main/INFO] (perde) Perde initialized.
[15:03:12] [Server thread/INFO] (Minecraft) Starting Minecraft server on 127.0.0.1:25572
[15:03:15] [Server thread/INFO] (Minecraft) Done (2.529s)! For help, type "help"
[15:03:17] [Server thread/INFO] (Minecraft) Stopping the server
[15:03:17] [Server thread/INFO] (Minecraft) ThreadedAnvilChunkStorage: All dimensions are saved
BUILD SUCCESSFUL in 24s
5 actionable tasks: 1 executed, 4 up-to-date
```

The mod list contains no MCEF. Existing Windows OSHI/performance-counter errors are retained in the full log; Minecraft continued to Done and shut down cleanly. No registry/system changes were attempted.

## Remaining in-game acceptance: all pending

| Check | Status and exact follow-up |
| --- | --- |
| (a) Public playback | **pending**: one Perde screen, load example.com and an ordinary YouTube video through the real panel; verify rendering/playback with the new private client/context, capture unchanged F2 screenshots under docs/phase2/screenshots. |
| (b) Server validation | **pending**: submit http://192.168.1.1, http://localhost:8080 and http://127.0.0.1 through the panel; capture translated action-bar rejection and unchanged screen state. Unit tests are already passing. |
| (c) Page attacks | **pending**: from a public page use opt-in fixture probes for file navigation, window.open, loopback fetch, private iframe, JS dialogs, media access and download link. Audit native result and logs; verify continued rendering and no dialogs/windows/downloads or hardware access. Permission-default logs alone are insufficient evidence. |
| (d) Popup/redirects | **pending**: verify denied popups and blocked-scheme navigation/HTTP redirects retain the displayed page. Include an actual public HTTP redirect response, not only a location assignment. Observe sentinel behavior and fix any regression before marking ready. |
| (e) Spike | **pending**: outside managed-screen range, /perde-spike place still renders; remove it and verify cleanup. |

See [opt-in fixture instructions](fixture/README.md). `policy` is development-only and absent from the default jar. It targets only the managed browser by reflection, and logs dispatch separately from native results. Once Talha explicitly finishes playing, run these checks, retain exact native logs/screenshots and provenance, resolve or explicitly review the API limits above, finish this report/PROGRESS, rerun build for any code changes, and only then mark the draft PR ready. No F2-3 work or merge in this task.
