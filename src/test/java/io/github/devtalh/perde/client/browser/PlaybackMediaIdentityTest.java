package io.github.devtalh.perde.client.browser;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class PlaybackMediaIdentityTest {
    @ParameterizedTest
    @ValueSource(strings = {
        "https://youtube.com/watch?v=aqz-KE-bpKQ", "https://www.youtube.com/watch?pp=test&v=aqz-KE-bpKQ&si=abc&t=60#player",
        "https://m.youtube.com/watch?v=aqz-KE-bpKQ&list=one&index=2", "https://music.youtube.com/watch?v=aqz-KE-bpKQ&feature=share",
        "http://YOUTUBE.COM/watch?v=aqz-KE-bpKQ", "https://youtu.be/aqz-KE-bpKQ?si=test",
        "https://www.youtube.com/embed/aqz-KE-bpKQ?autoplay=1", "https://www.youtube-nocookie.com/embed/aqz-KE-bpKQ",
        "https://youtube-nocookie.com/watch?v=aqz-KE-bpKQ", "https://youtube.com/shorts/aqz-KE-bpKQ/",
        "https://youtube.com/live/aqz-KE-bpKQ?feature=share", "https://youtube.com/watch?%76=aqz-KE-bpKQ",
        "https://youtu.be/%61qz-KE-bpKQ", "https://youtube.com/watch?v=&v=aqz-KE-bpKQ"
    })
    void youtubeVariantsShareOneIdentity(String url) {
        assertEquals("youtube:aqz-KE-bpKQ", PlaybackMediaIdentity.key(url));
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://www.example.com/episode/", "http://EXAMPLE.COM/episode?source=x#player", "https://example.com/episode///"})
    void otherSitesIgnoreQueryFragmentSchemeAndWww(String url) {
        assertEquals("example.com/episode", PlaybackMediaIdentity.key(url));
    }

    @Test void distinctVideosPathsAndHostsRemainDistinct() {
        assertNotEquals(PlaybackMediaIdentity.key("https://youtube.com/watch?v=AbC"), PlaybackMediaIdentity.key("https://youtu.be/abc"));
        assertNotEquals(PlaybackMediaIdentity.key("https://example.com/one"), PlaybackMediaIdentity.key("https://example.com/two"));
        assertNotEquals(PlaybackMediaIdentity.key("https://example.com/one"), PlaybackMediaIdentity.key("https://other.example.com/one"));
        assertEquals("youtube.com/playlist", PlaybackMediaIdentity.key("https://www.youtube.com/playlist?list=abc"));
        assertEquals("m.youtube.com/watch", PlaybackMediaIdentity.key("https://m.youtube.com/watch?feature=share"));
        assertEquals("youtube.com/watch", PlaybackMediaIdentity.key("https://youtube.com/watch?v="));
        assertEquals("example.com", PlaybackMediaIdentity.key("https://www.example.com/?foo=bar"));
    }

    @Test void lookalikesAreNotYoutubeAndInvalidUrlsHaveNoIdentity() {
        assertEquals("youtube.com.evil.example/watch", PlaybackMediaIdentity.key("https://youtube.com.evil.example/watch?v=abc"));
        assertEquals("evilyoutube.com/watch", PlaybackMediaIdentity.key("https://evilyoutube.com/watch?v=abc"));
        for (String url : new String[]{"about:blank", "javascript:alert(1)", "file:///test", "http://127.0.0.1/", "not a URL", "https://youtube.com/watch?v=%zz"})
            assertNull(PlaybackMediaIdentity.key(url), url);
        assertNull(PlaybackMediaIdentity.key(null));
    }

    @Test void acceptedUnicodeAuthoritiesAndDnsAliasesDoNotCrashOrChangeIdentity() {
        assertEquals("xn--bcher-kva.de/episode", PlaybackMediaIdentity.key("https://www.bücher.de/episode/"));
        assertEquals(PlaybackMediaIdentity.key("https://bücher.de/episode"), PlaybackMediaIdentity.key("https://xn--bcher-kva.de/episode"));
        assertEquals("xn--bcher-kva.de/episode", PlaybackMediaIdentity.key("https://test:pass@bücher.de:8443/episode"));
        assertEquals("youtube:aqz-KE-bpKQ", PlaybackMediaIdentity.key("https://www.YouTube.com./watch?v=aqz-KE-bpKQ"));
    }
}
