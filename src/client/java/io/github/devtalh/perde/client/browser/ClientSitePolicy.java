package io.github.devtalh.perde.client.browser;

import io.github.devtalh.perde.screen.SiteAllowlist;
import java.util.List;

/** A single atomic snapshot for CEF callback threads. No defaults are trusted before server sync. */
public final class ClientSitePolicy {
    private static volatile SiteAllowlist current;
    private ClientSitePolicy() { }
    public static void reset() { current = null; }
    public static void update(List<String> domains) { current = new SiteAllowlist(domains); }
    public static boolean received() { return current != null; }
    public static boolean allowsPage(String url) {
        SiteAllowlist snapshot = current;
        return snapshot != null && snapshot.allowsPage(url);
    }
    public static boolean allowsMainNavigation(String url) {
        return "about:blank".equalsIgnoreCase(url) || allowsPage(url);
    }
}
