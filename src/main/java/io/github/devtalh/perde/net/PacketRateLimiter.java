package io.github.devtalh.perde.net;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Rolling one-second window, counted only after a request passes validation. Server-thread confined. */
public final class PacketRateLimiter {
    private final Map<UUID, ArrayDeque<Long>> accepted = new HashMap<>();

    private final int maxPerSecond;
    public PacketRateLimiter() { this(10); }
    public PacketRateLimiter(int maxPerSecond) {
        if (maxPerSecond < 1) throw new IllegalArgumentException("Invalid packet limit");
        this.maxPerSecond = maxPerSecond;
    }

    public boolean tryAccept(UUID player, long nowMs) {
        ArrayDeque<Long> window = accepted.computeIfAbsent(player, ignored -> new ArrayDeque<>());
        while (!window.isEmpty() && nowMs - window.peekFirst() >= 1000) window.removeFirst();
        if (window.size() >= maxPerSecond) return false;
        window.addLast(nowMs);
        return true;
    }

    public void forget(UUID player) { accepted.remove(player); }
}
