package io.github.devtalh.perde.screen;

import java.util.Locale;

/** Panel time syntax, independent of Minecraft and the current locale. */
public final class PlaybackTime {
    private PlaybackTime() { }

    public static long parse(String input) {
        if (input == null || !input.trim().matches("[0-9]{1,4}:[0-9]{2}(:[0-9]{2})?")) {
            throw new IllegalArgumentException("Use mm:ss or h:mm:ss");
        }
        String[] parts = input.trim().split(":");
        long first = Long.parseLong(parts[0]), middle = Long.parseLong(parts[1]);
        long seconds;
        if (parts.length == 2) {
            if (middle >= 60) throw new IllegalArgumentException("Invalid seconds");
            seconds = first * 60 + middle;
        } else {
            long last = Long.parseLong(parts[2]);
            if (middle >= 60 || last >= 60) throw new IllegalArgumentException("Invalid minutes/seconds");
            seconds = first * 3600 + middle * 60 + last;
        }
        if (seconds * 1000 > PlaybackClock.MAX_POSITION_MS) throw new IllegalArgumentException("Position exceeds 24 hours");
        return seconds * 1000;
    }

    public static String format(long positionMs) {
        long seconds = PlaybackClock.clamp(positionMs) / 1000;
        return seconds < 3600 ? String.format(Locale.ROOT, "%02d:%02d", seconds / 60, seconds % 60)
                : String.format(Locale.ROOT, "%d:%02d:%02d", seconds / 3600, seconds / 60 % 60, seconds % 60);
    }
}
