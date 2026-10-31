package io.github.devtalh.perde.net;

import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;

class ServerTimeEstimatorTest {
    private final AtomicLong now = new AtomicLong();
    private final ServerTimeEstimator clock = new ServerTimeEstimator(now::get);
    @Test void midpointUsesMonotonicElapsedTimeAndLowestRtt() {
        assertFalse(clock.ready());
        assertThrows(IllegalStateException.class, clock::serverNowMs);
        now.set(1_020_000_000);
        assertTrue(clock.sample(1_000_000_000, now.get(), 10_000));
        assertEquals(10_010, clock.serverNowMs()); assertEquals(20, clock.rttMs());
        assertEquals(8990, clock.offsetMs());
        clock.sample(1_100_000_000, 1_200_000_000, 99_000);
        now.set(1_220_000_000);
        assertEquals(10_210, clock.serverNowMs());
        clock.sample(1_220_000_000, 1_222_000_000, 10_210);
        now.set(1_222_000_000);
        assertEquals(10_211, clock.serverNowMs()); assertEquals(2, clock.rttMs());
        now.addAndGet(1_000_000_000);
        assertEquals(11_211, clock.serverNowMs());
    }
    @Test void oldestLowestRttExpiresAfterEightNewSamplesAndResetRemovesConnection() {
        clock.sample(0, 0, 1000);
        for (int i = 1; i < 8; i++) clock.sample(i * 1_000_000L, (i + 1) * 1_000_000L, 2000);
        assertEquals(0, clock.rttMs());
        clock.sample(8_000_000, 10_000_000, 3000);
        assertEquals(1, clock.rttMs());
        now.set(10_000_000);
        assertEquals(2009, clock.serverNowMs());
        clock.reset(); assertFalse(clock.ready());
        assertThrows(IllegalStateException.class, clock::serverNowMs);
    }
    @Test void invalidOrStaleSamplesDoNotReplaceBest() {
        clock.sample(0, 10_000_000, 1000);
        assertFalse(clock.sample(20, 10, 5000));
        assertFalse(clock.sample(0, 10_000_000_001L, 5000));
        assertFalse(clock.sample(0, 10, -1));
        assertEquals(10, clock.rttMs());
        now.set(20_000_000); assertEquals(1015, clock.serverNowMs());
    }
    @Test void nanoTimeMayBeNegativeAndWrapAround() {
        long sent = Long.MAX_VALUE - 1_000_000, received = Long.MIN_VALUE + 999_999;
        assertTrue(clock.sample(sent, received, 1000));
        now.set(received); assertEquals(1001, clock.serverNowMs());
        now.addAndGet(1_000_000); assertEquals(1002, clock.serverNowMs());
    }
    @Test void fivePerRollingSecondAreAcceptedIndependentlyPerPlayerAndControls() {
        var limiter = new PacketRateLimiter(5);
        var player = new java.util.UUID(1, 1);
        for (int i = 0; i < 5; i++) assertTrue(limiter.tryAccept(player, 1000 + i));
        assertFalse(limiter.tryAccept(player, 1999));
        assertTrue(limiter.tryAccept(new java.util.UUID(2, 2), 1999));
        assertTrue(limiter.tryAccept(player, 2000));
        assertFalse(limiter.tryAccept(player, 2000));
        limiter.forget(player); assertTrue(limiter.tryAccept(player, 2000));
        assertThrows(IllegalArgumentException.class, () -> new PacketRateLimiter(0));
    }
}
