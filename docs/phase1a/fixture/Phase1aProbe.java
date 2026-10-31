package io.github.devtalh.perde.client.test;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.github.devtalh.perde.net.PerdePayloads;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import io.github.devtalh.perde.block.PerdeBlockEntity;
import io.github.devtalh.perde.screen.ScreenState;
import org.slf4j.LoggerFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/** Opt-in integration fixture, compiled only by probe.init.gradle and never bundled in Perde. */
public final class Phase1aProbe implements ClientModInitializer {
    private final Path commandFile = FabricLoader.getInstance().getGameDir().resolve("phase1a-probe-command.txt");
    private String lastCommand = "";
    private BlockPos watched;
    private ScreenState lastState;

    @Override public void onInitializeClient() {
        try { if (Files.exists(commandFile)) lastCommand = Files.readString(commandFile); }
        catch (java.io.IOException exception) { throw new IllegalStateException(exception); }
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            var url = ClientCommands.argument("url", StringArgumentType.greedyString()).executes(context -> {
                BlockPos pos = new BlockPos(IntegerArgumentType.getInteger(context, "x"),
                        IntegerArgumentType.getInteger(context, "y"), IntegerArgumentType.getInteger(context, "z"));
                // Deliberately bypasses client URL validation to exercise the real server gate.
                ClientPlayNetworking.send(new PerdePayloads.SetUrl(pos, StringArgumentType.getString(context, "url")));
                return 1;
            });
            var z = ClientCommands.argument("z", IntegerArgumentType.integer()).then(url);
            var y = ClientCommands.argument("y", IntegerArgumentType.integer()).then(z);
            var x = ClientCommands.argument("x", IntegerArgumentType.integer()).then(y);
            dispatcher.register(ClientCommands.literal("perde-f1a-test").then(x));
        });
    }

    private void tick(Minecraft client) {
        if (client.level == null || client.player == null || client.gameMode == null) return;
        try {
            if (Files.exists(commandFile)) {
                String command = Files.readString(commandFile);
                if (!command.equals(lastCommand)) {
                    lastCommand = command;
                    String[] args = command.strip().split("\\t", -1);
                    BlockPos pos = new BlockPos(Integer.parseInt(args[2]), Integer.parseInt(args[3]), Integer.parseInt(args[4]));
                    watched = pos;
                    switch (args[1]) {
                        case "open" -> client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND,
                                new BlockHitResult(Vec3.atCenterOf(pos), Direction.NORTH, pos, false));
                        case "url" -> ClientPlayNetworking.send(new PerdePayloads.SetUrl(pos, args[5]));
                        case "geometry" -> ClientPlayNetworking.send(new PerdePayloads.SetGeometry(pos,
                                Double.parseDouble(args[5]), Double.parseDouble(args[6]), Double.parseDouble(args[7]), Double.parseDouble(args[8])));
                        case "center" -> ClientPlayNetworking.send(new PerdePayloads.CenterScreen(pos));
                        case "access" -> ClientPlayNetworking.send(new PerdePayloads.SetAccessMode(pos, args[5]));
                        case "add" -> ClientPlayNetworking.send(new PerdePayloads.AddAllowedPlayer(pos, args[5]));
                        case "remove" -> ClientPlayNetworking.send(new PerdePayloads.RemoveAllowedPlayer(pos, UUID.fromString(args[5])));
                        case "watch" -> { }
                        default -> throw new IllegalArgumentException("Unknown fixture action");
                    }
                    LoggerFactory.getLogger("perde-f1a-probe").info("{} action {} at {}", client.player.getPlainTextName(), args[1], pos);
                }
            }
            if (watched != null) {
                ScreenState state = client.level.getBlockEntity(watched) instanceof PerdeBlockEntity entity ? entity.screenState() : null;
                if (!java.util.Objects.equals(state, lastState)) {
                    lastState = state;
                    LoggerFactory.getLogger("perde-f1a-probe").info("{} observed {} at {}", client.player.getPlainTextName(), state, watched);
                }
            }
        } catch (java.io.IOException | RuntimeException exception) {
            LoggerFactory.getLogger("perde-f1a-probe").error("Integration fixture failed", exception);
        }
    }
}
