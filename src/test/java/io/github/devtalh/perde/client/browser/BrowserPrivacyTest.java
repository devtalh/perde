package io.github.devtalh.perde.client.browser;

import org.cef.browser.TestRequestContext;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BrowserPrivacyTest {
    @Test void deniesAndReadsBackEveryRequiredNativePermission() {
        var context = new TestRequestContext();
        BrowserPrivacy.configure(context);
        assertEquals(11, context.preferences.size());
        for (String setting : BrowserPrivacy.DENIED) {
            assertEquals(2, context.preferences.get("profile.default_content_setting_values." + setting));
        }
        assertEquals(2, context.preferences.get("profile.default_content_setting_values.media_stream_mic"));
        assertEquals(2, context.preferences.get("profile.default_content_setting_values.clipboard"));
        assertEquals("disable_non_proxied_udp", context.preferences.get("webrtc.ip_handling_policy"));
        assertEquals(java.util.List.of(), context.preferences.get("webrtc.ip_handling_url"));
        assertEquals(java.util.List.of(), context.preferences.get("webrtc.local_ips_allowed_urls"));
    }

    @Test void unavailableWebRtcPreferenceAlsoFailsClosed() {
        var context = new TestRequestContext() {
            @Override public boolean hasPreference(String name) { return !name.equals("webrtc.ip_handling_policy"); }
        };
        assertThrows(IllegalStateException.class, () -> BrowserPrivacy.configure(context));
    }

    @Test void failsClosedForMissingImmutableRejectedOrIneffectivePreferences() {
        var missing = new TestRequestContext(); missing.available = false;
        assertThrows(IllegalStateException.class, () -> BrowserPrivacy.configure(missing));
        var immutable = new TestRequestContext(); immutable.writable = false;
        assertThrows(IllegalStateException.class, () -> BrowserPrivacy.configure(immutable));
        var rejected = new TestRequestContext(); rejected.error = "rejected";
        assertThrows(IllegalStateException.class, () -> BrowserPrivacy.configure(rejected));
        var ineffective = new TestRequestContext(); ineffective.verify = false;
        assertThrows(IllegalStateException.class, () -> BrowserPrivacy.configure(ineffective));
    }
}
