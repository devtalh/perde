package io.github.devtalh.perde.client.screen;

import io.github.devtalh.perde.block.PerdeBlockEntity;
import io.github.devtalh.perde.net.PerdePayloads;
import io.github.devtalh.perde.screen.ScreenPermissions;
import io.github.devtalh.perde.screen.ScreenState;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/** Panel state starts only from a server OpenPanel payload; later snapshots come from vanilla BE sync. */
final class PerdePanelModel {
    final BlockPos pos;
    private final ResourceKey<Level> dimension;
    private final boolean operator;
    private ScreenState state;
    private ScreenState observed;
    boolean valid = true;
    boolean canControl;
    boolean canAdmin;

    PerdePanelModel(PerdePayloads.OpenPanel payload) {
        Minecraft client = Minecraft.getInstance();
        pos = payload.pos().immutable();
        dimension = client.level.dimension();
        state = payload.state();
        canControl = payload.canControl();
        canAdmin = payload.canAdmin();
        operator = canAdmin && !state.owner().equals(client.player.getUUID());
        observed = client.level.getBlockEntity(pos) instanceof PerdeBlockEntity entity ? entity.screenState() : null;
    }

    ScreenState state() { return state; }

    boolean poll() {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null || !client.level.dimension().equals(dimension)
                || !(client.level.getBlockEntity(pos) instanceof PerdeBlockEntity entity)) {
            valid = false;
            return false;
        }
        ScreenState next = entity.screenState();
        if (next.equals(observed)) return false;
        observed = next;
        state = next;
        canControl = ScreenPermissions.canControl(state, client.player.getUUID(), operator ? 2 : 0);
        canAdmin = ScreenPermissions.canAdmin(state, client.player.getUUID(), operator ? 2 : 0);
        return true;
    }
}
