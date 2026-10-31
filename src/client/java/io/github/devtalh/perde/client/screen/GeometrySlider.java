package io.github.devtalh.perde.client.screen;

import io.github.devtalh.perde.screen.ScreenState;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import java.math.BigDecimal;
import java.util.function.DoubleConsumer;

final class GeometrySlider extends AbstractSliderButton {
    private final String label;
    private final double min, max;
    private final DoubleConsumer changed;
    private final Runnable committed;
    private boolean dirty;

    GeometrySlider(int x, int y, int width, String label, double min, double max, double initial,
                   DoubleConsumer changed, Runnable committed) {
        super(x, y, width, 20, Component.empty(), (initial - min) / (max - min));
        this.label = label; this.min = min; this.max = max; this.changed = changed; this.committed = committed;
        updateMessage();
    }

    private double exact() { return ScreenState.roundToGrid(min + value * (max - min), min, max); }
    void sync(double exact) { value = (exact - min) / (max - min); updateMessage(); }
    @Override protected void updateMessage() {
        setMessage(Component.translatable(label, BigDecimal.valueOf(exact()).stripTrailingZeros().toPlainString()));
    }
    @Override protected void applyValue() { dirty = true; changed.accept(exact()); }
    private void commit() { if (dirty) { dirty = false; committed.run(); } }
    @Override public void onRelease(MouseButtonEvent event) { super.onRelease(event); commit(); }
    @Override public boolean keyPressed(KeyEvent event) {
        if (active && (event.isLeft() || event.isRight())) {
            sync(ScreenState.roundToGrid(exact() + (event.isLeft() ? -1 : 1) / 16.0, min, max));
            dirty = true; changed.accept(exact()); return true;
        }
        return super.keyPressed(event);
    }
    @Override public boolean keyReleased(KeyEvent event) {
        if (event.isLeft() || event.isRight()) commit();
        return super.keyReleased(event);
    }
    @Override public void setFocused(boolean focused) { if (!focused) commit(); super.setFocused(focused); }
}
