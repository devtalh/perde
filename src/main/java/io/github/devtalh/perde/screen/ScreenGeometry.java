package io.github.devtalh.perde.screen;

/** World-space rectangle math, independent of the rendering and game APIs. */
public final class ScreenGeometry {
    public static final double EPSILON = 0.002;
    private ScreenGeometry() { }

    public record Rectangle(double x, double y, double z, int rightX, int rightZ, int frontX, int frontZ,
                            double width, double height) {
        public double minX() { return x - Math.abs(rightX) * width / 2; }
        public double maxX() { return x + Math.abs(rightX) * width / 2; }
        public double minZ() { return z - Math.abs(rightZ) * width / 2; }
        public double maxZ() { return z + Math.abs(rightZ) * width / 2; }
    }

    public static Rectangle rectangle(int blockX, int blockY, int blockZ, int frontX, int frontZ, ScreenState state) {
        if (Math.abs(frontX) + Math.abs(frontZ) != 1) throw new IllegalArgumentException("Horizontal unit facing required");
        int rightX = frontZ, rightZ = -frontX;
        return new Rectangle(blockX + 0.5 + frontX * (0.5 + EPSILON) + rightX * state.offsetX(),
                blockY + state.offsetY(), blockZ + 0.5 + frontZ * (0.5 + EPSILON) + rightZ * state.offsetX(),
                rightX, rightZ, frontX, frontZ, state.width(), state.height());
    }
}
