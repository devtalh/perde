package io.github.devtalh.perde.client.browser;

import com.mojang.blaze3d.textures.GpuTextureView;
import net.dimaskama.mcef.api.MCEFApi;
import net.dimaskama.mcef.api.MCEFBrowser;
import net.dimaskama.mcef.impl.MCEFBrowserImpl;
import io.github.devtalh.perde.Perde;
import io.github.devtalh.perde.screen.ScreenState;
import io.github.devtalh.perde.screen.UrlPolicy;
import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.browser.CefRequestContext;
import org.cef.handler.CefRequestContextHandlerAdapter;
import org.cef.handler.CefResourceRequestHandler;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.network.CefRequest;
import org.cef.misc.BoolRef;

/** Loaded only when Fabric reports MCEF present. Never waits on Chromium initialization. */
final class McefBrowserBackend implements BrowserBackend {
    private MCEFBrowser browser;
    private CefClient client;
    private Session session;
    private static final class Session {
        volatile String desiredUrl;
        volatile boolean configured, closed;
        volatile RuntimeException failure;
        Session(String url) { desiredUrl = ScreenState.validateUrl(url); }
    }

    @Override public boolean ready() { return MCEFApi.getInstanceFuture().isDone(); }

    @Override public void open(String url) {
        if (browser != null) throw new IllegalStateException("Browser slot is already occupied");
        Session state = new Session(url);
        session = state;
        if (MCEFApi.getInstanceFuture().getNow(null) == null) throw new IllegalStateException("MCEF is not initialized");
        var audit = new BlockedRequestLog((type, target) -> Perde.LOGGER.info("Perde browser blocked [{}]: {}", type, target));
        var policy = new PerdeBrowserPolicy(audit);
        client = CefApp.getInstance().createClient();
        CefRequestContext context = CefRequestContext.createContext(new CefRequestContextHandlerAdapter() {
            @Override public CefResourceRequestHandler getResourceRequestHandler(CefBrowser source, CefFrame frame,
                    CefRequest request, boolean navigation, boolean download, String initiator, BoolRef disable) {
                // Context belongs only to this screen, including requests without a browser (service workers).
                return policy.resources(navigation, download);
            }
        });
        if (context == null) { client.dispose(); client = null; throw new IllegalStateException("Could not create private browser context"); }
        // Same MCEF renderer/construction as df61843, but with our client/context. No shared handlers to replace or chain.
        browser = new MCEFBrowserImpl(client, "about:blank", false, context, null) {
            @Override public void onPaint(CefBrowser source, boolean popup, java.awt.Rectangle[] dirty,
                    java.nio.ByteBuffer buffer, int width, int height) {
                // Preserve the last safe texture while Chromium recovers a renderer-blocked navigation.
                if (UrlPolicy.allowsNavigation(source.getURL())) super.onPaint(source, popup, dirty, buffer, width, height);
            }
        };
        policy.own(browser.getCefBrowser());
        MCEFBrowser created = browser;
        policy.install(client, () -> {
            try {
                if (state.closed) { created.getCefBrowser().close(true); return; }
                BrowserPrivacy.configure(context);
                state.configured = true;
                if (!state.closed) created.getCefBrowser().loadURL(state.desiredUrl);
            } catch (RuntimeException exception) {
                state.failure = exception;
                Perde.LOGGER.error("Perde browser permission setup failed; no public page will be loaded.", exception);
                created.getCefBrowser().close(true);
            }
        }, () -> { }); // CefBrowser_N.onBeforeClose disposes its request context exactly once.
        browser.resize(1280, 720);
        browser.getCefBrowser().setCloseAllowed();
        browser.getCefBrowser().createImmediately();
        browser.getCefBrowser().setWindowlessFrameRate(30);
        browser.setFocus(false);
    }

    @Override public void checkHealth() { if (session != null && session.failure != null) throw session.failure; }
    @Override public void loadUrl(String url) {
        checkHealth();
        session.desiredUrl = ScreenState.validateUrl(url);
        if (session.configured && !session.closed) browser.getCefBrowser().loadURL(session.desiredUrl);
    }
    @Override public GpuTextureView textureView() { return browser == null ? null : browser.getTextureView(); }

    @Override public void close() {
        if (browser != null) {
            session.closed = true;
            MCEFBrowser closing = browser;
            browser = null;
            closing.close();
        }
        if (client != null) { client.dispose(); client = null; }
    }
}
