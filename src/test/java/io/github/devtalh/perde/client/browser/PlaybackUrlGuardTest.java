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

    @Test void ignoresHousekeepingAndEquivalentYoutubeRoutes() {
        var guard = new PlaybackUrlGuard();
        guard.committed("https://www.youtube.com/watch?v=aqz-KE-bpKQ&themeRefresh=1");
        for (String url : new String[]{"https://www.youtube.com/watch?v=aqz-KE-bpKQ", "https://m.youtube.com/watch?v=aqz-KE-bpKQ&pp=x&t=4&si=abc&feature=share&list=two&index=8#player",
                "https://youtu.be/aqz-KE-bpKQ", "https://www.youtube-nocookie.com/embed/aqz-KE-bpKQ"}) {
            guard.address(url);
            guard.committed(url);
            assertFalse(guard.takeRestore(), url);
        }
        guard.address("https://youtu.be/jNQXAC9IVRw");
        assertTrue(guard.takeRestore());
    }

    @Test void genericPagesIgnoreQueryButRestoreOnPathChange() {
        var guard = new PlaybackUrlGuard();
        guard.committed("https://www.example.com/episode/");
        guard.address("https://example.com/episode?player=2#video");
        assertFalse(guard.takeRestore());
        guard.address("https://example.com/another-episode");
        assertTrue(guard.takeRestore());
    }
}
