package io.github.devtalh.perde.net;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Rolling one-second window, counted only after a request passes validation. Server-thread confined. */
public final class PacketRateLimiter {
    private final Map<UUID, ArrayDeque<Long>> accepted = new HashMap<>();

    public boolean tryAccept(UUID player, long nowMs) {
        ArrayDeque<Long> window = accepted.computeIfAbsent(player, ignored -> new ArrayDeque<>());
        while (!window.isEmpty() && nowMs - window.peekFirst() >= 1000) window.removeFirst();
        if (window.size() >= 10) return false;
        window.addLast(nowMs);
        return true;
    }

    public void forget(UUID player) { accepted.remove(player); }
}
