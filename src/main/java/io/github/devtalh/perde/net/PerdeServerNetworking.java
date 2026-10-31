package io.github.devtalh.perde.net;

import io.github.devtalh.perde.block.PerdeBlockEntity;
import io.github.devtalh.perde.screen.ScreenPermissions;
import io.github.devtalh.perde.screen.ScreenState;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class PerdeServerNetworking {
    private static final PacketRateLimiter LIMITER = new PacketRateLimiter();
    private static final Map<UUID, OpenedScreen> OPENED = new HashMap<>();
    private record OpenedScreen(ResourceKey<Level> level, BlockPos pos) { }

    private PerdeServerNetworking() { }

    public static void initialize() {
        PerdePayloads.registerTypes();
        ServerPlayNetworking.registerGlobalReceiver(PerdePayloads.SetUrl.TYPE,
                (payload, context) -> handle(context.player(), payload.pos(), new ControlDecisions.SetUrl(payload.url())));
        ServerPlayNetworking.registerGlobalReceiver(PerdePayloads.SetGeometry.TYPE,
                (payload, context) -> handle(context.player(), payload.pos(), new ControlDecisions.SetGeometry(payload.width(), payload.height(), payload.offsetX(), payload.offsetY())));
        ServerPlayNetworking.registerGlobalReceiver(PerdePayloads.CenterScreen.TYPE,
                (payload, context) -> handle(context.player(), payload.pos(), new ControlDecisions.Center()));
        ServerPlayNetworking.registerGlobalReceiver(PerdePayloads.SetAccessMode.TYPE,
                (payload, context) -> handle(context.player(), payload.pos(), new ControlDecisions.SetAccess(payload.mode())));
        ServerPlayNetworking.registerGlobalReceiver(PerdePayloads.AddAllowedPlayer.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            // Authorize before looking up another player's identity. The final handler checks again.
            ControlDecisions.Context gate = context(player, payload.pos());
            ScreenState state = screenState(player, payload.pos(), gate.chunkLoaded());
            ControlDecisions.Rejection rejection = ControlDecisions.authorize(gate, state, true);
            if (rejection != null) { reject(player, rejection); return; }
            String name = payload.name().trim();
            UUID id = null;
            if (ControlDecisions.validPlayerName(name)) {
                ServerPlayer online = context.server().getPlayerList().getPlayerByName(name);
                if (online != null && online.getPlainTextName().equals(name)) {
                    id = online.getUUID();
                } else if (!context.server().usesAuthentication()) {
                    id = UUIDUtil.createOfflinePlayerUUID(name);
                }
            }
            handle(player, payload.pos(), new ControlDecisions.AddPlayer(id, name));
        });
        ServerPlayNetworking.registerGlobalReceiver(PerdePayloads.RemoveAllowedPlayer.TYPE,
                (payload, context) -> handle(context.player(), payload.pos(), new ControlDecisions.RemovePlayer(payload.uuid())));
        ServerPlayConnectionEvents.DISCONNECT.register((listener, server) -> {
            UUID id = listener.player.getUUID();
            LIMITER.forget(id);
            OPENED.remove(id);
        });
    }

    public static void openPanel(ServerPlayer player, PerdeBlockEntity entity) {
        if (entity.getLevel() != player.level() || player.getEyePosition().distanceToSqr(Vec3.atCenterOf(entity.getBlockPos())) > 64) {
            reject(player, ControlDecisions.Rejection.TOO_FAR);
            return;
        }
        OPENED.put(player.getUUID(), new OpenedScreen(player.level().dimension(), entity.getBlockPos().immutable()));
        ScreenState state = entity.screenState();
        int permissionLevel = permissionLevel(player);
        ServerPlayNetworking.send(player, new PerdePayloads.OpenPanel(entity.getBlockPos(), state,
                ScreenPermissions.canControl(state, player.getUUID(), permissionLevel),
                ScreenPermissions.canAdmin(state, player.getUUID(), permissionLevel)));
    }

    private static int permissionLevel(ServerPlayer player) {
        return player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER) ? 2 : 0;
    }

    private static ControlDecisions.Context context(ServerPlayer player, BlockPos pos) {
        OpenedScreen opened = OPENED.get(player.getUUID());
        boolean sameLevel = opened != null && opened.level().equals(player.level().dimension()) && opened.pos().equals(pos);
        boolean loaded = sameLevel && player.level().isLoaded(pos);
        boolean exists = loaded && player.level().getBlockEntity(pos) instanceof PerdeBlockEntity;
        return new ControlDecisions.Context(player.getUUID(), permissionLevel(player), sameLevel, loaded, exists,
                player.getEyePosition().distanceToSqr(Vec3.atCenterOf(pos)));
    }

    private static ScreenState screenState(ServerPlayer player, BlockPos pos, boolean loaded) {
        return loaded && player.level().getBlockEntity(pos) instanceof PerdeBlockEntity entity ? entity.screenState() : null;
    }

    private static void handle(ServerPlayer player, BlockPos pos, ControlDecisions.Change change) {
        // Fabric's typed play receiver runs on the server thread (verified in 6.3.4 sources).
        ControlDecisions.Context context = context(player, pos);
        ScreenState current = screenState(player, pos, context.chunkLoaded());
        ControlDecisions.Result result = ControlDecisions.decide(context, current, change, LIMITER, System.nanoTime() / 1_000_000);
        if (result.rejection() != null) { reject(player, result.rejection()); return; }
        if (result.rateLimited()) return;
        ((PerdeBlockEntity) player.level().getBlockEntity(pos)).setScreenState(result.state());
    }

    private static void reject(ServerPlayer player, ControlDecisions.Rejection rejection) {
        player.sendOverlayMessage(Component.translatable("perde.error." + rejection.name().toLowerCase(Locale.ROOT)));
    }
}
