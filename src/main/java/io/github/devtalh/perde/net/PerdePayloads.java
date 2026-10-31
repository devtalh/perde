package io.github.devtalh.perde.net;

import io.github.devtalh.perde.screen.AccessMode;
import io.github.devtalh.perde.screen.ScreenState;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import io.netty.handler.codec.DecoderException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class PerdePayloads {
    private PerdePayloads() { }
    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> type(String name) {
        return new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("perde", name));
    }

    public record OpenPanel(BlockPos pos, ScreenState state, boolean canControl, boolean canAdmin) implements CustomPacketPayload {
        public static final Type<OpenPanel> TYPE = PerdePayloads.type("open_panel");
        public static final StreamCodec<FriendlyByteBuf, OpenPanel> CODEC = CustomPacketPayload.codec(OpenPanel::write, OpenPanel::new);
        private OpenPanel(FriendlyByteBuf b) { this(b.readBlockPos(), readState(b), b.readBoolean(), b.readBoolean()); }
        private void write(FriendlyByteBuf b) { b.writeBlockPos(pos); writeState(b, state); b.writeBoolean(canControl); b.writeBoolean(canAdmin); }
        @Override public Type<OpenPanel> type() { return TYPE; }
    }

    public record SetUrl(BlockPos pos, String url) implements CustomPacketPayload {
        public static final Type<SetUrl> TYPE = PerdePayloads.type("set_url");
        public static final StreamCodec<FriendlyByteBuf, SetUrl> CODEC = CustomPacketPayload.codec(SetUrl::write, SetUrl::new);
        private SetUrl(FriendlyByteBuf b) { this(b.readBlockPos(), b.readUtf(4096)); }
        private void write(FriendlyByteBuf b) { b.writeBlockPos(pos); b.writeUtf(url, 4096); }
        @Override public Type<SetUrl> type() { return TYPE; }
    }

    public record SetGeometry(BlockPos pos, double width, double height, double offsetX, double offsetY) implements CustomPacketPayload {
        public static final Type<SetGeometry> TYPE = PerdePayloads.type("set_geometry");
        public static final StreamCodec<FriendlyByteBuf, SetGeometry> CODEC = CustomPacketPayload.codec(SetGeometry::write, SetGeometry::new);
        private SetGeometry(FriendlyByteBuf b) { this(b.readBlockPos(), b.readDouble(), b.readDouble(), b.readDouble(), b.readDouble()); }
        private void write(FriendlyByteBuf b) { b.writeBlockPos(pos); b.writeDouble(width); b.writeDouble(height); b.writeDouble(offsetX); b.writeDouble(offsetY); }
        @Override public Type<SetGeometry> type() { return TYPE; }
    }

    public record CenterScreen(BlockPos pos) implements CustomPacketPayload {
        public static final Type<CenterScreen> TYPE = PerdePayloads.type("center_screen");
        public static final StreamCodec<FriendlyByteBuf, CenterScreen> CODEC = CustomPacketPayload.codec(CenterScreen::write, CenterScreen::new);
        private CenterScreen(FriendlyByteBuf b) { this(b.readBlockPos()); }
        private void write(FriendlyByteBuf b) { b.writeBlockPos(pos); }
        @Override public Type<CenterScreen> type() { return TYPE; }
    }

    public record SetAccessMode(BlockPos pos, String mode) implements CustomPacketPayload {
        public static final Type<SetAccessMode> TYPE = PerdePayloads.type("set_access_mode");
        public static final StreamCodec<FriendlyByteBuf, SetAccessMode> CODEC = CustomPacketPayload.codec(SetAccessMode::write, SetAccessMode::new);
        private SetAccessMode(FriendlyByteBuf b) { this(b.readBlockPos(), b.readUtf(32)); }
        private void write(FriendlyByteBuf b) { b.writeBlockPos(pos); b.writeUtf(mode, 32); }
        @Override public Type<SetAccessMode> type() { return TYPE; }
    }

    public record AddAllowedPlayer(BlockPos pos, String name) implements CustomPacketPayload {
        public static final Type<AddAllowedPlayer> TYPE = PerdePayloads.type("add_allowed_player");
        public static final StreamCodec<FriendlyByteBuf, AddAllowedPlayer> CODEC = CustomPacketPayload.codec(AddAllowedPlayer::write, AddAllowedPlayer::new);
        private AddAllowedPlayer(FriendlyByteBuf b) { this(b.readBlockPos(), b.readUtf(64)); }
        private void write(FriendlyByteBuf b) { b.writeBlockPos(pos); b.writeUtf(name, 64); }
        @Override public Type<AddAllowedPlayer> type() { return TYPE; }
    }

    public record RemoveAllowedPlayer(BlockPos pos, UUID uuid) implements CustomPacketPayload {
        public static final Type<RemoveAllowedPlayer> TYPE = PerdePayloads.type("remove_allowed_player");
        public static final StreamCodec<FriendlyByteBuf, RemoveAllowedPlayer> CODEC = CustomPacketPayload.codec(RemoveAllowedPlayer::write, RemoveAllowedPlayer::new);
        private RemoveAllowedPlayer(FriendlyByteBuf b) { this(b.readBlockPos(), b.readUUID()); }
        private void write(FriendlyByteBuf b) { b.writeBlockPos(pos); b.writeUUID(uuid); }
        @Override public Type<RemoveAllowedPlayer> type() { return TYPE; }
    }

    public static void registerTypes() {
        PayloadTypeRegistry.clientboundPlay().register(OpenPanel.TYPE, OpenPanel.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(SetUrl.TYPE, SetUrl.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(SetGeometry.TYPE, SetGeometry.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(CenterScreen.TYPE, CenterScreen.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(SetAccessMode.TYPE, SetAccessMode.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(AddAllowedPlayer.TYPE, AddAllowedPlayer.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(RemoveAllowedPlayer.TYPE, RemoveAllowedPlayer.CODEC);
    }

    private static void writeState(FriendlyByteBuf b, ScreenState state) {
        b.writeUtf(state.url(), 2048);
        b.writeBoolean(state.playing()); b.writeLong(state.positionMs()); b.writeLong(state.serverTimestamp());
        b.writeDouble(state.volume()); b.writeDouble(state.width()); b.writeDouble(state.height());
        b.writeDouble(state.offsetX()); b.writeDouble(state.offsetY());
        b.writeUUID(state.owner()); b.writeUtf(state.ownerName(), 16); b.writeUtf(state.accessMode().name(), 32);
        b.writeVarInt(state.allowedPlayers().size());
        state.allowedPlayers().forEach((id, name) -> { b.writeUUID(id); b.writeUtf(name, 16); });
    }

    private static ScreenState readState(FriendlyByteBuf b) {
        String url = b.readUtf(2048);
        boolean playing = b.readBoolean(); long position = b.readLong(), timestamp = b.readLong();
        double volume = b.readDouble(), width = b.readDouble(), height = b.readDouble(), x = b.readDouble(), y = b.readDouble();
        UUID owner = b.readUUID(); String ownerName = b.readUtf(16); AccessMode mode = AccessMode.valueOf(b.readUtf(32));
        int count = b.readVarInt();
        if (count < 0 || count > ScreenState.MAX_ALLOWED_PLAYERS) throw new DecoderException("Invalid allowed-player count");
        Map<UUID, String> players = new LinkedHashMap<>();
        for (int i = 0; i < count; i++) players.put(b.readUUID(), b.readUtf(16));
        return new ScreenState(url, playing, position, timestamp, volume, width, height, x, y, owner, ownerName, mode, players);
    }
}
