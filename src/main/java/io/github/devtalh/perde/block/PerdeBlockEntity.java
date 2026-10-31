package io.github.devtalh.perde.block;

import io.github.devtalh.perde.registry.PerdeRegistry;
import io.github.devtalh.perde.screen.AccessMode;
import io.github.devtalh.perde.screen.ScreenState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class PerdeBlockEntity extends BlockEntity {
    private ScreenState state = ScreenState.defaults();

    public PerdeBlockEntity(BlockPos pos, BlockState blockState) { super(PerdeRegistry.BLOCK_ENTITY, pos, blockState); }
    public ScreenState screenState() { return state; }

    public void setScreenState(ScreenState value) {
        if (level != null && level.isClientSide()) throw new IllegalStateException("Only the server may change screen state");
        state = value;
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putString("url", state.url());
        output.putBoolean("playing", state.playing());
        output.putLong("positionMs", state.positionMs());
        output.putLong("serverTimestamp", state.serverTimestamp());
        output.putDouble("volume", state.volume());
        output.putDouble("width", state.width());
        output.putDouble("height", state.height());
        output.putDouble("offsetX", state.offsetX());
        output.putDouble("offsetY", state.offsetY());
        output.putString("owner", state.owner().toString());
        output.putString("ownerName", state.ownerName());
        output.putString("accessMode", state.accessMode().name());
        ValueOutput.ValueOutputList players = output.childrenList("allowedPlayers");
        state.allowedPlayers().forEach((id, name) -> {
            ValueOutput player = players.addChild();
            player.putString("uuid", id.toString());
            player.putString("name", name);
        });
    }

    @Override protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        ScreenState defaults = ScreenState.defaults();
        String url = ScreenState.savedUrl(input.getStringOr("url", ""));
        AccessMode mode;
        try { mode = AccessMode.valueOf(input.getStringOr("accessMode", "OWNER_ONLY")); }
        catch (IllegalArgumentException exception) { mode = AccessMode.OWNER_ONLY; }
        Map<UUID, String> players = new LinkedHashMap<>();
        for (ValueInput player : input.childrenListOrEmpty("allowedPlayers")) {
            if (players.size() >= ScreenState.MAX_ALLOWED_PLAYERS) break;
            UUID id = uuid(player.getStringOr("uuid", ""));
            if (!id.equals(ScreenState.UNOWNED)) players.put(id, player.getStringOr("name", ""));
        }
        state = new ScreenState(url, input.getBooleanOr("playing", false), input.getLongOr("positionMs", 0),
                input.getLongOr("serverTimestamp", 0), finite(input, "volume", defaults.volume()),
                finite(input, "width", defaults.width()), finite(input, "height", defaults.height()),
                finite(input, "offsetX", 0), finite(input, "offsetY", 0),
                uuid(input.getStringOr("owner", "")), input.getStringOr("ownerName", ""), mode, players);
    }

    private static UUID uuid(String value) {
        try { return UUID.fromString(value); }
        catch (IllegalArgumentException exception) { return ScreenState.UNOWNED; }
    }

    private static double finite(ValueInput input, String key, double fallback) {
        double value = input.getDoubleOr(key, fallback);
        return Double.isFinite(value) ? value : fallback;
    }

    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveCustomOnly(registries); }
}
