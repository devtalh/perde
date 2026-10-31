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
import io.github.devtalh.perde.screen.ScreenPermissions;
import io.github.devtalh.perde.registry.PerdeRegistry;
import net.minecraft.world.level.GameType;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
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
    private BlockPos mining;
    private BlockPos collecting;
    private int actionTicks;
    private int initialItems;
    private int miningTicks;

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
        if (client.level == null || client.player == null || client.gameMode == null) {
            client.options.keyUp.setDown(false);
            mining = collecting = null;
            return;
        }
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
                        case "close" -> client.gui.setScreen(null);
                        case "mine" -> {
                            if (client.gameMode.getPlayerMode() != GameType.SURVIVAL
                                    || !client.level.getBlockState(pos).is(PerdeRegistry.BLOCK)
                                    || client.player.getEyePosition().distanceTo(Vec3.atCenterOf(pos)) > 4.5) {
                                throw new IllegalStateException("Mining requires survival, a nearby Perde and normal reach");
                            }
                            client.gui.setScreen(null);
                            aim(client, Vec3.atCenterOf(pos));
                            initialItems = itemCount(client);
                            mining = pos;
                            actionTicks = 0;
                            float progress = client.level.getBlockState(pos).getDestroyProgress(client.player, client.level, pos);
                            if (!(progress > 0)) throw new IllegalStateException("Block cannot be mined");
                            miningTicks = (int) Math.ceil(1.0 / progress) + 2;
                            // Ordinary survival mining packets; the server checks reach, mode and elapsed hardness.
                            client.player.connection.send(new ServerboundPlayerActionPacket(Action.START_DESTROY_BLOCK, pos, Direction.NORTH));
                        }
                        case "collect" -> {
                            initialItems = itemCount(client);
                            collecting = pos;
                            actionTicks = 0;
                            aim(client, new Vec3(pos.getX() + 0.5, client.player.getEyeY(), pos.getZ() + 0.5));
                            client.options.keyUp.setDown(true);
                        }
                        case "status" -> {
                            ScreenState state = client.level.getBlockEntity(pos) instanceof PerdeBlockEntity entity ? entity.screenState() : null;
                            LoggerFactory.getLogger("perde-f1a-probe").info(
                                    "{} status mode={} items={} held={} block={} entity={} owner={} control={} admin={} state={} position={}",
                                    client.player.getPlainTextName(), client.gameMode.getPlayerMode(), itemCount(client),
                                    client.player.getMainHandItem(), client.level.getBlockState(pos), client.level.getBlockEntity(pos),
                                    state != null && state.owner().equals(client.player.getUUID()),
                                    state != null && ScreenPermissions.canControl(state, client.player.getUUID(), 0),
                                    state != null && ScreenPermissions.canAdmin(state, client.player.getUUID(), 0), state, client.player.position());
                        }
                        case "f2" -> {
                            if (!client.handleGlobalKeyPress(client.options.keyScreenshot.getDefaultKey(), false)) {
                                throw new IllegalStateException("Default F2 screenshot key is not bound");
                            }
                        }
                        case "quit" -> client.stop();
                        default -> throw new IllegalArgumentException("Unknown fixture action");
                    }
                    LoggerFactory.getLogger("perde-f1a-probe").info("{} action {} at {}", client.player.getPlainTextName(), args[1], pos);
                }
            }
            if (mining != null && ++actionTicks == miningTicks) {
                client.player.connection.send(new ServerboundPlayerActionPacket(Action.STOP_DESTROY_BLOCK, mining, Direction.NORTH));
            }
            if (mining != null && (actionTicks > 200 || client.level.getBlockState(mining).isAir())) {
                if (!client.level.getBlockState(mining).isAir()) {
                    client.player.connection.send(new ServerboundPlayerActionPacket(Action.ABORT_DESTROY_BLOCK, mining, Direction.NORTH));
                }
                LoggerFactory.getLogger("perde-f1a-probe").info("{} mining finished ticks={} air={} itemsBefore={} itemsNow={} mode={}",
                        client.player.getPlainTextName(), actionTicks, client.level.getBlockState(mining).isAir(),
                        initialItems, itemCount(client), client.gameMode.getPlayerMode());
                mining = null;
            }
            if (collecting != null && (++actionTicks > 100 || itemCount(client) > initialItems
                    || client.player.position().distanceTo(Vec3.atBottomCenterOf(collecting)) < 0.65)) {
                client.options.keyUp.setDown(false);
                LoggerFactory.getLogger("perde-f1a-probe").info("{} collection finished ticks={} itemsBefore={} itemsNow={} position={}",
                        client.player.getPlainTextName(), actionTicks, initialItems, itemCount(client), client.player.position());
                collecting = null;
            }
            if (watched != null) {
                ScreenState state = client.level.getBlockEntity(watched) instanceof PerdeBlockEntity entity ? entity.screenState() : null;
                if (!java.util.Objects.equals(state, lastState)) {
                    lastState = state;
                    LoggerFactory.getLogger("perde-f1a-probe").info("{} observed {} at {}", client.player.getPlainTextName(), state, watched);
                }
            }
        } catch (java.io.IOException | RuntimeException exception) {
            client.options.keyUp.setDown(false);
            mining = collecting = null;
            LoggerFactory.getLogger("perde-f1a-probe").error("Integration fixture failed", exception);
        }
    }

    private static int itemCount(Minecraft client) {
        int count = 0;
        var inventory = client.player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            var stack = inventory.getItem(slot);
            if (stack.is(PerdeRegistry.ITEM)) count += stack.getCount();
        }
        return count;
    }

    private static void aim(Minecraft client, Vec3 target) {
        Vec3 delta = target.subtract(client.player.getEyePosition());
        client.player.setYRot((float) Math.toDegrees(Math.atan2(-delta.x, delta.z)));
        client.player.setXRot((float) -Math.toDegrees(Math.atan2(delta.y, Math.hypot(delta.x, delta.z))));
    }
}
