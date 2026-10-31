package io.github.devtalh.perde.client.browser;

import io.github.devtalh.perde.screen.UrlPolicy;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** Navigation identity only. URL permission checks remain the responsibility of the browser policy. */
final class PlaybackMediaIdentity {
    private PlaybackMediaIdentity() { }

    static String key(String url) {
        if (!UrlPolicy.allowsPage(url)) return null;
        try {
            URI uri = URI.create(url);
            String host = uri.getHost().toLowerCase(Locale.ROOT);
            if (host.startsWith("www.")) host = host.substring(4);
            String path = uri.getRawPath() == null ? "" : uri.getRawPath();
            while (path.endsWith("/")) path = path.substring(0, path.length() - 1);
            boolean youtube = family(host, "youtube.com") || family(host, "youtube-nocookie.com");
            String video = null;
            if (family(host, "youtu.be")) video = segment(path, 1);
            else if (youtube) {
                if (path.equals("/watch")) video = queryVideo(uri.getRawQuery());
                else if (path.startsWith("/embed/") || path.startsWith("/shorts/") || path.startsWith("/live/")) video = segment(path, 2);
            }
            if (video != null && video.matches("[A-Za-z0-9_-]+")) return "youtube:" + video;
            return host + path;
        } catch (IllegalArgumentException exception) { return null; }
    }

    private static boolean family(String host, String base) { return host.equals(base) || host.endsWith("." + base); }
    private static String segment(String path, int index) {
        String[] parts = path.split("/", -1);
        return parts.length > index ? URLDecoder.decode(parts[index], StandardCharsets.UTF_8) : null;
    }
    private static String queryVideo(String query) {
        if (query == null) return null;
        for (String field : query.split("&")) {
            String[] pair = field.split("=", 2);
            if (pair.length == 2 && URLDecoder.decode(pair[0], StandardCharsets.UTF_8).equals("v")) {
                String value = URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
                if (!value.isEmpty()) return value;
            }
        }
        return null;
    }
}
