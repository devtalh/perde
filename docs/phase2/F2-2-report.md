# F2-2: browser navigation and privacy policy

Status: **partial, lead-approved allowlist implemented; runtime acceptance next.** Branch `feat/f2-2-browser-policy`, [draft PR #7](https://github.com/devtalh/perde/pull/7), based on main `6b75980`. Implementation commit: `bdc33b7`. [F2-1 PR #6](https://github.com/devtalh/perde/pull/6) was confirmed merged before implementation. [Linear LEA-185](https://linear.app/leak-rake/issue/LEA-185) and the Notion Perde hub were read; repository SPEC and the supplied F2-2 task govern current implementation. Linear field notes still return `Tool get_agent_context not found`.

## Lead decision and current follow-up

Talha + Claude approved a product trust boundary on 2026-10-31: operators manage a live site allowlist; listed sites are trusted. This mitigates which pages may run, without claiming that the native WebSocket bypass has been fixed. No MCEF/native/global flag or other-mod changes, and no local WebSocket canary repeat.

Core implementation now passes 197 tests and build. Only HTTP(S) main-frame pages in the server list are eligible. Subframes/resources retain the literal-host/scheme filters. Public saved URLs remain stored when removed, but cannot create/render a browser until allowed again. Before the first server list, the client permits only internal about:blank.

Config is atomically replaced and commands save before publishing/broadcasting a new snapshot. The full list is bounded at 4096 normalized domains for safe payload sizing. Missing config creates the eight requested defaults; invalid entries warn and are ignored; malformed existing files fail closed. Runtime console/client checks are still pending at this intermediate commit. A SPEC change proposal and final residual-risk report will follow after acceptance.

Talha authorized/resumed game testing. All requested in-game checks (a)-(e) were executed and their listed behavior passed after a navigation-recovery fix. An additional passive local canary nevertheless received a WebSocket handshake from the public managed page, without a JCEF request callback. The broader local-network goal therefore fails: do not mark ready or merge. This is no longer waiting for permission to start Minecraft. F2-3 must wait for the native request boundary to be resolved. [Canary evidence](f2-2-websocket-canary.txt).

## Resumed verification and fixes

- Final `gradlew.bat build --rerun-tasks --console=plain --warning-mode all`: **BUILD SUCCESSFUL in 6s, 9 tasks executed, 149 tests, zero failures/errors/skips**. [Build](f2-2-build.txt), [suite counts](f2-2-tests.csv). Original implementation-stage results below are historical.
- Final MCEF-free server: 44 mods, loopback-only binding, Done (0.316s), console stop/all dimensions saved, **BUILD SUCCESSFUL in 21s**. Explicit --nogui, CREATE_NO_WINDOW, repository-owned PID 42788 verified. [Final log](f2-2-server-final.txt), [binding/PID](f2-2-server-final-binding.txt). No test client/server remained afterward.
- Chromium replaced a file navigation with `about:blank#blocked` without delivering onBeforeBrowse. The initial test failed because the public page disappeared. Added owned main-frame address recovery and denied-redirect recovery to reload the last public URL; invalid-URL paints are withheld so the blocked page is not shown on the quad. This reload may reset DOM/media state. Regression tests cover recovery and browser identity.
- Chromium's [sanitized clipboard write context](https://github.com/chromium/chromium/blob/146.0.7680.179/components/permissions/contexts/clipboard_sanitized_write_permission_context.cc) grants sanitized write independently of the ordinary clipboard preference. The permission query reports granted. Actual write was denied with NotAllowedError / Document is not focused, using an unresolved data promise so no clipboard content was read or written. Added native focus-request cancellation and unexpected-focus revocation for owned display-only browsers. This boundary must be revisited before Phase 5 allows input/focus. Regression tests cover focus isolation.
- The opt-in fixture records actual native Runtime.evaluate results and can send raw permission-checked SetUrl packets while the real panel context is open. No product validation was removed. Browser/Target-level DevTools context-inspection commands timed out; no production policy depends on them and no default/global permission override was attempted.
- [Runtime excerpts](f2-2-runtime-evidence.txt), 12 unchanged vanilla F2-handler captures with [SHA-256 provenance](f2-2-provenance.csv). Representative playback, panel/action-bar, recovery, rendering and spike images were visually inspected. Full raw runs remain under ignored run/f2-2-verification. Final client shutdown saved every dimension and finished BUILD SUCCESSFUL in 5m 38s. No Computer Use/window activation or unrelated process control was used.

### Native request blocker

The passive owned loopback canary received an HTTP Upgrade request from the managed public page. Pure UrlPolicy correctly rejects this host, but the verified JCEF request/resource callbacks do not see the native WebSocket handshake. A browser error after the canary closes its accepted socket does not mean Perde blocked the connection. [Complete canary output](f2-2-websocket-canary.txt).

The probe accidentally had a trailing call producing a TypeError after WebSocket construction; the received network request still proves access. A repeat was rejected by automatic tool review with `blocked by policy`; no repeat is claimed and the rejection was not bypassed. The listener exited normally.

Resolving this requires an upstream-supported native WebSocket filter or an explicitly reviewed broader network boundary. No global flags, other-mod behavior, native binaries or JS security shims were changed to disguise the failure. Keep F2-2 partial and PR draft until the native bypass and affected checks are resolved.

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

There is no per-browser API to remove this global command-line switch. `CefBrowserSettings` in this JCEF exposes only the windowless frame-rate setting. Perde does **not** mutate global command-line state, replace CefApp, patch MCEF, or claim that CORS/same-origin isolation was restored. YouTube playback passed. **Players should not sign in to accounts inside Perde.** SPEC.md is unchanged; this report proposes adding that limitation to its security section during a separate design review.

Perde now constructs the same upstream `MCEFBrowserImpl` renderer with a **dedicated CefClient and isolated CefRequestContext per managed browser**. It uses the verified public implementation constructor, then resize, setCloseAllowed and createImmediately. This intentionally couples the backend to the pinned MCEF implementation; recheck the constructor/lifecycle on an upgrade. It neither modifies nor replaces handlers on the shared MCEF client. Spike and other mods remain on their own clients. Owned callbacks additionally require exact CefBrowser object identity. Native callbacks are attached before createImmediately, so the initial public request cannot precede handler installation.

The browser initially opens only `about:blank`. In the CEF UI-thread `onAfterCreated` callback it installs and reads back the required native permission defaults, then loads the desired public URL. Missing, immutable, rejected or ineffective settings fail closed: no public page is loaded, the native browser closes, and the manager reports browser failure. A per-creation session object prevents delayed callbacks from an older browser using a replacement browser's state. The manager still owns GPU cleanup on the Minecraft client thread. JCEF's `CefBrowser_N.onBeforeClose:132-134` disposes its request context; Perde does not dispose it a second time. The dedicated client is disposed on normal manager closure.

| Handler/API | Behavior |
| --- | --- |
| CefRequestHandler.onBeforeBrowse | Validate main and subframe navigations, including redirects delivered to this callback. Cancel invalid navigation before commit. |
| onOpenURLFromTab / CefLifeSpanHandler.onBeforePopup | Cancel all new-tab/popup requests. |
| getResourceRequestHandler / onBeforeResourceLoad | Apply navigation or resource policy; cancel downloads and invalid requests. A private context handler also covers browserless requests delivered by CEF, such as worker requests. |
| onResourceRedirect | Validate the target and replace a denied target with a non-network denied sentinel (`perde-blocked:redirect`). It cannot return a cancellation boolean. Navigation additionally stops and restores the last public page; actual public file/data redirects passed. |
| onProtocolExecution | Always deny OS protocol execution. |
| CefDownloadHandler | Claim onBeforeDownload without ever calling Continue; cancel every update callback. This avoids both default download handling and the file picker. |
| CefDialogHandler | Cancel the file callback and return handled. |
| CefJSDialogHandler | Suppress alert/confirm/prompt; accept leaving without showing beforeunload UI. |
| CefContextMenuHandler | Clear the native context menu, including clipboard/print/DevTools entries. |
| Request-context preferences | Set microphone, camera, geolocation, notifications, clipboard, automatic downloads, file-system read/write guards to CONTENT_SETTING_BLOCK (2), and verify readback before navigation. |

Permission preference names and BLOCK support were verified from Chromium **146.0.7680.179** [content settings registry](https://github.com/chromium/chromium/blob/146.0.7680.179/components/content_settings/core/browser/content_settings_registry.cc) and [preference-name conversion](https://github.com/chromium/chromium/blob/146.0.7680.179/components/content_settings/core/browser/website_settings_info.cc). The latter prefixes `profile.default_content_setting_values.` and converts hyphens to underscores.

### Explicit API limits and remaining risks

- This JCEF sources jar contains **no CefPermissionHandler or permission callbacks** for media, geolocation, notifications or clipboard. Native defaults are the available mitigation, with the sanitized-write focus limitation described above. Per-attempt permission logging cannot be implemented through a nonexistent handler. `permission-default/<setting>` INFO entries record configured policy once per page, **not an observed permission request or proof of a native denial**. Runtime denial was tested. The strict per-attempt permission audit requirement remains an API limitation for review. Chromium also rejects some popups before a Java callback; the fixture records dispatch and native null result without fabricating an audit callback.
- `BlockedRequestLog` logs each callback category once per accepted main-frame navigation, bounds URLs to 200 characters and sanitizes control/line-separator characters. Blocked navigation does not reset the log allowance. UI and IO threads share a synchronized audit. Native excerpts are retained in the resumed evidence file. Existing OSHI/AWT messages and CEF blocked-iframe timeout/parentless-dialog cancellation messages were retained; rendering and normal shutdown continued.
- Unit tests prove policy decisions and callback behavior without Chromium natives. They do not establish complete native interception of WebSockets, renderer-local schemes, WebRTC or every worker request. These callbacks are not a general network firewall. CEF itself tracks [worker requests bypassing request callbacks](https://github.com/chromiumembedded/cef/issues/3593); the private context fallback is implemented, but complete coverage in this pinned runtime remains unproven. Do not describe this implementation as guaranteeing isolation from all local-network traffic.
- Browser creation now uses a private client/context and does not reuse MCEF's shared cookie context. Cookie/cache persistence and native teardown must be observed in the resumed client checks. No filesystem cache deletion or global MCEF cache mutation is performed.

## Initial implementation-stage verification (historical, 2026-10-31)

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

## In-game acceptance results (2026-10-31, Europe/Istanbul)

| Check | Result and evidence |
| --- | --- |
| (a) Public playback | **PASS.** Real panel entry loads example.com and YouTube. Final video currentTime advanced 31.156445 to 47.639364, paused=false, readyState=4. [Example F2](screenshots/f2-2-a-example.png), [final YouTube F2](screenshots/f2-2-a-final-youtube.png). |
| (b) Server validation | **PASS.** Panel validation rejects all three addresses. Since it correctly prevents the packet, opt-in raw-url additionally sends each SetUrl in that real opened-panel context, without bypassing server authorization. Every request returns the translated action bar and unchanged state. [Panel](screenshots/f2-2-b-client-private.png), [private action bar](screenshots/f2-2-b-server-private.png), [localhost](screenshots/f2-2-b-server-localhost.png), [loopback](screenshots/f2-2-b-server-loopback.png). |
| (c) Listed page attacks | **PASS for the listed probes.** File navigation recovers; popup returns null; loopback fetch returns TypeError with request audit; private iframe is blocked/audited; alert/confirm/prompt return without UI (false/null); media returns NotAllowedError; blob download is denied/audited. Rendering continues, no new repository download file. Extra file picker returns zero files, beforeunload is suppressed, geolocation returns denial code 1, notifications return denied, clipboard read/write calls reject. [Final rendering](screenshots/f2-2-c-final-rendering.png). This does not erase the separate WebSocket failure. |
| (d) Popup/redirects | **PASS.** Actual public httpbingo HTTP 302 responses targeting file: and data: are denied and the managed browser returns to Example Domain at example.com. Response status/Location were independently checked without following them. [Redirect F2](screenshots/f2-2-d-file-redirect.png). Recovery reload may reset DOM/media state. |
| (e) Spike | **PASS.** Outside managed range at x=200, /perde-spike place renders YouTube using the unchanged shared MCEF client. Remove returns native registry=0. [F2](screenshots/f2-2-e-spike.png). An ordinary fill command created a repository-only test platform to prevent falling. |
| Additional local WebSocket canary | **FAIL, task blocker.** The owned local listener received an HTTP Upgrade request despite the literal policy; native request callbacks were bypassed. [Evidence](f2-2-websocket-canary.txt). |

See [opt-in fixture instructions](fixture/README.md). The fixture is absent from the default jar and targets only the managed browser by reflection. All requested (a)-(e) checks are executed, but the native WebSocket request boundary still fails the broader security goal. Resolve it and rerun the canary/affected checks before marking ready. No F2-3 work or merge in this task.
