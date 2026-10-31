package io.github.devtalh.perde.client.browser;

import org.cef.browser.TestRequestContext;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BrowserPrivacyTest {
    @Test void deniesAndReadsBackEveryRequiredNativePermission() {
        var context = new TestRequestContext();
        BrowserPrivacy.configure(context);
        assertEquals(8, context.preferences.size());
        assertTrue(context.preferences.values().stream().allMatch(Integer.valueOf(2)::equals));
        assertEquals(2, context.preferences.get("profile.default_content_setting_values.media_stream_mic"));
        assertEquals(2, context.preferences.get("profile.default_content_setting_values.clipboard"));
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
