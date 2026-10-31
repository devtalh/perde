package io.github.devtalh.perde.net;

import io.github.devtalh.perde.screen.AccessMode;
import io.github.devtalh.perde.screen.ScreenState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class ControlDecisionsTest {
    private static final UUID OWNER = new UUID(1, 1), OTHER = new UUID(2, 2);
    private final ScreenState state = ScreenState.defaults().withOwner(OWNER, "Owner");
    private final PacketRateLimiter limiter = new PacketRateLimiter();
    private ControlDecisions.Context context(UUID player, int level, double distance) {
        return new ControlDecisions.Context(player, level, true, true, true, distance);
    }
    private ControlDecisions.Result decide(ControlDecisions.Context ctx, ScreenState current, ControlDecisions.Change change) {
        return ControlDecisions.decide(ctx, current, change, limiter, 1000);
    }

    @Test void forgedStrangerCannotChangeAnything() {
        var result = decide(context(OTHER, 0, 1), state, new ControlDecisions.SetUrl("https://example.com"));
        assertEquals(ControlDecisions.Rejection.FORBIDDEN, result.rejection());
        assertSame(state, result.state());
    }

    @Test void checksWorldChunkEntityAndDistance() {
        assertEquals(ControlDecisions.Rejection.WRONG_LEVEL, ControlDecisions.authorize(new ControlDecisions.Context(OWNER, 4, false, true, true, 0), state, false));
        assertEquals(ControlDecisions.Rejection.UNLOADED, ControlDecisions.authorize(new ControlDecisions.Context(OWNER, 4, true, false, true, 0), state, false));
        assertEquals(ControlDecisions.Rejection.MISSING, ControlDecisions.authorize(new ControlDecisions.Context(OWNER, 4, true, true, false, 0), null, false));
        assertEquals(ControlDecisions.Rejection.TOO_FAR, decide(context(OWNER, 4, 64.001), state, new ControlDecisions.Center()).rejection());
        assertEquals(ControlDecisions.Rejection.TOO_FAR, decide(context(OWNER, 4, Double.NaN), state, new ControlDecisions.Center()).rejection());
        assertTrue(decide(context(OWNER, 0, 64), state, new ControlDecisions.Center()).accepted());
    }

    @ParameterizedTest @ValueSource(strings = {"javascript:alert(1)", "file:///C:/", "data:text/html,x", "ftp://example.com", "https:///path",
            "http://192.168.1.1", "http://localhost:8080", "http://127.0.0.1", "http://127.1", "http://[::ffff:127.0.0.1]"})
    void unsafeUrlsDoNotChangeState(String url) {
        var result = decide(context(OWNER, 0, 0), state, new ControlDecisions.SetUrl(url));
        assertEquals(ControlDecisions.Rejection.INVALID_URL, result.rejection());
        assertSame(state, result.state());
    }

    @Test void geometryClampedAndMediaUntouched() {
        var result = decide(context(OWNER, 0, 0), state, new ControlDecisions.SetGeometry(30, -4, 0.04, -30));
        assertTrue(result.accepted());
        assertEquals(16, result.state().width()); assertEquals(0.5, result.state().height());
        assertEquals(0.0625, result.state().offsetX()); assertEquals(-16, result.state().offsetY());
        assertEquals(state.positionMs(), result.state().positionMs());
        assertEquals(state.playing(), result.state().playing());
        var centered = decide(context(OWNER, 0, 0), result.state(), new ControlDecisions.Center()).state();
        assertEquals(0, centered.offsetX()); assertEquals(0, centered.offsetY()); assertEquals(16, centered.width());
        assertEquals(ControlDecisions.Rejection.INVALID_VALUE, decide(context(OWNER, 0, 0), state, new ControlDecisions.SetGeometry(4, Double.POSITIVE_INFINITY, 0, 0)).rejection());
    }

    @Test void controllerCannotAdministerEvenInEveryoneMode() {
        for (AccessMode mode : new AccessMode[]{AccessMode.SELECTED, AccessMode.EVERYONE}) {
            ScreenState shared = state.withAccessMode(mode).withAllowedPlayer(OTHER, "Other");
            assertTrue(decide(context(OTHER, 0, 1), shared, new ControlDecisions.SetUrl("https://example.com")).accepted());
            for (var request : new ControlDecisions.Change[]{new ControlDecisions.SetAccess("OWNER_ONLY"),
                    new ControlDecisions.AddPlayer(new UUID(3, 3), "NewPlayer"), new ControlDecisions.RemovePlayer(OTHER)}) {
                var result = decide(context(OTHER, 0, 1), shared, request);
                assertEquals(ControlDecisions.Rejection.FORBIDDEN, result.rejection()); assertSame(shared, result.state());
            }
        }
    }

    @Test void operatorOverrideAndPlayerLimit() {
        assertTrue(decide(context(OTHER, 2, 0), state, new ControlDecisions.SetAccess("EVERYONE")).accepted());
        assertEquals(ControlDecisions.Rejection.INVALID_VALUE, decide(context(OWNER, 0, 0), state, new ControlDecisions.SetAccess("invalid")).rejection());
        assertEquals(ControlDecisions.Rejection.PLAYER_OFFLINE, decide(context(OWNER, 0, 0), state, new ControlDecisions.AddPlayer(null, "Offline")).rejection());
        assertEquals(ControlDecisions.Rejection.INVALID_NAME, decide(context(OWNER, 0, 0), state, new ControlDecisions.AddPlayer(OTHER, "bad name")).rejection());
        ScreenState full = state;
        for (int i = 0; i < 32; i++) full = full.withAllowedPlayer(new UUID(7, i), "Player" + i);
        assertEquals(ControlDecisions.Rejection.PLAYER_LIMIT, decide(context(OWNER, 0, 0), full, new ControlDecisions.AddPlayer(OTHER, "Other")).rejection());
        assertTrue(decide(context(OWNER, 0, 0), full, new ControlDecisions.RemovePlayer(new UUID(7, 0))).accepted());
    }

    @Test void rollingRateLimitIsPerPlayerAndRejectionsAreFree() {
        for (int i = 0; i < 20; i++) assertFalse(decide(context(OWNER, 0, 0), state, new ControlDecisions.SetUrl("file:///x")).accepted());
        for (int i = 0; i < 10; i++) assertTrue(ControlDecisions.decide(context(OWNER, 0, 0), state, new ControlDecisions.Center(), limiter, 1000 + i).accepted());
        var limited = ControlDecisions.decide(context(OWNER, 0, 0), state, new ControlDecisions.Center(), limiter, 1999);
        assertTrue(limited.rateLimited()); assertSame(state, limited.state()); assertNull(limited.rejection());
        assertTrue(ControlDecisions.decide(context(OTHER, 2, 0), state, new ControlDecisions.Center(), limiter, 1999).accepted());
        assertTrue(ControlDecisions.decide(context(OWNER, 0, 0), state, new ControlDecisions.Center(), limiter, 2000).accepted());
        assertTrue(ControlDecisions.decide(context(OWNER, 0, 0), state, new ControlDecisions.Center(), limiter, 2000).rateLimited());
        limiter.forget(OWNER);
        assertTrue(ControlDecisions.decide(context(OWNER, 0, 0), state, new ControlDecisions.Center(), limiter, 2000).accepted());
    }
}
