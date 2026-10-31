package io.github.devtalh.perde.client.browser;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.Identifier;

/** Non-owning bridge following the proven spike. MCEF exclusively owns all browser GPU resources. */
public final class BrowserTexture extends AbstractTexture {
    public static final Identifier ID = Identifier.fromNamespaceAndPath("perde", "browser/active");

    static void initialize() {
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> client.getTextureManager().register(ID, new BrowserTexture()));
    }

    @Override public GpuTextureView getTextureView() {
        GpuTextureView view = PerdeBrowserManager.INSTANCE.textureView();
        return view != null ? view : Minecraft.getInstance().getTextureManager()
                .getTexture(MissingTextureAtlasSprite.getLocation()).getTextureView();
    }

    @Override public GpuTexture getTexture() { return getTextureView().texture(); }
    @Override public GpuSampler getSampler() { return RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR); }
    @Override public void close() { /* TextureManager must never close MCEF's resources. */ }
}
