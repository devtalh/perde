package io.github.devtalh.perde.client.browser;

import org.cef.browser.CefBrowser;
import org.cef.network.TestRequest;
import org.cef.browser.CefFrame;
import org.cef.callback.*;
import org.cef.handler.CefDialogHandler;
import org.cef.handler.CefJSDialogHandler;
import org.cef.misc.BoolRef;
import org.cef.misc.StringRef;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class PerdeBrowserPolicyTest {
    private final List<String> events = new ArrayList<>();
    private final BlockedRequestLog audit = new BlockedRequestLog((type, url) -> events.add(type + ":" + url));
    private final PerdeBrowserPolicy policy = new PerdeBrowserPolicy(audit);
    private final CefBrowser browser = stub(CefBrowser.class, "https://example.com");
    private final CefBrowser other = stub(CefBrowser.class, "https://other.example");
    private final AtomicInteger created = new AtomicInteger(), closed = new AtomicInteger();
    private final PerdeBrowserPolicy.Handlers handlers = policy.handlers(created::incrementAndGet, closed::incrementAndGet);

    PerdeBrowserPolicyTest() { policy.own(browser); }

    @SuppressWarnings("unchecked")
    private static <T> T stub(Class<T> type, String url) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class[]{type}, (object, method, args) -> switch (method.getName()) {
            case "getURL" -> url;
            case "isMain" -> true;
            case "toString" -> type.getSimpleName();
            case "hashCode" -> System.identityHashCode(object);
            case "equals" -> object == args[0];
            default -> method.getReturnType() == boolean.class ? false : method.getReturnType() == int.class ? 0 : null;
        });
    }

    @Test void mainAndSubframeNavigationAndRedirectsAreChecked() {
        assertTrue(handlers.request().onBeforeBrowse(browser, null, new TestRequest("file:///C:/"), false, false));
        assertTrue(handlers.request().onBeforeBrowse(browser, null, new TestRequest("http://192.168.1.1"), false, true));
        assertFalse(handlers.request().onBeforeBrowse(browser, null, new TestRequest("https://example.com"), false, true));
        assertTrue(handlers.request().onOpenURLFromTab(browser, null, "https://example.com", true));
        assertEquals(2, events.size()); // One navigation and one popup category.
    }

    @Test void otherBrowsersAndTheirHandlersAreUnaffected() {
        assertFalse(handlers.request().onBeforeBrowse(other, null, new TestRequest("file:///C:/"), false, false));
        assertFalse(handlers.life().onBeforePopup(other, null, "https://example.com", ""));
        assertNull(handlers.request().getResourceRequestHandler(other, null, new TestRequest("http://localhost"), false, false, "", new BoolRef()));
        assertFalse(handlers.js().onJSDialog(other, "", CefJSDialogHandler.JSDialogType.JSDIALOGTYPE_ALERT, "", "", null, new BoolRef()));
        handlers.life().onAfterCreated(other);
        handlers.life().onBeforeClose(other);
        assertEquals(0, created.get());
        assertEquals(0, closed.get());
        assertTrue(events.isEmpty());
    }

    @Test void nativeLifecycleAndPopups() {
        handlers.life().onAfterCreated(browser);
        assertEquals(1, created.get());
        assertTrue(handlers.life().onBeforePopup(browser, null, "https://example.com", "popup"));
        assertFalse(handlers.life().doClose(browser));
        handlers.life().onBeforeClose(browser);
        assertEquals(1, closed.get());
    }

    @Test void resourcesAndRedirectsIncludingBrowserlessWorkersAreBlocked() {
        var resource = handlers.request().getResourceRequestHandler(browser, null, new TestRequest("https://example.com"), false, false, "", new BoolRef());
        assertTrue(resource.onBeforeResourceLoad(browser, null, new TestRequest("http://127.0.0.1:1/")));
        assertTrue(resource.onBeforeResourceLoad(null, null, new TestRequest("http://192.168.1.1")));
        assertFalse(resource.onBeforeResourceLoad(browser, null, new TestRequest("blob:https://example.com/video")));
        assertFalse(resource.onBeforeResourceLoad(browser, null, new TestRequest("wss://example.com/socket")));
        var target = new StringRef("http://localhost:8080");
        resource.onResourceRedirect(browser, null, new TestRequest("https://example.com"), null, target);
        assertEquals("perde-blocked:redirect", target.get());
        assertTrue(resource.onBeforeResourceLoad(browser, null, new TestRequest(target.get())));
        var allow = new BoolRef(true);
        resource.onProtocolExecution(browser, null, new TestRequest("mailto:test@example.com"), allow);
        assertFalse(allow.get());
        var navigation = policy.resources(true, false);
        assertTrue(navigation.onBeforeResourceLoad(browser, null, new TestRequest("data:text/html,x")));
        assertFalse(navigation.onBeforeResourceLoad(browser, null, new TestRequest("about:blank")));
        assertTrue(policy.resources(false, true).onBeforeResourceLoad(browser, null, new TestRequest("https://example.com/download")));
    }

    @Test void dialogsAndDownloadsNeverContinueOrShowUi() {
        var suppress = new BoolRef();
        assertFalse(handlers.js().onJSDialog(browser, "https://example.com", CefJSDialogHandler.JSDialogType.JSDIALOGTYPE_ALERT,
                "x", "", (success, input) -> fail("No custom dialog callback"), suppress));
        assertTrue(suppress.get());
        assertTrue(handlers.js().onBeforeUnloadDialog(browser, "x", false, (success, input) -> assertTrue(success)));
        AtomicInteger cancel = new AtomicInteger();
        assertTrue(handlers.dialog().onFileDialog(browser, CefDialogHandler.FileDialogMode.FILE_DIALOG_OPEN, "", "", null, null, null,
                new CefFileDialogCallback() {
                    @Override public void Continue(java.util.Vector<String> paths) { fail("File selection must not continue"); }
                    @Override public void Cancel() { cancel.incrementAndGet(); }
                }));
        assertEquals(1, cancel.get());
        CefDownloadItem item = stub(CefDownloadItem.class, "https://example.com/download");
        assertTrue(handlers.download().onBeforeDownload(browser, item, "test", (path, dialog) -> fail("Download must not continue")));
        handlers.download().onDownloadUpdated(browser, item, new CefDownloadItemCallback() {
            @Override public void cancel() { cancel.incrementAndGet(); }
            @Override public void pause() { fail(); }
            @Override public void resume() { fail(); }
        });
        assertEquals(2, cancel.get());
    }

    @Test void auditIsBoundedSanitizedAndResetsOnlyOnAllowedMainNavigation() {
        audit.blocked("request", "a\r\nb" + "x".repeat(300));
        audit.blocked("request", "another");
        assertEquals(1, events.size());
        assertEquals(208, events.getFirst().length()); // Prefix plus at most 200 URL characters.
        assertFalse(events.getFirst().contains("\n"));
        handlers.request().onBeforeBrowse(browser, null, new TestRequest("file:///C:/"), false, false);
        audit.blocked("request", "still same page");
        assertEquals(2, events.size());
        handlers.request().onBeforeBrowse(browser, stub(CefFrame.class, ""), new TestRequest("https://example.com/next"), false, false);
        audit.blocked("request", "new page");
        assertTrue(events.getLast().endsWith("new page"));
    }
}
