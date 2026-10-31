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
            requirePreference(context, "profile.default_content_setting_values." + setting, 2); // CONTENT_SETTING_BLOCK
        }
        // Chromium 146: only proxied UDP/TCP, no per-origin policy override or local-IP exposure exception.
        requirePreference(context, "webrtc.ip_handling_policy", "disable_non_proxied_udp");
        requirePreference(context, "webrtc.ip_handling_url", new java.util.ArrayList<>());
        requirePreference(context, "webrtc.local_ips_allowed_urls", new java.util.ArrayList<>());
    }

    private static void requirePreference(CefRequestContext context, String key, Object value) {
        if (!context.hasPreference(key) || !context.canSetPreference(key)) {
            throw new IllegalStateException("Required Chromium preference unavailable: " + key);
        }
        String error = context.setPreference(key, value);
        if (error != null || !value.equals(context.getPreference(key))) {
            throw new IllegalStateException("Could not apply Chromium privacy preference: " + key + " (" + error + ")");
        }
    }
}
