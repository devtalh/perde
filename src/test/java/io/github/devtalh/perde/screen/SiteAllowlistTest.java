package io.github.devtalh.perde.screen;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class SiteAllowlistTest {
    @ParameterizedTest @CsvSource({
            "WWW.YOUTUBE.COM.,youtube.com", "https://www.YouTube.com/watch?v=x,youtube.com",
            "https://user:pass@www.Example.com:443/path?q=x#f,example.com",
            "bücher.example,xn--bcher-kva.example", "https://WWW.BÜCHER.example./path,xn--bcher-kva.example",
            "example。com,example.com", "  example.com  ,example.com"})
    void normalizesCommandAndConfigInputs(String input, String expected) {
        assertEquals(expected, SiteAllowlist.normalize(input));
    }

    @ParameterizedTest @ValueSource(strings = {"", "localhost", "www.localhost", "printer.local", "nas.lan", "box.home", "x.internal",
            "example", "https://single", "https://127.1", "8.8.8.8", "134744072", "0x08080808", "010.010.010.010",
            "https://[2606:4700:4700::1111]", "[::1]", "https://[::ffff:8.8.8.8]", "example.com:80", "*.youtube.com",
            "file:///C:/", "ftp://example.com", "example.com/path", "example.com evil.com", "https://%79outube.com", "https://example.com:99999"})
    void rejectsInvalidCommandArgumentsAndUnsafeDomains(String input) {
        assertThrows(IllegalArgumentException.class, () -> SiteAllowlist.normalize(input));
    }

    @Test void enforcesLabelBoundariesAndMainFrameScheme() {
        var policy = new SiteAllowlist(List.of("youtube.com"));
        for (String url : List.of("https://youtube.com", "http://m.youtube.com", "https://www.youtube.com.", "https://a.b.youtube.com")) {
            assertTrue(policy.allowsPage(url), url);
        }
        for (String url : List.of("https://youtube.com.evil.example", "https://evilyoutube.com", "https://youtube.com@evil.example",
                "https://youtubе.com", "data:text/html,x", "blob:https://youtube.com/id", "wss://youtube.com", "http://127.0.0.1")) {
            assertFalse(policy.allowsPage(url), url);
        }
        assertTrue(policy.allowsMainNavigation("about:blank"));
        assertFalse(policy.allowsMainNavigation("about:blank#blocked"));
        assertFalse(policy.allowsPage("about:blank"));
    }

    @Test void defaultsUpdatesAndImmutableSnapshots() {
        assertEquals(8, SiteAllowlist.defaults().domains().size());
        assertFalse(SiteAllowlist.defaults().allowsPage("https://example.com"));
        var source = new java.util.ArrayList<>(List.of("example.com"));
        var policy = new SiteAllowlist(source);
        source.clear();
        assertTrue(policy.allowsPage("https://example.com"));
        assertThrows(UnsupportedOperationException.class, () -> policy.domains().clear());
        assertEquals(policy, policy.adding("https://www.example.com/a"));
        assertFalse(policy.removing("example.com").allowsPage("https://example.com"));
        assertTrue(policy.removing("example.com").adding("example.com").allowsPage("https://sub.example.com"));
        assertThrows(IllegalArgumentException.class, () -> SiteAllowlist.normalize(null));
        String maximum = "a".repeat(63) + "." + "b".repeat(63) + "." + "c".repeat(63) + "." + "d".repeat(61);
        assertEquals(253, SiteAllowlist.normalize(maximum).length());
        assertThrows(IllegalArgumentException.class, () -> SiteAllowlist.normalize(maximum + "x"));
    }
}
