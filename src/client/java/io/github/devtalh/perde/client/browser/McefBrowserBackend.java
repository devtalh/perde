package io.github.devtalh.perde.client.browser;

import com.mojang.blaze3d.textures.GpuTextureView;
import net.dimaskama.mcef.api.MCEFApi;
import net.dimaskama.mcef.api.MCEFBrowser;

/** Loaded only when Fabric reports MCEF present. Never waits on Chromium initialization. */
final class McefBrowserBackend implements BrowserBackend {
    private MCEFBrowser browser;

    @Override public boolean ready() { return MCEFApi.getInstanceFuture().isDone(); }

    @Override public void open(String url) {
        if (browser != null) throw new IllegalStateException("Browser slot is already occupied");
        browser = MCEFApi.getInstanceFuture().getNow(null).createBrowser(url, false);
        browser.resize(1280, 720);
        browser.getCefBrowser().setWindowlessFrameRate(30);
        browser.setFocus(false);
    }

    @Override public void loadUrl(String url) { browser.getCefBrowser().loadURL(url); }
    @Override public GpuTextureView textureView() { return browser == null ? null : browser.getTextureView(); }

    @Override public void close() {
        if (browser != null) {
            MCEFBrowser closing = browser;
            browser = null;
            closing.close();
        }
    }
}
