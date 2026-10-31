package io.github.devtalh.perde.screen;

/** Pure authoritative media timeline, in server wall-clock milliseconds. */
public record PlaybackClock(boolean playing, long positionMs, long serverTimestamp) {
    public static final long MAX_POSITION_MS = 24 * 60 * 60 * 1000L;

    public PlaybackClock { positionMs = clamp(positionMs); }

    public long current(long now) {
        // Double arithmetic avoids overflow for corrupt/legacy timestamps, then clamps.
        return playing ? (long) Math.max(0, Math.min(MAX_POSITION_MS,
                positionMs + ((double) now - serverTimestamp))) : positionMs;
    }

    public PlaybackClock play(long now) { return new PlaybackClock(true, current(now), now); }
    public PlaybackClock pause(long now) { return new PlaybackClock(false, current(now), now); }
    public PlaybackClock seekAbsolute(double target, long now) {
        validate(target, 0, MAX_POSITION_MS);
        return new PlaybackClock(playing, Math.round(target), now);
    }
    public PlaybackClock seekRelative(double delta, long now) {
        validate(delta, -MAX_POSITION_MS, MAX_POSITION_MS);
        return new PlaybackClock(playing, clamp(Math.round(current(now) + delta)), now);
    }
    public PlaybackClock checkpoint(long now) { return new PlaybackClock(playing, current(now), now); }

    /** Same-session chunk reload keeps elapsed time; restart resumes the saved position. */
    public PlaybackClock loaded(long serverStartMs, long now) {
        return serverTimestamp < serverStartMs ? new PlaybackClock(playing, positionMs, now) : this;
    }

    public static long clamp(long value) { return Math.max(0, Math.min(MAX_POSITION_MS, value)); }
    private static void validate(double value, double min, double max) {
        if (!Double.isFinite(value) || value < min || value > max) throw new IllegalArgumentException("Invalid playback position");
    }
}
