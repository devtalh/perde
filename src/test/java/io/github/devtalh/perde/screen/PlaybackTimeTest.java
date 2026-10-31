package io.github.devtalh.perde.screen;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class PlaybackTimeTest {
    @Test void parsesBothFormsAndFormatsBoundaries() {
        assertEquals(0, PlaybackTime.parse("00:00"));
        assertEquals(90_000, PlaybackTime.parse(" 1:30 "));
        assertEquals(3_599_000, PlaybackTime.parse("59:59"));
        assertEquals(3_723_000, PlaybackTime.parse("1:02:03"));
        assertEquals(PlaybackClock.MAX_POSITION_MS, PlaybackTime.parse("24:00:00"));
        assertEquals(PlaybackClock.MAX_POSITION_MS, PlaybackTime.parse("1440:00"));
        assertEquals("00:00", PlaybackTime.format(-1000));
        assertEquals("00:01", PlaybackTime.format(1999));
        assertEquals("59:59", PlaybackTime.format(3_599_999));
        assertEquals("1:00:00", PlaybackTime.format(3_600_000));
        assertEquals("24:00:00", PlaybackTime.format(Long.MAX_VALUE));
        assertThrows(IllegalArgumentException.class, () -> PlaybackTime.parse(null));
    }
    @ParameterizedTest @ValueSource(strings = {"", "30", "1:2", "1:60", "1:60:00", "1:00:60", "-1:00", "1.5:00", "NaN:00", "Infinity", "25:00:00", "24:00:01", "1440:01", "999999999:00", "1:00:00:00", "1: 00", "+1:00", "１:00"})
    void rejectsInvalidOrOutOfRangeInput(String input) {
        assertThrows(IllegalArgumentException.class, () -> PlaybackTime.parse(input));
    }
}
