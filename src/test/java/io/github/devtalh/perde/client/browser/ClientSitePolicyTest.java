package io.github.devtalh.perde.client.browser;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ClientSitePolicyTest {
    @AfterEach void reset() { ClientSitePolicy.reset(); }

    @Test void failsClosedBeforeJoinSyncAndAfterDisconnectAndSupportsLiveRemoveAdd() {
        ClientSitePolicy.reset();
        assertFalse(ClientSitePolicy.received());
        assertTrue(ClientSitePolicy.allowsMainNavigation("about:blank"));
        assertFalse(ClientSitePolicy.allowsPage("https://youtube.com"));
        ClientSitePolicy.update(List.of("youtube.com"));
        assertTrue(ClientSitePolicy.received());
        assertTrue(ClientSitePolicy.allowsPage("https://m.youtube.com"));
        ClientSitePolicy.update(List.of());
        assertTrue(ClientSitePolicy.received());
        assertFalse(ClientSitePolicy.allowsPage("https://youtube.com"));
        ClientSitePolicy.update(List.of("youtube.com"));
        assertTrue(ClientSitePolicy.allowsPage("https://youtube.com"));
        ClientSitePolicy.reset();
        assertFalse(ClientSitePolicy.allowsPage("https://youtube.com"));
        assertFalse(ClientSitePolicy.received());
    }
}
