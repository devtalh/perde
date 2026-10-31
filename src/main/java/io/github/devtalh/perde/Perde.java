package io.github.devtalh.perde;

import net.fabricmc.api.ModInitializer;
import io.github.devtalh.perde.registry.PerdeRegistry;
import io.github.devtalh.perde.net.PerdeServerNetworking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Perde implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("perde");

    @Override
    public void onInitialize() {
        PerdeRegistry.initialize();
        PerdeServerNetworking.initialize();
        LOGGER.info("Perde initialized.");
    }
}
