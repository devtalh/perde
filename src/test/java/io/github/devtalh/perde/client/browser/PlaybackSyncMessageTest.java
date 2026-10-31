package io.github.devtalh.perde.client.browser;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class PlaybackSyncMessageTest {
    private static final String TOKEN = "b".repeat(64);
    private static final String VALID = "p1|" + TOKEN + "|12|1|921600|1234.5|60000|0|4|0|0|end";

    @Test void acceptsOnlyObservationsWithExpectedTokenAndPoll() {
        var parsed = PlaybackSyncMessage.parse(VALID, TOKEN, 12);
        assertNotNull(parsed);
        assertEquals(1234.5, parsed.media().positionMs());
        assertNull(PlaybackSyncMessage.parse(VALID, "a".repeat(64), 12));
        assertNull(PlaybackSyncMessage.parse(VALID, TOKEN, 13));
        assertNull(PlaybackSyncMessage.parse(null, TOKEN, 12));
        assertNull(PlaybackSyncMessage.parse(VALID + "|command", TOKEN, 12));
        assertNull(PlaybackSyncMessage.parse("a".repeat(513), TOKEN, 12));
    }

    @ParameterizedTest
    @ValueSource(strings = {"p2", "file:/etc/passwd", "{}", "play", ""})
    void unrelatedMessagesAreIgnored(String value) {
        assertNull(PlaybackSyncMessage.parse(value, TOKEN, 12));
    }

    @ParameterizedTest
    @ValueSource(strings = {"-1", "NaN", "Infinity", "1e99", "nope", ""})
    void invalidNumbersFailClosed(String value) {
        assertNull(PlaybackSyncMessage.parse(VALID.replace("1234.5", value), TOKEN, 12));
        assertNull(PlaybackSyncMessage.parse(VALID.replace("60000", value), TOKEN, 12));
        assertNull(PlaybackSyncMessage.parse(VALID.replace("921600", value), TOKEN, 12));
    }

    @Test void boundsAndBooleans() {
        for (String suffix : new String[]{"1|0|0|60000|0|4|0|0|end", "-1|100|0|60000|0|4|0|0|end",
                "0|100|0|60000|0|4|0|0|end", "1|100|0|60000|true|4|0|0|end", "1|100|0|60000|0|5|0|0|end",
                "1|100|0|60000|0|4|2|0|end", "1|100|0|60000|0|4|0|true|end", "1|100|0|60000|0|4|0|0|bad"})
            assertNull(PlaybackSyncMessage.parse("p1|" + TOKEN + "|12|" + suffix, TOKEN, 12), suffix);
    }

    @Test void acceptsNoVideoAndLiveSentinel() {
        assertNotNull(PlaybackSyncMessage.parse("p1|" + TOKEN + "|12|0|0|0|live|1|0|0|0|end", TOKEN, 12));
        var live = PlaybackSyncMessage.parse(VALID.replace("60000", "live"), TOKEN, 12);
        assertNotNull(live);
        assertEquals(Double.POSITIVE_INFINITY, live.media().durationMs());
    }
}
