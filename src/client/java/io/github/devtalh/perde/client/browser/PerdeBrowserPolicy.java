package io.github.devtalh.perde.client.browser;

import io.github.devtalh.perde.screen.UrlPolicy;
import org.cef.CefClient;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.callback.*;
import org.cef.handler.*;
import org.cef.misc.BoolRef;
import org.cef.misc.StringRef;
import org.cef.network.CefRequest;
import org.cef.network.CefResponse;

import java.util.Vector;

/** Installed on a private CefClient before native creation; never mutates MCEF's shared client. */
final class PerdeBrowserPolicy {
    private final BlockedRequestLog audit;
    private volatile CefBrowser owned;
    private volatile String lastPublicUrl;

    PerdeBrowserPolicy(BlockedRequestLog audit) { this.audit = audit; }
    void own(CefBrowser browser) { owned = browser; }
    private boolean owns(CefBrowser browser) { return browser != null && browser == owned; }

    boolean block(CefBrowser browser, String url, boolean navigation) {
        if (!owns(browser)) return false;
        if (navigation ? UrlPolicy.allowsNavigation(url) : UrlPolicy.allowsResource(url)) return false;
        audit.blocked(navigation ? "navigation" : "request", url);
        return true;
    }

    record Handlers(CefRequestHandler request, CefLifeSpanHandler life, CefDownloadHandler download,
                    CefDialogHandler dialog, CefJSDialogHandler js, CefContextMenuHandler menu,
                    CefDisplayHandler display, CefFocusHandler focus) { }

    void install(CefClient client, Runnable afterCreated, Runnable beforeClose) {
        Handlers handlers = handlers(afterCreated, beforeClose);
        client.addRequestHandler(handlers.request());
        client.addLifeSpanHandler(handlers.life());
        client.addDownloadHandler(handlers.download());
        client.addDialogHandler(handlers.dialog());
        client.addJSDialogHandler(handlers.js());
        client.addContextMenuHandler(handlers.menu());
        client.addDisplayHandler(handlers.display());
        client.addFocusHandler(handlers.focus());
    }

    Handlers handlers(Runnable afterCreated, Runnable beforeClose) {
        var requestHandler = new CefRequestHandlerAdapter() {
            @Override public boolean onBeforeBrowse(CefBrowser browser, CefFrame frame, CefRequest request, boolean gesture, boolean redirect) {
                if (block(browser, request.getURL(), true)) return true;
                if (owns(browser) && frame != null && frame.isMain()) {
                    audit.newPage();
                    auditPermissions(request.getURL());
                }
                return false;
            }
            @Override public boolean onOpenURLFromTab(CefBrowser browser, CefFrame frame, String url, boolean gesture) {
                // This callback requests another tab/browser, including ctrl/middle clicks.
                if (!owns(browser)) return false;
                audit.blocked("popup", url);
                return true;
            }
            @Override public CefResourceRequestHandler getResourceRequestHandler(CefBrowser browser, CefFrame frame,
                    CefRequest request, boolean navigation, boolean download, String initiator, BoolRef disable) {
                if (!owns(browser)) return null;
                if (download) audit.blocked("download", request.getURL());
                return resources(navigation, download);
            }
        };
        var life = new CefLifeSpanHandlerAdapter() {
            @Override public boolean onBeforePopup(CefBrowser browser, CefFrame frame, String url, String name) {
                if (!owns(browser)) return false;
                audit.blocked("popup", url);
                return true;
            }
            @Override public void onAfterCreated(CefBrowser browser) { if (owns(browser)) afterCreated.run(); }
            @Override public void onBeforeClose(CefBrowser browser) { if (owns(browser)) beforeClose.run(); }
        };
        var downloadHandler = new CefDownloadHandlerAdapter() {
            @Override public boolean onBeforeDownload(CefBrowser browser, CefDownloadItem item, String name, CefBeforeDownloadCallback callback) {
                if (!owns(browser)) return false;
                audit.blocked("download", item.getURL());
                // Claim handling without calling Continue: CEF cancels when the callback is released.
                return true;
            }
            @Override public void onDownloadUpdated(CefBrowser browser, CefDownloadItem item, CefDownloadItemCallback callback) {
                if (owns(browser)) { audit.blocked("download", item.getURL()); callback.cancel(); }
            }
        };
        var dialog = new CefDialogHandler() {
            @Override public boolean onFileDialog(CefBrowser browser, FileDialogMode mode, String title, String path,
                    Vector<String> filters, Vector<String> extensions, Vector<String> descriptions, CefFileDialogCallback callback) {
                if (!owns(browser)) return false;
                audit.blocked("file-dialog", browser.getURL());
                callback.Cancel();
                return true;
            }
        };
        var js = new CefJSDialogHandlerAdapter() {
            @Override public boolean onJSDialog(CefBrowser browser, String origin, JSDialogType type, String message,
                    String prompt, CefJSDialogCallback callback, BoolRef suppress) {
                if (!owns(browser)) return false;
                audit.blocked("js-dialog", origin);
                suppress.set(true);
                return false;
            }
            @Override public boolean onBeforeUnloadDialog(CefBrowser browser, String message, boolean reload, CefJSDialogCallback callback) {
                if (!owns(browser)) return false;
                audit.blocked("beforeunload", browser.getURL());
                callback.Continue(true, "");
                return true;
            }
        };
        // Prevent the native context menu (including copy, paste, print and DevTools).
        var menu = new CefContextMenuHandlerAdapter() {
            @Override public void onBeforeContextMenu(CefBrowser browser, CefFrame frame, CefContextMenuParams params, CefMenuModel model) {
                if (owns(browser)) model.clear();
            }
        };
        var display = new CefDisplayHandlerAdapter() {
            @Override public void onAddressChange(CefBrowser browser, CefFrame frame, String url) {
                if (!owns(browser) || frame == null || !frame.isMain()) return;
                if (UrlPolicy.allowsPage(url)) lastPublicUrl = url;
                else if (!UrlPolicy.allowsNavigation(url)) {
                    // Chromium may replace a renderer-blocked file navigation with about:blank#blocked
                    // without delivering onBeforeBrowse. Keep its pixels hidden and restore the public page.
                    audit.blocked("navigation", url);
                    restorePublicPage(browser);
                }
            }
        };
        // These are display-only browsers. Chromium auto-grants sanitized clipboard-write separately
        // from the clipboard content setting, but requires native document focus to use it.
        // Keep page/system focus requests from elevating this browser's existing unfocused state.
        var focus = new CefFocusHandlerAdapter() {
            @Override public boolean onSetFocus(CefBrowser browser, FocusSource source) {
                if (!owns(browser)) return false;
                audit.blocked("focus", browser.getURL());
                return true;
            }
            @Override public void onGotFocus(CefBrowser browser) {
                if (owns(browser)) browser.setFocus(false);
            }
        };
        return new Handlers(requestHandler, life, downloadHandler, dialog, js, menu, display, focus);
    }

    private void restorePublicPage(CefBrowser browser) {
        String restore = lastPublicUrl;
        if (restore != null && owns(browser)) { browser.stopLoad(); browser.loadURL(restore); }
    }

    private void auditPermissions(String url) {
        // This JCEF has no permission callbacks. These entries record configured native denials, not observed attempts.
        for (String setting : BrowserPrivacy.DENIED) audit.blocked("permission-default/" + setting, url);
    }

    CefResourceRequestHandler resources(boolean navigation, boolean download) {
        return new CefResourceRequestHandlerAdapter() {
            @Override public boolean onBeforeResourceLoad(CefBrowser browser, CefFrame frame, CefRequest request) {
                if (download) { audit.blocked("download", request.getURL()); return true; }
                return block(owned, request.getURL(), navigation);
            }
            @Override public void onResourceRedirect(CefBrowser browser, CefFrame frame, CefRequest request, CefResponse response, StringRef target) {
                if (block(owned, target.get(), navigation)) {
                    target.set("perde-blocked:redirect");
                    if (navigation) restorePublicPage(browser);
                }
            }
            @Override public void onProtocolExecution(CefBrowser browser, CefFrame frame, CefRequest request, BoolRef allow) {
                allow.set(false);
                audit.blocked("external-protocol", request.getURL());
            }
        };
    }
}
