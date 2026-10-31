package io.github.devtalh.perde.net;

import io.github.devtalh.perde.screen.AccessMode;
import io.github.devtalh.perde.screen.ScreenState;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PayloadNamesTest {
    @Test void oversizedSavedNamesCanBeSentAndDecodedByTheRealPanelCodec() {
        UUID owner = new UUID(1, 2), allowed = new UUID(3, 4);
        var state = new ScreenState("https://example.com", false, 0, 0, 1, 4, 2.25, 0, 0,
                owner, "OwnerNameLongerThanSixteen", AccessMode.SELECTED, Map.of(allowed, "AllowedNameLongerThanSixteen"));
        var payload = new PerdePayloads.OpenPanel(new BlockPos(1, 2, 3), state, true, true);
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            assertDoesNotThrow(() -> PerdePayloads.OpenPanel.CODEC.encode(buffer, payload));
            var decoded = PerdePayloads.OpenPanel.CODEC.decode(buffer);
            assertEquals(payload, decoded);
            assertEquals(16, decoded.state().ownerName().length());
            assertEquals(16, decoded.state().allowedPlayers().get(allowed).length());
            assertEquals(0, buffer.readableBytes());
        } finally { buffer.release(); }
    }
}
