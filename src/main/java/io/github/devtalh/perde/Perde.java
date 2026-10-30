package io.github.devtalh.perde;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Perde implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("perde");

    @Override
    public void onInitialize() {
        LOGGER.info("Perde initialized.");
    }
}
