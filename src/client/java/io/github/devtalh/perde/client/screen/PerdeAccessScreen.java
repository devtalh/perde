package io.github.devtalh.perde.client.screen;

import io.github.devtalh.perde.net.ControlDecisions;
import io.github.devtalh.perde.net.PerdePayloads;
import io.github.devtalh.perde.screen.AccessMode;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class PerdeAccessScreen extends Screen {
    private static final int PAGE_SIZE = 5;
    private final PerdePanelScreen parent;
    private final PerdePanelModel model;
    private int left, top, page;
    private String pendingName = "";
    private EditBox name;
    private Component error = Component.empty();
    private List<Map.Entry<UUID, String>> players = List.of();

    PerdeAccessScreen(PerdePanelScreen parent, PerdePanelModel model) {
        super(Component.translatable("perde.ui.access"));
        this.parent = parent; this.model = model;
    }

    @Override protected void init() {
        left = (width - 360) / 2; top = (height - 260) / 2;
        players = new ArrayList<>(model.state().allowedPlayers().entrySet());
        page = Math.min(page, Math.max(0, (players.size() - 1) / PAGE_SIZE));
        addRenderableWidget(Button.builder(modeLabel(model.state().accessMode()), button -> {
            if (!model.canAdmin) return;
            AccessMode[] modes = AccessMode.values();
            AccessMode next = modes[(model.state().accessMode().ordinal() + 1) % modes.length];
            ClientPlayNetworking.send(new PerdePayloads.SetAccessMode(model.pos, next.name()));
        }).bounds(left + 12, top + 44, 336, 20).build()).active = model.canAdmin;
        for (int row = 0; row < PAGE_SIZE && page * PAGE_SIZE + row < players.size(); row++) {
            var player = players.get(page * PAGE_SIZE + row);
            addRenderableWidget(Button.builder(Component.translatable("perde.ui.remove"), button -> {
                if (model.canAdmin) ClientPlayNetworking.send(new PerdePayloads.RemoveAllowedPlayer(model.pos, player.getKey()));
            }).bounds(left + 278, top + 84 + row * 18, 70, 16)
                    .tooltip(Tooltip.create(Component.literal(player.getKey().toString()))).build()).active = model.canAdmin;
        }
        addRenderableWidget(Button.builder(Component.translatable("perde.ui.previous"), button -> { rememberName(); page--; rebuildWidgets(); })
                .bounds(left + 12, top + 176, 76, 18).build()).active = page > 0;
        addRenderableWidget(Button.builder(Component.translatable("perde.ui.next"), button -> { rememberName(); page++; rebuildWidgets(); })
                .bounds(left + 272, top + 176, 76, 18).build()).active = (page + 1) * PAGE_SIZE < players.size();
        name = addRenderableWidget(new EditBox(font, left + 12, top + 202, 258, 20, Component.translatable("perde.ui.player_name")));
        name.setMaxLength(16); name.setValue(pendingName); name.active = model.canAdmin; name.setEditable(model.canAdmin);
        addRenderableWidget(Button.builder(Component.translatable("perde.ui.add"), button -> {
            if (!model.canAdmin) return;
            String value = name.getValue().trim();
            if (!ControlDecisions.validPlayerName(value)) { error = Component.translatable("perde.ui.invalid_name"); return; }
            error = Component.empty();
            ClientPlayNetworking.send(new PerdePayloads.AddAllowedPlayer(model.pos, value));
        }).bounds(left + 276, top + 202, 72, 20).build()).active = model.canAdmin;
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> onClose()).bounds(left + 130, top + 234, 100, 20).build());
    }

    private static Component modeLabel(AccessMode mode) {
        return Component.translatable("perde.ui.mode", Component.translatable("perde.access." + mode.name().toLowerCase(java.util.Locale.ROOT)));
    }
    private void rememberName() { if (name != null) pendingName = name.getValue(); }

    @Override public void tick() {
        if (model.poll()) { rememberName(); rebuildWidgets(); }
        if (!model.valid) minecraft.gui.setScreen(null);
        else if (!model.canAdmin) minecraft.gui.setScreen(parent);
    }
    @Override public void onClose() { minecraft.gui.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }

    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(left, top, left + 360, top + 260, 0xF018222B);
        graphics.centeredText(font, title, width / 2, top + 10, 0xFFFFFFFF);
        graphics.text(font, Component.translatable("perde.ui.owner", model.state().ownerName()), left + 12, top + 28, 0xFFCED7DD);
        graphics.text(font, Component.translatable("perde.ui.allowed", players.size(), 32), left + 12, top + 70, 0xFFCED7DD);
        for (int row = 0; row < PAGE_SIZE && page * PAGE_SIZE + row < players.size(); row++) {
            graphics.text(font, players.get(page * PAGE_SIZE + row).getValue(), left + 16, top + 88 + row * 18, 0xFFFFFFFF);
        }
        graphics.centeredText(font, Component.translatable("perde.ui.page", page + 1, Math.max(1, (players.size() + PAGE_SIZE - 1) / PAGE_SIZE)), width / 2, top + 181, 0xFFCED7DD);
        if (!error.getString().isEmpty()) graphics.textWithWordWrap(font, error, left + 12, top + 224, 336, 0xFFFF9090);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }
}
