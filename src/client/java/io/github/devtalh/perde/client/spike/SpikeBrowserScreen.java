package io.github.devtalh.perde.client.spike;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import io.github.devtalh.perde.Perde;
import net.dimaskama.mcef.api.MCEFApi;
import net.dimaskama.mcef.api.MCEFBrowser;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;
import java.util.concurrent.CompletionException;

public final class SpikeBrowserScreen extends Screen {
    private final String url;
    private final MCEFApi.Initialization initialization = SpikeBrowserClient.initialization();
    private MCEFBrowser browser;
    private boolean failed;

    public SpikeBrowserScreen(String url) {
        super(Component.literal("Perde MCEF GUI spike"));
        this.url = url;
    }

    @Override
    protected void init() {
        if (browser != null) {
            browser.resize(width, height);
        } else {
            createBrowserIfReady();
        }
    }

    @Override
    public void tick() {
        createBrowserIfReady();
    }

    private void createBrowserIfReady() {
        if (browser != null || failed || !initialization.getFuture().isDone()) {
            return;
        }
        try {
            // getNow never waits. Creation and resize stay on the client thread, after initialization.
            MCEFApi api = initialization.getFuture().getNow(null);
            browser = api.createBrowser(url, false);
            browser.resize(width, height);
            browser.getCefBrowser().setWindowlessFrameRate(30);
            browser.setFocus(true);
            Perde.LOGGER.info("Perde spike: browser created at {}x{}, requested 30 fps.", width, height);
        } catch (RuntimeException exception) {
            failed = true;
            closeBrowser();
            Throwable cause = exception instanceof CompletionException ? exception.getCause() : exception;
            Perde.LOGGER.error("Perde spike: cannot create browser.", cause);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xFF101018);
        if (browser == null) {
            String progress = failed ? "MCEF failed. See the log; press Esc to close."
                    : "MCEF: " + initialization.getStage() + " - "
                    + (initialization.getPercentage() < 0 ? "percentage unavailable"
                    : String.format(Locale.ROOT, "%.1f%%", initialization.getPercentage()));
            graphics.centeredText(font, progress, width / 2, height / 2, 0xFFFFFFFF);
            return;
        }
        GpuTextureView textureView = browser.getTextureView();
        if (textureView != null) {
            graphics.blit(textureView, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR),
                    0, 0, width, height, 0.0F, 1.0F, 0.0F, 1.0F);
        }
        graphics.requestCursor(browser.getCursorType());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        closeBrowser();
    }

    private void closeBrowser() {
        if (browser != null) {
            browser.close();
            browser = null;
            Perde.LOGGER.info("Perde spike: browser closed.");
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        if (browser != null) browser.onMouseClicked(event, doubled);
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (browser != null) browser.onMouseReleased(event);
        return true;
    }

    @Override
    public boolean mouseScrolled(double x, double y, double horizontalAmount, double verticalAmount) {
        if (browser != null) browser.onMouseScrolled((int) x, (int) y, verticalAmount);
        return true;
    }

    @Override
    public void mouseMoved(double x, double y) {
        if (browser != null) browser.onMouseMoved((int) x, (int) y);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        if (browser != null) browser.onKeyPressed(event);
        return true;
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        if (browser != null) browser.onKeyReleased(event);
        return true;
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (browser != null) browser.onCharTyped(event);
        return true;
    }
}
