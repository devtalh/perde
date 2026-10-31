package io.github.devtalh.perde.client.browser;

import io.github.devtalh.perde.screen.ScreenGeometry;

import java.util.List;
import java.util.Objects;

/** Pure selection policy. Candidate identities must be stable within the current level. */
public final class BrowserSelection {
    public static final int MAX_ACTIVE_BROWSERS = 1;
    public static final double OPEN_DISTANCE = 48;
    public static final double CLOSE_DISTANCE = 52;
    public static final double SWITCH_ADVANTAGE = 4;

    private BrowserSelection() { }

    public record Candidate<T>(T id, double distance) { }

    public static <T> T select(T active, List<Candidate<T>> candidates) {
        Candidate<T> current = null, nearest = null;
        for (Candidate<T> candidate : candidates) {
            if (!Double.isFinite(candidate.distance()) || candidate.distance() < 0) continue;
            if (Objects.equals(active, candidate.id()) && candidate.distance() <= CLOSE_DISTANCE) current = candidate;
            // Strict comparison retains insertion order for equal distances.
            if (candidate.distance() <= OPEN_DISTANCE && (nearest == null || candidate.distance() < nearest.distance())) {
                nearest = candidate;
            }
        }
        if (current == null) return nearest == null ? null : nearest.id();
        return nearest != null && nearest.distance() <= current.distance() - SWITCH_ADVANTAGE ? nearest.id() : current.id();
    }

    public static double distance(ScreenGeometry.Rectangle rectangle, double x, double y, double z) {
        double dx = Math.max(Math.max(rectangle.minX() - x, 0), x - rectangle.maxX());
        double dy = Math.max(Math.max(rectangle.y() - y, 0), y - rectangle.y() - rectangle.height());
        double dz = Math.max(Math.max(rectangle.minZ() - z, 0), z - rectangle.maxZ());
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
