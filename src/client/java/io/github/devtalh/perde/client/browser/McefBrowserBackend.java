package io.github.devtalh.perde.client.browser;

import com.mojang.blaze3d.textures.GpuTextureView;
import net.dimaskama.mcef.api.MCEFApi;
import net.dimaskama.mcef.api.MCEFBrowser;
import net.dimaskama.mcef.impl.MCEFBrowserImpl;
import io.github.devtalh.perde.Perde;
import io.github.devtalh.perde.screen.ScreenState;
import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.browser.CefRequestContext;
import org.cef.handler.CefRequestContextHandlerAdapter;
import org.cef.handler.CefResourceRequestHandler;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.network.CefRequest;
import org.cef.misc.BoolRef;
import org.cef.handler.CefLoadHandlerAdapter;

/** Loaded only when Fabric reports MCEF present. Never waits on Chromium initialization. */
final class McefBrowserBackend implements BrowserBackend {
    private MCEFBrowser browser;
    private CefClient client;
    private Session session;
    private BrowserPlaybackSync sync;

    McefBrowserBackend() {
        // Preserve eager asynchronous engine startup after removing the feasibility entrypoint.
        // Lazy engine initialization is a separate Phase 4 decision.
        MCEFApi.getInstanceFuture().whenComplete((api, failure) -> {
            if (failure == null) Perde.LOGGER.info("Perde: MCEF initialization completed.");
            else Perde.LOGGER.error("Perde: MCEF initialization failed. Browser features are unavailable.", failure);
        });
    }

    private static final class Session {
        volatile String desiredUrl;
        volatile boolean configured, closed;
        volatile RuntimeException failure;
        final PlaybackUrlGuard guard = new PlaybackUrlGuard();
        final java.util.concurrent.atomic.AtomicBoolean pageChanged = new java.util.concurrent.atomic.AtomicBoolean();
        Session(String url) { desiredUrl = ScreenState.validateUrl(url); }
    }

    @Override public boolean ready() { return MCEFApi.getInstanceFuture().isDone(); }

    @Override public void open(String url) {
        if (browser != null) throw new IllegalStateException("Browser slot is already occupied");
        Session state = new Session(url);
        session = state;
        if (MCEFApi.getInstanceFuture().getNow(null) == null) throw new IllegalStateException("MCEF is not initialized");
        var audit = new BlockedRequestLog((type, target) -> Perde.LOGGER.info("Perde browser blocked [{}]: {}", type, target));
        var policy = new PerdeBrowserPolicy(audit, ClientSitePolicy::allowsMainNavigation, state.guard::address);
        client = CefApp.getInstance().createClient();
        CefRequestContext context = CefRequestContext.createContext(new CefRequestContextHandlerAdapter() {
            @Override public CefResourceRequestHandler getResourceRequestHandler(CefBrowser source, CefFrame frame,
                    CefRequest request, boolean navigation, boolean download, String initiator, BoolRef disable) {
                // Context belongs only to this screen, including requests without a browser (service workers).
                return policy.resources(navigation, PerdeBrowserPolicy.isMainFrame(frame, request), download);
            }
        });
        if (context == null) { client.dispose(); client = null; throw new IllegalStateException("Could not create private browser context"); }
        // Same MCEF renderer/construction as df61843, but with our client/context. No shared handlers to replace or chain.
        browser = new MCEFBrowserImpl(client, "about:blank", false, context, null) {
            @Override public void onPaint(CefBrowser source, boolean popup, java.awt.Rectangle[] dirty,
                    java.nio.ByteBuffer buffer, int width, int height) {
                // Preserve the last safe texture while Chromium recovers a renderer-blocked navigation.
                try {
                    if (ClientSitePolicy.allowsMainNavigation(source.getURL())) super.onPaint(source, popup, dirty, buffer, width, height);
                } catch (LinkageError exception) { state.failure = McefCompatibility.incompatible(exception); }
            }
        };
        policy.own(browser.getCefBrowser());
        sync = new BrowserPlaybackSync(client, browser.getCefBrowser());
        client.addLoadHandler(new CefLoadHandlerAdapter() {
            @Override public void onLoadEnd(CefBrowser source, CefFrame frame, int status) {
                if (state.closed || source != browser.getCefBrowser() || frame == null || !frame.isMain()) return;
                state.guard.committed(frame.getURL());
                state.pageChanged.set(true);
            }
        });
        MCEFBrowser created = browser;
        policy.install(client, () -> {
            try {
                if (state.closed) { created.getCefBrowser().close(true); return; }
                BrowserPrivacy.configure(context);
                Perde.LOGGER.info("Perde browser privacy verified: globalContext={}, WebRTC={}, URL overrides={}, local-IP exceptions={}.",
                        context.isGlobal(), context.getPreference("webrtc.ip_handling_policy"),
                        context.getPreference("webrtc.ip_handling_url"), context.getPreference("webrtc.local_ips_allowed_urls"));
                state.configured = true;
                if (!state.closed) created.getCefBrowser().loadURL(state.desiredUrl);
            } catch (RuntimeException | LinkageError exception) {
                state.failure = exception instanceof LinkageError ? McefCompatibility.incompatible(exception) : (RuntimeException) exception;
                Perde.LOGGER.error("Perde browser permission setup failed; no public page will be loaded.", exception);
                try { created.getCefBrowser().close(true); }
                catch (LinkageError closingFailure) { state.failure = McefCompatibility.incompatible(closingFailure); }
            }
        }, () -> { }); // CefBrowser_N.onBeforeClose disposes its request context exactly once.
        browser.resize(1280, 720);
        browser.getCefBrowser().setCloseAllowed();
        browser.getCefBrowser().createImmediately();
        browser.getCefBrowser().setWindowlessFrameRate(30);
        browser.setFocus(false);
    }

    @Override public void checkHealth() { if (session != null && session.failure != null) throw session.failure; }
    @Override public boolean currentPageAllowed() { return browser == null || ClientSitePolicy.allowsMainNavigation(browser.getCefBrowser().getURL()); }
    @Override public void loadUrl(String url) {
        checkHealth();
        session.guard.reset();
        sync.newPage();
        session.desiredUrl = ScreenState.validateUrl(url);
        if (session.configured && !session.closed) browser.getCefBrowser().loadURL(session.desiredUrl);
    }
    @Override public GpuTextureView textureView() { return browser == null ? null : browser.getTextureView(); }

    @Override public void sync(io.github.devtalh.perde.screen.PlaybackClock playback, Long serverNowMs) {
        if (!session.configured || session.closed) return;
        if (session.guard.takeRestore()) {
            Perde.LOGGER.info("Perde sync: restoring server URL after page navigation.");
            sync.newPage();
            browser.getCefBrowser().loadURL(session.desiredUrl);
            return;
        }
        if (session.pageChanged.getAndSet(false)) sync.newPage();
        if (serverNowMs != null) sync.tick(playback, serverNowMs);
    }

    @Override public void close() {
        if (browser != null) {
            session.closed = true;
            if (sync != null) { sync.close(); sync = null; }
            MCEFBrowser closing = browser;
            browser = null;
            closing.close();
        }
        if (client != null) { client.dispose(); client = null; }
    }
}
