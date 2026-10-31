package io.github.devtalh.perde.net;

import io.github.devtalh.perde.screen.AccessMode;
import io.github.devtalh.perde.screen.ScreenPermissions;
import io.github.devtalh.perde.screen.ScreenState;
import java.util.UUID;

/** Pure authorization/validation decisions shared by the live handlers and unit tests. */
public final class ControlDecisions {
    private ControlDecisions() { }

    public record Context(UUID player, int permissionLevel, boolean sameLevel, boolean chunkLoaded,
                          boolean screenExists, double distanceSquared) { }

    public enum Rejection {
        WRONG_LEVEL, UNLOADED, MISSING, TOO_FAR, FORBIDDEN, INVALID_URL, INVALID_VALUE, PLAYER_OFFLINE, PLAYER_LIMIT, INVALID_NAME
    }

    public sealed interface Change {
        default boolean adminOnly() { return this instanceof SetAccess || this instanceof AddPlayer || this instanceof RemovePlayer; }
    }
    public record SetUrl(String url) implements Change { }
    public record SetGeometry(double width, double height, double offsetX, double offsetY) implements Change { }
    public record Center() implements Change { }
    public record SetAccess(String mode) implements Change { }
    public record AddPlayer(UUID uuid, String name) implements Change { }
    public record RemovePlayer(UUID uuid) implements Change { }
    public record Result(ScreenState state, Rejection rejection, boolean rateLimited) {
        public boolean accepted() { return rejection == null && !rateLimited; }
    }

    public static Rejection authorize(Context context, ScreenState state, boolean adminOnly) {
        if (!context.sameLevel()) return Rejection.WRONG_LEVEL;
        if (!context.chunkLoaded()) return Rejection.UNLOADED;
        if (!context.screenExists() || state == null) return Rejection.MISSING;
        if (!Double.isFinite(context.distanceSquared()) || context.distanceSquared() > 64) return Rejection.TOO_FAR;
        boolean allowed = adminOnly ? ScreenPermissions.canAdmin(state, context.player(), context.permissionLevel())
                : ScreenPermissions.canControl(state, context.player(), context.permissionLevel());
        return allowed ? null : Rejection.FORBIDDEN;
    }

    public static Result decide(Context context, ScreenState state, Change change, PacketRateLimiter limiter, long nowMs) {
        Rejection rejection = authorize(context, state, change.adminOnly());
        if (rejection != null) return new Result(state, rejection, false);
        if (change instanceof AddPlayer request) {
            if (!validPlayerName(request.name())) return new Result(state, Rejection.INVALID_NAME, false);
            if (request.uuid() == null) return new Result(state, Rejection.PLAYER_OFFLINE, false);
            if (state.allowedPlayers().size() >= ScreenState.MAX_ALLOWED_PLAYERS && !state.allowedPlayers().containsKey(request.uuid())) {
                return new Result(state, Rejection.PLAYER_LIMIT, false);
            }
        }
        ScreenState next;
        try {
            next = switch (change) {
                case SetUrl request -> state.withUrl(request.url());
                case SetGeometry request -> state.withGeometry(request.width(), request.height(), request.offsetX(), request.offsetY());
                case Center ignored -> state.withGeometry(state.width(), state.height(), 0, 0);
                case SetAccess request -> state.withAccessMode(AccessMode.valueOf(request.mode()));
                case AddPlayer request -> state.withAllowedPlayer(request.uuid(), request.name());
                case RemovePlayer request -> state.withoutAllowedPlayer(request.uuid());
            };
        } catch (IllegalArgumentException exception) {
            return new Result(state, change instanceof SetUrl ? Rejection.INVALID_URL : Rejection.INVALID_VALUE, false);
        }
        return limiter.tryAccept(context.player(), nowMs) ? new Result(next, null, false) : new Result(state, null, true);
    }

    public static boolean validPlayerName(String name) { return name != null && name.matches("[A-Za-z0-9_]{1,16}"); }
}
