package io.github.devtalh.perde.client.browser;

import com.mojang.blaze3d.textures.GpuTextureView;
import io.github.devtalh.perde.Perde;
import io.github.devtalh.perde.block.PerdeBlock;
import io.github.devtalh.perde.block.PerdeBlockEntity;
import io.github.devtalh.perde.screen.ScreenGeometry;
import io.github.devtalh.perde.screen.ScreenState;
import io.github.devtalh.perde.client.net.ClientPlaybackClock;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientBlockEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

public final class PerdeBrowserManager {
    public static final PerdeBrowserManager INSTANCE = new PerdeBrowserManager();
    private final Set<PerdeBlockEntity> loaded = new LinkedHashSet<>();
    private BrowserBackend backend;
    private ClientLevel level;
    private PerdeBlockEntity active;
    private String currentUrl = "";
    private boolean open, failed, incompatible;
    private long generation;

    private PerdeBrowserManager() { }

    public void initialize(boolean mcefAvailable) {
        if (mcefAvailable) {
            try { backend = McefCompatibility.createBackend(); }
            catch (ReflectiveOperationException | LinkageError exception) { incompatible(exception); }
        }
        BrowserTexture.initialize();
        ClientBlockEntityEvents.BLOCK_ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof PerdeBlockEntity screen) loaded.add(screen);
        });
        ClientBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register((entity, world) -> {
            if (entity instanceof PerdeBlockEntity screen) {
                loaded.remove(screen);
                if (active == screen) close("block removed/unloaded");
            }
        });
        ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register((client, world) -> changeLevel(world));
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(() -> reset("disconnect")));
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> reset("client stopping"));
    }

    private void changeLevel(ClientLevel world) {
        close("level change");
        level = world;
        loaded.removeIf(entity -> entity.getLevel() != world);
    }

    private void reset(String reason) {
        close(reason);
        loaded.clear();
        level = null;
    }

    private void tick(Minecraft client) {
        if (client.level != level) changeLevel(client.level);
        if (level == null || client.player == null) {
            close("left world");
            return;
        }
        loaded.removeIf(entity -> entity.isRemoved() || entity.getLevel() != level);
        var camera = client.gameRenderer.mainCamera().position();
        var candidates = new ArrayList<BrowserSelection.Candidate<PerdeBlockEntity>>();
        for (PerdeBlockEntity entity : loaded) {
            if (!validUrl(entity).isEmpty()) candidates.add(new BrowserSelection.Candidate<>(entity,
                    BrowserSelection.distance(rectangle(entity), camera.x, camera.y, camera.z)));
        }
        PerdeBlockEntity selected = BrowserSelection.select(active, candidates);
        if (selected != active) {
            close(selected == null ? "out of range/empty URL/unloaded" : "nearer screen selected");
            active = selected;
        }
        if (active == null || backend == null) return;
        if (open) {
            try { backend.checkHealth(); }
            catch (RuntimeException | LinkageError exception) { fail(exception); return; }
        }
        String url = validUrl(active);
        if (!url.equals(currentUrl)) {
            currentUrl = url;
            failed = false;
            if (open) {
                try {
                    backend.loadUrl(url);
                    Perde.LOGGER.info("Perde browser: URL updated at {} on existing browser.", active.getBlockPos());
                } catch (RuntimeException | LinkageError exception) { fail(exception); }
            }
        }
        if (!open && !failed && !incompatible) {
            try {
                if (!backend.ready()) return;
                backend.open(url);
                open = true;
                generation++;
                Perde.LOGGER.info("Perde browser: created at {}, 1280x720, 30 fps, unfocused, active=1/{}.",
                        active.getBlockPos(), BrowserSelection.MAX_ACTIVE_BROWSERS);
            } catch (RuntimeException | LinkageError exception) { fail(exception); }
        }
        if (open) {
            try { backend.sync(active.screenState().playback(), ClientPlaybackClock.ready() ? ClientPlaybackClock.serverNowMs() : null); }
            catch (RuntimeException | LinkageError exception) { fail(exception); }
        }
    }

    private void fail(Throwable exception) {
        requireClientThread();
        safeClose();
        open = false;
        failed = true;
        generation++;
        if (exception instanceof LinkageError || exception instanceof McefCompatibility.Incompatible) incompatible(exception);
        Perde.LOGGER.error("Perde browser: creation/navigation failed; retry after URL or selection changes.", exception);
    }

    private void incompatible(Throwable exception) {
        incompatible = true;
        Perde.LOGGER.error("Perde MCEF version incompatible; expected {}.", McefCompatibility.EXPECTED_VERSION, exception);
    }

    private void safeClose() {
        try { if (backend != null) backend.close(); }
        catch (LinkageError | RuntimeException exception) {
            if (exception instanceof LinkageError || exception instanceof McefCompatibility.Incompatible) incompatible(exception);
            else Perde.LOGGER.error("Perde browser cleanup failed.", exception);
        }
    }

    public void policyChanged() {
        requireClientThread();
        try {
            if (active != null && (validUrl(active).isEmpty() || (open && !backend.currentPageAllowed()))) close("site allowlist updated");
        } catch (RuntimeException | LinkageError exception) { fail(exception); }
    }

    private void close(String reason) {
        requireClientThread();
        safeClose();
        if (open) Perde.LOGGER.info("Perde browser: closed at {} ({}), active=0/{}.",
                active.getBlockPos(), reason, BrowserSelection.MAX_ACTIVE_BROWSERS);
        open = false;
        failed = false;
        active = null;
        currentUrl = "";
        generation++;
    }

    private static void requireClientThread() {
        if (!Minecraft.getInstance().isSameThread()) throw new IllegalStateException("Browser cleanup must use the client thread");
    }

    private static String validUrl(PerdeBlockEntity entity) {
        try {
            String url = ScreenState.validateUrl(entity.screenState().url());
            return ClientSitePolicy.allowsPage(url) ? url : "";
        }
        catch (IllegalArgumentException exception) { return ""; }
    }

    public static ScreenGeometry.Rectangle rectangle(PerdeBlockEntity entity) {
        Direction facing = entity.getBlockState().getValue(PerdeBlock.FACING);
        var pos = entity.getBlockPos();
        return ScreenGeometry.rectangle(pos.getX(), pos.getY(), pos.getZ(), facing.getStepX(), facing.getStepZ(), entity.screenState());
    }

    public String statusKey(PerdeBlockEntity entity) {
        if (entity.screenState().url().isEmpty()) return "perde.ui.no_page";
        if (incompatible) return "perde.browser.incompatible";
        if (backend == null) return "perde.browser.missing";
        if (!ClientSitePolicy.received()) return "perde.browser.policy_waiting";
        if (validUrl(entity).isEmpty()) return "perde.browser.site_not_allowed";
        if (active != entity) return "perde.browser.inactive";
        return failed ? "perde.browser.failed" : "perde.browser.loading";
    }

    /** Immutable ticket prevents a stale extraction from drawing a newly selected screen's browser. */
    public long frameTicket(PerdeBlockEntity entity) {
        return entity == active && textureView() != null ? generation : -1;
    }

    public boolean isCurrentFrame(long ticket) { return ticket >= 0 && ticket == generation && textureView() != null; }
    public GpuTextureView textureView() {
        try { return open ? backend.textureView() : null; }
        catch (RuntimeException | LinkageError exception) { fail(exception); return null; }
    }
}
