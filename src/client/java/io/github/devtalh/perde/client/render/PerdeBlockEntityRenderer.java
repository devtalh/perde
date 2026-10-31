package io.github.devtalh.perde.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.devtalh.perde.block.PerdeBlockEntity;
import io.github.devtalh.perde.client.browser.BrowserTexture;
import io.github.devtalh.perde.client.browser.PerdeBrowserManager;
import io.github.devtalh.perde.screen.ScreenGeometry;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class PerdeBlockEntityRenderer implements BlockEntityRenderer<PerdeBlockEntity, PerdeBlockEntityRenderer.State> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("perde", "textures/block/screen_placeholder.png");
    private final Font font;

    public PerdeBlockEntityRenderer(BlockEntityRendererProvider.Context context) { font = context.font(); }
    public static final class State extends BlockEntityRenderState {
        ScreenGeometry.Rectangle rectangle;
        List<FormattedCharSequence> lines = List.of();
        float textScale;
        long browserTicket = -1;
    }

    @Override public State createRenderState() { return new State(); }

    private static ScreenGeometry.Rectangle rectangle(PerdeBlockEntity entity) {
        return PerdeBrowserManager.rectangle(entity);
    }

    /** 26.2 has no BE render-AABB override: the vanilla global renderer path bypasses source-block section culling. */
    @Override public boolean shouldRenderOffScreen() { return true; }
    @Override public int getViewDistance() { return 52; }
    @Override public boolean shouldRender(PerdeBlockEntity entity, Vec3 camera) {
        var rect = rectangle(entity);
        var pos = entity.getBlockPos();
        AABB bounds = new AABB(Math.min(pos.getX(), rect.minX() - ScreenGeometry.EPSILON), Math.min(pos.getY(), rect.y()),
                Math.min(pos.getZ(), rect.minZ() - ScreenGeometry.EPSILON), Math.max(pos.getX() + 1, rect.maxX() + ScreenGeometry.EPSILON),
                Math.max(pos.getY() + 1, rect.y() + rect.height()), Math.max(pos.getZ() + 1, rect.maxZ() + ScreenGeometry.EPSILON));
        return bounds.distanceToSqr(camera) <= getViewDistance() * getViewDistance();
    }

    @Override public void extractRenderState(PerdeBlockEntity entity, State state, float partialTick, Vec3 camera, ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(entity, state, partialTick, camera, breakProgress);
        state.rectangle = rectangle(entity);
        state.browserTicket = PerdeBrowserManager.INSTANCE.frameTicket(entity);
        // Fixed readable glyph scale; long URLs wrap and scale down to stay inside the rectangle.
        float scale = (float) Math.min(1.0 / 32, state.rectangle.height() / 24);
        Component text = Component.translatable(PerdeBrowserManager.INSTANCE.statusKey(entity));
        List<FormattedCharSequence> lines;
        do {
            lines = font.split(text, Math.max(1, (int) ((state.rectangle.width() - 0.125) / scale)));
            if (lines.size() * 10 * scale <= state.rectangle.height() - 0.125) break;
            scale *= 0.8F;
        } while (scale > 0.0001F);
        state.lines = List.copyOf(lines);
        state.textScale = scale;
    }

    @Override public void submit(State state, PoseStack poses, SubmitNodeCollector collector, CameraRenderState camera) {
        var rect = state.rectangle;
        poses.pushPose();
        poses.translate(rect.x() - state.blockPos.getX(), rect.y() - state.blockPos.getY(), rect.z() - state.blockPos.getZ());
        poses.mulPose(Axis.YP.rotation((float) Math.atan2(rect.frontX(), rect.frontZ())));
        float half = (float) rect.width() / 2, height = (float) rect.height();
        boolean live = PerdeBrowserManager.INSTANCE.isCurrentFrame(state.browserTicket);
        collector.submitCustomGeometry(poses, RenderTypes.text(live ? BrowserTexture.ID : TEXTURE), (pose, vertices) -> {
            vertices.addVertex(pose, -half, 0, 0).setColor(-1).setUv(0, 1).setLight(LightCoordsUtil.FULL_BRIGHT);
            vertices.addVertex(pose, half, 0, 0).setColor(-1).setUv(1, 1).setLight(LightCoordsUtil.FULL_BRIGHT);
            vertices.addVertex(pose, half, height, 0).setColor(-1).setUv(1, 0).setLight(LightCoordsUtil.FULL_BRIGHT);
            vertices.addVertex(pose, -half, height, 0).setColor(-1).setUv(0, 0).setLight(LightCoordsUtil.FULL_BRIGHT);
        });
        if (live) {
            poses.popPose();
            return;
        }
        poses.translate(0, height / 2, 0.003);
        poses.scale(state.textScale, -state.textScale, state.textScale);
        for (int i = 0; i < state.lines.size(); i++) {
            var line = state.lines.get(i);
            collector.order(1).submitText(poses, -font.width(line) / 2.0F, i * 10 - state.lines.size() * 5,
                    line, false, Font.DisplayMode.NORMAL, LightCoordsUtil.FULL_BRIGHT, 0xFFE1E8EB, 0, 0);
        }
        poses.popPose();
    }
}
