package io.github.devtalh.perde.client.browser;

import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;

/** Bounded, thread-safe audit: at most one entry per category per accepted main-frame navigation. */
final class BlockedRequestLog {
    private final Set<String> seen = new HashSet<>();
    private final BiConsumer<String, String> sink;

    BlockedRequestLog(BiConsumer<String, String> sink) { this.sink = sink; }
    synchronized void newPage() { seen.clear(); }
    synchronized void blocked(String type, String url) {
        if (!seen.add(type)) return;
        String value = url == null ? "" : url;
        value = value.substring(0, Math.min(200, value.length())).replaceAll("[\\p{Cntrl}\\p{Zl}\\p{Zp}]", " ");
        sink.accept(type, value);
    }
}
