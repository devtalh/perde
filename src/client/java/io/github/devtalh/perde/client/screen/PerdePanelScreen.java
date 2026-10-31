package io.github.devtalh.perde.client.screen;

import io.github.devtalh.perde.net.PerdePayloads;
import io.github.devtalh.perde.screen.ScreenState;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class PerdePanelScreen extends Screen {
    private final PerdePanelModel model;
    private EditBox url;
    private GeometrySlider widthSlider, heightSlider, xSlider, ySlider;
    private Checkbox ratio;
    private boolean ratioLocked = true;
    private double screenWidth, screenHeight, offsetX, offsetY;
    private Component error = Component.empty();
    private int left, top;
    private boolean rebuilding;

    public PerdePanelScreen(PerdePayloads.OpenPanel payload) {
        super(Component.translatable("perde.ui.title"));
        model = new PerdePanelModel(payload);
    }

    @Override protected void init() {
        left = (width - 360) / 2; top = (height - 240) / 2;
        url = addRenderableWidget(new EditBox(font, left + 12, top + 44, 258, 20, Component.translatable("perde.ui.url")));
        url.setMaxLength(ScreenState.MAX_URL_LENGTH);
        url.setValue(model.state().url());
        url.setEditable(model.canControl); url.active = model.canControl;
        Button open = addRenderableWidget(Button.builder(Component.translatable("perde.ui.open"), button -> openUrl()).bounds(left + 276, top + 44, 72, 20).build());
        open.active = model.canControl;
        if (model.canAdmin) {
            addRenderableWidget(Button.builder(Component.literal("\u2699"), button -> minecraft.gui.setScreen(new PerdeAccessScreen(this, model)))
                    .bounds(left + 324, top + 7, 24, 20).tooltip(Tooltip.create(Component.translatable("perde.ui.access"))).build());
        }
        screenWidth = model.state().width(); screenHeight = model.state().height();
        offsetX = model.state().offsetX(); offsetY = model.state().offsetY();
        widthSlider = addRenderableWidget(new GeometrySlider(left + 12, top + 76, 165, "perde.ui.width", 1, 16, screenWidth, value -> {
            screenWidth = value;
            if (ratioLocked) { screenHeight = ScreenState.height16By9(value); heightSlider.sync(screenHeight); }
        }, this::sendGeometry));
        heightSlider = addRenderableWidget(new GeometrySlider(left + 183, top + 76, 165, "perde.ui.height", 0.5, 9, screenHeight,
                value -> screenHeight = value, this::sendGeometry));
        ratio = addRenderableWidget(Checkbox.builder(Component.translatable("perde.ui.ratio"), font).pos(left + 12, top + 103).selected(ratioLocked)
                .onValueChange((box, selected) -> {
                    ratioLocked = selected;
                    if (selected) { screenHeight = ScreenState.height16By9(screenWidth); heightSlider.sync(screenHeight); sendGeometry(); }
                    heightSlider.active = model.canControl && !ratioLocked;
                }).build());
        xSlider = addRenderableWidget(new GeometrySlider(left + 12, top + 130, 165, "perde.ui.offset_x", -16, 16, offsetX,
                value -> offsetX = value, this::sendGeometry));
        ySlider = addRenderableWidget(new GeometrySlider(left + 183, top + 130, 165, "perde.ui.offset_y", -16, 16, offsetY,
                value -> offsetY = value, this::sendGeometry));
        widthSlider.active = xSlider.active = ySlider.active = ratio.active = model.canControl;
        heightSlider.active = model.canControl && !ratioLocked;
        Button center = addRenderableWidget(Button.builder(Component.translatable("perde.ui.center"), button -> {
            if (model.canControl) ClientPlayNetworking.send(new PerdePayloads.CenterScreen(model.pos));
        }).bounds(left + 12, top + 160, 100, 20).build());
        center.active = model.canControl;
        Button placement = addRenderableWidget(Button.builder(Component.translatable("perde.ui.placement"), button -> {})
                .bounds(left + 118, top + 160, 230, 20).tooltip(Tooltip.create(Component.translatable("perde.ui.placement_later"))).build());
        placement.active = false;
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose()).bounds(left + 130, top + 212, 100, 20).build());
    }

    private void openUrl() {
        if (!model.canControl) return;
        try {
            String value = ScreenState.validateUrl(url.getValue());
            error = Component.empty();
            ClientPlayNetworking.send(new PerdePayloads.SetUrl(model.pos, value));
        } catch (IllegalArgumentException exception) { error = Component.translatable("perde.error.invalid_url"); }
    }

    private void sendGeometry() {
        if (model.canControl && model.valid && !rebuilding) ClientPlayNetworking.send(new PerdePayloads.SetGeometry(model.pos, screenWidth, screenHeight, offsetX, offsetY));
    }

    @Override public void tick() {
        ScreenState previous = model.state();
        boolean previousControl = model.canControl, previousAdmin = model.canAdmin;
        if (model.poll()) {
            rebuilding = true;
            try {
                if (previousControl != model.canControl || previousAdmin != model.canAdmin) {
                    rebuildWidgets();
                } else {
                    ScreenState state = model.state();
                    if (!previous.url().equals(state.url())) url.setValue(state.url());
                    screenWidth = state.width(); screenHeight = state.height();
                    offsetX = state.offsetX(); offsetY = state.offsetY();
                    widthSlider.sync(screenWidth); heightSlider.sync(screenHeight);
                    xSlider.sync(offsetX); ySlider.sync(offsetY);
                }
            } finally { rebuilding = false; }
        }
        if (!model.valid) minecraft.gui.setScreen(null);
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(left, top, left + 360, top + 240, 0xF018222B);
        graphics.centeredText(font, title, width / 2, top + 12, 0xFFFFFFFF);
        graphics.text(font, Component.translatable("perde.ui.url"), left + 12, top + 32, 0xFFCED7DD);
        if (!model.canControl) graphics.textWithWordWrap(font, Component.translatable("perde.ui.watch_only"), left + 12, top + 188, 336, 0xFFFFD391);
        else graphics.textWithWordWrap(font, error, left + 12, top + 187, 336, 0xFFFF9090);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }
}
