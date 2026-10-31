package io.github.devtalh.perde.client.net;

import io.github.devtalh.perde.Perde;
import io.github.devtalh.perde.net.PerdePayloads;
import io.github.devtalh.perde.net.ServerTimeEstimator;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import java.util.LinkedHashSet;
import java.util.Set;

/** Connection-scoped clock sampling. No browser/video operations. Client-thread confined. */
public final class ClientPlaybackClock {
    private static final ServerTimeEstimator ESTIMATOR = new ServerTimeEstimator(System::nanoTime);
    private static final Set<Long> PENDING = new LinkedHashSet<>();
    private static boolean connected;
    private static int initialSamples;
    private static long nextSample;
    private ClientPlaybackClock() { }

    public static void initialize() {
        ClientPlayConnectionEvents.INIT.register((listener, client) -> reset());
        ClientPlayConnectionEvents.JOIN.register((listener, sender, client) -> {
            reset(); connected = true;
        });
        ClientPlayConnectionEvents.DISCONNECT.register((listener, client) -> reset());
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!connected || client.getConnection() == null) return;
            long now = System.nanoTime();
            if (initialSamples != 0 && now - nextSample < 0) return;
            if (PENDING.size() >= 8) PENDING.remove(PENDING.iterator().next());
            PENDING.add(now);
            ClientPlayNetworking.send(new PerdePayloads.TimeRequest(now));
            if (initialSamples < 5) initialSamples++;
            nextSample = now + (initialSamples < 5 ? 200_000_000L : 15_000_000_000L);
        });
        ClientPlayNetworking.registerGlobalReceiver(PerdePayloads.TimeResponse.TYPE, (payload, context) -> {
            long received = System.nanoTime();
            if (!connected || !PENDING.remove(payload.clientNanos())) return;
            if (ESTIMATOR.sample(payload.clientNanos(), received, payload.serverMs())) {
                Perde.LOGGER.info("Perde clock player={} offsetMs={} rttMs={} serverNowMs={}",
                        context.player().getPlainTextName(), ESTIMATOR.offsetMs(), ESTIMATOR.rttMs(), ESTIMATOR.serverNowMs());
            }
        });
    }
    public static boolean ready() { return ESTIMATOR.ready(); }
    public static long serverNowMs() { return ESTIMATOR.serverNowMs(); }
    public static double rttMs() { return ESTIMATOR.rttMs(); }
    public static double offsetMs() { return ESTIMATOR.offsetMs(); }
    private static void reset() {
        connected = false; initialSamples = 0; nextSample = 0; PENDING.clear(); ESTIMATOR.reset();
    }
}
