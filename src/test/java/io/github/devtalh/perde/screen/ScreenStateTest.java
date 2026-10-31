package io.github.devtalh.perde.screen;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ScreenStateTest {
    @ParameterizedTest @ValueSource(strings = {"https://example.com", "http://localhost:25570/a?q=x#b", "HTTPS://example.com"})
    void acceptsHttpUrls(String url) { assertEquals(url, ScreenState.validateUrl("  " + url + "  ")); }

    @ParameterizedTest @ValueSource(strings = {"javascript:alert(1)", "file:///C:/", "data:text/html,test", "ftp://example.com", "https:///path", "https://", "//example.com", "https://exa mple.com"})
    void rejectsUnsafeUrls(String url) { assertThrows(IllegalArgumentException.class, () -> ScreenState.validateUrl(url)); }

    @Test void emptyAndLengthLimit() {
        assertEquals("", ScreenState.validateUrl("  "));
        String maximum = "https://example.com/" + "a".repeat(2028);
        assertEquals(2048, maximum.length());
        assertEquals(maximum, ScreenState.validateUrl(maximum));
        assertThrows(IllegalArgumentException.class, () -> ScreenState.validateUrl(maximum + "a"));
    }

    @Test void clampsAndRoundsGeometry() {
        ScreenState state = ScreenState.defaults().withGeometry(99, -2, -99, 99);
        assertEquals(16, state.width()); assertEquals(0.5, state.height());
        assertEquals(-16, state.offsetX()); assertEquals(16, state.offsetY());
        state = state.withGeometry(4.04, 2.27, -0.09, 0.04);
        assertEquals(4.0625, state.width()); assertEquals(2.25, state.height());
        assertEquals(-0.0625, state.offsetX()); assertEquals(0.0625, state.offsetY());
    }

    @ParameterizedTest @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void rejectsNonFiniteGeometry(double value) {
        assertThrows(IllegalArgumentException.class, () -> ScreenState.defaults().withGeometry(value, 2, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> ScreenState.clamp(value, 0, 1));
    }

    @Test void ratioHelperAndDefaults() {
        assertEquals(2.25, ScreenState.height16By9(4));
        assertEquals(4.5, ScreenState.height16By9(8));
        assertEquals(9, ScreenState.height16By9(100));
        assertEquals(0.5625, ScreenState.height16By9(1));
        assertEquals(AccessMode.OWNER_ONLY, ScreenState.defaults().accessMode());
        assertEquals(1, ScreenState.defaults().volume());
        assertEquals(0, ScreenState.clamp(-1, 0, 1));
        assertEquals(1, ScreenState.clamp(2, 0, 1));
    }

    @Test void allowedPlayersBoundedAndImmutable() {
        ScreenState state = ScreenState.defaults();
        for (int i = 0; i < 32; i++) state = state.withAllowedPlayer(new UUID(1, i), "Player" + i);
        ScreenState full = state;
        assertThrows(IllegalArgumentException.class, () -> full.withAllowedPlayer(new UUID(2, 0), "Extra"));
        assertEquals(32, full.withAllowedPlayer(new UUID(1, 0), "Renamed").allowedPlayers().size());
        assertEquals(31, full.withoutAllowedPlayer(new UUID(1, 0)).allowedPlayers().size());
        assertThrows(UnsupportedOperationException.class, () -> full.allowedPlayers().clear());
        Map<UUID, String> mutable = new LinkedHashMap<>();
        mutable.put(new UUID(3, 0), "Test");
        ScreenState copied = new ScreenState("", false, 5, 10, 1, 4, 2.25, 0, 0,
                ScreenState.UNOWNED, "", AccessMode.OWNER_ONLY, mutable);
        mutable.clear();
        assertEquals(1, copied.allowedPlayers().size());
        assertEquals(5, copied.withUrl("https://example.com").positionMs());
        assertEquals(10, copied.withGeometry(8, 4.5, 0, 0).serverTimestamp());
    }
}
