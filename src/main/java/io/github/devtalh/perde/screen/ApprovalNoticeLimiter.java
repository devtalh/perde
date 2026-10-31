package io.github.devtalh.perde.screen;

import java.util.HashMap;
import java.util.Map;

/** Server-thread only. A domain may notify operators at most once per minute, across all players. */
public final class ApprovalNoticeLimiter {
    public static final long INTERVAL_MS = 60_000;
    private final Map<String, Long> last = new HashMap<>();

    public boolean tryAcquire(String domain, long nowMs) {
        last.entrySet().removeIf(entry -> nowMs - entry.getValue() >= INTERVAL_MS);
        if (last.containsKey(domain) || last.size() >= SiteAllowlist.MAX_DOMAINS) return false;
        last.put(domain, nowMs);
        return true;
    }
}
