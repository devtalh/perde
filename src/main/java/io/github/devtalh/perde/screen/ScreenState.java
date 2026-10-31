package io.github.devtalh.perde.screen;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Immutable server state; contains no Minecraft or client dependencies. */
public record ScreenState(String url, boolean playing, long positionMs, long serverTimestamp,
                          double volume, double width, double height, double offsetX, double offsetY,
                          UUID owner, String ownerName, AccessMode accessMode, Map<UUID, String> allowedPlayers) {
    public static final int MAX_URL_LENGTH = 2048;
    public static final int MAX_ALLOWED_PLAYERS = 32;
    public static final int MAX_PLAYER_NAME_LENGTH = 16;
    public static final UUID UNOWNED = new UUID(0, 0);

    public ScreenState {
        url = validateUrl(url);
        volume = clamp(volume, 0, 1);
        width = roundToGrid(width, 1, 16);
        height = roundToGrid(height, 0.5, 9);
        offsetX = roundToGrid(offsetX, -16, 16);
        offsetY = roundToGrid(offsetY, -16, 16);
        Objects.requireNonNull(owner);
        ownerName = boundedPlayerName(ownerName);
        Objects.requireNonNull(accessMode);
        if (allowedPlayers.size() > MAX_ALLOWED_PLAYERS) {
            throw new IllegalArgumentException("Too many allowed players");
        }
        Map<UUID, String> players = new LinkedHashMap<>();
        allowedPlayers.forEach((id, name) -> players.put(Objects.requireNonNull(id), boundedPlayerName(name)));
        allowedPlayers = Collections.unmodifiableMap(players);
    }

    public static String boundedPlayerName(String name) {
        Objects.requireNonNull(name);
        return name.substring(0, Math.min(name.length(), MAX_PLAYER_NAME_LENGTH));
    }

    public static ScreenState defaults() {
        return new ScreenState("", false, 0, 0, 1, 4, 2.25, 0, 0, UNOWNED, "", AccessMode.OWNER_ONLY, Map.of());
    }

    public static String validateUrl(String input) {
        Objects.requireNonNull(input);
        String url = input.trim();
        if (url.length() > MAX_URL_LENGTH) throw new IllegalArgumentException("URL is too long");
        if (url.isEmpty()) return url;
        if (!UrlPolicy.allowsPage(url)) throw new IllegalArgumentException("Only public HTTP(S) URLs are allowed");
        return url;
    }

    /** Invalid legacy URLs are cleared without discarding the rest of the saved screen state. */
    public static String savedUrl(String input) {
        try { return validateUrl(input); }
        catch (IllegalArgumentException exception) { return ""; }
    }

    public static double clamp(double value, double min, double max) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException("Non-finite value");
        return Math.max(min, Math.min(max, value));
    }

    public static double roundToGrid(double value, double min, double max) {
        return Math.round(clamp(value, min, max) * 16) / 16.0;
    }

    /** The UI lock is quantized to the same sixteenth-block grid as server geometry. */
    public static double height16By9(double width) {
        return roundToGrid(roundToGrid(width, 1, 16) * 9 / 16, 0.5, 9);
    }

    public ScreenState withOwner(UUID id, String name) {
        return copy(url, width, height, offsetX, offsetY, id, name, accessMode, allowedPlayers);
    }

    public ScreenState withUrl(String value) {
        return copy(value, width, height, offsetX, offsetY, owner, ownerName, accessMode, allowedPlayers);
    }

    public ScreenState withGeometry(double w, double h, double x, double y) {
        return copy(url, w, h, x, y, owner, ownerName, accessMode, allowedPlayers);
    }

    public ScreenState withAccessMode(AccessMode value) {
        return copy(url, width, height, offsetX, offsetY, owner, ownerName, value, allowedPlayers);
    }

    public ScreenState withAllowedPlayer(UUID id, String name) {
        Map<UUID, String> players = new LinkedHashMap<>(allowedPlayers);
        players.put(id, name);
        return copy(url, width, height, offsetX, offsetY, owner, ownerName, accessMode, players);
    }

    public ScreenState withoutAllowedPlayer(UUID id) {
        Map<UUID, String> players = new LinkedHashMap<>(allowedPlayers);
        players.remove(id);
        return copy(url, width, height, offsetX, offsetY, owner, ownerName, accessMode, players);
    }

    private ScreenState copy(String page, double w, double h, double x, double y, UUID id, String name,
                             AccessMode mode, Map<UUID, String> players) {
        return new ScreenState(page, playing, positionMs, serverTimestamp, volume, w, h, x, y, id, name, mode, players);
    }
}
