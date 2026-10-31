package io.github.devtalh.perde.screen;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import java.util.IdentityHashMap;
import java.util.Map;

/** Each integrated/dedicated server has its own start epoch, registered before levels load. */
public final class PerdeServerClock {
    private static final Map<MinecraftServer, Long> STARTS = new IdentityHashMap<>();
    private PerdeServerClock() { }
    public static void initialize() {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> STARTS.put(server, System.currentTimeMillis()));
        ServerLifecycleEvents.SERVER_STOPPED.register(STARTS::remove);
    }
    public static long startMs(MinecraftServer server) {
        Long start = STARTS.get(server);
        if (start == null) throw new IllegalStateException("Perde server clock not started");
        return start;
    }
}
