package io.github.devtalh.perde.client.spike;

import com.mojang.blaze3d.textures.GpuTextureView;
import io.github.devtalh.perde.Perde;
import net.dimaskama.mcef.api.MCEFApi;
import net.dimaskama.mcef.api.MCEFBrowser;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

import java.util.concurrent.CompletionException;

public final class SpikeWorldBrowser {
    private static MCEFBrowser browser;
    private static ClientLevel level;
    private static Placement placement;
    private static String pendingUrl;

    private SpikeWorldBrowser() {
    }

    public static void initialize() {
        ClientTickEvents.END_CLIENT_TICK.register(SpikeWorldBrowser::tick);
        // Fabric can invoke disconnect from a Netty event loop; GPU cleanup belongs to the client thread.
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
                client.execute(() -> close("disconnect")));
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> close("client stopping"));
    }

    public static int place(FabricClientCommandSource source, String url) {
        if (source.getClient().level == null || source.getClient().player == null) {
            source.sendError(Component.literal("Join a world before placing a spike screen."));
            return 0;
        }
        close("replaced");
        Direction forward = source.getPlayer().getDirection();
        Vec3 center = source.getPlayer().getEyePosition().add(
                forward.getStepX() * 3.0, 0.0, forward.getStepZ() * 3.0);
        placement = new Placement(center, forward.getOpposite());
        level = source.getClient().level;
        pendingUrl = url;
        createBrowserIfReady();
        if (placement == null) {
            source.sendError(Component.literal("Cannot create the spike browser. See the log."));
            return 0;
        }
        source.sendFeedback(Component.literal(browser == null
                ? "Spike screen placed. Waiting for MCEF initialization."
                : "Spike screen placed (4 x 2.25 blocks, 1280 x 720)."));
        Perde.LOGGER.info("Perde spike: world screen placed at {}, facing {}.", center, forward.getOpposite());
        return 1;
    }

    public static int remove(FabricClientCommandSource source) {
        close("removed");
        source.sendFeedback(Component.literal("Spike screen removed."));
        return 1;
    }

    public static int play(FabricClientCommandSource source) {
        return execute(source, "play", "const result = video.play(); if (result) result.catch(() => {});");
    }

    public static int pause(FabricClientCommandSource source) {
        return execute(source, "pause", "video.pause();");
    }

    public static int seek(FabricClientCommandSource source, double seconds) {
        if (!Double.isFinite(seconds) || seconds < 0.0) {
            source.sendError(Component.literal("Seek seconds must be finite and nonnegative."));
            return 0;
        }
        String target = Double.toString(seconds);
        return execute(source, "seek " + target, "video.currentTime = Number.isFinite(video.duration)"
                + " ? Math.min(" + target + ", video.duration) : " + target + ";");
    }

    private static int execute(FabricClientCommandSource source, String action, String body) {
        if (browser == null || level != source.getClient().level) {
            source.sendError(Component.literal("No ready spike browser. Use /perde-spike place first."));
            return 0;
        }
        // A page may have no video, or the media may not yet be seekable/playable.
        String script = "(() => { try { const video = document.querySelector('video');"
                + " if (!video) return; " + body + " } catch (error) {} })();";
        browser.getCefBrowser().executeJavaScript(script, browser.getCefBrowser().getURL(), 0);
        source.sendFeedback(Component.literal("Spike video JavaScript requested: " + action));
        Perde.LOGGER.info("Perde spike: world browser JavaScript requested: {}.", action);
        return 1;
    }

    private static void tick(Minecraft client) {
        if (browser == null && pendingUrl == null && placement == null) {
            return;
        }
        if (client.level != level || client.player == null) {
            close("left world");
            return;
        }
        createBrowserIfReady();
    }

    private static void createBrowserIfReady() {
        if (pendingUrl == null || !SpikeBrowserClient.initialization().getFuture().isDone()) {
            return;
        }
        try {
            // Poll only; browser creation and resize always happen on the client thread.
            MCEFApi api = SpikeBrowserClient.initialization().getFuture().getNow(null);
            browser = api.createBrowser(pendingUrl, false);
            browser.resize(1280, 720);
            browser.getCefBrowser().setWindowlessFrameRate(30);
            browser.setFocus(false);
            pendingUrl = null;
            Perde.LOGGER.info("Perde spike: world browser created at 1280x720, requested 30 fps.");
        } catch (RuntimeException exception) {
            close("creation failed");
            Throwable cause = exception instanceof CompletionException ? exception.getCause() : exception;
            Perde.LOGGER.error("Perde spike: cannot create world browser.", cause);
        }
    }

    private static void close(String reason) {
        placement = null;
        level = null;
        pendingUrl = null;
        if (browser != null) {
            browser.close();
            browser = null;
            Perde.LOGGER.info("Perde spike: world browser closed ({}).", reason);
        }
    }

    public static Placement placement() {
        return placement;
    }

    public static GpuTextureView textureView() {
        // The first frame may be absent, and MCEF can replace its texture view.
        return browser == null ? null : browser.getTextureView();
    }

    public record Placement(Vec3 center, Direction facing) {
    }
}
