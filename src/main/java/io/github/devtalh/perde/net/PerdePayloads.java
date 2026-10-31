package io.github.devtalh.perde.net;

import io.github.devtalh.perde.screen.AccessMode;
import io.github.devtalh.perde.screen.ScreenState;
import io.github.devtalh.perde.screen.SiteAllowlist;
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
import java.util.List;

public final class PerdePayloads {
    private PerdePayloads() { }
    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> type(String name) {
        return new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("perde", name));
    }

    public record Play(BlockPos pos) implements CustomPacketPayload {
        public static final Type<Play> TYPE = PerdePayloads.type("play");
        public static final StreamCodec<FriendlyByteBuf, Play> CODEC = CustomPacketPayload.codec(Play::write, Play::new);
        private Play(FriendlyByteBuf b) { this(b.readBlockPos()); }
        private void write(FriendlyByteBuf b) { b.writeBlockPos(pos); }
        @Override public Type<Play> type() { return TYPE; }
    }

    public record Pause(BlockPos pos) implements CustomPacketPayload {
        public static final Type<Pause> TYPE = PerdePayloads.type("pause");
        public static final StreamCodec<FriendlyByteBuf, Pause> CODEC = CustomPacketPayload.codec(Pause::write, Pause::new);
        private Pause(FriendlyByteBuf b) { this(b.readBlockPos()); }
        private void write(FriendlyByteBuf b) { b.writeBlockPos(pos); }
        @Override public Type<Pause> type() { return TYPE; }
    }

    public record SeekRelative(BlockPos pos, double deltaMs) implements CustomPacketPayload {
        public static final Type<SeekRelative> TYPE = PerdePayloads.type("seek_relative");
        public static final StreamCodec<FriendlyByteBuf, SeekRelative> CODEC = CustomPacketPayload.codec(SeekRelative::write, SeekRelative::new);
        private SeekRelative(FriendlyByteBuf b) { this(b.readBlockPos(), b.readDouble()); }
        private void write(FriendlyByteBuf b) { b.writeBlockPos(pos); b.writeDouble(deltaMs); }
        @Override public Type<SeekRelative> type() { return TYPE; }
    }

    public record SeekAbsolute(BlockPos pos, double positionMs) implements CustomPacketPayload {
        public static final Type<SeekAbsolute> TYPE = PerdePayloads.type("seek_absolute");
        public static final StreamCodec<FriendlyByteBuf, SeekAbsolute> CODEC = CustomPacketPayload.codec(SeekAbsolute::write, SeekAbsolute::new);
        private SeekAbsolute(FriendlyByteBuf b) { this(b.readBlockPos(), b.readDouble()); }
        private void write(FriendlyByteBuf b) { b.writeBlockPos(pos); b.writeDouble(positionMs); }
        @Override public Type<SeekAbsolute> type() { return TYPE; }
    }

    public record TimeRequest(long clientNanos) implements CustomPacketPayload {
        public static final Type<TimeRequest> TYPE = PerdePayloads.type("time_request");
        public static final StreamCodec<FriendlyByteBuf, TimeRequest> CODEC = CustomPacketPayload.codec(TimeRequest::write, TimeRequest::new);
        private TimeRequest(FriendlyByteBuf b) { this(b.readLong()); }
        private void write(FriendlyByteBuf b) { b.writeLong(clientNanos); }
        @Override public Type<TimeRequest> type() { return TYPE; }
    }

    public record TimeResponse(long clientNanos, long serverMs) implements CustomPacketPayload {
        public static final Type<TimeResponse> TYPE = PerdePayloads.type("time_response");
        public static final StreamCodec<FriendlyByteBuf, TimeResponse> CODEC = CustomPacketPayload.codec(TimeResponse::write, TimeResponse::new);
        private TimeResponse(FriendlyByteBuf b) { this(b.readLong(), b.readLong()); }
        private void write(FriendlyByteBuf b) { b.writeLong(clientNanos); b.writeLong(serverMs); }
        @Override public Type<TimeResponse> type() { return TYPE; }
    }

    public record AllowedDomains(List<String> domains) implements CustomPacketPayload {
        public AllowedDomains { domains = new SiteAllowlist(domains).domains(); }
        public static final Type<AllowedDomains> TYPE = PerdePayloads.type("allowed_domains");
        public static final StreamCodec<FriendlyByteBuf, AllowedDomains> CODEC = CustomPacketPayload.codec(AllowedDomains::write, AllowedDomains::new);
        private AllowedDomains(FriendlyByteBuf b) { this(readDomains(b)); }
        private void write(FriendlyByteBuf b) {
            b.writeVarInt(domains.size());
            domains.forEach(domain -> b.writeUtf(domain, 253));
        }
        @Override public Type<AllowedDomains> type() { return TYPE; }
        private static List<String> readDomains(FriendlyByteBuf b) {
            int count = b.readVarInt();
            if (count < 0 || count > SiteAllowlist.MAX_DOMAINS) throw new DecoderException("Invalid domain count");
            var result = new java.util.ArrayList<String>(count);
            for (int i = 0; i < count; i++) result.add(b.readUtf(253));
            return result;
        }
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
        PayloadTypeRegistry.serverboundPlay().register(Play.TYPE, Play.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(Pause.TYPE, Pause.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(SeekRelative.TYPE, SeekRelative.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(SeekAbsolute.TYPE, SeekAbsolute.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(TimeRequest.TYPE, TimeRequest.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(TimeResponse.TYPE, TimeResponse.CODEC);

        PayloadTypeRegistry.clientboundPlay().register(OpenPanel.TYPE, OpenPanel.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(AllowedDomains.TYPE, AllowedDomains.CODEC);
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
        b.writeUUID(state.owner()); b.writeUtf(ScreenState.boundedPlayerName(state.ownerName()), ScreenState.MAX_PLAYER_NAME_LENGTH); b.writeUtf(state.accessMode().name(), 32);
        b.writeVarInt(state.allowedPlayers().size());
        state.allowedPlayers().forEach((id, name) -> { b.writeUUID(id); b.writeUtf(ScreenState.boundedPlayerName(name), ScreenState.MAX_PLAYER_NAME_LENGTH); });
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
