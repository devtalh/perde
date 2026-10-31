package io.github.devtalh.perde.client;

import io.github.devtalh.perde.Perde;
import io.github.devtalh.perde.client.browser.PerdeBrowserManager;
import io.github.devtalh.perde.client.net.PerdeClientNetworking;
import io.github.devtalh.perde.client.render.PerdeBlockEntityRenderer;
import io.github.devtalh.perde.registry.PerdeRegistry;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import io.github.devtalh.perde.client.spike.SpikeBrowserClient;
import io.github.devtalh.perde.client.spike.SpikeBrowserCommand;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public class PerdeClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Perde.LOGGER.info("Perde client initialized.");
        PerdeClientNetworking.initialize();
        BlockEntityRenderers.register(PerdeRegistry.BLOCK_ENTITY, PerdeBlockEntityRenderer::new);
        boolean mcefAvailable = FabricLoader.getInstance().isModLoaded("mcef-modern");
        if (mcefAvailable) {
            SpikeBrowserClient.initialize();
        } else {
            Perde.LOGGER.warn("MCEF Modern is missing. Perde browser features are disabled; install the Minecraft 26.2 build of mcef-modern.");
        }
        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            SpikeBrowserCommand.register(mcefAvailable);
        }
        PerdeBrowserManager.INSTANCE.initialize(mcefAvailable);
    }
}
