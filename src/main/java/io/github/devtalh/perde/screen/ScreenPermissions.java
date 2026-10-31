package io.github.devtalh.perde.screen;

import java.util.UUID;

public final class ScreenPermissions {
    private ScreenPermissions() { }

    public static boolean canAdmin(ScreenState state, UUID player, int permissionLevel) {
        return state.owner().equals(player) || permissionLevel >= 2;
    }

    public static boolean canControl(ScreenState state, UUID player, int permissionLevel) {
        return canAdmin(state, player, permissionLevel) || state.accessMode() == AccessMode.EVERYONE
                || (state.accessMode() == AccessMode.SELECTED && state.allowedPlayers().containsKey(player));
    }
}
