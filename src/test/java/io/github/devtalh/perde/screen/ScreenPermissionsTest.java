package io.github.devtalh.perde.screen;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class ScreenPermissionsTest {
    @ParameterizedTest @EnumSource(AccessMode.class)
    void permissionMatrix(AccessMode mode) {
        UUID owner = new UUID(1, 1), selected = new UUID(2, 2), stranger = new UUID(3, 3);
        ScreenState state = ScreenState.defaults().withOwner(owner, "Owner").withAccessMode(mode).withAllowedPlayer(selected, "Selected");
        assertTrue(ScreenPermissions.canControl(state, owner, 0));
        assertTrue(ScreenPermissions.canAdmin(state, owner, 0));
        assertTrue(ScreenPermissions.canControl(state, stranger, 2));
        assertTrue(ScreenPermissions.canAdmin(state, stranger, 2));
        assertFalse(ScreenPermissions.canAdmin(state, stranger, 1));
        assertFalse(ScreenPermissions.canAdmin(state, selected, 0));
        assertEquals(mode == AccessMode.EVERYONE, ScreenPermissions.canControl(state, stranger, 0));
        assertEquals(mode != AccessMode.OWNER_ONLY, ScreenPermissions.canControl(state, selected, 0));
        assertEquals(mode == AccessMode.EVERYONE, ScreenPermissions.canControl(state, stranger, 1));
    }
}
