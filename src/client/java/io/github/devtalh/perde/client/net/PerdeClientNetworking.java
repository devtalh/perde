package io.github.devtalh.perde.client.net;

import io.github.devtalh.perde.client.screen.PerdePanelScreen;
import io.github.devtalh.perde.net.PerdePayloads;
import io.github.devtalh.perde.client.browser.ClientSitePolicy;
import io.github.devtalh.perde.client.browser.PerdeBrowserManager;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import io.github.devtalh.perde.Perde;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class PerdeClientNetworking {
    private PerdeClientNetworking() { }

    public static void initialize() {
        ClientPlaybackClock.initialize();
        ClientPlayConnectionEvents.INIT.register((listener, client) -> ClientSitePolicy.reset());
        ClientPlayConnectionEvents.DISCONNECT.register((listener, client) -> ClientSitePolicy.reset());
        ClientPlayNetworking.registerGlobalReceiver(PerdePayloads.AllowedDomains.TYPE, (payload, context) -> context.client().execute(() -> {
            ClientSitePolicy.update(payload.domains());
            PerdeBrowserManager.INSTANCE.policyChanged();
            Perde.LOGGER.info("Perde allowlist received: {}", payload.domains());
        }));
        ClientPlayNetworking.registerGlobalReceiver(PerdePayloads.OpenPanel.TYPE, (payload, context) ->
                context.client().schedule(() -> {
                    if (context.client().level != null && context.client().player != null) {
                        context.client().gui.setScreen(new PerdePanelScreen(payload));
                    }
                }));
    }
}
