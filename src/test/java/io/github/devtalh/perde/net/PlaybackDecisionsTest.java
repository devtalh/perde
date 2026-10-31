package io.github.devtalh.perde.net;

import io.github.devtalh.perde.screen.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.UUID;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

class PlaybackDecisionsTest {
    private static final UUID OWNER = new UUID(1, 1), VIEWER = new UUID(2, 2);
    private final ScreenState state = ScreenState.defaults().withOwner(OWNER, "Owner")
            .withUrl("https://youtube.com", 1000).withPlayback(new PlaybackClock(true, 20_000, 1000));
    private static Stream<ControlDecisions.Change> changes() {
        return Stream.of(new ControlDecisions.Play(), new ControlDecisions.Pause(),
                new ControlDecisions.SeekRelative(-10_000), new ControlDecisions.SeekAbsolute(45_000));
    }
    private ControlDecisions.Context gate(UUID player, int op, boolean level, boolean loaded, boolean exists, double distance) {
        return new ControlDecisions.Context(player, op, level, loaded, exists, distance);
    }
    private ControlDecisions.Result decide(ControlDecisions.Context gate, ScreenState s, ControlDecisions.Change change) {
        return ControlDecisions.decide(gate, s, change, new PacketRateLimiter(), 100, SiteAllowlist.defaults(), 2000);
    }
    @ParameterizedTest @MethodSource("changes")
    void everyPlaybackActionUsesEveryServerGate(ControlDecisions.Change change) {
        var contexts = new ControlDecisions.Context[]{gate(OWNER, 0, false, true, true, 0), gate(OWNER, 0, true, false, true, 0),
                gate(OWNER, 0, true, true, false, 0), gate(OWNER, 0, true, true, true, 64.01),
                gate(OWNER, 0, true, true, true, Double.NaN), gate(VIEWER, 0, true, true, true, 0)};
        var reasons = new ControlDecisions.Rejection[]{ControlDecisions.Rejection.WRONG_LEVEL, ControlDecisions.Rejection.UNLOADED,
                ControlDecisions.Rejection.MISSING, ControlDecisions.Rejection.TOO_FAR, ControlDecisions.Rejection.TOO_FAR, ControlDecisions.Rejection.FORBIDDEN};
        for (int i = 0; i < contexts.length; i++) {
            var result = decide(contexts[i], state, change);
            assertEquals(reasons[i], result.rejection()); assertSame(state, result.state()); assertFalse(result.accepted());
        }
        assertEquals(ControlDecisions.Rejection.MISSING, decide(gate(OWNER, 0, true, true, true, 0), null, change).rejection());
        assertTrue(decide(gate(OWNER, 0, true, true, true, 64), state, change).accepted());
        assertTrue(decide(gate(VIEWER, 2, true, true, true, 0), state, change).accepted());
        assertTrue(decide(gate(VIEWER, 0, true, true, true, 0), state.withAccessMode(AccessMode.EVERYONE), change).accepted());
        assertTrue(decide(gate(VIEWER, 0, true, true, true, 0), state.withAccessMode(AccessMode.SELECTED).withAllowedPlayer(VIEWER, "Viewer"), change).accepted());
        assertEquals(ControlDecisions.Rejection.FORBIDDEN, decide(gate(VIEWER, 0, true, true, true, 0), state.withAccessMode(AccessMode.SELECTED), change).rejection());
    }
    @Test void acceptedActionsUseWallClockAndShareTheExistingControlLimit() {
        var gate = gate(OWNER, 0, true, true, true, 0);
        assertEquals(new PlaybackClock(true, 21_000, 2000), decide(gate, state, new ControlDecisions.Play()).state().playback());
        assertEquals(new PlaybackClock(false, 21_000, 2000), decide(gate, state, new ControlDecisions.Pause()).state().playback());
        assertEquals(new PlaybackClock(true, 11_000, 2000), decide(gate, state, new ControlDecisions.SeekRelative(-10_000)).state().playback());
        assertEquals(new PlaybackClock(true, 45_000, 2000), decide(gate, state, new ControlDecisions.SeekAbsolute(45_000)).state().playback());
        var limiter = new PacketRateLimiter();
        for (int i = 0; i < 10; i++) assertTrue(ControlDecisions.decide(gate, state, new ControlDecisions.Center(), limiter, 1000, SiteAllowlist.defaults(), 2000).accepted());
        changes().forEach(change -> {
            var result = ControlDecisions.decide(gate, state, change, limiter, 1000, SiteAllowlist.defaults(), 2000);
            assertTrue(result.rateLimited()); assertSame(state, result.state());
        });
    }
    @Test void forgedSeekValuesAreRejectedWithoutChangingStateOrConsumingLimit() {
        var gate = gate(OWNER, 0, true, true, true, 0);
        var limiter = new PacketRateLimiter();
        for (double input : new double[]{Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, -86400001, 86400001}) {
            for (var change : new ControlDecisions.Change[]{new ControlDecisions.SeekAbsolute(input), new ControlDecisions.SeekRelative(input)}) {
                var result = ControlDecisions.decide(gate, state, change, limiter, 1000, SiteAllowlist.defaults(), 2000);
                assertEquals(ControlDecisions.Rejection.INVALID_VALUE, result.rejection()); assertSame(state, result.state());
            }
        }
        assertEquals(ControlDecisions.Rejection.INVALID_VALUE, decide(gate, state, new ControlDecisions.SeekAbsolute(-1)).rejection());
        for (int i = 0; i < 10; i++) assertTrue(ControlDecisions.decide(gate, state, new ControlDecisions.Play(), limiter, 1000, SiteAllowlist.defaults(), 2000).accepted());
    }
    @Test void urlPipelineResetsOnlyAfterValidationAndOnlyWhenChanged() {
        var gate = gate(OWNER, 0, true, true, true, 0);
        assertEquals(new PlaybackClock(true, 0, 2000), decide(gate, state, new ControlDecisions.SetUrl("https://youtu.be/abc")).state().playback());
        assertEquals(new PlaybackClock(false, 0, 2000), decide(gate, state, new ControlDecisions.SetUrl("")).state().playback());
        assertEquals(state.playback(), decide(gate, state, new ControlDecisions.SetUrl("https://youtube.com")).state().playback());
        for (String url : new String[]{"https://example.com", "file:///secret"}) {
            var result = decide(gate, state, new ControlDecisions.SetUrl(url)); assertFalse(result.accepted()); assertSame(state, result.state());
        }
    }
}
