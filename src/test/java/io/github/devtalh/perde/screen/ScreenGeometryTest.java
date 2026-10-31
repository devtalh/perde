package io.github.devtalh.perde.screen;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ScreenGeometryTest {
    @Test void rectangleUsesFrontFaceBottomAndViewerRightForAllFacings() {
        ScreenState state = ScreenState.defaults().withGeometry(8, 4.5, 2, 3);
        for (int[] facing : new int[][]{{0, -1}, {1, 0}, {0, 1}, {-1, 0}}) {
            int fx = facing[0], fz = facing[1];
            var rectangle = ScreenGeometry.rectangle(10, 20, 30, fx, fz, state);
            assertEquals(10.5 + fx * 0.502 + fz * 2, rectangle.x(), 1e-9);
            assertEquals(30.5 + fz * 0.502 - fx * 2, rectangle.z(), 1e-9);
            assertEquals(23, rectangle.y());
            assertEquals(8, rectangle.width()); assertEquals(4.5, rectangle.height());
            assertEquals(fz, rectangle.rightX()); assertEquals(-fx, rectangle.rightZ());
            assertEquals(8 * Math.abs(fz), rectangle.maxX() - rectangle.minX(), 1e-9);
            assertEquals(8 * Math.abs(fx), rectangle.maxZ() - rectangle.minZ(), 1e-9);
        }
    }
    @Test void defaultsAreCenteredAtBlockBottom() {
        var rectangle = ScreenGeometry.rectangle(0, 0, 0, 0, -1, ScreenState.defaults());
        assertEquals(0.5, rectangle.x()); assertEquals(0, rectangle.y());
        assertEquals(-0.002, rectangle.z(), 1e-9);
        assertEquals(-1.5, rectangle.minX()); assertEquals(2.5, rectangle.maxX());
    }
}
