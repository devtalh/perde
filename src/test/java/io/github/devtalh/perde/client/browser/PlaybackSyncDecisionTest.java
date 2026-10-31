package io.github.devtalh.perde.client.browser;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;
import static io.github.devtalh.perde.client.browser.PlaybackSyncDecision.*;

class PlaybackSyncDecisionTest {
    @ParameterizedTest
    @CsvSource({"true,true,PLAY", "true,false,NONE", "false,true,NONE", "false,false,PAUSE"})
    void transportTable(boolean playing, boolean paused, Transport expected) {
        assertEquals(new Action(null, expected), decide(10_000, playing, new Media(10_000, 100_000, paused, 4, false), 100, null, false));
    }

    @ParameterizedTest
    @CsvSource({"8499,10000", "8500,-1", "11500,-1", "11501,10000", "0,10000"})
    void strictDriftBoundaryAndLateJoin(double position, long seek) {
        assertEquals(seek < 0 ? null : seek,
                decide(10_000, true, new Media(position, 100_000, false, 4, false), 100, null, false).seekMs());
    }

    @ParameterizedTest
    @CsvSource({"1999,false", "2000,true", "2001,true"})
    void debounceDoesNotSuppressTransport(long elapsed, boolean seek) {
        var result = decide(60_000, true, new Media(0, 100_000, true, 4, false), elapsed + 50, 50L, false);
        assertEquals(seek ? 60_000L : null, result.seekMs());
        assertEquals(Transport.PLAY, result.transport());
    }

    @Test void liveAndUnknownDurationNeverSeek() {
        for (double duration : new double[]{Double.POSITIVE_INFINITY, Double.NaN, 100_000}) {
            var live = new Media(200_000, duration, false, 4, true);
            assertEquals(new Action(null, Transport.PAUSE), decide(0, false, live, 0, null, false));
            assertEquals(new Action(null, Transport.PLAY), decide(0, true, new Media(0, duration, true, 4, true), 0, null, false));
        }
        assertNull(decide(90_000, true, new Media(0, Double.NaN, false, 4, false), 0, null, false).seekMs());
    }

    @Test void endOfVideoClampsAndPausesRegardlessOfServerPlay() {
        assertEquals(new Action(99_950L, Transport.PAUSE), decide(150_000, true, new Media(50_000, 100_000, false, 4, false), 0, null, false));
        assertEquals(Action.NONE, decide(150_000, true, new Media(99_950, 100_000, true, 4, false), 0, null, false));
        assertEquals(new Action(0L, Transport.PAUSE), decide(100, true, new Media(5000, 20, false, 4, false), 0, null, false));
    }

    @Test void bufferingWaitsForMetadataAndPreservesDebounce() {
        assertEquals(new Action(null, Transport.PAUSE), decide(60_000, false, new Media(0, 100_000, false, 0, false), 0, null, false));
        assertEquals(60_000L, decide(60_000, false, new Media(0, 100_000, true, 1, false), 0, null, false).seekMs());
        assertNull(decide(60_000, false, new Media(0, 0, true, 0, false), 0, null, false).seekMs());
    }

    @Test void exemptionSuppressesAllActionsUntilCleared() {
        var media = new Media(0, 100_000, true, 4, false);
        assertEquals(Action.NONE, decide(60_000, true, media, 0, null, true));
        assertEquals(new Action(60_000L, Transport.PLAY), decide(60_000, true, media, 0, null, false));
    }

    @Test void invalidPositionDoesNothing() {
        for (double position : new double[]{-1, Double.NaN, Double.POSITIVE_INFINITY})
            assertEquals(Action.NONE, decide(60_000, true, new Media(position, 100_000, true, 4, false), 0, null, false));
    }
}
