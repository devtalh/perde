package io.github.devtalh.perde.screen;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class SiteAllowlistConfigTest {
    @TempDir Path directory;

    @Test void createsDefaultsAndRoundTripsAtomicChanges() throws Exception {
        Path file = directory.resolve("config/perde-server.json");
        assertEquals(SiteAllowlist.defaults(), SiteAllowlistConfig.load(file, message -> fail(message)));
        assertTrue(Files.readString(file).contains("allowedDomains"));
        var next = SiteAllowlist.defaults().adding("https://www.example.com/path").removing("kick.com");
        SiteAllowlistConfig.save(file, next);
        assertEquals(next, SiteAllowlistConfig.load(file, message -> fail(message)));
        try (var files = Files.list(file.getParent())) { assertEquals(List.of(file), files.toList()); }
    }

    @Test void ignoresInvalidEntriesWithWarningsAndDoesNotBroadenPolicy() throws Exception {
        Path file = directory.resolve("perde-server.json");
        Files.writeString(file, "{\"allowedDomains\":[\"WWW.YOUTUBE.COM.\",\"https://example.com/a\",\"localhost\",\"8.8.8.8\",5,null,\"youtube.com\"]}");
        var warnings = new ArrayList<String>();
        var policy = SiteAllowlistConfig.load(file, warnings::add);
        assertEquals(List.of("youtube.com", "example.com"), policy.domains());
        assertEquals(4, warnings.size());
        SiteAllowlistConfig.save(file, policy);
        assertEquals(policy, SiteAllowlistConfig.load(file, message -> fail(message)));
    }

    @Test void corruptOrMissingPropertyFailsClosedWithoutReplacingExistingFile() throws Exception {
        Path file = directory.resolve("perde-server.json");
        for (String input : List.of("{broken", "{}", "{\"allowedDomains\":true}", "null")) {
            Files.writeString(file, input);
            var warnings = new ArrayList<String>();
            assertEquals(SiteAllowlist.empty(), SiteAllowlistConfig.load(file, warnings::add));
            assertFalse(warnings.isEmpty());
            assertEquals(input, Files.readString(file));
        }
    }

    @Test void failedReplacementDoesNotDeleteExistingTargetOrLeaveTempFiles() throws Exception {
        Path target = Files.createDirectory(directory.resolve("perde-server.json"));
        Path sentinel = Files.writeString(target.resolve("sentinel"), "retain");
        assertThrows(java.io.IOException.class, () -> SiteAllowlistConfig.save(target, SiteAllowlist.defaults()));
        assertEquals("retain", Files.readString(sentinel));
        try (var files = Files.list(directory)) { assertEquals(List.of(target), files.toList()); }
    }
}
