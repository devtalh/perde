package io.github.devtalh.perde.client.browser;

/** Pure media decision. Times and debounce use monotonic milliseconds on the client. */
public final class PlaybackSyncDecision {
    public static final long DRIFT_MS = 1500, SEEK_COOLDOWN_MS = 2000;
    public enum Transport { NONE, PLAY, PAUSE }
    public record Media(double positionMs, double durationMs, boolean paused, int readyState, boolean live) { }
    public record Action(Long seekMs, Transport transport) {
        static final Action NONE = new Action(null, Transport.NONE);
    }

    private PlaybackSyncDecision() { }

    public static Action decide(long targetMs, boolean playing, Media media, long nowMs,
                                Long lastSeekMs, boolean exempt) {
        if (exempt || !Double.isFinite(media.positionMs()) || media.positionMs() < 0) return Action.NONE;
        boolean live = media.live() || !Double.isFinite(media.durationMs());
        boolean ended = !live && media.durationMs() > 0 && targetMs >= media.durationMs();
        long target = ended ? Math.max(0, (long) media.durationMs() - 50) : Math.max(0, targetMs);
        boolean shouldPlay = playing && !ended;
        Long seek = null;
        if (!live && media.readyState() >= 1 && media.durationMs() > 0
                && Math.abs(media.positionMs() - target) > DRIFT_MS
                && (lastSeekMs == null || nowMs - lastSeekMs >= SEEK_COOLDOWN_MS)) seek = target;
        Transport transport = shouldPlay == media.paused() ? (shouldPlay ? Transport.PLAY : Transport.PAUSE) : Transport.NONE;
        return new Action(seek, transport);
    }
}
