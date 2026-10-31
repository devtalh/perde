package io.github.devtalh.perde.screen;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class UrlPolicyTest {
    @ParameterizedTest @ValueSource(strings = {
            "localhost", "LOCALHOST.", "a.localhost", "printer.local", "router.lan.", "nas.home", "x.internal",
            "127.0.0.1", "127.255.255.255", "127.1", "127.0.1", "0x7f000001", "2130706433", "0177.1",
            "0X7F.0.0.1", "0x7f.1", "0", "0.0.0.0", "0.255.255.255", "10.0.0.0", "10.255.255.255",
            "172.16.0.0", "172.31.255.255", "192.168.0.0", "192.168.255.255", "169.254.1.1",
            "100.64.0.0", "100.127.255.255", "[::1]", "[::]", "[fc00::1]", "[fdff::1]", "[fe80::1]", "[febf::1]",
            "[::ffff:127.0.0.1]", "[::ffff:192.168.1.1]", "[::ffff:a00:1]", "[::ffff:ac10:1]",
            "[::ffff:a9fe:1]", "[::ffff:6440:1]", "[::ffff:0:1]", "127.1.", "0x7f000001.",
            "localhost\u3002", "a\uff0elocalhost", "192\u3002168\u30021\u30021"})
    void blocksPrivateHostsEverywhere(String host) {
        for (String scheme : new String[]{"http", "https", "ws", "wss"}) {
            String url = scheme + "://user:pass@" + host + ":8080/path";
            assertFalse(UrlPolicy.allowsNavigation(url), url);
            assertFalse(UrlPolicy.allowsResource(url), url);
        }
        assertFalse(UrlPolicy.allowsResource("blob:https://" + host + "/id"));
        assertThrows(IllegalArgumentException.class, () -> ScreenState.validateUrl("http://" + host));
        assertEquals("", ScreenState.savedUrl("http://" + host));
    }

    @ParameterizedTest @ValueSource(strings = {"https://example.com", "HTTP://EXAMPLE.COM./a?q=x#b", "https://user:pass@example.com:443/",
            "https://localhost@example.com", "http://8.8.8.8", "http://134744072", "http://0x08080808", "http://010.010.010.010",
            "http://8.8", "http://8.8.8", "http://172.15.255.255", "http://172.32.0.0", "http://100.63.255.255",
            "http://100.128.0.0", "https://[2606:4700:4700::1111]", "https://[::ffff:8.8.8.8]", "https://localhost.example.com"})
    void allowsPublicPages(String url) {
        assertTrue(UrlPolicy.allowsPage(url), url);
        assertTrue(UrlPolicy.allowsNavigation(url), url);
        assertTrue(UrlPolicy.allowsResource(url), url);
        assertEquals(url, ScreenState.validateUrl(url));
        assertEquals(url, ScreenState.savedUrl(url));
    }

    @ParameterizedTest @ValueSource(strings = {"file:///C:/", "javascript:alert(1)", "chrome://settings", "chrome-extension://id/",
            "devtools://devtools", "view-source:https://example.com", "ftp://example.com", "unknown://example.com",
            "about:config", "about:blank?x", "https:///path", "https://", "//example.com", "https://exa mple.com",
            "http://%31%32%37.0.0.1", "http://[fe80::1%25eth0]", "http://example.com:65536", "http://example.com:abc",
            "http://127.0.0.1\\@example.com", "http://4294967296", "http://1.2.3.256", "http://08", "http://example.com.."})
    void rejectsUnknownOrAmbiguousRequests(String url) {
        assertFalse(UrlPolicy.allowsNavigation(url), url);
        assertFalse(UrlPolicy.allowsResource(url), url);
    }

    @ParameterizedTest @ValueSource(strings = {"data:text/plain,hello", "blob:https://example.com/id", "blob:null/id", "wss://example.com/socket", "WS://example.com"})
    void videoSubresourcesAreNotNavigations(String url) {
        assertTrue(UrlPolicy.allowsResource(url), url);
        assertFalse(UrlPolicy.allowsNavigation(url), url);
    }

    @Test void blankIsOnlyAnInternalNavigationException() {
        assertTrue(UrlPolicy.allowsNavigation("about:blank"));
        assertTrue(UrlPolicy.allowsNavigation("ABOUT:blank"));
        assertFalse(UrlPolicy.allowsPage("about:blank"));
        assertFalse(UrlPolicy.allowsResource("about:blank"));
        assertFalse(UrlPolicy.allowsPage(null));
        assertFalse(UrlPolicy.allowsResource(null));
        assertFalse(UrlPolicy.allowsNavigation(null));
    }
}
