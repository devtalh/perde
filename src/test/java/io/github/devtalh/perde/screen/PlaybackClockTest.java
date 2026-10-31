package io.github.devtalh.perde.screen;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class PlaybackClockTest {
    @Test void playPauseAndRepeatedActionsUseTheCurrentPosition() {
        var start = new PlaybackClock(false, 10_000, 1000);
        assertEquals(10_000, start.current(9000));
        var play = start.play(2000);
        assertEquals(new PlaybackClock(true, 10_000, 2000), play);
        assertEquals(new PlaybackClock(true, 11_000, 3000), play.play(3000));
        var pause = play.pause(4000);
        assertEquals(new PlaybackClock(false, 12_000, 4000), pause);
        assertEquals(12_000, pause.current(10_000));
        assertEquals(new PlaybackClock(false, 12_000, 5000), pause.pause(5000));
        assertEquals(13_000, pause.play(6000).current(7000));
    }
    @Test void seekIsBasedOnLivePositionAndPreservesPlaying() {
        var playing = new PlaybackClock(true, 20_000, 1000);
        assertEquals(new PlaybackClock(true, 31_000, 2000), playing.seekRelative(10_000, 2000));
        assertEquals(new PlaybackClock(true, 0, 2000), playing.seekRelative(-30_000, 2000));
        assertEquals(new PlaybackClock(true, PlaybackClock.MAX_POSITION_MS, 2000), playing.seekRelative(PlaybackClock.MAX_POSITION_MS, 2000));
        assertEquals(new PlaybackClock(true, 55_000, 2000), playing.seekAbsolute(55_000, 2000));
        assertFalse(playing.pause(1500).seekAbsolute(0, 2000).playing());
    }
    @Test void clampsSavedDataElapsedTimeAndOverflow() {
        assertEquals(0, new PlaybackClock(false, -1, 0).current(10));
        assertEquals(PlaybackClock.MAX_POSITION_MS, new PlaybackClock(true, Long.MAX_VALUE, Long.MIN_VALUE).current(Long.MAX_VALUE));
        assertEquals(0, new PlaybackClock(true, 0, Long.MAX_VALUE).current(Long.MIN_VALUE));
        assertEquals(0, new PlaybackClock(true, 10, 100).current(0));
        assertEquals(PlaybackClock.MAX_POSITION_MS, new PlaybackClock(true, 0, 0).current(Long.MAX_VALUE));
    }
    @ParameterizedTest @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, -86400001, 86400001})
    void rejectsInvalidSeeks(double input) {
        var clock = new PlaybackClock(true, 123, 500);
        assertThrows(IllegalArgumentException.class, () -> clock.seekAbsolute(input, 1000));
        assertThrows(IllegalArgumentException.class, () -> clock.seekRelative(input, 1000));
    }
    @Test void absoluteRejectsNegativeButRelativeAcceptsIt() {
        assertThrows(IllegalArgumentException.class, () -> new PlaybackClock(false, 1, 0).seekAbsolute(-1, 1));
        assertEquals(0, new PlaybackClock(false, 1, 0).seekRelative(-1, 1).positionMs());
    }
    @Test void saveReloadAndRestartHaveDifferentElapsedTimeRules() {
        var clock = new PlaybackClock(true, 5000, 2000);
        var saved = clock.checkpoint(4000);
        assertEquals(new PlaybackClock(true, 7000, 4000), saved);
        assertSame(saved, saved.loaded(1000, 10_000));
        assertEquals(13_000, saved.loaded(1000, 10_000).current(10_000));
        var restarted = saved.loaded(20_000, 30_000);
        assertEquals(new PlaybackClock(true, 7000, 30_000), restarted);
        assertEquals(8000, restarted.current(31_000));
        assertSame(saved, saved.loaded(4000, 10_000));
        var paused = clock.pause(4000).checkpoint(5000).loaded(20_000, 30_000);
        assertEquals(7000, paused.current(31_000)); assertFalse(paused.playing());
    }
    @Test void urlChangesResetPlaybackButGeometryAndAccessDoNot() {
        var state = ScreenState.defaults().withUrl("https://youtube.com", 1000)
                .withPlayback(new PlaybackClock(false, 12345, 2000));
        assertSame(state, state.withUrl(" https://youtube.com ", 3000));
        var changed = state.withUrl("https://youtu.be/abc", 3000);
        assertEquals(new PlaybackClock(true, 0, 3000), changed.playback());
        assertEquals(new PlaybackClock(false, 0, 4000), changed.withUrl("", 4000).playback());
        assertEquals(state.playback(), state.withGeometry(8, 4.5, 1, 2).playback());
        assertEquals(state.playback(), state.withAccessMode(AccessMode.EVERYONE).playback());
        assertEquals(state.playback(), state.withOwner(new UUID(1, 1), "Owner").playback());
        assertEquals(state.playback(), state.withAllowedPlayer(new UUID(2, 2), "Other").withoutAllowedPlayer(new UUID(2, 2)).playback());
    }
}
