package io.github.devtalh.perde.client.browser;

/** Optional implementation access is checked before loading our MCEF backend. */
final class McefCompatibility {
    static final String EXPECTED_VERSION = "0.3.3+mc26.2.jcef146.0.10";
    private McefCompatibility() { }

    static BrowserBackend createBackend() throws ReflectiveOperationException {
        verify(McefCompatibility.class.getClassLoader());
        return new McefBrowserBackend();
    }

    static void verify(ClassLoader loader) throws ReflectiveOperationException {
        Class<?> impl = Class.forName("net.dimaskama.mcef.impl.MCEFBrowserImpl", false, loader);
        verifyImplementation(impl, loader);
    }

    static void verifyImplementation(Class<?> impl, ClassLoader loader) throws ReflectiveOperationException {
        Class<?> client = Class.forName("org.cef.CefClient", false, loader);
        Class<?> browser = Class.forName("org.cef.browser.CefBrowser", false, loader);
        Class<?> context = Class.forName("org.cef.browser.CefRequestContext", false, loader);
        Class<?> settings = Class.forName("org.cef.CefBrowserSettings", false, loader);
        impl.getConstructor(client, String.class, boolean.class, context, settings);
        impl.getMethod("resize", int.class, int.class);
        impl.getMethod("setFocus", boolean.class);
        impl.getMethod("close");
        impl.getMethod("getTextureView");
        if (impl.getMethod("getCefBrowser").getReturnType() != browser) throw new NoSuchMethodException("getCefBrowser return type");
        impl.getMethod("onPaint", browser, boolean.class, java.awt.Rectangle[].class, java.nio.ByteBuffer.class, int.class, int.class);
        browser.getMethod("setCloseAllowed");
        browser.getMethod("createImmediately");
        browser.getMethod("setWindowlessFrameRate", int.class);
        browser.getMethod("loadURL", String.class);
        browser.getMethod("getURL");
        browser.getMethod("getFrameIdentifiers");
        browser.getMethod("getFrameByIdentifier", String.class);
        Class<?> frame = Class.forName("org.cef.browser.CefFrame", false, loader);
        frame.getMethod("executeJavaScript", String.class, String.class, int.class);
        frame.getMethod("getIdentifier");
        frame.getMethod("isValid");
        frame.getMethod("dispose");
        Class<?> router = Class.forName("org.cef.browser.CefMessageRouter", false, loader);
        Class<?> handler = Class.forName("org.cef.handler.CefMessageRouterHandler", false, loader);
        router.getMethod("create", handler);
        router.getMethod("dispose");
        client.getMethod("addMessageRouter", router);
        client.getMethod("removeMessageRouter", router);
    }

    static RuntimeException incompatible(Throwable cause) {
        return new Incompatible(cause);
    }

    static final class Incompatible extends RuntimeException {
        Incompatible(Throwable cause) { super("MCEF version incompatible; expected " + EXPECTED_VERSION, cause); }
    }
}
