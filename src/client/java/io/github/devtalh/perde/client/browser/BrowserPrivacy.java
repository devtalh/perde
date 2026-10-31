package io.github.devtalh.perde.client.browser;

import org.cef.browser.CefRequestContext;
import java.util.List;

/** Chromium 146 native content settings, applied on the CEF UI thread before any public page. */
final class BrowserPrivacy {
    static final List<String> DENIED = List.of("media_stream_mic", "media_stream_camera", "geolocation",
            "notifications", "clipboard", "automatic_downloads", "file_system_read_guard", "file_system_write_guard");
    private BrowserPrivacy() { }

    static void configure(CefRequestContext context) {
        for (String setting : DENIED) {
            String key = "profile.default_content_setting_values." + setting;
            if (!context.hasPreference(key) || !context.canSetPreference(key)) {
                throw new IllegalStateException("Required Chromium permission preference unavailable: " + key);
            }
            String error = context.setPreference(key, 2); // CONTENT_SETTING_BLOCK
            if (error != null || !Integer.valueOf(2).equals(context.getPreference(key))) {
                throw new IllegalStateException("Could not deny Chromium permission: " + key + " (" + error + ")");
            }
        }
    }
}
