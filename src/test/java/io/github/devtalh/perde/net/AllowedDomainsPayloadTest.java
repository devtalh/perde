package io.github.devtalh.perde.net;

import io.github.devtalh.perde.screen.SiteAllowlist;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class AllowedDomainsPayloadTest {
    @Test void fullPolicyRoundTripNormalizesAndIsImmutable() {
        var payload = new PerdePayloads.AllowedDomains(List.of("WWW.YOUTUBE.COM.", "https://example.com/path"));
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            PerdePayloads.AllowedDomains.CODEC.encode(buffer, payload);
            var decoded = PerdePayloads.AllowedDomains.CODEC.decode(buffer);
            assertEquals(payload, decoded);
            assertEquals(List.of("youtube.com", "example.com"), decoded.domains());
            assertThrows(UnsupportedOperationException.class, () -> decoded.domains().clear());
            assertEquals(0, buffer.readableBytes());
        } finally { buffer.release(); }
    }

    @Test void rejectsOversizedAndNegativeCountsBeforeAllocation() {
        for (int count : new int[]{-1, SiteAllowlist.MAX_DOMAINS + 1}) {
            var buffer = new FriendlyByteBuf(Unpooled.buffer());
            try {
                buffer.writeVarInt(count);
                assertThrows(DecoderException.class, () -> PerdePayloads.AllowedDomains.CODEC.decode(buffer));
            } finally { buffer.release(); }
        }
    }
}
