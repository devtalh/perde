package io.github.devtalh.perde.screen;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.function.Consumer;

/** Server-owned JSON with fail-closed parsing and atomic replacement, independent of Minecraft. */
public final class SiteAllowlistConfig {
    private SiteAllowlistConfig() { }

    public static SiteAllowlist load(Path file, Consumer<String> warning) throws IOException {
        if (!Files.exists(file)) {
            SiteAllowlist defaults = SiteAllowlist.defaults();
            save(file, defaults);
            return defaults;
        }
        var domains = new ArrayList<String>();
        try (var reader = Files.newBufferedReader(file)) {
            var root = JsonParser.parseReader(reader);
            if (!root.isJsonObject() || !root.getAsJsonObject().has("allowedDomains")
                    || !root.getAsJsonObject().get("allowedDomains").isJsonArray()) {
                warning.accept("Expected an allowedDomains array; allowing no sites");
                return SiteAllowlist.empty();
            }
            for (var entry : root.getAsJsonObject().getAsJsonArray("allowedDomains")) {
                if (domains.size() >= SiteAllowlist.MAX_DOMAINS) {
                    warning.accept("Too many allowed domains; ignoring remaining entries");
                    break;
                }
                try {
                    if (!entry.isJsonPrimitive() || !entry.getAsJsonPrimitive().isString()) throw new IllegalArgumentException();
                    domains.add(SiteAllowlist.normalize(entry.getAsString()));
                } catch (IllegalArgumentException exception) {
                    warning.accept("Ignoring invalid allowed-domain entry");
                }
            }
        } catch (com.google.gson.JsonParseException | IllegalStateException exception) {
            warning.accept("Invalid allowlist JSON; allowing no sites");
            return SiteAllowlist.empty();
        }
        return new SiteAllowlist(domains);
    }

    public static void save(Path file, SiteAllowlist policy) throws IOException {
        Path target = file.toAbsolutePath();
        Files.createDirectories(target.getParent());
        Path temporary = Files.createTempFile(target.getParent(), "perde-server-", ".tmp");
        try {
            JsonObject root = new JsonObject();
            JsonArray domains = new JsonArray();
            policy.domains().forEach(domains::add);
            root.add("allowedDomains", domains);
            Files.writeString(temporary, new Gson().toJson(root) + System.lineSeparator());
            // No non-atomic fallback: a failed write must leave the previous policy intact.
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(temporary); }
    }
}
