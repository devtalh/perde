package io.github.devtalh.perde.screen;

import com.mojang.brigadier.arguments.StringArgumentType;
import io.github.devtalh.perde.Perde;
import io.github.devtalh.perde.net.PerdePayloads;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

import java.io.IOException;
import java.nio.file.Path;
import java.util.IdentityHashMap;
import java.util.Map;

/** All mutable state is server-thread owned, independent for each integrated/dedicated server. */
public final class PerdeServerAllowlist {
    private static final Map<MinecraftServer, PerdeServerAllowlist> SERVERS = new IdentityHashMap<>();
    private final Path file;
    private SiteAllowlist policy;
    private final ApprovalNoticeLimiter notices = new ApprovalNoticeLimiter();

    private PerdeServerAllowlist(Path file) {
        this.file = file;
        try { policy = SiteAllowlistConfig.load(file, warning -> Perde.LOGGER.warn("Perde allowlist: {}", warning)); }
        catch (IOException exception) {
            policy = SiteAllowlist.empty();
            Perde.LOGGER.error("Cannot load Perde server allowlist; allowing no sites.", exception);
        }
    }

    public static void initialize() {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> SERVERS.put(server,
                new PerdeServerAllowlist(FabricLoader.getInstance().getConfigDir().resolve("perde-server.json"))));
        ServerLifecycleEvents.SERVER_STOPPED.register(SERVERS::remove);
        ServerPlayConnectionEvents.JOIN.register((listener, sender, server) -> send(server, listener.player));
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            var wl = Commands.literal("wl").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                    .then(Commands.literal("list").executes(context -> list(context.getSource())))
                    .then(Commands.literal("add").then(Commands.argument("domain", StringArgumentType.greedyString())
                            .executes(context -> change(context.getSource(), StringArgumentType.getString(context, "domain"), true))))
                    .then(Commands.literal("remove").then(Commands.argument("domain", StringArgumentType.greedyString())
                            .executes(context -> change(context.getSource(), StringArgumentType.getString(context, "domain"), false))))
                    .build();
            var root = dispatcher.register(Commands.literal("perde"));
            root.addChild(wl);
            root.addChild(Commands.literal("whitelist").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).redirect(wl).build());
        });
    }

    public static SiteAllowlist policy(MinecraftServer server) {
        PerdeServerAllowlist service = SERVERS.get(server);
        return service == null ? SiteAllowlist.empty() : service.policy;
    }

    private static void send(MinecraftServer server, ServerPlayer player) {
        ServerPlayNetworking.send(player, new PerdePayloads.AllowedDomains(policy(server).domains()));
    }

    private static int list(CommandSourceStack source) {
        var domains = policy(source.getServer()).domains();
        source.sendSuccess(() -> Component.translatableWithFallback("perde.wl.list", "Allowed sites (%s): %s", domains.size(), String.join(", ", domains)), false);
        return domains.size();
    }

    private static int change(CommandSourceStack source, String input, boolean add) {
        PerdeServerAllowlist service = SERVERS.get(source.getServer());
        if (service == null) { source.sendFailure(Component.translatableWithFallback("perde.wl.unavailable", "The server site policy is not available yet.")); return 0; }
        String domain;
        SiteAllowlist next;
        try {
            domain = SiteAllowlist.normalize(input);
            next = add ? service.policy.adding(domain) : service.policy.removing(domain);
        } catch (IllegalArgumentException exception) {
            source.sendFailure(Component.translatableWithFallback("perde.wl.invalid_domain", "Enter a public domain or HTTP(S) URL, not an IP/local name (maximum 4096 sites)."));
            return 0;
        }
        if (next.equals(service.policy)) {
            source.sendFailure(Component.translatableWithFallback(add ? "perde.wl.already_allowed" : "perde.wl.not_present",
                    add ? "%s is already on the allowlist." : "%s is not on the allowlist.", domain));
            return 0;
        }
        try { SiteAllowlistConfig.save(service.file, next); }
        catch (IOException exception) {
            Perde.LOGGER.error("Cannot save Perde allowlist; retaining the previous policy.", exception);
            source.sendFailure(Component.translatableWithFallback("perde.wl.save_failed", "Could not save the site policy. The previous allowlist is still active."));
            return 0;
        }
        service.policy = next;
        for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) send(source.getServer(), player);
        source.sendSuccess(() -> Component.translatableWithFallback(add ? "perde.wl.added" : "perde.wl.removed",
                add ? "Allowed site: %s" : "Removed allowed site: %s", domain), true);
        Perde.LOGGER.info("Perde allowlist {}: {} ({} domains).", add ? "added" : "removed", domain, next.domains().size());
        return 1;
    }

    public static void requestApproval(MinecraftServer server, ServerPlayer requester, String url) {
        PerdeServerAllowlist service = SERVERS.get(server);
        if (service == null) return;
        String domain = SiteAllowlist.normalize(url);
        if (!service.notices.tryAcquire(domain, System.nanoTime() / 1_000_000)) return;
        Component allow = Component.translatable("perde.wl.allow").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)
                .withStyle(style -> style.withClickEvent(new ClickEvent.RunCommand("/perde wl add " + domain)));
        Component message = Component.translatable("perde.wl.request", requester.getDisplayName(), domain).append(" ").append(allow);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) player.sendSystemMessage(message);
        }
    }
}
