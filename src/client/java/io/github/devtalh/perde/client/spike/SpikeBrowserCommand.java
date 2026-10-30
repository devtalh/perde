package io.github.devtalh.perde.client.spike;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

import java.net.URI;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public final class SpikeBrowserCommand {
    private static final String DEFAULT_URL = "https://www.youtube.com/watch?v=dQw4w9WgXcQ";

    private SpikeBrowserCommand() {
    }

    public static void register(boolean mcefAvailable) {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, context) -> dispatcher.register(
                literal("perde-spike").then(literal("gui")
                        .executes(command -> open(command.getSource(), DEFAULT_URL, mcefAvailable))
                        .then(argument("url", StringArgumentType.greedyString())
                                .executes(command -> open(command.getSource(),
                                        StringArgumentType.getString(command, "url"), mcefAvailable))))));
    }

    private static int open(FabricClientCommandSource source, String url, boolean mcefAvailable) {
        if (!mcefAvailable) {
            source.sendError(Component.literal("MCEF Modern is missing. Perde browser features are disabled."));
            return 0;
        }
        try {
            URI uri = URI.create(url);
            if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null) {
                throw new IllegalArgumentException("Expected an http/https URL with a host");
            }
        } catch (IllegalArgumentException exception) {
            source.sendError(Component.literal("Only valid http/https URLs are allowed."));
            return 0;
        }
        // Commands run from the chat screen; open on the next client task so chat cannot close it.
        source.getClient().schedule(() -> source.getClient().gui.setScreen(new SpikeBrowserScreen(url)));
        return 1;
    }
}
