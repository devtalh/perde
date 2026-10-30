package io.github.devtalh.perde.client.spike;

import io.github.devtalh.perde.Perde;
import net.dimaskama.mcef.api.MCEFApi;
import net.fabricmc.loader.api.FabricLoader;

public final class SpikeBrowserClient {
    private static MCEFApi.Initialization initialization;

    private SpikeBrowserClient() {
    }

    public static void initialize() {
        initialization = MCEFApi.initialize();
        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            SpikeWorldBrowser.initialize();
            SpikeWorldRenderer.initialize();
        }
        MCEFApi.getInstanceFuture().whenComplete((api, failure) -> {
            if (failure == null) {
                Perde.LOGGER.info("Perde spike: MCEF initialization completed.");
            } else {
                Perde.LOGGER.error("Perde spike: MCEF initialization failed. Browser features are unavailable.", failure);
            }
        });
    }

    static MCEFApi.Initialization initialization() {
        return initialization;
    }
}
