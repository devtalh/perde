# F2-2: browser navigation and privacy policy

Status: **done under the lead-approved trusted-site boundary** (2026-10-31, Europe/Istanbul). Branch `feat/f2-2-browser-policy`, [PR #7](https://github.com/devtalh/perde/pull/7), based on merged F2-1/main `6b75980`. Implementation checkpoints: `bdc33b7`, `33755ba`, `98b7396`. PR is eligible for ready after latest-head CI; do not merge. Next separate task: F2-3.

[Linear LEA-185](https://linear.app/leak-rake/issue/LEA-185) and the Notion Perde hub were read. Linear field notes still return `Tool get_agent_context not found`. Repository SPEC governs design except the explicit Talha + Claude decision supplied on 2026-10-31 below. SPEC.md remains unchanged.

## Accepted decision and residual risk

Talha + Claude explicitly chose an operator-managed SITE ALLOWLIST as the product trust boundary, rather than a network proxy. Only trusted sites may be opened. **The native WebSocket bypass is not fixed.** An earlier passive canary demonstrated that JCEF request/resource callbacks did not see a WebSocket handshake and an owned loopback listener received it. [Original evidence](f2-2-websocket-canary.txt). This failure is retained, not reclassified as a successful native denial. The follow-up does not repeat the canary, as instructed.

A malicious or compromised allowed page, or code it loads, may still reach the local network through native paths outside these callbacks. Listed sites and their loaded code are trusted. DNS rebinding and public names resolving to private addresses remain outside literal URL validation. The allowlist reduces which main pages may run; it is not a network firewall. Subframes/resources are not restricted to listed domains, to preserve video/CDN use, and therefore retain this residual trust exposure.

MCEF itself adds `--disable-web-security` process-wide. This pinned API has no per-browser removal; Perde does not patch MCEF, native binaries, global flags or other mods. Same-origin/CORS protection is not restored. Players should not sign in to accounts inside Perde. The old draft/blocker conclusion is superseded only by the explicit lead acceptance of this product boundary, not by a claim that native isolation now works.

## Final implementation

- Pure Java `screen.SiteAllowlist` applies HTTP(S) plus exact-domain/subdomain matching to server SetUrl and client main-frame navigation, including redirects. `youtube.com.evil.example` and `evilyoutube.com` do not match `youtube.com`. Exact internal `about:blank` is the only pre-sync main-frame exception.
- Defaults: youtube.com, youtu.be, youtube-nocookie.com, twitch.tv, kick.com, w2g.tv, vimeo.com, dailymotion.com. `config/perde-server.json` stores `allowedDomains`. Missing files get defaults. Invalid entries warn and are ignored; malformed existing files fail closed without overwriting them. Writes use a same-directory temporary file and atomic replacement. Failed saves retain the active policy; there is no non-atomic fallback.
- Bare domains or full HTTP(S) URLs normalize with IDN-to-ASCII, lowercase, removal of one leading www and trailing dot. IP literals, private/local names, single-label names, invalid arguments and domains longer than 253 characters are rejected. The list is capped at 4096 domains to bound the S2C payload.
- `/perde wl add <domain-or-url>`, `remove <domain>` and `list`, alias `/perde whitelist`, require normal permission level >=2. The console works too, with readable English translation fallbacks. Changes save immediately before full-list broadcast; joining players receive the full list. Client initialization/disconnect resets policy to closed until sync.
- An authorized unlisted SetUrl returns the translated requester action bar, retains the previous state and sends every online operator a clickable approval component. Its `/perde wl add <domain>` action passes through normal command permission checks. Notices are limited to one per domain per 60 seconds; unauthorized/invalid requests cannot generate approvals.
- Removing an active site's domain closes its native browser and displays the localized site-not-allowed placeholder immediately. The stored public URL is retained. Adding the domain back restores the screen without another URL edit or restart. Malformed/private legacy saved URLs are still cleared by the existing `ScreenState.savedUrl` helper; public unlisted URLs are retained but gated from activation/rendering.
- Main-frame navigation and recovery use the current list; subframe/resource requests retain `UrlPolicy` literal-host and scheme restrictions. The paint filter withholds disallowed committed main pages. File/about:blank#blocked and denied redirects recover to the last allowed main page, which may reset DOM/media state. If that page is no longer allowed, recovery uses blank and the manager closes the browser.
- All new text is translated in English and Turkish; 57 keys match in both language files. Server control permission, opened-panel context, proximity and request-rate checks remain authoritative.

### Private native context and verified sources

Verified [MCEF Modern df61843](https://github.com/DimasKama/mcef-modern/tree/df61843), the matching [JCEF sources](https://repo.maven.apache.org/maven2/me/friwi/jcef-api/jcef-d3de827+cef-146.0.10+g8219561+chromium-146.0.7680.179/jcef-api-jcef-d3de827+cef-146.0.10+g8219561+chromium-146.0.7680.179-sources.jar) and [native context factory](https://github.com/chromiumembedded/java-cef/blob/d3de827/native/CefRequestContext_N.cpp). Copies are in ignored run/f2-2-sources. The MCEF shared client has no upstream add*Handler registrations in this version. Perde uses its own CefClient and isolated CefRequestContext, retaining the existing upstream renderer constructor/lifecycle; it never replaces shared handlers. Owned callbacks require exact browser identity and are installed before native creation. Spike continues using its original shared client.

The first native page is blank. Before public navigation, CEF's UI-thread callback installs and reads back all required settings. Missing, immutable, rejected or ineffective preferences fail closed and close the browser. A session object isolates late callbacks; Minecraft-thread manager cleanup owns the GPU bridge. JCEF onBeforeClose disposes the request context, so Perde does not dispose it twice.

Chromium **146.0.7680.179** sources verified the WebRTC keys and values: [chrome_pref_names.h](https://github.com/chromium/chromium/blob/146.0.7680.179/chrome/common/pref_names.h), [policy constants](https://github.com/chromium/chromium/blob/146.0.7680.179/third_party/blink/common/peerconnection/webrtc_ip_handling_policy.cc), [policy meaning](https://github.com/chromium/chromium/blob/146.0.7680.179/third_party/blink/public/common/peerconnection/webrtc_ip_handling_policy.h), [renderer preference application](https://github.com/chromium/chromium/blob/146.0.7680.179/chrome/browser/renderer_preferences_util.cc). JCEF's native JNI value conversion supports Java Lists. Every private context requires:

| Preference | Required value |
| --- | --- |
| webrtc.ip_handling_policy | disable_non_proxied_udp |
| webrtc.ip_handling_url | empty list, no URL overrides |
| webrtc.local_ips_allowed_urls | empty list, no local-IP exceptions |

This disables non-proxied UDP/local address exposure according to the pinned Chromium policy; it does not disable all WebRTC or fix WebSocket interception. Actual native readback at 18:51:42 and 18:54:11: `globalContext=false, WebRTC=disable_non_proxied_udp, URL overrides=[], local-IP exceptions=[]`. Failure paths have unit coverage.

Eight native permission defaults block microphone, camera, geolocation, notifications, clipboard, automatic downloads and file-system read/write guards using CONTENT_SETTING_BLOCK (2). Sources: Chromium [registry](https://github.com/chromium/chromium/blob/146.0.7680.179/components/content_settings/core/browser/content_settings_registry.cc), [preference-name conversion](https://github.com/chromium/chromium/blob/146.0.7680.179/components/content_settings/core/browser/website_settings_info.cc). `permission-default/*` logs mean configured policy, not observed permission attempts. Pinned JCEF has no CefPermissionHandler, so strict per-attempt permission audit is unavailable.

### Handler scope and compatibility guard

| Handler/API | Scope |
| --- | --- |
| onBeforeBrowse / main-frame address callbacks | Live allowlist for main pages; existing policy for subframes; recover last allowed URL. |
| getResourceRequestHandler / onBeforeResourceLoad | Existing scheme/literal filters; main-frame allowlist; cancel downloads; private-context browserless fallback. |
| onResourceRedirect | Validate target; denied sentinel plus main-page recovery. |
| onOpenURLFromTab / onBeforePopup / onProtocolExecution | Deny new windows/tabs and OS protocol execution. |
| Download / file dialog / JS dialog / context menu | Refuse Continue, cancel downloads/pickers, suppress dialogs and remove native menu. |
| Native focus callbacks | Reject focus for display-only owned browsers; revoke unexpected focus. |

Chromium's [sanitized clipboard write](https://github.com/chromium/chromium/blob/146.0.7680.179/components/permissions/contexts/clipboard_sanitized_write_permission_context.cc) query can report granted independently of the clipboard preference. Earlier actual read/write tests rejected access; write used an unresolved data promise and did not read/write user clipboard data. The unfocused display-only boundary must be reviewed before Phase 5 input. Some popups are rejected by Chromium before Java callbacks. Worker/native request completeness is unproven, including [CEF issue 3593](https://github.com/chromiumembedded/cef/issues/3593). Audit logs are synchronized, bounded to one category per allowed navigation and sanitized/truncated to 200 characters.

`McefCompatibility` checks the internal MCEFBrowserImpl class, constructor and required method signatures before loading the backend. Reflection and LinkageError failures are contained, logged with expected **0.3.3+mc26.2.jcef146.0.10**, and shown as a translated incompatible-version status instead of crashing. Async creation/paint and manager texture/close paths are guarded too. Unit tests cover missing class/constructor/method and wrapped linkage failures; the real pinned version passed runtime creation. Future incompatible native behavior still requires upgrade testing; this is a guard for internal Java API coupling, not a promise of arbitrary-version compatibility.

## Final verification

All commands used Java/Javac 25.0.4 and repository-local GRADLE_USER_HOME. Test worlds/configs/natives stayed under run/. No Computer Use/window activation, unrelated Minecraft process control or system changes.

```text
.\gradlew.bat build --rerun-tasks --console=plain --warning-mode all
BUILD SUCCESSFUL in 9s
9 actionable tasks: 9 executed
197 tests, 0 failures, 0 errors, 0 skipped
```

[Build](f2-2-allowlist-build.txt), [suite counts](f2-2-allowlist-tests.csv), [isolation/hash checks](f2-2-allowlist-checks.txt). Tests cover domain normalization/bounds/look-alikes, command argument validation, immutable matching, config invalid entries and atomic failure retention, notice limit, payload bounds, client sync/reset, authoritative decisions, main/subframe separation, recovery/focus/identity, privacy preferences and compatibility signatures. No test/assertion was disabled or weakened. The initial compatibility test used a ClassLoader substitution with the wrong binary name; its seam was corrected to check real Class signatures, retaining the expected failure assertion.

The default jar bundles no MCEF/JCEF, nested jars, tests or fixture. Common source has no client/MCEF/JCEF imports. English/Turkish parity: 57 keys. SPEC.md, AGENTS.md and production spike code unchanged; Git diff/whitespace reviewed.

Final MCEF-free runServer: 44 mods, explicit --nogui and Windows CREATE_NO_WINDOW. Verified repository-owned PID **41296**, only **127.0.0.1:25575**, Done (0.440s). The saved 11-domain test config survived restart and `/perde wl list` returned it. Console stop saved all dimensions; **BUILD SUCCESSFUL in 50s**, exit 0. [Final log](f2-2-allowlist-server-final.txt), [listener/PID evidence](f2-2-allowlist-server-binding.txt). [Earlier full acceptance server log](f2-2-allowlist-server.txt) includes default creation and console add/list/remove. No listener remained after shutdown. Test client quit normally, BUILD SUCCESSFUL in 9m 49s, native registry zero before quit. Existing OSHI/performance-counter diagnostics, dev profile-key/Realms warnings and bare AWT shutdown prefixes remain documented; no Perde crash occurred.

### Allowlist runtime acceptance

[Native/client excerpts](f2-2-allowlist-runtime.txt). These are real results, not dispatch acknowledgements. Nine new unmodified vanilla F2 copies plus 12 earlier captures have [SHA-256 provenance](f2-2-provenance.csv), all 21 source/evidence pairs verified. Representative approval, placeholder, playback and spike captures were visually inspected.

| Check | Result |
| --- | --- |
| Console default/add/list/remove | PASS. Missing config created eight defaults; add example.com =9; list=9; whitelist remove =8. Readable console feedback. |
| Join and permission gate | PASS. Client received full list including pre-join example.com. After deop, normal `/perde wl add denied.example` was denied and list remained unchanged. Op restored. Pre-sync/reset fail-closed behavior also has unit coverage. |
| Unlisted panel URL + approval | PASS. example.org rejected; old example.com state/browser retained; requester action bar and op chat shown. Native chat component has `[Allow]`, RunCommand `/perde wl add example.org`. Three requests within 60 seconds produced one notice. [F2](screenshots/f2-2-wl-approval-request.png). |
| Normal approval command | PASS. Ran the component's command as operator; full list updated, same panel URL accepted immediately and browser id1 reloaded to Example Domain. [F2](screenshots/f2-2-wl-approved-live.png). |
| Remove and restore live | PASS. Remove example.org closed id1, native count=0, site-not-allowed placeholder; client and server NBT retained https://example.org. Add back created id2, same saved URL loaded without edit/restart. [Removed](screenshots/f2-2-wl-removed-placeholder.png), [restored](screenshots/f2-2-wl-restored-live.png). |
| Main-frame redirect | PASS. Allowed httpbingo.org returned HTTP302 Location https://example.net (independent no-follow check retained). example.net was unlisted; native navigation denial logged, recovered to https://example.org/ and rendered Example Domain. [F2](screenshots/f2-2-wl-redirect-blocked.png). |
| WebRTC/private context | PASS for actual required native readback. globalContext=false, all three WebRTC preferences match; no global context mutation. |
| YouTube | PASS. Native currentTime 3.668526 to 10.703503, paused=false, readyState=4. Minecraft master=0, browser video muted=true/volume=0. [F2](screenshots/f2-2-wl-youtube-muted.png). |
| Spike regression | PASS. Outside managed range, unchanged `/perde-spike place https://example.com` created shared-client browser id4 and rendered. Remove returned native count=0. Quiet URL honors Talha's zero-audio request. [F2](screenshots/f2-2-wl-spike-stable.png). |

The fixture sets only the repository test client's master volume to zero. Chromium audio is separate: test-only Page.enable + Page.addScriptToEvaluateOnNewDocument forced media mute/volume0 before opening YouTube, with successful native proof on a quiet page first. Page.addScript without Page.enable initially did not run; that was detected before video navigation and corrected. This script is for test audio only, is not product code or a security mitigation, and did not change production permissions. A first spike capture followed falling before its platform loaded; after loading the platform and normal teleport, the stable capture above confirms rendering. Neither orchestration issue was hidden by changing product checks.

### Earlier requested checks (a)-(e), retained evidence

All original checks were executed before the lead follow-up, not left pending. [Earlier exact runtime excerpts](f2-2-runtime-evidence.txt), [149-test build](f2-2-build.txt), [earlier server check](f2-2-server-final.txt).

| Check | Result and evidence |
| --- | --- |
| (a) Public playback | PASS. example.com and YouTube rendered; video time 31.156445 to 47.639364, paused=false, ready=4. [F2](screenshots/f2-2-a-final-youtube.png). Repeated above on final allowlist implementation, muted. |
| (b) Server validation | PASS. Panel and normal raw SetUrl under a real opened-panel permission context rejected private/localhost/loopback addresses and retained state/action bar. [F2](screenshots/f2-2-b-server-private.png). |
| (c) Listed page attacks | PASS for listed probes only: file recovery, popup null, loopback fetch/private iframe rejection+audit, JS dialogs suppressed, getUserMedia NotAllowedError, blob download denied, continued rendering. Extra file picker/beforeunload/geolocation/notifications/clipboard checks also denied. [F2](screenshots/f2-2-c-final-rendering.png). This never proved all native network isolation. |
| (d) Redirects | PASS. Public HTTP302 to file/data rejected and recovered. [F2](screenshots/f2-2-d-file-redirect.png). Final follow-up also verified allowed-to-unlisted HTTP302. |
| (e) Spike | PASS. Earlier unchanged spike rendered YouTube; final follow-up rendered quiet example.com and removed to zero browsers. [Earlier F2](screenshots/f2-2-e-spike.png). |
| Extra WebSocket canary | Historical FAIL. Verified native callback bypass, explicitly accepted as residual risk via the lead decision; not repeated. [Evidence](f2-2-websocket-canary.txt). |

The original canary script had a trailing TypeError after WebSocket construction; the received handshake still proves access. A historical repeat was rejected by automatic tool review with `blocked by policy`; no repeat or workaround is claimed. The listener exited normally. Browser/Target DevTools context-inspection commands timed out in the earlier session; no production behavior depends on them.

## SPEC change proposal (not applied)

The explicit lead decision authorizes this implementation while leaving SPEC unchanged for review. Proposed amendments:

1. Replace unrestricted HTTP(S) page entry with HTTP(S) plus operator-managed trusted-site main-frame allowlist, including redirects and stored-screen activation. Document the eight defaults, IDN normalization, 253-character domain and 4096-domain bounds. Subframes/CDNs retain existing scheme/literal restrictions.
2. Define config/perde-server.json, atomic writes and invalid-entry/fail-closed handling. Add `/perde wl add/remove/list` and alias whitelist, permission >=2, immediate save/full S2C updates on join/change, no pre-sync main navigation except blank. Keep removed public saved URLs and restore after re-adding.
3. Add translated approval requests to all ops, permission-gated clickable command and per-domain 60-second notice limit. Client rejection never substitutes for server authorization.
4. Specify per-private-context WebRTC policy/readback and fail-closed creation; guard the pinned internal MCEF API and localized incompatible status.
5. State the trusted-site residual risk explicitly: native WebSocket/worker paths and DNS rebinding are not comprehensively filtered, allowed-page code is trusted, and --disable-web-security remains upstream. No account sign-in; no claim of network sandboxing or per-attempt permission audit. Revisit clipboard/focus before Phase 5.

No F2-3 implementation or merge is included. Remaining work is human review/final manual acceptance and the separate F2-3 full-pack/culling/spike-removal task.
