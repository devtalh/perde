package io.github.devtalh.perde.client.test;

import io.github.devtalh.perde.block.PerdeBlockEntity;
import io.github.devtalh.perde.client.browser.PerdeBrowserManager;
import io.github.devtalh.perde.client.screen.PerdePanelScreen;
import io.github.devtalh.perde.net.PerdePayloads;
import io.github.devtalh.perde.registry.PerdeRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;

/** Opt-in, focus-free test orchestration. Never packaged in the production jar. */
public final class Phase2Probe implements ClientModInitializer {
    private static final Logger LOG = LoggerFactory.getLogger("perde-f2-probe");
    private final Path commandFile = FabricLoader.getInstance().getGameDir().resolve("phase2-command.txt");
    private String previous = "";
    private int walkTicks;
    private int ticks;
    private String lastAudit = "";

    @Override public void onInitializeClient() {
        try { if (Files.exists(commandFile)) previous = Files.readString(commandFile); }
        catch (Exception exception) { throw new IllegalStateException(exception); }
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    private void tick(Minecraft client) {
        try {
            if (++ticks % 20 == 0) {
                String audit = browserAudit();
                if (!audit.equals(lastAudit)) {
                    lastAudit = audit;
                    LOG.info("Native browser audit {}", audit);
                }
            }
            if (walkTicks > 0 && --walkTicks == 0) {
                client.options.keyUp.setDown(false);
                LOG.info("Walk finished at {}", client.player.position());
            }
            if (!Files.exists(commandFile)) return;
            String input = Files.readString(commandFile);
            if (input.equals(previous)) return;
            previous = input;
            String[] args = input.split("\t", -1);
            String action = args[1];
            if (action.equals("load")) {
                if (client.level != null) throw new IllegalStateException("Leave the current world first");
                client.createWorldOpenFlows().openWorld(args[2], () -> client.gui.setScreen(new TitleScreen()));
            } else if (action.equals("quit")) {
                client.stop();
            } else if (action.equals("f2")) {
                if (!client.handleGlobalKeyPress(client.options.keyScreenshot.getDefaultKey(), false)) {
                    throw new IllegalStateException("F2 is not bound");
                }
            } else {
                if (client.level == null || client.player == null || client.gameMode == null) throw new IllegalStateException("Join a world first");
                switch (action) {
                    case "policy" -> runPolicyProbe(args[2]);
                    case "evaluate" -> evaluate(args[2], args[3]);
                    case "devtools" -> devTools(args[2], args[3], args[4]);
                    case "raw-url" -> ClientPlayNetworking.send(new PerdePayloads.SetUrl(pos(args), args[5]));
                    case "command" -> client.player.connection.sendCommand(args[2]);
                    case "leave" -> client.disconnectFromWorld(Component.literal("F2-1 save and quit test"));
                    case "close" -> client.gui.setScreen(null);
                    case "aim" -> aim(client, new Vec3(number(args, 2), number(args, 3), number(args, 4)));
                    case "walk" -> {
                        client.gui.setScreen(null);
                        client.player.setYRot(Float.parseFloat(args[2]));
                        client.player.setXRot(0);
                        walkTicks = Integer.parseInt(args[3]);
                        client.options.keyUp.setDown(true);
                    }
                    case "open" -> client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND,
                            new BlockHitResult(Vec3.atCenterOf(pos(args)), Direction.NORTH, pos(args), false));
                    case "panel-url" -> {
                        if (!(client.gui.screen() instanceof PerdePanelScreen panel)) throw new IllegalStateException("Expected real server-opened panel");
                        EditBox edit = panel.children().stream().filter(EditBox.class::isInstance).map(EditBox.class::cast).findFirst().orElseThrow();
                        edit.setValue(args[2]);
                        Button button = panel.children().stream().filter(Button.class::isInstance).map(Button.class::cast)
                                .filter(candidate -> candidate.getMessage().getString().equals(Component.translatable("perde.ui.open").getString())).findFirst().orElseThrow();
                        if (!button.active) throw new IllegalStateException("Open button is disabled");
                        button.onPress(new MouseButtonInfo(0, 0));
                    }
                    case "geometry" -> ClientPlayNetworking.send(new PerdePayloads.SetGeometry(pos(args), number(args, 5), number(args, 6), number(args, 7), number(args, 8)));
                    case "equip" -> {
                        if (client.gameMode.getPlayerMode() != GameType.CREATIVE) throw new IllegalStateException("Creative only");
                        ItemStack stack = new ItemStack(PerdeRegistry.ITEM);
                        client.player.getInventory().setSelectedSlot(0);
                        client.player.getInventory().setSelectedItem(stack);
                        client.gameMode.handleCreativeModeItemAdd(stack, 36);
                    }
                    case "place" -> {
                        if (!client.player.getMainHandItem().is(PerdeRegistry.ITEM)) throw new IllegalStateException("Equip Perde first");
                        BlockPos support = pos(args).below();
                        client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND,
                                new BlockHitResult(Vec3.atCenterOf(support).add(0, 0.5, 0), Direction.UP, support, false));
                    }
                    case "break" -> {
                        if (client.gameMode.getPlayerMode() != GameType.CREATIVE || client.player.getEyePosition().distanceTo(Vec3.atCenterOf(pos(args))) > 4.5) {
                            throw new IllegalStateException("Creative break requires ordinary reach");
                        }
                        client.gameMode.startDestroyBlock(pos(args), Direction.NORTH);
                    }
                    case "status" -> {
                        var entity = client.level.getBlockEntity(pos(args));
                        LOG.info("Status player={} mode={} position={} block={} state={} ticket={} status={} thread={}",
                                client.player.getPlainTextName(), client.gameMode.getPlayerMode(), client.player.position(), client.level.getBlockState(pos(args)),
                                entity instanceof PerdeBlockEntity screen ? screen.screenState() : null,
                                entity instanceof PerdeBlockEntity screen ? PerdeBrowserManager.INSTANCE.frameTicket(screen) : -1,
                                entity instanceof PerdeBlockEntity screen ? PerdeBrowserManager.INSTANCE.statusKey(screen) : "removed", Thread.currentThread().getName());
                        LOG.info("Native browser audit {}", browserAudit());
                        var overlay = client.gui.hud.getClass().getDeclaredField("overlayMessageString");
                        overlay.setAccessible(true);
                        Object message = overlay.get(client.gui.hud);
                        LOG.info("F2-2 action bar: {}", message instanceof Component text ? text.getString() : message);
                    }
                    default -> throw new IllegalArgumentException("Unknown action: " + action);
                }
            }
            LOG.info("Action {} completed", action);
        } catch (Exception exception) {
            client.options.keyUp.setDown(false);
            walkTicks = 0;
            LOG.error("F2-1 fixture failed", exception);
        }
    }

    private static double number(String[] args, int index) { return Double.parseDouble(args[index]); }

    private static void evaluate(String label, String expression) throws Exception {
        if (!FabricLoader.getInstance().isDevelopmentEnvironment()) throw new IllegalStateException("Development only");
        Object browser = managedBrowser();
        Object devTools = Class.forName("org.cef.browser.CefBrowser").getMethod("getDevToolsClient").invoke(browser);
        if (devTools == null) throw new IllegalStateException("Native browser not ready");
        var parameters = new com.google.gson.JsonObject();
        parameters.addProperty("expression", expression);
        parameters.addProperty("returnByValue", true);
        parameters.addProperty("awaitPromise", true);
        parameters.addProperty("userGesture", true);
        devTools(label, "Runtime.evaluate", parameters.toString());
    }

    private static void devTools(String label, String method, String parameters) throws Exception {
        if (!FabricLoader.getInstance().isDevelopmentEnvironment()) throw new IllegalStateException("Development only");
        Object browser = managedBrowser();
        Object devTools = Class.forName("org.cef.browser.CefBrowser").getMethod("getDevToolsClient").invoke(browser);
        if (devTools == null) throw new IllegalStateException("Native browser not ready");
        javax.swing.SwingUtilities.invokeLater(() -> {
            try {
                var result = (java.util.concurrent.CompletableFuture<?>) devTools.getClass()
                        .getMethod("executeDevToolsMethod", String.class, String.class)
                        .invoke(devTools, method, parameters);
                result.orTimeout(10, java.util.concurrent.TimeUnit.SECONDS).whenComplete((value, failure) -> {
                    if (failure != null) LOG.error("F2-2 native evaluation failed: " + label, failure);
                    else LOG.info("F2-2 native result {}: {}", label, value);
                });
            } catch (Exception exception) { LOG.error("F2-2 native command dispatch failed: " + label, exception); }
        });
    }

    /** F2-2 opt-in probes. Only a managed Perde browser is targeted, never the spike or another mod. */
    private static Object managedBrowser() throws Exception {
        var backendField = PerdeBrowserManager.class.getDeclaredField("backend");
        backendField.setAccessible(true);
        Object backend = backendField.get(PerdeBrowserManager.INSTANCE);
        if (backend == null) throw new IllegalStateException("MCEF backend absent");
        var browserField = backend.getClass().getDeclaredField("browser");
        browserField.setAccessible(true);
        Object wrapper = browserField.get(backend);
        if (wrapper == null) throw new IllegalStateException("No managed Perde browser");
        return Class.forName("net.dimaskama.mcef.api.MCEFBrowser").getMethod("getCefBrowser").invoke(wrapper);
    }

    private static void runPolicyProbe(String name) throws Exception {
        if (!FabricLoader.getInstance().isDevelopmentEnvironment()) throw new IllegalStateException("Development only");
        String script = switch (name) {
            case "file" -> "location.href='file:///C:/';";
            case "popup" -> "window.open('https://example.com');";
            case "loopback" -> "fetch('http://127.0.0.1:1/').then(()=>console.error('F2-2 unexpected fetch success')).catch(e=>console.log('F2-2 fetch denied',e.name));";
            case "iframe" -> "{const f=document.createElement('iframe');f.src='http://192.168.1.1';document.body.append(f);}";
            case "alert" -> "alert('x');confirm('x');prompt('x');";
            case "media" -> "navigator.mediaDevices.getUserMedia({audio:true,video:true}).then(s=>{s.getTracks().forEach(t=>t.stop());console.error('F2-2 unexpected media access');}).catch(e=>console.log('F2-2 media denied',e.name));";
            case "download" -> "{const a=document.createElement('a');a.href=URL.createObjectURL(new Blob(['F2-2']));a.download='perde-policy-test.txt';a.click();}";
            case "data-navigation" -> "location.href='data:text/html,blocked';";
            case "beforeunload" -> "window.onbeforeunload=()=> 'F2-2';location.href='https://example.com/?f2-2=unload';";
            default -> throw new IllegalArgumentException("Unknown policy probe: " + name);
        };
        Object browser = managedBrowser();
        Class<?> type = Class.forName("org.cef.browser.CefBrowser");
        String url = (String) type.getMethod("getURL").invoke(browser);
        if (!io.github.devtalh.perde.screen.UrlPolicy.allowsPage(url)) throw new IllegalStateException("Load a public page first");
        type.getMethod("executeJavaScript", String.class, String.class, int.class).invoke(browser, script, url, 0);
        LOG.info("F2-2 issued probe {} to managed browser on {}. Native outcome still needs audit.", name, url);
    }
    /** Test-only observation of JCEF's own browser registry, verified against the resolved JCEF jar. */
    private static String browserAudit() throws Exception {
        if (!FabricLoader.getInstance().isModLoaded("mcef-modern")) return "MCEF absent";
        Class<?> apiClass = Class.forName("net.dimaskama.mcef.api.MCEFApi");
        var future = (java.util.concurrent.CompletableFuture<?>) apiClass.getMethod("getInstanceFuture").invoke(null);
        if (!future.isDone() || future.isCompletedExceptionally()) return "initializing/failed";
        Object api = future.getNow(null);
        var field = api.getClass().getDeclaredField("client");
        field.setAccessible(true);
        Object cefClient = field.get(api);
        var all = Class.forName("org.cef.CefClient").getDeclaredMethod("getAllBrowser");
        all.setAccessible(true);
        var browsers = new java.util.ArrayList<Object>(java.util.Arrays.asList((Object[]) all.invoke(cefClient)));
        try {
            Object managed = managedBrowser();
            Object managedClient = Class.forName("org.cef.browser.CefBrowser").getMethod("getClient").invoke(managed);
            if (managedClient != cefClient) browsers.addAll(java.util.Arrays.asList((Object[]) all.invoke(managedClient)));
        } catch (IllegalStateException absent) { /* No managed browser is expected outside screen range. */ }
        var browserType = Class.forName("org.cef.browser.CefBrowser");
        var result = new StringBuilder("count=" + browsers.size());
        for (Object browser : browsers) result.append(" [id=").append(browserType.getMethod("getIdentifier").invoke(browser))
                .append(" url=").append(browserType.getMethod("getURL").invoke(browser)).append(']');
        return result.toString();
    }
    private static BlockPos pos(String[] args) { return new BlockPos(Integer.parseInt(args[2]), Integer.parseInt(args[3]), Integer.parseInt(args[4])); }
    private static void aim(Minecraft client, Vec3 target) {
        Vec3 delta = target.subtract(client.player.getEyePosition());
        client.player.setYRot((float) Math.toDegrees(Math.atan2(-delta.x, delta.z)));
        client.player.setXRot((float) -Math.toDegrees(Math.atan2(delta.y, Math.hypot(delta.x, delta.z))));
    }
}
