package io.github.devtalh.perde.client;

import io.github.devtalh.perde.Perde;
import net.fabricmc.api.ClientModInitializer;

public class PerdeClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Perde.LOGGER.info("Perde client initialized.");
    }
}
