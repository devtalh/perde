package io.github.devtalh.perde.client.browser;

import io.github.devtalh.perde.screen.ScreenGeometry;
import io.github.devtalh.perde.screen.ScreenState;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BrowserSelectionTest {
    private BrowserSelection.Candidate<String> screen(String id, double distance) {
        return new BrowserSelection.Candidate<>(id, distance);
    }

    @Test void opensOnlyNearestAtOrWithin48() {
        assertEquals(1, BrowserSelection.MAX_ACTIVE_BROWSERS);
        assertNull(BrowserSelection.select(null, List.of()));
        assertNull(BrowserSelection.select(null, List.of(screen("A", 48.001))));
        assertEquals("A", BrowserSelection.select(null, List.of(screen("A", 48))));
        assertEquals("B", BrowserSelection.select(null, List.of(screen("A", 20), screen("B", 10))));
    }

    @Test void retainsCurrentUntilAnotherIsAtLeastFourBlocksCloser() {
        assertEquals("A", BrowserSelection.select("A", List.of(screen("A", 20), screen("B", 16.001))));
        assertEquals("B", BrowserSelection.select("A", List.of(screen("A", 20), screen("B", 16))));
        assertEquals("B", BrowserSelection.select("B", List.of(screen("A", 16), screen("B", 16))));
        assertEquals("A", BrowserSelection.select(null, List.of(screen("A", 16), screen("B", 16))));
    }

    @Test void retainsTo52ButNeverCreatesBeyond48() {
        assertEquals("A", BrowserSelection.select("A", List.of(screen("A", 52))));
        assertEquals("A", BrowserSelection.select("A", List.of(screen("A", 52), screen("B", 48.001))));
        assertEquals("B", BrowserSelection.select("A", List.of(screen("A", 52), screen("B", 48))));
        assertNull(BrowserSelection.select("A", List.of(screen("A", 52.001))));
        assertNull(BrowserSelection.select("A", List.of(screen("B", 49))));
    }

    @Test void removedUnloadedOrEmptyUrlCandidateCannotRetainActive() {
        assertNull(BrowserSelection.select("A", List.of()));
        assertEquals("B", BrowserSelection.select("A", List.of(screen("B", 47))));
    }

    @Test void invalidDistancesAreIgnored() {
        assertNull(BrowserSelection.select("A", List.of(screen("A", Double.NaN), screen("B", -1),
                screen("C", Double.POSITIVE_INFINITY))));
    }

    @Test void measuresRectangleIncludingOffsetsAndVerticalEdgesInAllFacings() {
        for (int[] facing : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
            var rect = ScreenGeometry.rectangle(0, 0, 0, facing[0], facing[1],
                    ScreenState.defaults().withGeometry(16, 9, 16, 16));
            assertEquals(0, BrowserSelection.distance(rect, rect.x(), rect.y() + 4, rect.z()), 1e-9);
            assertEquals(48, BrowserSelection.distance(rect, rect.x() + rect.frontX() * 48,
                    rect.y() + 4, rect.z() + rect.frontZ() * 48), 1e-9);
            assertEquals(5, BrowserSelection.distance(rect, rect.x() + rect.rightX() * 11,
                    rect.y() + 13, rect.z() + rect.rightZ() * 11), 1e-9);
            assertEquals(4, BrowserSelection.distance(rect, rect.x(), rect.y() - 4, rect.z()), 1e-9);
        }
    }
}
