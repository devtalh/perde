package io.github.devtalh.perde.client.net;

import io.github.devtalh.perde.client.screen.PerdePanelScreen;
import io.github.devtalh.perde.net.PerdePayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class PerdeClientNetworking {
    private PerdeClientNetworking() { }

    public static void initialize() {
        ClientPlayNetworking.registerGlobalReceiver(PerdePayloads.OpenPanel.TYPE, (payload, context) ->
                context.client().schedule(() -> {
                    if (context.client().level != null && context.client().player != null) {
                        context.client().gui.setScreen(new PerdePanelScreen(payload));
                    }
                }));
    }
}
