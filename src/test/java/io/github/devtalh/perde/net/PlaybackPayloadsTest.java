package io.github.devtalh.perde.net;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PlaybackPayloadsTest {
    private <T> void roundTrip(StreamCodec<FriendlyByteBuf, T> codec, T payload) {
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            codec.encode(buffer, payload); assertEquals(payload, codec.decode(buffer)); assertEquals(0, buffer.readableBytes());
        } finally { buffer.release(); }
    }
    @Test void allClockAndPlaybackPayloadsUseRealCodecs() {
        var pos = new BlockPos(-1, 120, 300);
        roundTrip(PerdePayloads.Play.CODEC, new PerdePayloads.Play(pos));
        roundTrip(PerdePayloads.Pause.CODEC, new PerdePayloads.Pause(pos));
        for (double value : new double[]{-30_000, 86_400_000, Double.NaN, Double.POSITIVE_INFINITY}) {
            roundTrip(PerdePayloads.SeekAbsolute.CODEC, new PerdePayloads.SeekAbsolute(pos, value));
            roundTrip(PerdePayloads.SeekRelative.CODEC, new PerdePayloads.SeekRelative(pos, value));
        }
        roundTrip(PerdePayloads.TimeRequest.CODEC, new PerdePayloads.TimeRequest(Long.MIN_VALUE));
        roundTrip(PerdePayloads.TimeResponse.CODEC, new PerdePayloads.TimeResponse(Long.MIN_VALUE, 1_793_000_000_000L));
    }
}
