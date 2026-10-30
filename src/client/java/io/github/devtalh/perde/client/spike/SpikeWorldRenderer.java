package io.github.devtalh.perde.client.spike;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;

public final class SpikeWorldRenderer {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("perde", "spike/browser");
    private static final RenderStateDataKey<SpikeWorldBrowser.Placement> PLACEMENT =
            RenderStateDataKey.create(() -> "Perde spike placement");

    private SpikeWorldRenderer() {
    }

    public static void initialize() {
        // The client entrypoint runs before TextureManager and the GPU sampler cache exist.
        ClientLifecycleEvents.CLIENT_STARTED.register(client ->
                client.getTextureManager().register(TEXTURE, new BrowserTexture()));
        // Only immutable geometry crosses extraction/drawing, never the level or browser.
        LevelExtractionEvents.END_EXTRACTION.register(context ->
                context.levelState().setData(PLACEMENT, SpikeWorldBrowser.placement()));
        LevelRenderEvents.COLLECT_SUBMITS.register(SpikeWorldRenderer::submit);
    }

    private static void submit(LevelRenderContext context) {
        SpikeWorldBrowser.Placement placement = context.levelState().getData(PLACEMENT);
        if (placement == null || SpikeWorldBrowser.textureView() == null) {
            return;
        }
        Vec3 offset = placement.center().subtract(context.levelState().cameraRenderState.pos);
        // Right x up points towards the front normal. TEXT culls the reverse winding.
        float rightX = placement.facing().getStepZ() * 2.0F;
        float rightZ = -placement.facing().getStepX() * 2.0F;
        PoseStack poses = context.poseStack();
        poses.pushPose();
        poses.translate(offset.x, offset.y, offset.z);
        // Same dynamic-texture submission path as vanilla MapRenderer in 26.2.
        context.submitNodeCollector().submitCustomGeometry(poses, RenderTypes.text(TEXTURE), (pose, vertices) -> {
            vertices.addVertex(pose, -rightX, -1.125F, -rightZ).setColor(-1).setUv(0, 1).setLight(LightCoordsUtil.FULL_BRIGHT);
            vertices.addVertex(pose, rightX, -1.125F, rightZ).setColor(-1).setUv(1, 1).setLight(LightCoordsUtil.FULL_BRIGHT);
            vertices.addVertex(pose, rightX, 1.125F, rightZ).setColor(-1).setUv(1, 0).setLight(LightCoordsUtil.FULL_BRIGHT);
            vertices.addVertex(pose, -rightX, 1.125F, -rightZ).setColor(-1).setUv(0, 0).setLight(LightCoordsUtil.FULL_BRIGHT);
        });
        poses.popPose();
    }

    /** Non-owning bridge: MCEF alone creates, replaces and closes the GPU resources. */
    private static final class BrowserTexture extends AbstractTexture {
        @Override
        public GpuTextureView getTextureView() {
            // RenderSetup resolves this every frame, including after an MCEF texture replacement.
            GpuTextureView view = SpikeWorldBrowser.textureView();
            return view != null ? view : Minecraft.getInstance().getTextureManager()
                    .getTexture(MissingTextureAtlasSprite.getLocation()).getTextureView();
        }

        @Override
        public GpuTexture getTexture() {
            return getTextureView().texture();
        }

        @Override
        public GpuSampler getSampler() {
            return RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
        }

        @Override
        public void close() {
            // TextureManager must not close MCEF-owned views on reload or shutdown.
        }
    }
}
