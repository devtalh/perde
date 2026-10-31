package io.github.devtalh.perde.client.browser;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PlaybackUrlGuardTest {
    @Test void permitsInitialRedirectsAndIgnoresInternalBlank() {
        var guard = new PlaybackUrlGuard();
        guard.committed("about:blank");
        guard.address("https://youtu.be/example");
        guard.address("https://www.youtube.com/watch?v=example");
        guard.committed("https://www.youtube.com/watch?v=example");
        guard.address("https://www.youtube.com/watch?v=example");
        assertFalse(guard.takeRestore());
    }
    @Test void restoresHistoryChangesAndSubsequentDocumentCommits() {
        for (boolean history : new boolean[]{true, false}) {
            var guard = new PlaybackUrlGuard();
            guard.committed("https://www.youtube.com/watch?v=one");
            if (history) guard.address("https://www.youtube.com/watch?v=two");
            else guard.committed("https://www.youtube.com/watch?v=two");
            assertTrue(guard.takeRestore());
            assertFalse(guard.takeRestore());
            guard.committed("https://www.youtube.com/watch?v=one");
            assertFalse(guard.takeRestore());
        }
    }
    @Test void explicitServerUrlResetsBaseline() {
        var guard = new PlaybackUrlGuard();
        guard.committed("https://example.com/");
        guard.reset();
        guard.committed("https://example.org/");
        guard.address("https://example.org/");
        assertFalse(guard.takeRestore());
    }
}
